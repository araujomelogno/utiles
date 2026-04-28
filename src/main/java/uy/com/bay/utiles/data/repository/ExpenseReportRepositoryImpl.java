package uy.com.bay.utiles.data.repository;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import uy.com.bay.utiles.data.ExpenseReport;
import uy.com.bay.utiles.data.ExpenseReportDTO;
import uy.com.bay.utiles.data.ExpenseRequestType;
import uy.com.bay.utiles.data.Study;
import uy.com.bay.utiles.data.Surveyor;

public class ExpenseReportRepositoryImpl implements ExpenseReportRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Double sumAmount(Specification<ExpenseReport> spec) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Double> query = cb.createQuery(Double.class);
        Root<ExpenseReport> root = query.from(ExpenseReport.class);

        query.select(cb.coalesce(cb.sum(root.get("amount")), 0.0));

        if (spec != null) {
            Predicate predicate = spec.toPredicate(root, query, cb);
            if (predicate != null) {
                query.where(predicate);
            }
        }

        return entityManager.createQuery(query).getSingleResult();
    }

    @Override
    public Page<ExpenseReportDTO> findAllDtos(Specification<ExpenseReport> spec, Pageable pageable) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<ExpenseReportDTO> query = cb.createQuery(ExpenseReportDTO.class);
        Root<ExpenseReport> root = query.from(ExpenseReport.class);

        Join<ExpenseReport, Study> studyJoin = root.join("study", JoinType.LEFT);
        Join<ExpenseReport, Surveyor> surveyorJoin = root.join("surveyor", JoinType.LEFT);
        Join<ExpenseReport, ExpenseRequestType> conceptJoin = root.join("concept", JoinType.LEFT);

        query.select(cb.construct(ExpenseReportDTO.class,
                root.get("id"),
                studyJoin.get("name"),
                surveyorJoin.get("firstName"),
                surveyorJoin.get("lastName"),
                root.get("date"),
                root.get("amount"),
                conceptJoin.get("name"),
                root.get("expenseStatus")));

        if (spec != null) {
            Predicate predicate = spec.toPredicate(root, query, cb);
            if (predicate != null) {
                query.where(predicate);
            }
        }

        if (pageable.getSort().isSorted()) {
            List<Order> orders = new ArrayList<>();
            for (Sort.Order order : pageable.getSort()) {
                Path<?> path = resolveSortPath(root, studyJoin, surveyorJoin, conceptJoin, order.getProperty());
                orders.add(order.isAscending() ? cb.asc(path) : cb.desc(path));
            }
            query.orderBy(orders);
        }

        TypedQuery<ExpenseReportDTO> typedQuery = entityManager.createQuery(query);
        if (pageable.isPaged()) {
            typedQuery.setFirstResult((int) pageable.getOffset());
            typedQuery.setMaxResults(pageable.getPageSize());
        }

        List<ExpenseReportDTO> results = typedQuery.getResultList();
        long total = countDtos(spec);

        return new PageImpl<>(results, pageable, total);
    }

    private long countDtos(Specification<ExpenseReport> spec) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<ExpenseReport> countRoot = countQuery.from(ExpenseReport.class);
        countQuery.select(cb.count(countRoot));

        if (spec != null) {
            Predicate predicate = spec.toPredicate(countRoot, countQuery, cb);
            if (predicate != null) {
                countQuery.where(predicate);
            }
        }
        return entityManager.createQuery(countQuery).getSingleResult();
    }

    private Path<?> resolveSortPath(Root<ExpenseReport> root, Join<ExpenseReport, Study> studyJoin,
            Join<ExpenseReport, Surveyor> surveyorJoin, Join<ExpenseReport, ExpenseRequestType> conceptJoin,
            String property) {
        if (property == null || property.isEmpty()) {
            return root.get("id");
        }
        String[] parts = property.split("\\.");
        Path<?> base;
        switch (parts[0]) {
            case "study":
                base = studyJoin;
                break;
            case "surveyor":
                base = surveyorJoin;
                break;
            case "concept":
                base = conceptJoin;
                break;
            default:
                base = root;
                return base.get(property);
        }
        for (int i = 1; i < parts.length; i++) {
            base = base.get(parts[i]);
        }
        return base;
    }
}
