package uy.com.bay.utiles.dto.whatsapp;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

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

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getLanguage() {
		return language;
	}

	public void setLanguage(String language) {
		this.language = language;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public List<WhatsappComponent> getComponents() {
		return components;
	}

	public void setComponents(List<WhatsappComponent> components) {
		this.components = components;
	}
    
    
}
