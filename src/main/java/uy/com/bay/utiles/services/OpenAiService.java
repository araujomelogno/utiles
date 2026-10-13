package uy.com.bay.utiles.services;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.Duration;
import java.util.List;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

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
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
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
	private final AudioDurationService audioDurationService;

	@Autowired
	public OpenAiService(ObjectMapper objectMapper, AudioDurationService audioDurationService) {
		this.objectMapper = objectMapper;
		this.audioDurationService = audioDurationService;
	}

	private RestTemplate buildRestTemplate() {
		HttpComponentsClientHttpRequestFactory rf = new HttpComponentsClientHttpRequestFactory();
		rf.setConnectTimeout((int) Duration.ofSeconds(10).toMillis());
		rf.setConnectionRequestTimeout((int) Duration.ofSeconds(10).toMillis());
		rf.setReadTimeout((int) Duration.ofSeconds(1200).toMillis()); // ajustá según duración del audio
		return new RestTemplate(rf);
	}

	protected double getDuration(File file) throws IOException {
		return audioDurationService.getDuration(file);
	}

	protected String callApi(byte[] bytes, String filename) {
		int maxRetries = 5;
		long baseBackoffMs = 5_000L;
		long maxBackoffMs = 120_000L;
		for (int attempt = 1; attempt <= maxRetries; attempt++) {
			try {
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

				ResponseEntity<String> response = restTemplate.postForEntity(OPENAI_API_URL, requestEntity,
						String.class);

				return response.getBody();
			} catch (ResourceAccessException e) {
				// Errores transitorios a nivel de red/socket (timeouts de lectura, conexión)
				logger.warn("API call failed - network error (attempt {}/{}): {}", attempt, maxRetries, e.getMessage());
				if (attempt == maxRetries) {
					throw e;
				}
				sleepBeforeRetry(backoffMs(baseBackoffMs, maxBackoffMs, attempt));
			} catch (HttpServerErrorException | HttpClientErrorException.TooManyRequests e) {
				// 5xx (incluye 504 Gateway Timeout) y 429 Too Many Requests son reintentables
				logger.warn("API call failed - server returned {} (attempt {}/{}): {}", e.getStatusCode(), attempt,
						maxRetries, e.getMessage());
				if (attempt == maxRetries) {
					throw e;
				}
				long retryAfterMs = parseRetryAfterMs(e.getResponseHeaders());
				long backoff = retryAfterMs > 0 ? Math.min(retryAfterMs, maxBackoffMs)
						: backoffMs(baseBackoffMs, maxBackoffMs, attempt);
				sleepBeforeRetry(backoff);
			}
		}
		throw new RuntimeException("API call failed after retries");
	}

	private static long backoffMs(long baseMs, long maxMs, int attempt) {
		long backoff = baseMs * (1L << (attempt - 1)); // exponencial: base, 2x, 4x, ...
		return Math.min(backoff, maxMs);
	}

	private static long parseRetryAfterMs(HttpHeaders headers) {
		if (headers == null) {
			return -1L;
		}
		String retryAfter = headers.getFirst(HttpHeaders.RETRY_AFTER);
		if (retryAfter == null || retryAfter.isBlank()) {
			return -1L;
		}
		try {
			return Long.parseLong(retryAfter.trim()) * 1000L;
		} catch (NumberFormatException ignored) {
			// El header puede venir como fecha HTTP; en ese caso usamos el backoff por defecto
			return -1L;
		}
	}

	private static void sleepBeforeRetry(long millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException ie) {
			Thread.currentThread().interrupt();
			throw new RuntimeException("Interrumpido durante reintentos al servicio de transcripción", ie);
		}
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

			if (duration <= 1200 && bytes.length <= Mp4AudioChunks.MAX_UPLOAD_BYTES) {
				return callApi(bytes, filename);
			} else if (Mp4AudioFile.isMp4(tempFile)) {
				return processChunksMp4(tempFile);
			} else {
				return processChunksWav(tempFile);
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
			AudioFormat decodedFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, baseFormat.getSampleRate(), 16,
					baseFormat.getChannels(), baseFormat.getChannels() * 2, baseFormat.getSampleRate(), false);

			try (AudioInputStream din = AudioSystem.getAudioInputStream(decodedFormat, in)) {
				// Chunk size: 6MB (approx 35s-40s depending on bitrate, safer for uploads)
				long bytesPerChunk = 6 * 1024 * 1024;
				int frameSize = decodedFormat.getFrameSize();
				if (frameSize <= 0)
					frameSize = 4;
				// Align to frame size
				bytesPerChunk = (bytesPerChunk / frameSize) * frameSize;

				byte[] buffer = new byte[(int) bytesPerChunk];
				int chunkIndex = 0;

				while (true) {
					int totalRead = 0;
					while (totalRead < buffer.length) {
						int read = din.read(buffer, totalRead, buffer.length - totalRead);
						if (read == -1)
							break;
						totalRead += read;
					}
					if (totalRead == 0)
						break;

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
							if (responseNode.has("task"))
								task = responseNode.get("task").asText();
							if (responseNode.has("language"))
								language = responseNode.get("language").asText();
						}

						if (responseNode.has("text")) {
							if (combinedText.length() > 0)
								combinedText.append(" ");
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
							totalProcessedDuration += approxDuration;
						}

					} finally {
						chunkFile.delete();
					}
					chunkIndex++;
				}
			}
		}

		if (task != null)
			mergedResponse.put("task", task);
		if (language != null)
			mergedResponse.put("language", language);
		mergedResponse.put("duration", totalProcessedDuration);
		mergedResponse.put("text", combinedText.toString());
		mergedResponse.set("segments", combinedSegments);

		return objectMapper.writeValueAsString(mergedResponse);
	}

	private String processChunksMp4(File inputFile) throws Exception {
		ObjectNode merged = objectMapper.createObjectNode();
		ArrayNode segments = objectMapper.createArrayNode();
		StringBuilder text = new StringBuilder();
		Mp4AudioChunks.process(inputFile, (bytes, offset, duration) -> {
			JsonNode response = objectMapper.readTree(callApi(bytes, "chunk.m4a"));
			if (offset == 0) {
				if (response.has("task")) {
					merged.set("task", response.get("task"));
				}
				if (response.has("language")) {
					merged.set("language", response.get("language"));
				}
			}
			if (response.has("text")) {
				if (text.length() > 0) {
					text.append(' ');
				}
				text.append(response.get("text").asText());
			}
			if (response.path("segments").isArray()) {
				for (JsonNode segment : response.path("segments")) {
					if (segment.isObject()) {
						ObjectNode adjusted = segment.deepCopy();
						if (adjusted.has("start")) {
							adjusted.put("start", adjusted.path("start").asDouble() + offset);
						}
						if (adjusted.has("end")) {
							adjusted.put("end", adjusted.path("end").asDouble() + offset);
						}
						segments.add(adjusted);
					}
				}
			}
			// Use encoded sample timing, including silent portions, even if the API omits duration.
			merged.put("duration", offset + duration);
		});
		merged.put("text", text.toString());
		merged.set("segments", segments);
		return objectMapper.writeValueAsString(merged);
	}

}
