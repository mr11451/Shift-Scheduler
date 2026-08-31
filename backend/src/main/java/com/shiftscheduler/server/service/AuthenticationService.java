package com.shiftscheduler.server.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shiftscheduler.server.domain.PasswordResetToken;
import com.shiftscheduler.server.domain.Staff;
import com.shiftscheduler.server.dto.LoginRequest;
import com.shiftscheduler.server.dto.LoginResponse;
import com.shiftscheduler.server.dto.PasswordResetRequestResponse;
import com.shiftscheduler.server.repository.PasswordResetTokenRepository;
import com.shiftscheduler.server.repository.StaffRepository;
import com.shiftscheduler.server.util.JwtTokenUtil;
import com.shiftscheduler.server.util.PasswordUtil;

@Service
public class AuthenticationService {
    
    @Autowired
    private StaffRepository staffRepository;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private PasswordResetEmailService passwordResetEmailService;

    @Value("${app.password-reset.base-url}")
    private String passwordResetBaseUrl;

    private final SecureRandom secureRandom = new SecureRandom();
    
    /**
     * Authenticate a staff by staff code/password and issue a JWT on success.
     */
    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        // Find staff by staff code
        Staff staff = staffRepository.findByStaffCode(request.staffCode())
            .orElseThrow(() -> new IllegalArgumentException("スタッフコードまたはパスワードが正しくありません。"));
        
        // Check if staff is active
        if (!staff.getIsActive()) {
            throw new IllegalArgumentException("このスタッフアカウントは無効です。");
        }
        
        // Verify password
        if (staff.getPasswordHash() == null || !PasswordUtil.verifyPassword(request.password(), staff.getPasswordHash())) {
            throw new IllegalArgumentException("スタッフコードまたはパスワードが正しくありません。");
        }
        
        // Generate JWT token
        String token = JwtTokenUtil.generateToken(staff.getId(), staff.getStaffCode(), staff.getRoleLevel().name());
        
        return new LoginResponse(
            staff.getId(),
            staff.getStaffCode(),
            staff.getStaffName(),
            staff.getRoleLevel().name(),
            token
        );
    }

    /**
     * Issue a password reset token/verification code and email it, or return it directly
     * when the email service is unavailable.
     */
    @Transactional
    public PasswordResetRequestResponse requestPasswordReset(Long staffId) {
        if (staffId == null) {
            throw new IllegalArgumentException("認証情報を確認できません。");
        }

        Staff staff = staffRepository.findById(staffId)
            .orElseThrow(() -> new IllegalArgumentException("スタッフが見つかりません。"));
        if (!staff.getIsActive()) {
            throw new IllegalArgumentException("このスタッフアカウントは無効です。");
        }
        if (staff.getEmail() == null || staff.getEmail().isBlank()) {
            throw new IllegalArgumentException("登録メールアドレスがありません。管理者にお問い合わせください。");
        }

        OffsetDateTime now = OffsetDateTime.now();
        passwordResetTokenRepository.findByStaffIdAndUsedAtIsNull(staffId)
            .forEach(existingToken -> existingToken.setUsedAt(now));

        String token = generateToken();
        String verificationCode = String.format("%06d", secureRandom.nextInt(1_000_000));
        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setStaff(staff);
        resetToken.setTokenHash(sha256(token));
        resetToken.setVerificationCodeHash(sha256(verificationCode));
        resetToken.setExpiresAt(now.plusHours(1));
        passwordResetTokenRepository.save(resetToken);

        String accessUrl = passwordResetBaseUrl.replaceAll("/+$", "") + "/" + staffId + "/" + token;
        if (!passwordResetEmailService.isAvailable()) {
            return new PasswordResetRequestResponse(
                false,
                "メール送信が設定されていないため、パスワード変更情報を表示します。",
                accessUrl,
                verificationCode
            );
        }

        try {
            passwordResetEmailService.send(staff.getEmail(), staff.getStaffName(), accessUrl, verificationCode);
            return new PasswordResetRequestResponse(
                true,
                "パスワード変更用のURLと確認コードをメールで送信しました。",
                null,
                null
            );
        } catch (RuntimeException e) {
            return new PasswordResetRequestResponse(
                false,
                "メールを送信できなかったため、パスワード変更情報を表示します。",
                accessUrl,
                verificationCode
            );
        }
    }

    /**
     * Validate a password reset token and verification code, then set the new password.
     */
    @Transactional
    public void resetPassword(Long staffId, String token, String verificationCode, String newPassword) {
        if (newPassword == null || newPassword.length() < 8) {
            throw new IllegalArgumentException("新しいパスワードは8文字以上で入力してください。");
        }

        PasswordResetToken resetToken = passwordResetTokenRepository
            .findByStaffIdAndTokenHashAndUsedAtIsNull(staffId, sha256(token))
            .orElseThrow(() -> new IllegalArgumentException("アクセスURLが無効か、すでに使用されています。"));
        if (!resetToken.getExpiresAt().isAfter(OffsetDateTime.now())) {
            throw new IllegalArgumentException("アクセスURLの有効期限が切れています。再度パスワード変更を依頼してください。");
        }
        if (!MessageDigest.isEqual(
                resetToken.getVerificationCodeHash().getBytes(StandardCharsets.UTF_8),
                sha256(verificationCode).getBytes(StandardCharsets.UTF_8))) {
            throw new IllegalArgumentException("確認コードが正しくありません。");
        }

        Staff staff = resetToken.getStaff();
        staff.setPasswordHash(PasswordUtil.hashPassword(newPassword));
        OffsetDateTime passwordChangedAt = OffsetDateTime.now();
        staff.setPasswordChangedAt(passwordChangedAt);
        resetToken.setUsedAt(passwordChangedAt);
    }

    /**
     * Generate a random URL-safe token used in password reset links.
     */
    private String generateToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * Hash a value with SHA-256 so raw tokens/codes are never stored.
     */
    private String sha256(String value) {
        try {
            return Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-256")
                .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("トークンの生成に失敗しました。", e);
        }
    }
}
