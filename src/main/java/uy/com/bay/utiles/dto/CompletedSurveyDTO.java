package uy.com.bay.utiles.dto;

import java.time.LocalDate;

public class CompletedSurveyDTO {

    private String surveyor;
    private String studyName;
    private LocalDate created;
    private long count;

    public CompletedSurveyDTO(String surveyor, String studyName, LocalDate created, long count) {
        this.surveyor = surveyor;
        this.studyName = studyName;
        this.created = created;
        this.count = count;
    }

    public String getSurveyor() {
        return surveyor;
    }

    public void setSurveyor(String surveyor) {
        this.surveyor = surveyor;
    }

    public String getStudyName() {
        return studyName;
    }

    public void setStudyName(String studyName) {
        this.studyName = studyName;
    }

    public LocalDate getCreated() {
        return created;
    }

    public void setCreated(LocalDate created) {
        this.created = created;
    }

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }
}
