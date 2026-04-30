package uy.com.bay.utiles.data.service;

import java.util.List;

import org.springframework.stereotype.Service;

import uy.com.bay.utiles.data.QuestionEncodingTemplate;
import uy.com.bay.utiles.data.repository.QuestionEncodingTemplateRepository;

@Service
public class QuestionEncodingTemplateService {

    private final QuestionEncodingTemplateRepository repository;

    public QuestionEncodingTemplateService(QuestionEncodingTemplateRepository repository) {
        this.repository = repository;
    }

    public QuestionEncodingTemplate save(QuestionEncodingTemplate template) {
        return repository.save(template);
    }

    public List<QuestionEncodingTemplate> findAll() {
        return repository.findAll();
    }

    public void delete(QuestionEncodingTemplate template) {
        repository.delete(template);
    }
}
