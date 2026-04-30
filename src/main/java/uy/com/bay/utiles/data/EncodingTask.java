package uy.com.bay.utiles.data;

import java.util.Date;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Lob;

@Entity
public class EncodingTask extends AbstractEntity {

	private Date created;

	@Enumerated(EnumType.STRING)
	private Status status;

	private String surveyFileName;

	@Lob
	private byte[] surveyFileContent;

	@Lob
	private byte[] codeMappingFileContent;

	@Lob
	private byte[] encodedBaseFile;

	private Date processed;

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

	public String getSurveyFileName() {
		return surveyFileName;
	}

	public void setSurveyFileName(String surveyFileName) {
		this.surveyFileName = surveyFileName;
	}

	public byte[] getSurveyFileContent() {
		return surveyFileContent;
	}

	public void setSurveyFileContent(byte[] surveyFileContent) {
		this.surveyFileContent = surveyFileContent;
	}

	public byte[] getCodeMappingFileContent() {
		return codeMappingFileContent;
	}

	public void setCodeMappingFileContent(byte[] codeMappingFileContent) {
		this.codeMappingFileContent = codeMappingFileContent;
	}

	public byte[] getEncodedBaseFile() {
		return encodedBaseFile;
	}

	public void setEncodedBaseFile(byte[] encodedBaseFile) {
		this.encodedBaseFile = encodedBaseFile;
	}

	public Date getProcessed() {
		return processed;
	}

	public void setProcessed(Date processed) {
		this.processed = processed;
	}

}
