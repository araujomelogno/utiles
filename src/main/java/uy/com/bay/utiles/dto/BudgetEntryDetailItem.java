package uy.com.bay.utiles.dto;

import java.math.BigDecimal;

public class BudgetEntryDetailItem {

	private String tipo;
	private String detalle;
	private String surveyor;
	private String date;
	private BigDecimal cantidad;
	private BigDecimal costoUnitario;

	public BudgetEntryDetailItem(String tipo, String detalle, Number cantidad, Number costoUnitario, String surveyor,
			String date) {
		this.tipo = tipo;
		this.detalle = detalle;
		this.cantidad = cantidad != null ? new BigDecimal(String.valueOf(cantidad)) : BigDecimal.ZERO;
		this.costoUnitario = costoUnitario != null ? new BigDecimal(String.valueOf(costoUnitario)) : BigDecimal.ZERO;
		this.surveyor = surveyor;
		this.date = date;
		
	}

	public String getTipo() {
		return tipo;
	}

	public void setTipo(String tipo) {
		this.tipo = tipo;
	}

	public String getDetalle() {
		return detalle;
	}

	public void setDetalle(String detalle) {
		this.detalle = detalle;
	}

	public BigDecimal getCantidad() {
		return cantidad;
	}

	public void setCantidad(BigDecimal cantidad) {
		this.cantidad = cantidad;
	}

	public BigDecimal getCostoUnitario() {
		return costoUnitario;
	}

	public void setCostoUnitario(BigDecimal costoUnitario) {
		this.costoUnitario = costoUnitario;
	}

	public BigDecimal getTotal() {
		return this.cantidad.multiply(this.costoUnitario);
	}

	public String getDate() {
		return date;
	}

	public void setDate(String date) {
		this.date = date;
	}

	public String getSurveyor() {
		return surveyor;
	}

	public void setSurveyor(String surveyor) {
		this.surveyor = surveyor;
	}
}