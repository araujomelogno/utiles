package uy.com.bay.utiles.repo;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import uy.com.bay.utiles.entities.OdooCost;

@Repository
public interface OdooCostRepository extends JpaRepository<OdooCost, Long> {

    Optional<OdooCost> findByMoveId(String moveId);
}
