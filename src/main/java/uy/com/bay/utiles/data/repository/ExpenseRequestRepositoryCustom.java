package uy.com.bay.utiles.data.repository;

import org.springframework.data.jpa.domain.Specification;
import uy.com.bay.utiles.data.ExpenseRequest;

public interface ExpenseRequestRepositoryCustom {
    Double sumAmount(Specification<ExpenseRequest> spec);
}
