package com.example.Uplink_Backend.modules.opportunity.repository;

import com.example.Uplink_Backend.modules.opportunity.entity.Opportunity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OpportunityRepository extends JpaRepository<Opportunity, UUID> {
    Page<Opportunity> findByIsActiveTrue(Pageable pageable);
    Page<Opportunity> findByTypeAndIsActiveTrue(String type, Pageable pageable);
    List<Opportunity> findByCompanyId(UUID companyId);

    @Query("SELECT o FROM Opportunity o WHERE o.isActive = true " +
            "AND (cast(:type as string) IS NULL OR LOWER(o.type) = LOWER(cast(:type as string))) " +
            "AND (cast(:workMode as string) IS NULL OR LOWER(o.workMode) = LOWER(cast(:workMode as string))) " +
            "AND (cast(:location as string) IS NULL OR LOWER(o.location) LIKE LOWER(CONCAT('%', cast(:location as string), '%'))) " +
            "AND (cast(:keyword as string) IS NULL OR LOWER(o.title) LIKE LOWER(CONCAT('%', cast(:keyword as string), '%')) OR LOWER(o.description) LIKE LOWER(CONCAT('%', cast(:keyword as string), '%')))")
    Page<Opportunity> searchOpportunities(@Param("type") String type,
                                          @Param("workMode") String workMode,
                                          @Param("location") String location,
                                          @Param("keyword") String keyword,
                                          Pageable pageable);

    long countByIsActiveTrue();
}
