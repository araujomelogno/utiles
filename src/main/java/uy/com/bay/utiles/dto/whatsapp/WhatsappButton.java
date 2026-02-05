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
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public String getText() {
		return text;
	}
	public void setText(String text) {
		this.text = text;
	}
	public Long getFlowId() {
		return flowId;
	}
	public void setFlowId(Long flowId) {
		this.flowId = flowId;
	}
	public String getFlowAction() {
		return flowAction;
	}
	public void setFlowAction(String flowAction) {
		this.flowAction = flowAction;
	}
	public String getNavigateScreen() {
		return navigateScreen;
	}
	public void setNavigateScreen(String navigateScreen) {
		this.navigateScreen = navigateScreen;
	}
    
    
    
}
