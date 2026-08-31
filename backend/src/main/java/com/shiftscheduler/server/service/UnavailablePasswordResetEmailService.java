package com.shiftscheduler.server.service;

import org.springframework.stereotype.Service;

/**
 * Fallback used when SMTP is not configured; every send attempt fails loudly
 * so callers fall back to displaying the reset link/code directly.
 */
@Service
public class UnavailablePasswordResetEmailService implements PasswordResetEmailService {
    @Override
    public boolean isAvailable() {
        return false;
    }

    @Override
    public void send(String to, String staffName, String accessUrl, String verificationCode) {
        throw new IllegalStateException("メール送信が設定されていません。");
    }

    @Override
    public void sendInitialLogin(String to, String staffName, String accessUrl, String loginCode, String initialPassword) {
        throw new IllegalStateException("メール送信が設定されていません。");
    }
}