package uy.com.bay.utiles.tasks;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import uy.com.bay.utiles.data.AlchemerAnswer;
import uy.com.bay.utiles.data.JobType;
import uy.com.bay.utiles.data.Status;
import uy.com.bay.utiles.data.Task;
import uy.com.bay.utiles.data.repository.AlchemerAnswerRepository;
import uy.com.bay.utiles.data.repository.TaskRepository;

@Component
public class AlchemerAnswerRetriever {

	private static final Logger LOGGER = LoggerFactory.getLogger(AlchemerAnswerRetriever.class);

	private final TaskRepository taskRepository;
	private final AlchemerAnswerRepository alchemerAnswerRepository;
	private final RestTemplate restTemplate;
	private final Executor alchemerExecutor;

	@Value("${alchemer.api.token}")
	private String apiToken;

	@Value("${alchemer.api.token.secret}")
	private String apiTokenSecret;

	public AlchemerAnswerRetriever(TaskRepository taskRepository, AlchemerAnswerRepository alchemerAnswerRepository,
			@Qualifier("alchemerExecutor") Executor alchemerExecutor) {
		this.taskRepository = taskRepository;
		this.alchemerAnswerRepository = alchemerAnswerRepository;
		this.restTemplate = new RestTemplate();
		this.alchemerExecutor = alchemerExecutor;
	}

	@Scheduled(cron = "0 */1 * * * *")
	public void retrieveAlchemerAnswers() {
		LOGGER.info("Starting Alchemer Answer Retriever task...");
		Date cutoffDate = Date.from(Instant.now().minus(1, ChronoUnit.DAYS));
		List<Task> pendingTasks = taskRepository.findPendingOrStuckRunning(JobType.ALCHEMERANSWERRETRIEVAL, cutoffDate);
		LOGGER.info("Found {} pending/stuck tasks.", pendingTasks.size());

		pendingTasks.forEach(task -> CompletableFuture.runAsync(() -> processTask(task), alchemerExecutor));

		LOGGER.info("Alchemer Answer Retriever task finished.");
	}

	private void processTask(Task task) {
		task.setStatus(Status.RUNNING);
		task.setProcessDate(new Date());
		taskRepository.save(task);
		if (alchemerAnswerRepository.findByResponseIdAndSurveyId(task.getResponseId().longValue(), task.getSurveyId())
				.isEmpty()) {
			try {
				LOGGER.info("Processing task ID: {}", task.getId());

				String url = String.format(
						"https://api.alchemer.com/v5/survey/%d/surveyresponse/%d?api_token=%s&api_token_secret=%s",
						task.getSurveyId(), task.getResponseId(), apiToken, apiTokenSecret);

				String response = restTemplate.getForObject(url, String.class);
				String surveyor = "";
				ObjectMapper mapper = new ObjectMapper();
				JsonNode root = mapper.readTree(response);
				if (root.path("result_ok").asBoolean()) {

					if (root.path("data").path("is_test_data").asBoolean() == false
							&& root.path("data").path("status").asText().equalsIgnoreCase("Complete")) {
						JsonNode surveyData = root.path("data").path("survey_data");
						JsonNode urlData = root.path("data").path("url_variables");

						String url2 = String.format(
								"https://api.alchemer.com/v5/survey/%d?api_token=%s&api_token_secret=%s",
								task.getSurveyId(), apiToken, apiTokenSecret);

						String response2 = restTemplate.getForObject(url2, String.class);

						ObjectMapper mapper2 = new ObjectMapper();
						JsonNode root2 = mapper2.readTree(response2);

						JsonNode teamArray = root2.path("data").path("team");
						String studyTeam = Optional.ofNullable(teamArray).filter(JsonNode::isArray)
								.filter(arr -> arr.size() > 0).map(arr -> arr.get(0))
								.map(node -> node.path("name").asText()).orElse("");

						String linkId = root.path("data").path("link_id").asText();

						String campaignName = "sin campaña activa";
						try {
							String url3 = String.format(
									"https://api.alchemer.com/v5/survey/%d/surveycampaign/%s?api_token=%s&api_token_secret=%s",
									task.getSurveyId(), linkId, apiToken, apiTokenSecret);

							String response3 = restTemplate.getForObject(url3, String.class);
							ObjectMapper mapper3 = new ObjectMapper();
							JsonNode root3 = mapper3.readTree(response3);

							campaignName = root3.path("data").path("name").asText();
						} catch (Exception e) {
							LOGGER.info("Task ID: {} la campaña fue borrada.");
						}

						if (urlData != null) {
							JsonNode surveyorNode = urlData.path("agente");
							if (surveyorNode.get("value") != null)
								surveyor = surveyorNode.get("value").asText();
						}

						Iterator<Map.Entry<String, JsonNode>> fields = surveyData.fields();
						while (fields.hasNext()) {
							Map.Entry<String, JsonNode> entry = fields.next();
							JsonNode answerNode = entry.getValue();

							String type = answerNode.path("type").asText();
							if (answerNode.path("shown").asBoolean()) {
								switch (type) {
								case "RADIO":
								case "MENU":
								case "HIDDEN":
								case "ESSAY":
								case "NPS":
								case "RANK":
								case "TEXTBOX":

									Long alchemerId = answerNode.path("id").asLong();
									AlchemerAnswer alchemerAnswer = new AlchemerAnswer();
									alchemerAnswer.setAlchemerId(alchemerId);
									alchemerAnswer.setType(type);
									alchemerAnswer.setQuestion(answerNode.path("question").asText());
									alchemerAnswer.setSectionId(answerNode.path("section_id").asInt());
									alchemerAnswer.setAnswer(answerNode.path("answer").asText());
									alchemerAnswer.setShown(true);
									alchemerAnswer.setSurveyId(task.getSurveyId());
									alchemerAnswer.setStudyName(task.getStudyName());
									alchemerAnswer.setResponseId(task.getResponseId());
									alchemerAnswer.setCreated(LocalDate.now());
									alchemerAnswer.setSurveyor(surveyor);
									alchemerAnswer.setStudyTeam(studyTeam);
									alchemerAnswer.setCampaignName(campaignName);
									if (!alchemerAnswerRepository
											.existsByStudyNameAndResponseIdAndSurveyIdAndAlchemerId(task.getStudyName(),
													task.getResponseId(), task.getSurveyId(), alchemerId)) {
										alchemerAnswerRepository.save(alchemerAnswer);
									}
									break;
								case "parent":
									processParentAnswer(answerNode, task, task.getStudyName(), surveyor, studyTeam,
											campaignName);
									break;
								default:
									LOGGER.warn("Unknown answer type: {}", type);
									break;
								}
							}
						}
						task.setStatus(Status.DONE);
						taskRepository.save(task);
						LOGGER.info("Task ID: {} processed successfully.", task.getId());

					} else {
						task.setStatus(Status.DONE);
						taskRepository.save(task);
						LOGGER.info("ES TEST o se borro la respuesta  processing task ID: {}", task.getId());
					}
				} else {
					task.setStatus(Status.PENDING);
					taskRepository.save(task);
					LOGGER.info("Error processing task ID: {}", task.getId());
				}

			} catch (Exception e) {
				task.setStatus(Status.PENDING);
				taskRepository.save(task);
				LOGGER.error("Error processing task ID: {}", task.getId(), e);
			}
		} else {
			task.setStatus(Status.DONE);
			taskRepository.save(task);
		}
	}

