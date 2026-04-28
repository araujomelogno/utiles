package uy.com.bay.utiles.data;

import java.util.Date;
import java.util.Objects;

public class ExpenseReportDTO {

	private Long id;
	private String studyName;
	private String surveyorFirstName;
	private String surveyorLastName;
	private Date date;
	private Double amount;
	private String conceptName;
	private ExpenseReportStatus expenseStatus;

	public ExpenseReportDTO() {
	}

	public ExpenseReportDTO(Long id, String studyName, String surveyorFirstName, String surveyorLastName, Date date,
			Double amount, String conceptName, ExpenseReportStatus expenseStatus) {
		this.id = id;
		this.studyName = studyName;
		this.surveyorFirstName = surveyorFirstName;
		this.surveyorLastName = surveyorLastName;
		this.date = date;
		this.amount = amount;
		this.conceptName = conceptName;
		this.expenseStatus = expenseStatus;
	}

	public static ExpenseReportDTO fromEntity(ExpenseReport entity) {
		if (entity == null) {
			return null;
		}
		return new ExpenseReportDTO(entity.getId(),
				entity.getStudy() != null ? entity.getStudy().getName() : null,
				entity.getSurveyor() != null ? entity.getSurveyor().getFirstName() : null,
				entity.getSurveyor() != null ? entity.getSurveyor().getLastName() : null, entity.getDate(),
				entity.getAmount(), entity.getConcept() != null ? entity.getConcept().getName() : null,
				entity.getExpenseStatus());
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getStudyName() {
		return studyName;
	}

	public void setStudyName(String studyName) {
		this.studyName = studyName;
	}

	public String getSurveyorFirstName() {
		return surveyorFirstName;
	}

	public void setSurveyorFirstName(String surveyorFirstName) {
		this.surveyorFirstName = surveyorFirstName;
	}

	public String getSurveyorLastName() {
		return surveyorLastName;
	}

	public void setSurveyorLastName(String surveyorLastName) {
		this.surveyorLastName = surveyorLastName;
	}

	public String getSurveyorName() {
		String first = surveyorFirstName != null ? surveyorFirstName : "";
		String last = surveyorLastName != null ? surveyorLastName : "";
		String name = (first + " " + last).trim();
		return name.isEmpty() ? "" : name;
	}

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	public Double getAmount() {
		return amount;
	}

	public void setAmount(Double amount) {
		this.amount = amount;
	}

	public String getConceptName() {
		return conceptName;
	}

	public void setConceptName(String conceptName) {
		this.conceptName = conceptName;
	}

	public ExpenseReportStatus getExpenseStatus() {
		return expenseStatus;
	}

	public void setExpenseStatus(ExpenseReportStatus expenseStatus) {
		this.expenseStatus = expenseStatus;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof ExpenseReportDTO that))
			return false;
		return id != null && id.equals(that.id);
	}

	@Override
	public int hashCode() {
		return id != null ? Objects.hash(id) : System.identityHashCode(this);
	}
}
