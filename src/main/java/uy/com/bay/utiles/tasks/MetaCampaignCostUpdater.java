package uy.com.bay.utiles.tasks;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.annotation.PostConstruct;
import uy.com.bay.utiles.data.Fieldwork;
import uy.com.bay.utiles.data.repository.FieldworkRepository;
import uy.com.bay.utiles.data.service.FieldworkService;

@Component
public class MetaCampaignCostUpdater {

	private static final Logger logger = LoggerFactory.getLogger(MetaCampaignCostUpdater.class);

	private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

	private final FieldworkRepository fieldworkRepository;
	private final FieldworkService fieldworkService;
	private final RestTemplate restTemplate;
	private final ObjectMapper objectMapper = new ObjectMapper();

	@Value("${meta.campaign.id}")
	private String campaignId;

	@Value("${meta.access.token}")
	private String accessToken;

	public MetaCampaignCostUpdater(FieldworkRepository fieldworkRepository, FieldworkService fieldworkService,
			RestTemplateBuilder restTemplateBuilder) {
		this.fieldworkRepository = fieldworkRepository;
		this.fieldworkService = fieldworkService;
		this.restTemplate = restTemplateBuilder.setConnectTimeout(java.time.Duration.ofSeconds(30))
				.setReadTimeout(java.time.Duration.ofSeconds(60)).build();
	}

	@PostConstruct
	public void init() {
		logger.info("[SCHED] MetaCampaignCostUpdater bean initialized");
	}

	@Scheduled(cron = "0 0 4 * * *")
	@Transactional
	public void updateMetaCampaignCosts() {
		logger.info("Starting MetaCampaignCostUpdater...");

		LocalDate today = LocalDate.now();
		String dateEnd = today.format(DATE_FORMATTER);
		String dateInit = today.minusYears(2).format(DATE_FORMATTER);

		String timeRange = String.format("{\"since\":\"%s\",\"until\":\"%s\"}", dateInit, dateEnd);

		URI uri = UriComponentsBuilder
				.fromHttpUrl("https://graph.facebook.com/v21.0/" + campaignId + "/campaigns")
				.queryParam("access_token", accessToken)
				.queryParam("fields", "name,status,insights{spend}")
				.queryParam("time_range", timeRange)
				.build()
				.encode(StandardCharsets.UTF_8)
				.toUri();

		try {
			String response = restTemplate.getForObject(uri, String.class);
			if (response == null) {
				logger.warn("MetaCampaignCostUpdater: empty response from Meta API");
				return;
			}

			JsonNode root = objectMapper.readTree(response);
			JsonNode data = root.path("data");
			if (!data.isArray()) {
				logger.warn("MetaCampaignCostUpdater: response does not contain a data array");
				return;
			}

			for (JsonNode campaign : data) {
				String name = campaign.path("name").asText("");
				JsonNode insightsData = campaign.path("insights").path("data");
				if (!insightsData.isArray() || insightsData.size() == 0) {
					continue;
				}
				JsonNode spendNode = insightsData.get(0).path("spend");
				if (spendNode.isMissingNode() || spendNode.isNull()) {
					continue;
				}

				double spend;
				try {
					spend = Double.parseDouble(spendNode.asText("0"));
				} catch (NumberFormatException e) {
					logger.warn("MetaCampaignCostUpdater: could not parse spend value '{}'", spendNode.asText());
					continue;
				}

				if (spend == 0d) {
					continue;
				}
				if (name == null || !name.startsWith("S00")) {
					continue;
				}

				int spaceIdx = name.indexOf(' ');
				String fragment = spaceIdx > 0 ? name.substring(0, spaceIdx) : name;

				List<Fieldwork> matches = fieldworkRepository.findAllByStudy_NameContainingIgnoreCase(fragment);
				if (matches.isEmpty()) {
					logger.info("MetaCampaignCostUpdater: no fieldwork found for fragment '{}'", fragment);
					continue;
				}
				for (Fieldwork fw : matches) {
					fw.setCampaignSpent(spend);
					fieldworkService.save(fw);
				}
			}
		} catch (Exception e) {
			logger.error("MetaCampaignCostUpdater: error while updating Meta campaign costs", e);
		}

		logger.info("MetaCampaignCostUpdater finished.");
	}
}
