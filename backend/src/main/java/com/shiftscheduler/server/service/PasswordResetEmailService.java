package com.shiftscheduler.server.service;

/**
 * Sends password-reset and initial-login emails; implementations may be a no-op when
 * SMTP is not configured (see {@link UnavailablePasswordResetEmailService}).
 */
public interface PasswordResetEmailService {
    /**
     * Whether this implementation can actually deliver email (vs. just logging/no-op).
     */
    boolean isAvailable();

    /**
     * Send a password-reset link and verification code to the staff member.
     */
    void send(String to, String staffName, String accessUrl, String verificationCode);

    /**
     * Send initial login credentials to a newly provisioned member.
     */
    void sendInitialLogin(String to, String staffName, String accessUrl, String loginCode, String initialPassword);
}