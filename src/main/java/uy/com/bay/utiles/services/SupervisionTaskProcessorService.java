package uy.com.bay.utiles.services;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.jaudiotagger.audio.AudioFileIO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.FileCopyUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import uy.com.bay.utiles.data.Status;
import uy.com.bay.utiles.data.SupervisionTask;
import uy.com.bay.utiles.data.repository.SupervisionTaskRepository;
import uy.com.bay.utiles.dto.AudioFile;

@Service
public class SupervisionTaskProcessorService {

    private static final Logger logger = LoggerFactory.getLogger(SupervisionTaskProcessorService.class);

    private final SupervisionTaskRepository supervisionTaskRepository;
    private final OpenAiService openAiService;
    private final ChatClient chatClient;

    private final String basePrompt;

    public SupervisionTaskProcessorService(SupervisionTaskRepository supervisionTaskRepository,
                                          OpenAiService openAiService,
                                          ChatClient.Builder chatClientBuilder) {
        this.supervisionTaskRepository = supervisionTaskRepository;
        this.openAiService = openAiService;
        this.chatClient = chatClientBuilder.build();

        String loadedPrompt = "";
        try (InputStream inputStream = getClass().getResourceAsStream("/prompts/supervision2.txt")) {
            if (inputStream != null) {
                byte[] byteArray = FileCopyUtils.copyToByteArray(inputStream);
                loadedPrompt = new String(byteArray, StandardCharsets.UTF_8);
            } else {
                logger.error("Prompt file not found: /prompts/supervision2.txt");
            }
        } catch (IOException e) {
            logger.error("Error loading prompt /prompts/supervision2.txt", e);
        }
        this.basePrompt = loadedPrompt;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processSingleTask(Long taskId) {
        SupervisionTask task = supervisionTaskRepository.findById(taskId).orElse(null);
        if (task == null) {
            logger.warn("SupervisionTask not found id={}", taskId);
            return;
        }

        logger.info("Processing Supervision Task: {}", task.getId());

        try {
            // (Opcional pero útil) marcar RUNNING y persistir rápido
            task.setStatus(Status.RUNNING);
            supervisionTaskRepository.save(task);
            supervisionTaskRepository.flush();

            AudioFile audioFile = new AudioFile(task.getFileName(),
                    new ByteArrayInputStream(task.getAudioContent()));

            // Duración
            File tempFile = null;
            try {
                tempFile = File.createTempFile("audio_", "_" + task.getFileName());
                try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                    fos.write(task.getAudioContent());
                }
                org.jaudiotagger.audio.AudioFile taggedFile = AudioFileIO.read(tempFile);
                int duration = taggedFile.getAudioHeader().getTrackLength();
                task.setTotalAudioDuration((double) duration);
            } catch (Exception e) {
                logger.error("Error calculating audio duration for task {}: {}", task.getId(), e.getMessage(), e);
            } finally {
                if (tempFile != null) {
                    //noinspection ResultOfMethodCallIgnored
                    tempFile.delete();
                }
            }

            // Transcripción
            String transcription = openAiService.transcribeAudio(audioFile);
            task.setOutput(prettyPrint(transcription));

            // Segmentos diarize
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(transcription);
            JsonNode segments = root.path("segments");
            if (segments.isArray()) {
                double totalDuration = 0d;
                for (JsonNode segment : segments) {
                    if (segment.has("speaker") && segment.has("start") && segment.has("end")) {
                        String speaker = segment.get("speaker").asText();
                        double start = segment.get("start").asDouble();
                        double end = segment.get("end").asDouble();
                        totalDuration += (end - start);
                        task.getDurationBySpeakers().merge(speaker, (end - start), Double::sum);
                    }
                }
                task.setSpeakingDuration((int) totalDuration);
            }

            // Questionnaire
            String questionnaireString = "";
            if (task.getQuestionnaire() != null) {
                try (InputStream is = new ByteArrayInputStream(task.getQuestionnaire());
                     XWPFDocument doc = new XWPFDocument(is);
                     XWPFWordExtractor extractor = new XWPFWordExtractor(doc)) {
                    questionnaireString = extractor.getText();
                } catch (Exception e) {
                    logger.error("Error extracting text from questionnaire for task {}", task.getId(), e);
                }
            }

            // Evaluación
            String formattedPrompt = basePrompt.formatted(questionnaireString, transcription);
            String response = chatClient.prompt()
                    .user(formattedPrompt)
                    .call()
                    .content()
                    .replace("```json", "")
                    .replace("```", "");

            task.setEvaluationOutput(prettyPrint(response));

            JsonNode rootNode = new ObjectMapper().readTree(task.getEvaluationOutput());
            int puntajeGlobal = rootNode.path("puntaje_global").asInt();
            task.setAiScore(puntajeGlobal);

            task.setStatus(Status.DONE);
            task.setProcessed(new Date());

        } catch (Exception e) {
            logger.error("Error processing supervision task {}", task.getId(), e);
            task.setStatus(Status.ERROR);
        } finally {
            // Persist final
            supervisionTaskRepository.save(task);
            supervisionTaskRepository.flush();
        }
    }

    public String prettyPrint(String json) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        Object jsonObject = mapper.readValue(json, Object.class);
        ObjectWriter writer = mapper.writerWithDefaultPrettyPrinter();
        return writer.writeValueAsString(jsonObject);
    }
}
