package uy.com.bay.utiles.data.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import uy.com.bay.utiles.data.Fieldwork;
import uy.com.bay.utiles.data.Study;

public interface FieldworkRepository extends JpaRepository<Fieldwork, Long>, JpaSpecificationExecutor<Fieldwork> {
	Optional<Fieldwork> findByAlchemerId(String alchemerId);

	Optional<Fieldwork> findByDoobloId(String doobloId);

	List<Fieldwork> findAllByStudy(Study study);

	List<Fieldwork> findAllByInitPlannedDateLessThanAndEndPlannedDateGreaterThan(LocalDate endDate,
			LocalDate startDate);

	List<Fieldwork> findAllByInitPlannedDateAfterAndAlchemerIdIsNotNull(LocalDate date);
}