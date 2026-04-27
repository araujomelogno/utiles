package uy.com.bay.utiles.config;

import java.io.IOException;
import java.net.http.HttpClient;
import java.time.Duration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

@Configuration
public class OpenAiTimeoutConfig {

	private static final ObjectMapper EXTRA_BODY_MAPPER = new ObjectMapper();

	@Bean
	public RestClient.Builder restClientBuilder() {

		HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofMinutes(240)).build();

		JdkClientHttpRequestFactory rf = new JdkClientHttpRequestFactory(client);

		// ⬇️ ESTE es el read timeout
		rf.setReadTimeout(Duration.ofMinutes(250));

		return RestClient.builder().requestFactory(rf).requestInterceptor(stripEmptyExtraBodyInterceptor());
	}

	// Workaround for Spring AI 1.1.1–1.1.4 sending an empty "extra_body": {} that
	// OpenAI rejects with HTTP 400 "Unknown parameter: 'extra_body'".
	private static ClientHttpRequestInterceptor stripEmptyExtraBodyInterceptor() {
		return (request, body, execution) -> {
			byte[] payload = body;
			MediaType contentType = request.getHeaders().getContentType();
			if (payload != null && payload.length > 0 && contentType != null
					&& MediaType.APPLICATION_JSON.includes(contentType)) {
				try {
					JsonNode root = EXTRA_BODY_MAPPER.readTree(payload);
					if (root.isObject() && root.has("extra_body")) {
						JsonNode extraBody = root.get("extra_body");
						if (extraBody == null || extraBody.isNull()
								|| (extraBody.isObject() && extraBody.isEmpty())) {
							((ObjectNode) root).remove("extra_body");
							payload = EXTRA_BODY_MAPPER.writeValueAsBytes(root);
						}
					}
				} catch (IOException ignored) {
					// Not JSON or unparseable — forward the original body unchanged.
				}
			}
			return execution.execute(request, payload);
		};
	}
}