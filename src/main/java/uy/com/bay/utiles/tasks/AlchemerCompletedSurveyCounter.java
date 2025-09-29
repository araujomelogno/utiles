package uy.com.bay.utiles.tasks;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import uy.com.bay.utiles.data.Fieldwork;
import uy.com.bay.utiles.data.repository.AlchemerSurveyResponseDataRepository;
import uy.com.bay.utiles.data.repository.FieldworkRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.logging.Logger;

@Component
public class AlchemerCompletedSurveyCounter {

    private static final Logger LOGGER = Logger.getLogger(AlchemerCompletedSurveyCounter.class.getName());

    @Autowired
    private FieldworkRepository fieldworkRepository;

    @Autowired
    private AlchemerSurveyResponseDataRepository alchemerSurveyResponseDataRepository;

    @Scheduled(cron = "0 0 */2 * * *") // "At minute 0 past every 2nd hour."
    public void countAlchemerCompletedSurveys() {
        LOGGER.info("Starting AlchemerCompletedSurveyCounter task...");

        LocalDate threeMonthsAgo = LocalDate.now().minusMonths(3);
        List<Fieldwork> fieldworks = fieldworkRepository.findAllByInitPlannedDateAfterAndAlchemerIdIsNotNull(threeMonthsAgo);

        for (Fieldwork fieldwork : fieldworks) {
            try {
                if (fieldwork.getAlchemerId() != null && !fieldwork.getAlchemerId().isEmpty()) {
                    int surveyId = Integer.parseInt(fieldwork.getAlchemerId());
                    long count = alchemerSurveyResponseDataRepository.countBySurveyId(surveyId);

                    Integer previousCompleted = fieldwork.getCompleted() == null ? 0 : fieldwork.getCompleted();
                    int newCompleted = (int) count;

                    fieldwork.setCompleted(newCompleted);

                    if (previousCompleted == 0 && newCompleted > 0) {
                        fieldwork.setInitDate(LocalDate.now());
                        LOGGER.info(String.format("Fieldwork ID %d initDate set to %s.", fieldwork.getId(), fieldwork.getInitDate()));
                    }

                    fieldworkRepository.save(fieldwork);
                    LOGGER.info(String.format("Updated fieldwork ID %d: set completed count to %d.", fieldwork.getId(), newCompleted));
                }
            } catch (NumberFormatException e) {
                LOGGER.warning(String.format("Could not parse alchemerId '%s' for fieldwork ID %d. Skipping.",
                        fieldwork.getAlchemerId(), fieldwork.getId()));
            } catch (Exception e) {
                LOGGER.severe(String.format("An error occurred while processing fieldwork ID %d: %s",
                        fieldwork.getId(), e.getMessage()));
            }
        }
        LOGGER.info("AlchemerCompletedSurveyCounter task finished.");
    }
}