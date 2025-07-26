package uy.com.bay.utiles.data.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uy.com.bay.utiles.data.entity.Task;

public interface TaskRepository extends JpaRepository<Task, Long> {
}
