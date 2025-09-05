package uy.com.bay.utiles.data.repository;

import org.springframework.data.jpa.domain.Specification;
import uy.com.bay.utiles.data.ExpenseReport;

public interface ExpenseReportRepositoryCustom {
    Double sumAmount(Specification<ExpenseReport> spec);
}
