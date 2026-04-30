package uy.com.bay.utiles.data;

import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
public class ColumnMapping extends AbstractEntity {

    private String questionVariable = "";
    private boolean toCode;
    private boolean generateCodes;
    private String fineTuning = "";
    private String question = "";
    private Integer minimumCodifications = 1;
    private Integer maximumCodifications = 1;
    private Integer minimunQuestionsWithCode = 3;

    @ManyToOne
    @JoinColumn(name = "question_encoding_template_id")
    private QuestionEncodingTemplate questionEncodingTemplate;

    public ColumnMapping() {
    }

    public ColumnMapping(String originalName) {
        this.questionVariable = originalName;
    }

    public String getQuestionVariable() {
        return questionVariable;
    }

    public void setQuestionVariable(String questionVariable) {
        this.questionVariable = questionVariable;
    }

    public boolean isToCode() {
        return toCode;
    }

    public void setToCode(boolean toCode) {
        this.toCode = toCode;
    }

    public boolean isGenerateCodes() {
        return generateCodes;
    }

    public void setGenerateCodes(boolean generateCodes) {
        this.generateCodes = generateCodes;
    }

    public String getFineTuning() {
        return fineTuning;
    }

    public void setFineTuning(String fineTuning) {
        this.fineTuning = fineTuning;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public Integer getMinimumCodifications() {
        return minimumCodifications;
    }

    public void setMinimumCodifications(Integer minimumCodifications) {
        this.minimumCodifications = minimumCodifications;
    }

    public Integer getMaximumCodifications() {
        return maximumCodifications;
    }

    public void setMaximumCodifications(Integer maximumCodifications) {
        this.maximumCodifications = maximumCodifications;
    }

    public Integer getMinimunQuestionsWithCode() {
        return minimunQuestionsWithCode;
    }

    public void setMinimunQuestionsWithCode(Integer minimunQuestionsWithCode) {
        this.minimunQuestionsWithCode = minimunQuestionsWithCode;
    }

    public QuestionEncodingTemplate getQuestionEncodingTemplate() {
        return questionEncodingTemplate;
    }

    public void setQuestionEncodingTemplate(QuestionEncodingTemplate questionEncodingTemplate) {
        this.questionEncodingTemplate = questionEncodingTemplate;
    }
}
