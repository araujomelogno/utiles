package uy.com.bay.utiles.data.service;

import org.springframework.stereotype.Service;

import uy.com.bay.utiles.data.Task;
import uy.com.bay.utiles.data.repository.TaskRepository;

import java.util.List;

@Service
public class TaskService {

    private final TaskRepository taskRepository;

    public TaskService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    public List<Task> findAll() {
        return taskRepository.findAll();
    }
}
