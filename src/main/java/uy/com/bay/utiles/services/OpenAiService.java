package uy.com.bay.utiles.services;

import java.time.Duration;
import java.util.List;

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

import uy.com.bay.utiles.dto.AudioFile;

@Service
public class OpenAiService {

	@Value("${spring.ai.openai.api-key}")
	private String openaiApiKey;

	private static final String OPENAI_API_URL = "https://api.openai.com/v1/audio/transcriptions";

	private RestTemplate buildRestTemplate() {
		HttpComponentsClientHttpRequestFactory rf = new HttpComponentsClientHttpRequestFactory();
		rf.setConnectTimeout((int) Duration.ofSeconds(10).toMillis());
		rf.setConnectionRequestTimeout((int) Duration.ofSeconds(10).toMillis());
		rf.setReadTimeout((int) Duration.ofSeconds(1200).toMillis()); // ajustá según duración del audio
		return new RestTemplate(rf);
	}

	public String transcribeAudioTotal(AudioFile audioFile) throws Exception {
		RestTemplate restTemplate = buildRestTemplate();

		HttpHeaders headers = new HttpHeaders();
		headers.setBearerAuth(openaiApiKey);
		headers.setContentType(MediaType.MULTIPART_FORM_DATA);
		headers.setAccept(List.of(MediaType.APPLICATION_JSON));

		// 1) Leé TODO a bytes para tener content-length real (y evitar streams “raros”)
		byte[] bytes = audioFile.getInputStream().readAllBytes();

		// 2) Resource con filename + length real
		ByteArrayResource filePart = new ByteArrayResource(bytes) {
			@Override
			public String getFilename() {
				return audioFile.getFilename();
			}
		};

		MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
		body.add("file", filePart);
		body.add("model", "gpt-4o-transcribe-diarize");
		body.add("response_format", "diarized_json");
		body.add("chunking_strategy", "auto");
		// Opcional: si querés timestamps más finos (palabra), ver
		// timestamp_granularities en el spec :contentReference[oaicite:4]{index=4}

		HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

		ResponseEntity<String> response = restTemplate.postForEntity(OPENAI_API_URL, requestEntity, String.class);

		return response.getBody();
	}

}
