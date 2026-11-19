package com.example.Uplink_Backend.modules.qa.repository;

import com.example.Uplink_Backend.modules.qa.entity.AnswerHelpful;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AnswerHelpfulRepository extends JpaRepository<AnswerHelpful, UUID> {
    Optional<AnswerHelpful> findByAnswerIdAndUserId(UUID answerId, UUID userId);
    boolean existsByAnswerIdAndUserId(UUID answerId, UUID userId);
    void deleteByAnswerIdAndUserId(UUID answerId, UUID userId);
    long countByAnswerId(UUID answerId);
}

