package com.example.Uplink_Backend.modules.qa.repository;

import com.example.Uplink_Backend.modules.qa.entity.QuestionDoubt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface QuestionDoubtRepository extends JpaRepository<QuestionDoubt, UUID> {
    Optional<QuestionDoubt> findByQuestionIdAndUserId(UUID questionId, UUID userId);
    boolean existsByQuestionIdAndUserId(UUID questionId, UUID userId);
    void deleteByQuestionIdAndUserId(UUID questionId, UUID userId);
    long countByQuestionId(UUID questionId);
}

