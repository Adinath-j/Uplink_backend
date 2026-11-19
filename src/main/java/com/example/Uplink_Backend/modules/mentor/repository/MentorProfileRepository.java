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
           "(CAST(:domain AS string) IS NULL OR LOWER(m.domains) LIKE LOWER(CONCAT('%', CAST(:domain AS string), '%'))) AND " +
           "(CAST(:keyword AS string) IS NULL OR (LOWER(m.name) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')) OR " +
           "LOWER(m.organization) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')) OR " +
           "LOWER(m.designation) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')) OR " +
           "LOWER(m.skills) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')))) " +
           "ORDER BY m.rating DESC, m.sessionsCompleted DESC")
    Page<MentorProfile> searchMentors(@Param("domain") String domain,
                                     @Param("keyword") String keyword,
                                     Pageable pageable);
}

