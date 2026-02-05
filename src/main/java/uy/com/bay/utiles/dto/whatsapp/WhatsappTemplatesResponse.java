package uy.com.bay.utiles.dto.whatsapp;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class WhatsappTemplatesResponse {
    private List<WhatsappTemplate> data;

	public List<WhatsappTemplate> getData() {
		return data;
	}

	public void setData(List<WhatsappTemplate> data) {
		this.data = data;
	}
    
    
    
}
