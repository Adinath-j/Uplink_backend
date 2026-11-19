package com.example.Uplink_Backend.modules.opportunity.repository;

import com.example.Uplink_Backend.modules.opportunity.entity.SavedOpportunity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SavedOpportunityRepository extends JpaRepository<SavedOpportunity, UUID> {
    Optional<SavedOpportunity> findByUserIdAndOpportunityId(UUID userId, UUID opportunityId);
    List<SavedOpportunity> findByUserIdOrderByCreatedAtDesc(UUID userId);
    boolean existsByUserIdAndOpportunityId(UUID userId, UUID opportunityId);
    void deleteByUserIdAndOpportunityId(UUID userId, UUID opportunityId);
}

