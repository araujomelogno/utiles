package uy.com.bay.utiles.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import uy.com.bay.utiles.data.AlchemerSurveyResponse;
import uy.com.bay.utiles.data.AlchemerSurveyResponseData;
import uy.com.bay.utiles.data.Proyecto;
import uy.com.bay.utiles.data.ProyectoRepository;
import uy.com.bay.utiles.data.repository.AlchemerSurveyResponseRepository;
import uy.com.bay.utiles.data.repository.TaskRepository;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class AlchemerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProyectoRepository proyectoRepository;

    @Autowired
    private AlchemerSurveyResponseRepository alchemerSurveyResponseRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Test
    public void testReceiveAlchemerResponse() throws Exception {
        Proyecto proyecto = new Proyecto();
        proyecto.setAlchemerId("8399892");
        proyecto.setName("Test Project");
        proyectoRepository.save(proyecto);

        String json = "{\n" +
                "  \"webhook_name\": \"On Response Processed\",\n" +
                "  \"data\": {\n" +
                "    \"is_test\": false,\n" +
                "    \"session_id\": \"c77f3b3b0fabc2ebddb3e91505130ea3\",\n" +
                "    \"response_id\": 1,\n" +
                "    \"account_id\": 477885,\n" +
                "    \"survey_id\": 8399892,\n" +
                "    \"response_status\": \"Complete\",\n" +
                "    \"url_variables\": {\n" +
                "      \"sguid\": \"WVY1eqt1byRWHbWwlY\"\n" +
                "    },\n" +
                "    \"survey_link\": {\n" +
                "      \"id\": 4372465,\n" +
                "      \"type\": \"email\",\n" +
                "      \"name\": \"camaña mail\"\n" +
                "    },\n" +
                "    \"contact\": {\n" +
                "      \"Email\": \"pepear2aujo@gmail.com\",\n" +
                "      \"First Name\": \"nacho\",\n" +
                "      \"Last Name\": \"araujo\"\n" +
                "    }\n" +
                "  }\n" +
                "}";

        mockMvc.perform(MockMvcRequestBuilders.post("/api/alchemer/response")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk());
    }
}
