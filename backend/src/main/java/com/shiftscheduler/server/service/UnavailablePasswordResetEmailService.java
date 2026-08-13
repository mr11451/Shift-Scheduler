package com.shiftscheduler.server.service;

import org.springframework.stereotype.Service;

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