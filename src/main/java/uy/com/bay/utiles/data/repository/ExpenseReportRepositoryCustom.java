package uy.com.bay.utiles.data.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import uy.com.bay.utiles.data.ExpenseReport;
import uy.com.bay.utiles.data.ExpenseReportDTO;

public interface ExpenseReportRepositoryCustom {
    Double sumAmount(Specification<ExpenseReport> spec);

    Page<ExpenseReportDTO> findAllDtos(Specification<ExpenseReport> spec, Pageable pageable);
}
