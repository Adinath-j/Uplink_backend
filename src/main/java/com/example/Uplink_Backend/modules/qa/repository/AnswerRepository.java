package com.example.Uplink_Backend.modules.qa.repository;

import com.example.Uplink_Backend.modules.qa.entity.Answer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AnswerRepository extends JpaRepository<Answer, UUID> {
    List<Answer> findByQuestionIdOrderByIsAcceptedDescHelpfulCountDescCreatedAtAsc(UUID questionId);
    long countByQuestionId(UUID questionId);
}

