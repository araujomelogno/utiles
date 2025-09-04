package uy.com.bay.utiles.data.repository;

import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import uy.com.bay.utiles.data.ExpenseRequest;

public class ExpenseRequestRepositoryImpl implements ExpenseRequestRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Double sumAmount(Specification<ExpenseRequest> spec) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Double> query = cb.createQuery(Double.class);
        Root<ExpenseRequest> root = query.from(ExpenseRequest.class);

        query.select(cb.coalesce(cb.sum(root.get("amount")), 0.0));

        if (spec != null) {
            Predicate predicate = spec.toPredicate(root, query, cb);
            if (predicate != null) {
                query.where(predicate);
            }
        }

        return entityManager.createQuery(query).getSingleResult();
    }
}
