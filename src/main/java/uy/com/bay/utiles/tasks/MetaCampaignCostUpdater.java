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
//	@Scheduled(cron = "2 * * * * *")
	@Transactional
	public void updateMetaCampaignCosts() {
		logger.info("Starting MetaCampaignCostUpdater...");

		LocalDate today = LocalDate.now();
		String dateEnd = today.format(DATE_FORMATTER);
		String dateInit = today.minusYears(2).format(DATE_FORMATTER);

		String timeRange = String.format("{\"since\":\"%s\",\"until\":\"%s\"}", dateInit, dateEnd);

		URI uri = UriComponentsBuilder.fromHttpUrl("https://graph.facebook.com/v25.0/" + campaignId + "/campaigns")
				.queryParam("access_token", accessToken).queryParam("fields", "name,insights{spend,impressions,clicks}")
				.queryParam("time_range", timeRange).queryParam("limit", 99).build().encode(StandardCharsets.UTF_8)
				.toUri();

		String nextUrl = uri.toString();
		int pageCount = 0;
		try {
			while (nextUrl != null) {
				pageCount++;
				logger.info("MetaCampaignCostUpdater: fetching page {}", pageCount);
				String response = restTemplate.getForObject(URI.create(nextUrl), String.class);
				if (response == null) {
					logger.warn("MetaCampaignCostUpdater: empty response from Meta API");
					break;
				}

				JsonNode root = objectMapper.readTree(response);
				JsonNode data = root.path("data");
				if (!data.isArray()) {
					logger.warn("MetaCampaignCostUpdater: response does not contain a data array");
					break;
				}

				processCampaigns(data);

				JsonNode nextNode = root.path("paging").path("next");
				if (nextNode.isMissingNode() || nextNode.isNull()) {
					nextNode = root.path("next");
				}
				if (nextNode.isMissingNode() || nextNode.isNull() || nextNode.asText("").isEmpty()) {
					nextUrl = null;
				} else {
					nextUrl = nextNode.asText();
				}
			}
		} catch (Exception e) {
			logger.error("MetaCampaignCostUpdater: error while updating Meta campaign costs", e);
		}

		logger.info("MetaCampaignCostUpdater finished after {} page(s).", pageCount);
	}

	private void processCampaigns(JsonNode data) {
		for (JsonNode campaign : data) {
			JsonNode insightsData = campaign.path("insights").path("data");
			if (!insightsData.isArray() || insightsData.size() == 0) {
				continue;
			}
			JsonNode insight = insightsData.get(0);
			String campaignName = insight.path("campaign_name").asText("");
			JsonNode spendNode = insight.path("spend");
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
			if (campaignName == null || !campaignName.startsWith("S00")) {
				continue;
			}

			int spaceIdx = campaignName.indexOf(' ');
			String fragment = spaceIdx > 0 ? campaignName.substring(0, spaceIdx) : campaignName;
			logger.info("Se encotnr[o spend  " + spend);
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
	}
}
