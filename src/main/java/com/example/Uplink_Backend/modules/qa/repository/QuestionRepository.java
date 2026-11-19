package com.example.Uplink_Backend.modules.qa.repository;

import com.example.Uplink_Backend.modules.qa.entity.Question;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface QuestionRepository extends JpaRepository<Question, UUID> {

    Page<Question> findByAuthorId(UUID authorId, Pageable pageable);

    Page<Question> findByIsUnansweredTrue(Pageable pageable);

    @Query("SELECT q FROM Question q ORDER BY (q.sameDoubtCount + q.answerCount) DESC, q.createdAt DESC")
    Page<Question> findTrending(Pageable pageable);

    @Query("SELECT q FROM Question q WHERE " +
           "(:topic IS NULL OR LOWER(q.topicTags) LIKE LOWER(CONCAT('%', :topic, '%'))) AND " +
           "(:keyword IS NULL OR (LOWER(q.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(q.description) LIKE LOWER(CONCAT('%', :keyword, '%')))) " +
           "ORDER BY q.createdAt DESC")
    Page<Question> searchQuestions(@Param("topic") String topic,
                                   @Param("keyword") String keyword,
                                   Pageable pageable);
}

