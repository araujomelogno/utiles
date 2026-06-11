package uy.com.bay.utiles.tasks;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

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
import uy.com.bay.utiles.data.Study;
import uy.com.bay.utiles.data.StudyRepository;

@Component
public class MetaCampaignCostUpdater {

	private static final Logger logger = LoggerFactory.getLogger(MetaCampaignCostUpdater.class);

	private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
	private static final String CENTRAL_STUDY_NAME = "S0000- CENTRAL";

	private final StudyRepository studyRepository;
	private final RestTemplate restTemplate;
	private final ObjectMapper objectMapper = new ObjectMapper();

	@Value("${meta.campaign.id}")
	private String campaignId;

	@Value("${meta.access.token}")
	private String accessToken;

	public MetaCampaignCostUpdater(StudyRepository studyRepository, RestTemplateBuilder restTemplateBuilder) {
		this.studyRepository = studyRepository;
		this.restTemplate = restTemplateBuilder.setConnectTimeout(java.time.Duration.ofSeconds(30))
				.setReadTimeout(java.time.Duration.ofSeconds(60)).build();
	}

	@PostConstruct
	public void init() {
		logger.info("[SCHED] MetaCampaignCostUpdater bean initialized");
	}

	@Scheduled(cron = "0 0 19 * * *")
	@Transactional
	public void updateMetaCampaignCosts() {
		logger.info("Starting MetaCampaignCostUpdater...");

		LocalDate today = LocalDate.now();
		String dateEnd = today.format(DATE_FORMATTER);
		String dateInit = today.minusYears(2).format(DATE_FORMATTER);

		String timeRange = String.format("{\"since\":\"%s\",\"until\":\"%s\"}", dateInit, dateEnd);

		URI uri = UriComponentsBuilder.fromHttpUrl("https://graph.facebook.com/v25.0/" + campaignId + "/insights")
				.queryParam("access_token", accessToken).queryParam("fields", "campaign_name,spend")
				.queryParam("level", "campaign").queryParam("time_range", timeRange).queryParam("limit", 99).build()
				.encode(StandardCharsets.UTF_8).toUri();

		String nextUrl = uri.toString();
		int pageCount = 0;
		Map<String, Double> aggregated = new HashMap<>();
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

				Map<String, Double> pageResult = processCampaigns(data);
				for (Map.Entry<String, Double> entry : pageResult.entrySet()) {
					aggregated.merge(entry.getKey(), entry.getValue(), Double::sum);
				}

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

		applyAggregatedSpendToStudies(aggregated);

		logger.info("MetaCampaignCostUpdater finished after {} page(s).", pageCount);
	}

	private Map<String, Double> processCampaigns(JsonNode data) {
		Map<String, Double> result = new HashMap<>();
		for (JsonNode campaign : data) {

			String campaignName = campaign.path("campaign_name").asText("");
			JsonNode spendNode = campaign.path("spend");
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
			result.merge(fragment, spend, Double::sum);
		}
		return result;
	}

	private void applyAggregatedSpendToStudies(Map<String, Double> aggregated) {
		Date today = Date.from(LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant());
		for (Map.Entry<String, Double> entry : aggregated.entrySet()) {
			String fragment = entry.getKey();
			Double totalSpend = entry.getValue();

			Optional<Study> studyOpt = studyRepository.findFirstByNameStartingWith(fragment);
			if (studyOpt.isEmpty()) {
				studyOpt = studyRepository.findByName(CENTRAL_STUDY_NAME);
				if (studyOpt.isEmpty()) {
					Study central = new Study();
					central.setName(CENTRAL_STUDY_NAME);
					studyOpt = Optional.of(studyRepository.save(central));
				}
			}

			Study study = studyOpt.get();
			double previousSum = 0d;
			if (study.getMetaCostByDate() != null) {
				for (Double v : study.getMetaCostByDate().values()) {
					if (v != null) {
						previousSum += v;
					}
				}
			}
			double delta = totalSpend - previousSum;
			if (study.getMetaCostByDate() == null) {
				study.setMetaCostByDate(new HashMap<>());
			}
			study.getMetaCostByDate().merge(today, delta, Double::sum);
			studyRepository.save(study);
		}
	}
}
