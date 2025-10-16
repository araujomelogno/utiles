package uy.com.bay.utiles.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uy.com.bay.utiles.data.ExtraConcept;

@Repository
public interface ExtraConceptRepository extends JpaRepository<ExtraConcept, Integer> {
}