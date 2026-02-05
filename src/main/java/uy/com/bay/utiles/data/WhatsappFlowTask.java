package uy.com.bay.utiles.data;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;

@Entity
public class WhatsappFlowTask extends AbstractEntity {

	private Date created;

	@Enumerated(EnumType.STRING)
	private Status status;

	private Date schedule;

	private Date processedDate;

	@Column(name = "recipient")
	private String to;

	@ElementCollection
	@CollectionTable(name = "whatsapp_flow_task_parameters", joinColumns = @JoinColumn(name = "task_id"))
	@Column(name = "parameter_value")
	private List<String> parameters = new ArrayList<>();

	private String templateName;

	private String firstScreenName;

	private String language;

	private String wamid;

	private String responseStatus;

	private String headerParameter;

	private String urlParameter;

	private boolean hasHeaderParameter;

	private boolean hasBodyParameters;

	private Boolean hasUrlParameter;

	@Enumerated(EnumType.STRING)
	private TaskType type;

	@Lob
	private String input;

	@Lob
	private String output;

	public Date getCreated() {
		return created;
	}

	public void setCreated(Date created) {
		this.created = created;
	}

	public Status getStatus() {
		return status;
	}

	public void setStatus(Status status) {
		this.status = status;
	}

	public Date getSchedule() {
		return schedule;
	}

	public void setSchedule(Date schedule) {
		this.schedule = schedule;
	}

	public Date getProcessedDate() {
		return processedDate;
	}

	public void setProcessedDate(Date processedDate) {
		this.processedDate = processedDate;
	}

	public String getTo() {
		return to;
	}

	public void setTo(String to) {
		this.to = to;
	}

	public List<String> getParameters() {
		return parameters;
	}

	public void setParameters(List<String> parameters) {
		this.parameters = parameters;
	}

	public String getTemplateName() {
		return templateName;
	}

	public void setTemplateName(String templateName) {
		this.templateName = templateName;
	}

	public String getFirstScreenName() {
		return firstScreenName;
	}

	public void setFirstScreenName(String firstScreenName) {
		this.firstScreenName = firstScreenName;
	}

	public String getLanguage() {
		return language;
	}

	public void setLanguage(String language) {
		this.language = language;
	}

	public String getWamid() {
		return wamid;
	}

	public void setWamid(String wamid) {
		this.wamid = wamid;
	}

	public String getResponseStatus() {
		return responseStatus;
	}

	public void setResponseStatus(String responseStatus) {
		this.responseStatus = responseStatus;
	}

	public String getInput() {
		return input;
	}

	public void setInput(String input) {
		this.input = input;
	}

	public String getOutput() {
		return output;
	}

	public void setOutput(String output) {
		this.output = output;
	}

	public String getHeaderParameter() {
		return headerParameter;
	}

	public void setHeaderParameter(String headerParameter) {
		this.headerParameter = headerParameter;
	}

	public String getUrlParameter() {
		return urlParameter;
	}

	public void setUrlParameter(String urlParameter) {
		this.urlParameter = urlParameter;
	}

	public boolean isHasHeaderParameter() {
		return hasHeaderParameter;
	}

	public void setHasHeaderParameter(boolean hasHeaderParameter) {
		this.hasHeaderParameter = hasHeaderParameter;
	}

	public boolean isHasBodyParameters() {
		return hasBodyParameters;
	}

	public void setHasBodyParameters(boolean hasBodyParameters) {
		this.hasBodyParameters = hasBodyParameters;
	}

	public Boolean getHasUrlParameter() {
		return hasUrlParameter;
	}

	public void setHasUrlParameter(Boolean hasUrlParameter) {
		this.hasUrlParameter = hasUrlParameter;
	}

	public TaskType getType() {
		return type;
	}

	public void setType(TaskType type) {
		this.type = type;
	}
}
