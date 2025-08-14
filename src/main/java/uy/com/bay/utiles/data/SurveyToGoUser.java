package uy.com.bay.utiles.data;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SurveyToGoUser {

    @JsonProperty("ID")
    private String id;

    @JsonProperty("Username")
    private String username;

    @JsonProperty("ExternalRefID")
    private String externalRefID;

    @JsonProperty("IsAccountDisabled")
    private boolean isAccountDisabled;

    @JsonProperty("IsAccountLocked")
    private boolean isAccountLocked;

    // Getters and setters

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getExternalRefID() {
        return externalRefID;
    }

    public void setExternalRefID(String externalRefID) {
        this.externalRefID = externalRefID;
    }

    public boolean isAccountDisabled() {
        return isAccountDisabled;
    }

    public void setAccountDisabled(boolean accountDisabled) {
        isAccountDisabled = accountDisabled;
    }

    public boolean isAccountLocked() {
        return isAccountLocked;
    }

    public void setAccountLocked(boolean accountLocked) {
        isAccountLocked = accountLocked;
    }
}
