package uy.com.bay.utiles.entities;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import uy.com.bay.utiles.data.AbstractEntity;
import uy.com.bay.utiles.data.Study;

@Entity
public class Budget extends AbstractEntity {

	private LocalDate created;

	@OneToMany(mappedBy = "budget", cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
	private Set<BudgetEntry> entries = new HashSet<>();

	@OneToOne
	private Study study;

	private String name;

	public LocalDate getCreated() {
		return created;
	}

	public void setCreated(LocalDate created) {
		this.created = created;
	}

	public Set<BudgetEntry> getEntries() {
		return entries;
	}

	public void setEntries(Set<BudgetEntry> entries) {
		this.entries = entries;
	}

	public Study getStudy() {
		return study;
	}

	public void setStudy(Study study) {
		this.study = study;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}