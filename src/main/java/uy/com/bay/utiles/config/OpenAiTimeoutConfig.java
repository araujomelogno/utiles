package uy.com.bay.utiles.config;

import java.net.http.HttpClient;
import java.time.Duration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class OpenAiTimeoutConfig {

	@Bean
	public RestClient.Builder restClientBuilder() {

		HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofMinutes(20)).build();

		JdkClientHttpRequestFactory rf = new JdkClientHttpRequestFactory(client);

		// ⬇️ ESTE es el read timeout
		rf.setReadTimeout(Duration.ofMinutes(25));

		return RestClient.builder().requestFactory(rf);
	}
}