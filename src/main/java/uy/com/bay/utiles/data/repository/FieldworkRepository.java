package uy.com.bay.utiles.data.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uy.com.bay.utiles.data.Fieldwork;
import uy.com.bay.utiles.data.Study;

public interface FieldworkRepository extends JpaRepository<Fieldwork, Long>, JpaSpecificationExecutor<Fieldwork> {
	@Query("SELECT f FROM Fieldwork f JOIN f.alchemerId a WHERE a = :alchemerId")
	Optional<Fieldwork> findByAlchemerId(@Param("alchemerId") String alchemerId);

	@Query("SELECT f FROM Fieldwork f JOIN f.doobloId d WHERE d = :doobloId")
	Optional<Fieldwork> findByDoobloId(@Param("doobloId") String doobloId);

	List<Fieldwork> findAllByStudy(Study study);

	@Query("SELECT DISTINCT f FROM Fieldwork f " +
			"LEFT JOIN FETCH f.study s " +
			"LEFT JOIN FETCH s.budget b " +
			"LEFT JOIN FETCH b.entries be " +
			"LEFT JOIN FETCH be.extras " +
			"LEFT JOIN FETCH be.fieldworks " +
			"LEFT JOIN FETCH be.expenseRequests " +
			"LEFT JOIN FETCH be.odooCosts " +
			"WHERE f.initPlannedDate < :endDate AND f.endPlannedDate > :startDate")
	List<Fieldwork> findAllByInitPlannedDateLessThanAndEndPlannedDateGreaterThan(
			@Param("endDate") LocalDate endDate,
			@Param("startDate") LocalDate startDate);

	@Query("SELECT DISTINCT f FROM Fieldwork f WHERE f.initPlannedDate > :date AND SIZE(f.alchemerId) > 0")
	List<Fieldwork> findAllByInitPlannedDateAfterAndAlchemerIdIsNotNull(@Param("date") LocalDate date);
}