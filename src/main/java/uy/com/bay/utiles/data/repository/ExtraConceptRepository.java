package uy.com.bay.utiles.data.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import uy.com.bay.utiles.data.ExtraConcept;

public interface ExtraConceptRepository extends JpaRepository<ExtraConcept, Long>, JpaSpecificationExecutor<ExtraConcept> {
}