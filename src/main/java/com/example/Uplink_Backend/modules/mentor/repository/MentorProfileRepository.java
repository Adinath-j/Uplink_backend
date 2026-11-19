package com.example.Uplink_Backend.modules.mentor.repository;

import com.example.Uplink_Backend.modules.mentor.entity.MentorProfile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface MentorProfileRepository extends JpaRepository<MentorProfile, UUID> {

    @Query("SELECT m FROM MentorProfile m WHERE " +
           "(:domain IS NULL OR LOWER(m.domains) LIKE LOWER(CONCAT('%', :domain, '%'))) AND " +
           "(:keyword IS NULL OR (LOWER(m.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(m.organization) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(m.designation) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(m.skills) LIKE LOWER(CONCAT('%', :keyword, '%')))) " +
           "ORDER BY m.rating DESC, m.sessionsCompleted DESC")
    Page<MentorProfile> searchMentors(@Param("domain") String domain,
                                     @Param("keyword") String keyword,
                                     Pageable pageable);
}

