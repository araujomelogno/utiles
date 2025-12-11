package uy.com.bay.utiles.dto.aiencoding.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Coding {
    @JsonProperty("response_id")
    private String responseId;
    @JsonProperty("assigned_code")
    private String assignedCode;
    private String comment;

    public String getResponseId() {
        return responseId;
    }

    public void setResponseId(String responseId) {
        this.responseId = responseId;
    }

    public String getAssignedCode() {
        return assignedCode;
    }

    public void setAssignedCode(String assignedCode) {
        this.assignedCode = assignedCode;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
