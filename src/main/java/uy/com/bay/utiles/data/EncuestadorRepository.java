package uy.com.bay.utiles.data;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface EncuestadorRepository extends JpaRepository<Encuestador, Long>, JpaSpecificationExecutor<Encuestador> {

}
