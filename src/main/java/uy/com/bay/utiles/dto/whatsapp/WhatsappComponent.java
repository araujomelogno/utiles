package uy.com.bay.utiles.dto.whatsapp;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class WhatsappComponent {
    private String type;
    private String format;
    private String text;
    private List<WhatsappButton> buttons;
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public String getFormat() {
		return format;
	}
	public void setFormat(String format) {
		this.format = format;
	}
	public String getText() {
		return text;
	}
	public void setText(String text) {
		this.text = text;
	}
	public List<WhatsappButton> getButtons() {
		return buttons;
	}
	public void setButtons(List<WhatsappButton> buttons) {
		this.buttons = buttons;
	}
    
    
}
