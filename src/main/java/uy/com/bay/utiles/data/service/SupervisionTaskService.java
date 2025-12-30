package uy.com.bay.utiles.data.service;

import org.springframework.stereotype.Service;
import uy.com.bay.utiles.data.Status;
import uy.com.bay.utiles.data.SupervisionTask;
import uy.com.bay.utiles.data.repository.SupervisionTaskRepository;

import java.util.Date;
import java.util.List;

@Service
public class SupervisionTaskService {

    private final SupervisionTaskRepository repository;

    public SupervisionTaskService(SupervisionTaskRepository repository) {
        this.repository = repository;
    }

    public List<SupervisionTask> findByCreatedBetween(Date from, Date to, String fileName, Status status) {
        return repository.findByCreatedBetweenOrderByCreatedDesc(from, to, fileName, status);
    }
}