	private void processParentAnswer(JsonNode answerNode, Task task, String surveyTitle, String surveyor,
			String studyTeam, String campaignName) {
		if (answerNode.has("options")) {
			Iterator<Map.Entry<String, JsonNode>> options = answerNode.path("options").fields();
			while (options.hasNext()) {
				Map.Entry<String, JsonNode> optionEntry = options.next();
				JsonNode optionNode = optionEntry.getValue();
				if (optionNode.path("shown").asBoolean(true)) { // Assume shown if not present
					Long alchemerId = optionNode.path("id").asLong();

					AlchemerAnswer alchemerAnswer = new AlchemerAnswer();
					alchemerAnswer.setAlchemerId(alchemerId);
					alchemerAnswer.setType(optionNode.path("type").asText("parent_option"));
					alchemerAnswer.setQuestion(answerNode.path("question").asText());
					alchemerAnswer.setAnswer(optionNode.path("answer").asText());
					alchemerAnswer.setSectionId(answerNode.path("section_id").asInt());
					alchemerAnswer.setShown(true);
					alchemerAnswer.setSurveyId(task.getSurveyId());
					alchemerAnswer.setStudyName(surveyTitle);
					alchemerAnswer.setResponseId(task.getResponseId());
					alchemerAnswer.setSurveyor(surveyor);
					alchemerAnswer.setCreated(LocalDate.now());
					alchemerAnswer.setStudyTeam(studyTeam);
					alchemerAnswer.setCampaignName(campaignName);
					if (!alchemerAnswerRepository.existsByStudyNameAndResponseIdAndSurveyIdAndAlchemerId(
							task.getStudyName(), task.getResponseId(), task.getSurveyId(), alchemerId)) {
						alchemerAnswerRepository.save(alchemerAnswer);
					}

				}
			}
		}

		if (answerNode.has("subquestions")) {
			Iterator<Map.Entry<String, JsonNode>> subquestions = answerNode.path("subquestions").fields();
			while (subquestions.hasNext()) {
				Map.Entry<String, JsonNode> subquestionEntry = subquestions.next();
				JsonNode subquestionNode = subquestionEntry.getValue();
				Iterator<Map.Entry<String, JsonNode>> answers = subquestionNode.fields();
				while (answers.hasNext()) {
					Map.Entry<String, JsonNode> answerEntry = answers.next();
					JsonNode answer = answerEntry.getValue();
					if (answer.path("shown").asBoolean(true)) { // Assume shown if not present
						Long alchemerId = answer.path("id").asLong();

						AlchemerAnswer alchemerAnswer = new AlchemerAnswer();
						alchemerAnswer.setAlchemerId(alchemerId);
						alchemerAnswer.setType(answer.path("type").asText("parent_subquestion"));
						alchemerAnswer.setQuestion(answer.path("question").asText());
						alchemerAnswer.setAnswer(answer.path("answer").asText());
						alchemerAnswer.setSectionId(answerNode.path("section_id").asInt());
						alchemerAnswer.setShown(true);
						alchemerAnswer.setSurveyId(task.getSurveyId());
						alchemerAnswer.setResponseId(task.getResponseId());
						alchemerAnswer.setStudyName(surveyTitle);
						alchemerAnswer.setSurveyor(surveyor);
						alchemerAnswer.setCreated(LocalDate.now());
						alchemerAnswer.setStudyTeam(studyTeam);
						alchemerAnswer.setCampaignName(campaignName);
						if (!alchemerAnswerRepository.existsByStudyNameAndResponseIdAndSurveyIdAndAlchemerId(
								task.getStudyName(), task.getResponseId(), task.getSurveyId(), alchemerId)) {
							alchemerAnswerRepository.save(alchemerAnswer);

						}

					}
				}
			}
		}
	}
}
