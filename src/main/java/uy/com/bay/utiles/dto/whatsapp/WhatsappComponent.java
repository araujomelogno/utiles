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
}
