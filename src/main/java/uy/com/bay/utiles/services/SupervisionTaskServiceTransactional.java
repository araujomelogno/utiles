package uy.com.bay.utiles.services;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Date;
import java.util.List;

import org.jaudiotagger.audio.AudioFileIO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import uy.com.bay.utiles.data.Status;
import uy.com.bay.utiles.data.SupervisionTask;
import uy.com.bay.utiles.data.repository.SupervisionTaskRepository;
import uy.com.bay.utiles.dto.AudioFile;

@Service
public class SupervisionTaskServiceTransactional {

	private static final Logger logger = LoggerFactory.getLogger(SupervisionTaskServiceTransactional.class);
	private final SupervisionTaskRepository supervisionTaskRepository;
	private final OpenAiService openAiService;

	public SupervisionTaskServiceTransactional(SupervisionTaskRepository supervisionTaskRepository,
			OpenAiService openAiService) {
		this.supervisionTaskRepository = supervisionTaskRepository;
		this.openAiService = openAiService;
	}

	@Transactional
	public void processPendingTasks() {
		List<SupervisionTask> pendingTasks = supervisionTaskRepository.findByStatus(Status.PENDING);
		for (SupervisionTask task : pendingTasks) {
			try {
				AudioFile audioFile = new AudioFile(task.getFileName(),
						new ByteArrayInputStream(task.getAudioContent()));

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
					logger.error("Error calculating audio duration for task {}: {}", task.getId(), e.getMessage());
				} finally {
					if (tempFile != null) {
						tempFile.delete();
					}
				}

				String transcription = openAiService.transcribeAudio(audioFile);
				task.setOutput(prettyPrint(transcription));
				task.setStatus(Status.DONE);
				task.setProcessed(new Date());

				ObjectMapper mapper = new ObjectMapper();
				JsonNode root = mapper.readTree(transcription);
				JsonNode segments = root.path("segments");
				if (segments.isArray()) {
					double totalDuration= 0d;
					for (JsonNode segment : segments) {
						if (segment.has("speaker") && segment.has("start") && segment.has("end")) {
							String speaker = segment.get("speaker").asText();
							double start = segment.get("start").asDouble();
							double end = segment.get("end").asDouble();
							totalDuration += end-start;
							task.getDurationBySpeakers().merge(speaker, end - start, Double::sum);
						}
					}
					task.setTotalAudioDuration(totalDuration);
				}
			} catch (Exception e) {
				logger.error("Error processing supervision task {}: {}", task.getId(), e.getMessage());
				task.setStatus(Status.ERROR);
			} finally {
				supervisionTaskRepository.save(task);
			}
		}
	}

	public String prettyPrint(String json) throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		Object jsonObject = mapper.readValue(json, Object.class);
		ObjectWriter writer = mapper.writerWithDefaultPrettyPrinter();
		return writer.writeValueAsString(jsonObject);
	}
}
