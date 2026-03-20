package uy.com.bay.utiles.services;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import uy.com.bay.utiles.dto.AlchemerStudy;

@Service
public class AlchemerSurveyService {

	private static final Logger LOGGER = LoggerFactory.getLogger(AlchemerSurveyService.class);

	@Value("${alchemer.api.token}")
	private String apiToken;

	@Value("${alchemer.api.token.secret}")
	private String apiTokenSecret;

	private final RestTemplate restTemplate = new RestTemplate();

	public List<AlchemerStudy> fetchRecentSurveys() {
		try {
			LocalDate cutoff = LocalDate.now().minusMonths(4);
			String cutoffStr = cutoff.format(DateTimeFormatter.ISO_LOCAL_DATE);

			String url = String.format(
					"https://api.alchemer.com/v5/survey?api_token=%s&api_token_secret=%s"
							+ "&resultsperpage=100"
							+ "&filter[field][0]=date_created&filter[operator][0]=>&filter[value][0]=%s",
					apiToken, apiTokenSecret, cutoffStr);

			String response = restTemplate.getForObject(url, String.class);
			ObjectMapper mapper = new ObjectMapper();
			JsonNode root = mapper.readTree(response);
			JsonNode dataArray = root.path("data");

			List<AlchemerStudy> studies = new ArrayList<>();
			if (dataArray.isArray()) {
				for (JsonNode node : dataArray) {
					int id = node.path("id").asInt();
					String title = node.path("title").asText();
					studies.add(new AlchemerStudy(id, title));
				}
			}
			return studies;
		} catch (Exception e) {
			LOGGER.error("Error fetching surveys from Alchemer API", e);
			return List.of();
		}
	}
}
