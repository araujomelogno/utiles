package uy.com.bay.utiles.services;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Date;

import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.openxml4j.opc.OPCPackage;
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
			OpenAiService openAiService, ChatClient.Builder chatClientBuilder) {
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

			AudioFile audioFile = new AudioFile(task.getFileName(), new ByteArrayInputStream(task.getAudioContent()));

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
					// noinspection ResultOfMethodCallIgnored
					tempFile.delete();
				}
			}

			// Transcripción
			String transcription = openAiService.transcribeAudioTotal(audioFile);
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

			String questionnaireString = extractQuestionnaireText(task.getQuestionnaire(),
					task.getQuestionnaireFileName());

			// Evaluación
			String formattedPrompt = basePrompt.formatted(questionnaireString, transcription);
			task.setFullPrompt(formattedPrompt);

			String response = chatClient.prompt().user(formattedPrompt).call().content().replace("```json", "")
					.replace("```", "");

			task.setEvaluationOutput(prettyPrint(response));

			JsonNode evaluationRoot = new ObjectMapper().readTree(task.getEvaluationOutput());
			JsonNode porItem = evaluationRoot.path("por_item");
			if (porItem.isArray()) {
				task.getCoincidenceByItem().clear();
				task.getScoreByItem().clear();
				for (JsonNode item : porItem) {
					String itemId = item.path("item_id").asText();
					if (itemId == null || itemId.isEmpty()) {
						continue;
					}
					task.getCoincidenceByItem().put(itemId, item.path("calidad_coincidencia").asText());
					task.getScoreByItem().put(itemId, item.path("puntaje_item").asInt());
				}
			}

//			{
//				  "puntaje_global" : 47,
//				  "puntajes_componentes" : {
//				    "cobertura" : 50,
//				    "fidelidad" : 45,
//				    "neutralidad" : 60,
//				    "fluidez_operacional" : 40
//				  },
//				  "resumen" : {
//				    "total_items_esperados" : 26,
//				    "items_encontrados" : 13,
//				    "items_faltantes" : 13,
//				    "problemas_mayores" : [ "omision", "cambio_significado", "cambio_opciones" ],
//				    "problemas_menores" : [ "parcial", "direccionamiento", "texto_agregado", "problema_orden" ]
//			}

			JsonNode rootNode = new ObjectMapper().readTree(task.getEvaluationOutput());
			int puntajeGlobal = rootNode.path("puntaje_global").asInt();

			int cobertura = rootNode.path("puntajes_componentes").path("cobertura").asInt();
			int fidelidad = rootNode.path("puntajes_componentes").path("fidelidad").asInt();
			int neutralidad = rootNode.path("puntajes_componentes").path("neutralidad").asInt();
			int fluidez_operacional = rootNode.path("puntajes_componentes").path("fluidez_operacional").asInt();

			int total_items_esperados = rootNode.path("resumen").path("total_items_esperados").asInt();
			int items_encontrados = rootNode.path("resumen").path("items_encontrados").asInt();
			int items_faltantes = rootNode.path("resumen").path("items_faltantes").asInt();
			String problemas_mayores = rootNode.path("resumen").path("problemas_mayores").toString();
			String problemas_menores = rootNode.path("resumen").path("problemas_menores").toString();

			task.setAiScore(puntajeGlobal);
			task.setScoreCobertura(cobertura);
			task.setScoreFidelidad(fidelidad);
			task.setScoreNeutralidad(neutralidad);
			task.setScoreFluidez(fluidez_operacional);
			task.setItemsEsperados(total_items_esperados);
			task.setItemsEncontrados(items_encontrados);
			task.setItemsFaltantes(items_faltantes);
			task.setProblemasMayores(problemas_mayores);
			task.setProblemasMenores(problemas_menores);
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

	public static String extractQuestionnaireText(byte[] data, String fileName) {
		if (data == null || data.length == 0)
			return "";

		String lowerName = fileName != null ? fileName.toLowerCase() : "";

		// DOCX (ZIP) empieza con 'PK'; también se detecta por extensión como fallback
		boolean looksZip = data.length >= 2 && data[0] == 'P' && data[1] == 'K';
		boolean nameDocx = lowerName.endsWith(".docx");

		// DOC (Word 97-2003) empieza con D0 CF 11 E0 A1 B1 1A E1 (OLE2); también por
		// extensión
		byte[] oleSig = new byte[] { (byte) 0xD0, (byte) 0xCF, (byte) 0x11, (byte) 0xE0, (byte) 0xA1, (byte) 0xB1,
				(byte) 0x1A, (byte) 0xE1 };
		boolean looksOle = data.length >= 8 && Arrays.equals(Arrays.copyOfRange(data, 0, 8), oleSig);
		boolean nameDoc = lowerName.endsWith(".doc");

		// PDF empieza con %PDF
		boolean looksPdf = data.length >= 4 && data[0] == '%' && data[1] == 'P' && data[2] == 'D' && data[3] == 'F';

		logger.debug(
				"Extracting questionnaire text: file='{}', looksZip={}, nameDocx={}, looksOle={}, nameDoc={}, looksPdf={}, bytes={}",
				fileName, looksZip, nameDocx, looksOle, nameDoc, looksPdf, data.length);

		if (looksZip || nameDocx) {
			try (InputStream is = new ByteArrayInputStream(data);
					XWPFDocument doc = new XWPFDocument(OPCPackage.open(is));
					XWPFWordExtractor extractor = new XWPFWordExtractor(doc)) {
				return extractor.getText();
			} catch (Exception e) {
				logger.warn("No se pudo extraer texto DOCX de '{}': {}”, fileName, e.getMessage()");
			}
		}

		if (looksOle || nameDoc) {
			try (InputStream is = new ByteArrayInputStream(data);
					HWPFDocument doc = new HWPFDocument(is);
					WordExtractor extractor = new WordExtractor(doc)) {
				return extractor.getText();
			} catch (Exception e) {
				logger.warn("No se pudo extraer texto DOC de '{}': {}”, fileName, e.getMessage()");
			}
		}

		if (looksPdf) {
			return "";
		}

		// Fallback: asumir texto plano UTF-8
		return new String(data, StandardCharsets.UTF_8);
	}

	public String prettyPrint(String json) throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		Object jsonObject = mapper.readValue(json, Object.class);
		ObjectWriter writer = mapper.writerWithDefaultPrettyPrinter();
		return writer.writeValueAsString(jsonObject);
	}
}
