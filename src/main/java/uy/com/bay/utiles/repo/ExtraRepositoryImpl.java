package uy.com.bay.utiles.repo;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import uy.com.bay.utiles.data.ExtraConcept;
import uy.com.bay.utiles.data.Study;
import uy.com.bay.utiles.data.Surveyor;
import uy.com.bay.utiles.entities.Extra;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ExtraRepositoryImpl implements ExtraRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<Extra> findExtrasByFilters(LocalDate fechaDesde, LocalDate fechaHasta,
                                           List<Surveyor> encuestadores, List<Study> estudios,
                                           List<ExtraConcept> conceptos) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Extra> query = cb.createQuery(Extra.class);
        Root<Extra> extra = query.from(Extra.class);

        List<Predicate> predicates = new ArrayList<>();

        if (fechaDesde != null) {
            predicates.add(cb.greaterThanOrEqualTo(extra.get("date"), fechaDesde));
        }
        if (fechaHasta != null) {
            predicates.add(cb.lessThanOrEqualTo(extra.get("date"), fechaHasta));
        }
        if (encuestadores != null && !encuestadores.isEmpty()) {
            predicates.add(extra.get("surveyor").in(encuestadores));
        }
        if (estudios != null && !estudios.isEmpty()) {
            predicates.add(extra.get("study").in(estudios));
        }
        if (conceptos != null && !conceptos.isEmpty()) {
            predicates.add(extra.get("concept").in(conceptos));
        }

        query.where(predicates.toArray(new Predicate[0]));

        return entityManager.createQuery(query).getResultList();
    }
}