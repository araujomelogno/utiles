package uy.com.bay.utiles.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uy.com.bay.utiles.entities.BudgetEntry;

@Repository
public interface BudgetEntryRepository extends JpaRepository<BudgetEntry, Long> {
}