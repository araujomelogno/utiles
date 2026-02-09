package uy.com.bay.utiles.tasks;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.http.MediaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uy.com.bay.utiles.data.WhatsappFlowTask;
import uy.com.bay.utiles.data.Status;
import uy.com.bay.utiles.data.repository.WhatsappFlowTaskRepository;

import java.util.Date;
import java.util.List;

@Component
public class WhatsappSender {

    private static final Logger logger = LoggerFactory.getLogger(WhatsappSender.class);

    private final WhatsappFlowTaskRepository whatsappFlowTaskRepository;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    @Value("${whatsapp.phone.number.id}")
    private String phoneNumberId;

    @Value("${whatsapp.api.token}")
    private String apiToken;

    public WhatsappSender(WhatsappFlowTaskRepository whatsappFlowTaskRepository, ObjectMapper objectMapper) {
        this.whatsappFlowTaskRepository = whatsappFlowTaskRepository;
        this.objectMapper = objectMapper;
        this.restClient = RestClient.builder().build();
    }

    @Scheduled(cron = "0 0/2 * * * ?")
    public void sendMessages() {
        logger.info("Starting WhatsappSender task...");
        List<WhatsappFlowTask> tasks = whatsappFlowTaskRepository.findAllByStatusAndScheduleBefore(Status.PENDING, new Date());

        for (WhatsappFlowTask task : tasks) {
            processTask(task);
        }
        logger.info("WhatsappSender task finished.");
    }

    private void processTask(WhatsappFlowTask task) {
        try {
            String url = "https://graph.facebook.com/v19.0/" + phoneNumberId + "/messages";

            String responseBody = restClient.post()
                    .uri(url)
                    .header("Authorization", "Bearer " + apiToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(task.getInput())
                    .retrieve()
                    .body(String.class);

            task.setProcessedDate(new Date());
            task.setOutput(responseBody);

            if (responseBody != null) {
                JsonNode rootNode = objectMapper.readTree(responseBody);

                // Parse according to user instructions
                if (rootNode.has("message_status")) {
                    task.setResponseStatus(rootNode.get("message_status").asText());
                }

                if (rootNode.has("id")) {
                    task.setWamid(rootNode.get("id").asText());
                }

                // Also check standard Facebook structure just in case, as 'id' is usually inside 'messages' array
                if (rootNode.has("messages")) {
                    JsonNode messages = rootNode.get("messages");
                    if (messages.isArray() && messages.size() > 0) {
                         JsonNode firstMessage = messages.get(0);
                         if (firstMessage.has("id") && task.getWamid() == null) {
                             task.setWamid(firstMessage.get("id").asText());
                         }
                         if (firstMessage.has("message_status") && task.getResponseStatus() == null) {
                             task.setResponseStatus(firstMessage.get("message_status").asText());
                         }
                    }
                }
            }

            task.setStatus(Status.DONE);
            whatsappFlowTaskRepository.save(task);

        } catch (Exception e) {
            logger.error("Error processing WhatsappFlowTask id: " + task.getId(), e);
            task.setStatus(Status.ERROR);
            task.setProcessedDate(new Date()); 
            String currentOutput = task.getOutput();
            String errorMsg = "Error: " + e.getMessage();
            task.setOutput(currentOutput == null ? errorMsg : currentOutput + "\n" + errorMsg);

            whatsappFlowTaskRepository.save(task);
        }
    }
}
