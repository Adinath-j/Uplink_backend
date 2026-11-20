package com.example.Uplink_Backend.modules.auth.repository;

import com.example.Uplink_Backend.modules.auth.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    Optional<User> findByPhone(String phone);
    boolean existsByEmail(String email);
    boolean existsByPhone(String phone);
    Page<User> findByIsDeletedFalse(Pageable pageable);
    long countByIsActiveTrue();

    @Query("SELECT u FROM User u WHERE u.id <> :excludeUserId AND (u.isDeleted = false OR u.isDeleted IS NULL) AND (u.isActive = true OR u.isActive IS NULL) AND " +
           "(LOWER(u.fullName) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(u.email) LIKE LOWER(CONCAT('%', :query, '%')))")
    List<User> searchUsers(@Param("query") String query, @Param("excludeUserId") UUID excludeUserId, Pageable pageable);

    @Query("SELECT u FROM User u WHERE u.id <> :excludeUserId AND (u.isDeleted = false OR u.isDeleted IS NULL) AND (u.isActive = true OR u.isActive IS NULL) ORDER BY u.fullName ASC")
    List<User> findSuggestedUsers(@Param("excludeUserId") UUID excludeUserId, Pageable pageable);
}
