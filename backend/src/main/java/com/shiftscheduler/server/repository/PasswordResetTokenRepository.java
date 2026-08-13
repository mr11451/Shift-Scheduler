package com.shiftscheduler.server.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.shiftscheduler.server.domain.PasswordResetToken;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    List<PasswordResetToken> findByStaffIdAndUsedAtIsNull(Long staffId);
    Optional<PasswordResetToken> findByStaffIdAndTokenHashAndUsedAtIsNull(Long staffId, String tokenHash);
}