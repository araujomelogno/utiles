package uy.com.bay.utiles.dto.aiencoding;

public class QuestionAICode {
	private String code;

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	// contains() usará esto
	@Override
	public boolean equals(Object o) {

		if (this == o)
			return true;

		if (o == null || getClass() != o.getClass())
			return false;

		QuestionAICode otro = (QuestionAICode) o;

		// comparar SOLO por id
		return code != null && code.equals(otro.getCode());
	}

	@Override
	public int hashCode() {
		return code != null ? code.hashCode() : 0;
	}

}
