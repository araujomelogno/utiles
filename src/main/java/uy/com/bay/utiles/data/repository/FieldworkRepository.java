package uy.com.bay.utiles.data.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import uy.com.bay.utiles.data.Fieldwork;
import uy.com.bay.utiles.data.Study;

public interface FieldworkRepository extends JpaRepository<Fieldwork, Long>, JpaSpecificationExecutor<Fieldwork> {
	Optional<Study> findByAlchemerId(String alchemerId);

	Optional<Study> findByDoobloId(String doobloId);
}
