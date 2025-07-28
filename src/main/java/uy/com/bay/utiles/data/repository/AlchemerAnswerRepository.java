package uy.com.bay.utiles.data.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import uy.com.bay.utiles.data.AlchemerAnswer;

public interface AlchemerAnswerRepository extends JpaRepository<AlchemerAnswer, Long>, JpaSpecificationExecutor<AlchemerAnswer> {
}
