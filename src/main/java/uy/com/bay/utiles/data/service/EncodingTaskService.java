package uy.com.bay.utiles.data.service;

import java.util.Date;
import java.util.List;

import org.springframework.stereotype.Service;

import uy.com.bay.utiles.data.EncodingTask;
import uy.com.bay.utiles.data.Status;
import uy.com.bay.utiles.data.repository.EncodingTaskRepository;

@Service
public class EncodingTaskService {

    private final EncodingTaskRepository repository;

    public EncodingTaskService(EncodingTaskRepository repository) {
        this.repository = repository;
    }

    public List<EncodingTask> findByCreatedBetween(Date from, Date to, String fileName, Status status) {
        return repository.findByCreatedBetweenOrderByCreatedDesc(from, to, fileName, status);
    }

    public EncodingTask save(EncodingTask task) {
        return repository.save(task);
    }
}
