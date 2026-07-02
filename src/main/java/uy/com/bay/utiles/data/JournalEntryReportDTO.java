package uy.com.bay.utiles.data;

import java.util.Date;

/**
 * Lightweight projection used to build the expenses Excel report.
 *
 * <p>
 * It carries only the scalar fields of {@link JournalEntry}, {@link Study} and
 * {@link Surveyor} that the report actually renders. Loading the report through
 * this projection avoids materializing the full {@code JournalEntry} entity
 * graph, whose eagerly-fetched attachment collections
 * ({@link ExpenseTransferFile} / {@link ExpenseReportFile}) hold {@code @Lob}
 * binary content and previously caused {@link OutOfMemoryError} when many
 * entries were exported at once.
 */
public class JournalEntryReportDTO {

	private final String detail;
	private final String obs;
	private final Date date;
	private final Operation operation;
	private final Double amount;
	private final Source source;

	private final String studyName;
	private final String studyOdooId;
	private final String studyObs;
	private final String studyClientName;
	private final String studyArea;
	private final Double studyTotalReportedCost;
	private final Double studyTotalTransfered;
	private final Double studyExpectedRevenue;

	private final String surveyorFirstName;
	private final String surveyorLastName;
	private final String surveyorLogin;
	private final String surveyorCi;
	private final String surveyorSurveyToGoId;
	private final Double surveyorBalance;

	public JournalEntryReportDTO(String detail, String obs, Date date, Operation operation, Double amount,
			Source source, String studyName, String studyOdooId, String studyObs, String studyClientName,
			String studyArea, Double studyTotalReportedCost, Double studyTotalTransfered, Double studyExpectedRevenue,
			String surveyorFirstName, String surveyorLastName, String surveyorLogin, String surveyorCi,
			String surveyorSurveyToGoId, Double surveyorBalance) {
		this.detail = detail;
		this.obs = obs;
		this.date = date;
		this.operation = operation;
		this.amount = amount;
		this.source = source;
		this.studyName = studyName;
		this.studyOdooId = studyOdooId;
		this.studyObs = studyObs;
		this.studyClientName = studyClientName;
		this.studyArea = studyArea;
		this.studyTotalReportedCost = studyTotalReportedCost;
		this.studyTotalTransfered = studyTotalTransfered;
		this.studyExpectedRevenue = studyExpectedRevenue;
		this.surveyorFirstName = surveyorFirstName;
		this.surveyorLastName = surveyorLastName;
		this.surveyorLogin = surveyorLogin;
		this.surveyorCi = surveyorCi;
		this.surveyorSurveyToGoId = surveyorSurveyToGoId;
		this.surveyorBalance = surveyorBalance;
	}

	public String getDetail() {
		return detail;
	}

	public String getObs() {
		return obs;
	}

	public Date getDate() {
		return date;
	}

	public Operation getOperation() {
		return operation;
	}

	public Double getAmount() {
		return amount;
	}

	public Source getSource() {
		return source;
	}

	public String getStudyName() {
		return studyName;
	}

	public String getStudyOdooId() {
		return studyOdooId;
	}

	public String getStudyObs() {
		return studyObs;
	}

	public String getStudyClientName() {
		return studyClientName;
	}

	public String getStudyArea() {
		return studyArea;
	}

	public Double getStudyTotalReportedCost() {
		return studyTotalReportedCost;
	}

	public Double getStudyTotalTransfered() {
		return studyTotalTransfered;
	}

	public Double getStudyExpectedRevenue() {
		return studyExpectedRevenue;
	}

	public String getSurveyorFirstName() {
		return surveyorFirstName;
	}

	public String getSurveyorLastName() {
		return surveyorLastName;
	}

	public String getSurveyorLogin() {
		return surveyorLogin;
	}

	public String getSurveyorCi() {
		return surveyorCi;
	}

	public String getSurveyorSurveyToGoId() {
		return surveyorSurveyToGoId;
	}

	public Double getSurveyorBalance() {
		return surveyorBalance;
	}
}
