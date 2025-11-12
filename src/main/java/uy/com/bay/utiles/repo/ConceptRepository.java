package uy.com.bay.utiles.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import uy.com.bay.utiles.entities.Concept;

public interface ConceptRepository extends JpaRepository<Concept, Long> {
}
