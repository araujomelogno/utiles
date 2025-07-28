package uy.com.bay.utiles.tasks;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import uy.com.bay.utiles.data.AlchemerAnswer;
import uy.com.bay.utiles.data.JobType;
import uy.com.bay.utiles.data.Status;
import uy.com.bay.utiles.data.Task;
import uy.com.bay.utiles.data.repository.AlchemerAnswerRepository;
import uy.com.bay.utiles.data.repository.TaskRepository;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

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
        List<Task> pendingTasks = taskRepository.findByJobTypeAndStatus(JobType.ALCHEMERANSWERRETRIEVAL, Status.PENDING);
        LOGGER.info("Found {} pending tasks.", pendingTasks.size());

        for (Task task : pendingTasks) {
            try {
                LOGGER.info("Processing task ID: {}", task.getId());
                String url = String.format("https://api.alchemer.com/v5/survey/%d/surveyresponse/%d?api_token=%s&api_token_secret=%s",
                        task.getSurveyId(), task.getResponseId(), apiToken, apiTokenSecret);

                String response = restTemplate.getForObject(url, String.class);
                LOGGER.info("Response from Alchemer API: {}", response);

                ObjectMapper mapper = new ObjectMapper();
                JsonNode root = mapper.readTree(response);
                JsonNode surveyData = root.path("data").path("survey_data");

                Iterator<Map.Entry<String, JsonNode>> fields = surveyData.fields();
                while (fields.hasNext()) {
                    Map.Entry<String, JsonNode> entry = fields.next();
                    JsonNode answerNode = entry.getValue();

                    AlchemerAnswer alchemerAnswer = new AlchemerAnswer();
                    alchemerAnswer.setId(answerNode.path("id").asLong());
                    alchemerAnswer.setType(answerNode.path("type").asText());
                    alchemerAnswer.setQuestion(answerNode.path("question").asText());
                    alchemerAnswer.setSectionId(answerNode.path("section_id").asInt());
                    alchemerAnswer.setAnswer(answerNode.path("answer").asText());
                    alchemerAnswer.setShown(answerNode.path("shown").asBoolean());
                    alchemerAnswer.setSurveyId(task.getSurveyId());
                    alchemerAnswer.setResponseId(task.getResponseId());

                    alchemerAnswerRepository.save(alchemerAnswer);
                    LOGGER.info("Saved answer for question ID: {}", alchemerAnswer.getId());
                }

                task.setStatus(Status.DONE);
                taskRepository.save(task);
                LOGGER.info("Task ID: {} processed successfully.", task.getId());

            } catch (Exception e) {
            	task.setStatus(Status.DONE);
                taskRepository.save(task);
                LOGGER.error("Error processing task ID: {}", task.getId(), e);
            }
        }
        LOGGER.info("Alchemer Answer Retriever task finished.");
    }
}
