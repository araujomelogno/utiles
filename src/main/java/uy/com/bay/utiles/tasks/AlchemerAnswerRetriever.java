package uy.com.bay.utiles.tasks;

import java.time.LocalDate;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

	@Value("${alchemer.api.token}")
	private String apiToken;

	@Value("${alchemer.api.token.secret}")
	private String apiTokenSecret;

	public AlchemerAnswerRetriever(TaskRepository taskRepository, AlchemerAnswerRepository alchemerAnswerRepository) {
		this.taskRepository = taskRepository;
		this.alchemerAnswerRepository = alchemerAnswerRepository;
		this.restTemplate = new RestTemplate();
	}

	@Scheduled(cron = "0 */1 * * * *")
	public void retrieveAlchemerAnswers() {
		LOGGER.info("Starting Alchemer Answer Retriever task...");
		List<Task> pendingTasks = taskRepository.findByJobTypeAndStatus(JobType.ALCHEMERANSWERRETRIEVAL,
				Status.PENDING);
		LOGGER.info("Found {} pending tasks.", pendingTasks.size());
		for (Task task : pendingTasks) {
			if (alchemerAnswerRepository
					.findByResponseIdAndSurveyId(task.getResponseId().longValue(), task.getSurveyId()).isEmpty()) {
				try {
					LOGGER.info("Processing task ID: {}", task.getId());

					String url = String.format(
							"https://api.alchemer.com/v5/survey/%d/surveyresponse/%d?api_token=%s&api_token_secret=%s",
							task.getSurveyId(), task.getResponseId(), apiToken, apiTokenSecret);

					String response = restTemplate.getForObject(url, String.class);
					String surveyor = "";
					ObjectMapper mapper = new ObjectMapper();
					JsonNode root = mapper.readTree(response);
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

					String url3 = String.format(
							"https://api.alchemer.com/v5/survey/%d/surveycampaign/%s?api_token=%s&api_token_secret=%s",
							task.getSurveyId(), linkId, apiToken, apiTokenSecret);

					String response3 = restTemplate.getForObject(url3, String.class);
					ObjectMapper mapper3 = new ObjectMapper();
					JsonNode root3 = mapper3.readTree(response3);

					String campaignName = root3.path("data").path("name").asText();
					if (urlData != null) {
						JsonNode surveyorNode = urlData.path("agente");
						if (surveyorNode.get("value") != null)
							surveyor = surveyorNode.get("value").asText();
					}

					if (root.path("result_ok").asBoolean()) {
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
									alchemerAnswerRepository.save(alchemerAnswer);
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
						LOGGER.warn("Task ID: {} not processed successfully, se mantiene en PENDING.", task.getId());
					}

				} catch (Exception e) {
					task.setStatus(Status.ERROR);
					taskRepository.save(task);
					LOGGER.error("Error processing task ID: {}", task.getId(), e);
				}
			} else {
				task.setStatus(Status.DONE);
				taskRepository.save(task);
			}

		}
		LOGGER.info("Alchemer Answer Retriever task finished.");
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
					if (alchemerAnswerRepository.findByAlchemerId(alchemerId).isEmpty()) {
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
						alchemerAnswerRepository.save(alchemerAnswer);
						LOGGER.info("Saved answer for parent question ID: {}, option ID: {}",
								answerNode.path("id").asLong(), alchemerId);
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
						if (alchemerAnswerRepository.findByAlchemerId(alchemerId).isEmpty()) {
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
							alchemerAnswerRepository.save(alchemerAnswer);
							LOGGER.info("Saved answer for parent question ID: {}, subquestion ID: {}",
									answerNode.path("id").asLong(), alchemerId);
						}
					}
				}
			}
		}
	}
}
