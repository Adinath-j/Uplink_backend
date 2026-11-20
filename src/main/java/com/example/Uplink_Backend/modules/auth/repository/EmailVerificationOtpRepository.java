package com.example.Uplink_Backend.modules.auth.repository;

import com.example.Uplink_Backend.modules.auth.entity.EmailVerificationOtp;
import com.example.Uplink_Backend.modules.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmailVerificationOtpRepository extends JpaRepository<EmailVerificationOtp, UUID> {

    Optional<EmailVerificationOtp> findTopByEmailIgnoreCaseOrderByCreatedAtDesc(String email);

    List<EmailVerificationOtp> findByEmailIgnoreCase(String email);

    @Modifying
    @Query("DELETE FROM EmailVerificationOtp o WHERE LOWER(o.email) = LOWER(:email)")
    void deleteByEmailIgnoreCase(@Param("email") String email);

    @Modifying
    void deleteByUser(User user);
}
