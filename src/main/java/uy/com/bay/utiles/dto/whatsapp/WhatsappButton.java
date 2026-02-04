package uy.com.bay.utiles.dto.whatsapp;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class WhatsappButton {
    private String type;
    private String text;
    @JsonProperty("flow_id")
    private Long flowId;
    @JsonProperty("flow_action")
    private String flowAction;
    @JsonProperty("navigate_screen")
    private String navigateScreen;
}
