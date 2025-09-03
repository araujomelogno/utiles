package uy.com.bay.utiles.data.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import uy.com.bay.utiles.data.Fieldwork;

public interface FieldworkRepository extends JpaRepository<Fieldwork, Long>, JpaSpecificationExecutor<Fieldwork> {
	Optional<Fieldwork> findByAlchemerId(String alchemerId);

	Optional<Fieldwork> findByDoobloId(String doobloId);
}
