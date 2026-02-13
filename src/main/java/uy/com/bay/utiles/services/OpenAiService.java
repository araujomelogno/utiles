package uy.com.bay.utiles.services;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.jaudiotagger.audio.AudioFileIO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import uy.com.bay.utiles.dto.AudioFile;

@Service
public class OpenAiService {

	private static final Logger logger = LoggerFactory.getLogger(OpenAiService.class);

	@Value("${spring.ai.openai.api-key}")
	private String openaiApiKey;

	private static final String OPENAI_API_URL = "https://api.openai.com/v1/audio/transcriptions";

	private final ObjectMapper objectMapper;

	@Autowired
	public OpenAiService(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	private RestTemplate buildRestTemplate() {
		HttpComponentsClientHttpRequestFactory rf = new HttpComponentsClientHttpRequestFactory();
		rf.setConnectTimeout((int) Duration.ofSeconds(10).toMillis());
		rf.setConnectionRequestTimeout((int) Duration.ofSeconds(10).toMillis());
		rf.setReadTimeout((int) Duration.ofSeconds(1200).toMillis()); // ajustá según duración del audio
		return new RestTemplate(rf);
	}

	protected double getDuration(File file) {
		try {
			org.jaudiotagger.audio.AudioFile audioFile = AudioFileIO.read(file);
			return audioFile.getAudioHeader().getTrackLength();
		} catch (Exception e) {
			logger.warn("Could not determine duration using jaudiotagger: {}", e.getMessage());
			return 0;
		}
	}

	protected String callApi(byte[] bytes, String filename) {
		RestTemplate restTemplate = buildRestTemplate();

		HttpHeaders headers = new HttpHeaders();
		headers.setBearerAuth(openaiApiKey);
		headers.setContentType(MediaType.MULTIPART_FORM_DATA);
		headers.setAccept(List.of(MediaType.APPLICATION_JSON));

		// Resource con filename + length real
		ByteArrayResource filePart = new ByteArrayResource(bytes) {
			@Override
			public String getFilename() {
				return filename;
			}
		};

		MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
		body.add("file", filePart);
		body.add("model", "gpt-4o-transcribe-diarize");
		body.add("response_format", "diarized_json");
		body.add("chunking_strategy", "auto");

		HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

		ResponseEntity<String> response = restTemplate.postForEntity(OPENAI_API_URL, requestEntity, String.class);

		return response.getBody();
	}

	public String transcribeAudioTotal(AudioFile audioFile) throws Exception {
		byte[] bytes = audioFile.getInputStream().readAllBytes();

		File tempFile = null;
		try {
			String filename = audioFile.getFilename();
			String suffix = ".tmp";
			if (filename != null && filename.contains(".")) {
				suffix = filename.substring(filename.lastIndexOf("."));
			}
			tempFile = File.createTempFile("openai_audio_", suffix);
			try (FileOutputStream fos = new FileOutputStream(tempFile)) {
				fos.write(bytes);
			}

			double duration = getDuration(tempFile);

			if (duration <= 1200) {
				return callApi(bytes, filename);
			} else {
				// Try robust WAV splitting first
				try {
					return processChunksWav(tempFile);
				} catch (UnsupportedAudioFileException | IllegalArgumentException | java.io.IOException e) {
					logger.warn("WAV splitting failed (unsupported format or IO error?), falling back to byte splitting: {}", e.getMessage());
					return processChunksBytes(bytes, filename, duration);
				}
			}
		} finally {
			if (tempFile != null && tempFile.exists()) {
				// noinspection ResultOfMethodCallIgnored
				tempFile.delete();
			}
		}
	}

	private String processChunksWav(File inputFile) throws Exception {
		// Prepare merge containers
		ObjectNode mergedResponse = objectMapper.createObjectNode();
		StringBuilder combinedText = new StringBuilder();
		ArrayNode combinedSegments = objectMapper.createArrayNode();
		String task = null;
		String language = null;
		double currentOffset = 0.0;
		double totalProcessedDuration = 0.0;

		try (AudioInputStream in = AudioSystem.getAudioInputStream(inputFile)) {
			AudioFormat baseFormat = in.getFormat();
			// Decode to PCM
			AudioFormat decodedFormat = new AudioFormat(
					AudioFormat.Encoding.PCM_SIGNED,
					baseFormat.getSampleRate(),
					16,
					baseFormat.getChannels(),
					baseFormat.getChannels() * 2,
					baseFormat.getSampleRate(),
					false);

			try (AudioInputStream din = AudioSystem.getAudioInputStream(decodedFormat, in)) {
				// Chunk size: 20MB (approx 2 mins)
				long bytesPerChunk = 20 * 1024 * 1024;
				int frameSize = decodedFormat.getFrameSize();
				if (frameSize <= 0) frameSize = 4;
				// Align to frame size
				bytesPerChunk = (bytesPerChunk / frameSize) * frameSize;

				byte[] buffer = new byte[(int) bytesPerChunk];
				int chunkIndex = 0;

				while (true) {
					int totalRead = 0;
					while (totalRead < buffer.length) {
						int read = din.read(buffer, totalRead, buffer.length - totalRead);
						if (read == -1) break;
						totalRead += read;
					}
					if (totalRead == 0) break;

					File chunkFile = File.createTempFile("openai_chunk_" + chunkIndex, ".wav");
					try {
						ByteArrayInputStream bais = new ByteArrayInputStream(buffer, 0, totalRead);
						AudioInputStream chunkStream = new AudioInputStream(bais, decodedFormat, totalRead / frameSize);
						AudioSystem.write(chunkStream, AudioFileFormat.Type.WAVE, chunkFile);

						byte[] chunkBytes = java.nio.file.Files.readAllBytes(chunkFile.toPath());
						String responseJson = callApi(chunkBytes, chunkFile.getName());

						JsonNode responseNode = objectMapper.readTree(responseJson);

						// Merge logic
						if (chunkIndex == 0) {
							if (responseNode.has("task")) task = responseNode.get("task").asText();
							if (responseNode.has("language")) language = responseNode.get("language").asText();
						}

						if (responseNode.has("text")) {
							if (combinedText.length() > 0) combinedText.append(" ");
							combinedText.append(responseNode.get("text").asText());
						}

						if (responseNode.has("segments")) {
							JsonNode segments = responseNode.get("segments");
							if (segments.isArray()) {
								for (JsonNode segment : segments) {
									if (segment.isObject()) {
										ObjectNode segmentObj = (ObjectNode) segment;
										if (segmentObj.has("start")) {
											segmentObj.put("start", segmentObj.get("start").asDouble() + currentOffset);
										}
										if (segmentObj.has("end")) {
											segmentObj.put("end", segmentObj.get("end").asDouble() + currentOffset);
										}
										combinedSegments.add(segmentObj);
									}
								}
							}
						}

						if (responseNode.has("duration")) {
							double duration = responseNode.get("duration").asDouble();
							currentOffset += duration;
							totalProcessedDuration += duration;
						} else {
							double approxDuration = (double) totalRead / (decodedFormat.getFrameRate() * frameSize);
							currentOffset += approxDuration;
						}

					} finally {
						chunkFile.delete();
					}
					chunkIndex++;
				}
			}
		}

		if (task != null) mergedResponse.put("task", task);
		if (language != null) mergedResponse.put("language", language);
		mergedResponse.put("duration", totalProcessedDuration);
		mergedResponse.put("text", combinedText.toString());
		mergedResponse.set("segments", combinedSegments);

		return objectMapper.writeValueAsString(mergedResponse);
	}

	private String processChunksBytes(byte[] bytes, String filename, double totalDuration) throws Exception {
		// Fallback implementation: split by bytes (risky but necessary for unsupported formats without ffmpeg)
		long totalBytes = bytes.length;
		double chunkDurationSeconds = 1000.0;
		long bytesPerChunk = (long) (totalBytes * (chunkDurationSeconds / totalDuration));
		if (bytesPerChunk < 1024) bytesPerChunk = totalBytes;

		int numberOfChunks = (int) Math.ceil((double) totalBytes / bytesPerChunk);

		ObjectNode mergedResponse = objectMapper.createObjectNode();
		StringBuilder combinedText = new StringBuilder();
		ArrayNode combinedSegments = objectMapper.createArrayNode();
		String task = null;
		String language = null;
		double currentOffset = 0.0;
		double totalProcessedDuration = 0.0;

		for (int i = 0; i < numberOfChunks; i++) {
			int start = (int) (i * bytesPerChunk);
			int end = (int) Math.min((i + 1) * bytesPerChunk, totalBytes);
			if (start >= end) break;

			byte[] chunkBytes = Arrays.copyOfRange(bytes, start, end);
			String chunkFilename = "chunk_" + i + "_" + filename;

			String responseJson = callApi(chunkBytes, chunkFilename);
			JsonNode responseNode = objectMapper.readTree(responseJson);

			if (i == 0) {
				if (responseNode.has("task")) task = responseNode.get("task").asText();
				if (responseNode.has("language")) language = responseNode.get("language").asText();
			}

			if (responseNode.has("text")) {
				if (combinedText.length() > 0) combinedText.append(" ");
				combinedText.append(responseNode.get("text").asText());
			}

			if (responseNode.has("segments")) {
				JsonNode segments = responseNode.get("segments");
				if (segments.isArray()) {
					for (JsonNode segment : segments) {
						if (segment.isObject()) {
							ObjectNode segmentObj = (ObjectNode) segment;
							if (segmentObj.has("start")) {
								segmentObj.put("start", segmentObj.get("start").asDouble() + currentOffset);
							}
							if (segmentObj.has("end")) {
								segmentObj.put("end", segmentObj.get("end").asDouble() + currentOffset);
							}
							combinedSegments.add(segmentObj);
						}
					}
				}
			}

			if (responseNode.has("duration")) {
				double duration = responseNode.get("duration").asDouble();
				currentOffset += duration;
				totalProcessedDuration += duration;
			} else {
				currentOffset += chunkDurationSeconds;
			}
		}

		if (task != null) mergedResponse.put("task", task);
		if (language != null) mergedResponse.put("language", language);
		mergedResponse.put("duration", totalProcessedDuration);
		mergedResponse.put("text", combinedText.toString());
		mergedResponse.set("segments", combinedSegments);

		return objectMapper.writeValueAsString(mergedResponse);
	}

}
