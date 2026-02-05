package uy.com.bay.utiles.dto.whatsapp;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class WhatsappTemplate {
    private String name;
    private String status;
    private String language;
    private String id;
    private List<WhatsappComponent> components;

    public String getNavigateScreen() {
        if (components != null) {
            for (WhatsappComponent component : components) {
                if ("BUTTONS".equals(component.getType()) && component.getButtons() != null) {
                    for (WhatsappButton button : component.getButtons()) {
                        if ("FLOW".equals(button.getType()) && button.getNavigateScreen() != null) {
                            return button.getNavigateScreen();
                        }
                    }
                }
            }
        }
        return null;
    }
}
