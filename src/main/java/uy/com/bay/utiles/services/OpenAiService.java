package uy.com.bay.utiles.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import uy.com.bay.utiles.dto.AudioFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class OpenAiService {

    @Value("${openai.api.key}")
    private String openaiApiKey;

    private static final String OPENAI_API_URL = "https://api.openai.com/v1/audio/transcriptions";

    public Map<String, String> transcribeAudio(List<AudioFile> audioFiles) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(openaiApiKey);
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        Map<String, String> results = new HashMap<>();

        for (AudioFile audioFile : audioFiles) {
            try {
                MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
                body.add("file", new InputStreamResource(audioFile.getInputStream()) {
                    @Override
                    public String getFilename() {
                        return audioFile.getFilename();
                    }
                    @Override
                    public long contentLength() throws IOException {
                        return -1; // We don't know the content length beforehand
                    }
                });
                body.add("model", "gpt-4o-transcribe-diarize");
                body.add("response_format", "diarized_json");

                HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
                ResponseEntity<String> response = restTemplate.postForEntity(OPENAI_API_URL, requestEntity, String.class);

                results.put(audioFile.getFilename(), response.getBody());
            } catch (Exception e) {
                e.printStackTrace();
                results.put(audioFile.getFilename(), "Error during transcription: " + e.getMessage());
            }
        }
        return results;
    }
}
