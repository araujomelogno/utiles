package uy.com.bay.utiles.dto.aiencoding.response;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Coding {
    @JsonProperty("response_id")
    private String responseId;
    private List<AssignedCode> codes = new ArrayList<>();

    public String getResponseId() {
        return responseId;
    }

    public void setResponseId(String responseId) {
        this.responseId = responseId;
    }

    public List<AssignedCode> getCodes() {
        return codes;
    }

    public void setCodes(List<AssignedCode> codes) {
        this.codes = codes;
    }
}
