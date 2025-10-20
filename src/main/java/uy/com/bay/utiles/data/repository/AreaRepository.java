package uy.com.bay.utiles.data.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import uy.com.bay.utiles.data.Area;

public interface AreaRepository extends JpaRepository<Area, Long>, JpaSpecificationExecutor<Area> {
}
