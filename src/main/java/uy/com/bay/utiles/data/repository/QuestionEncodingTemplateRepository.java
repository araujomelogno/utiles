package uy.com.bay.utiles.data.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import uy.com.bay.utiles.data.QuestionEncodingTemplate;

@Repository
public interface QuestionEncodingTemplateRepository extends JpaRepository<QuestionEncodingTemplate, Long> {
}
