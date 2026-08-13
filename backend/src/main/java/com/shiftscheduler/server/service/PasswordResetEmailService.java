package com.shiftscheduler.server.service;

public interface PasswordResetEmailService {
    boolean isAvailable();

    void send(String to, String staffName, String accessUrl, String verificationCode);

    void sendInitialLogin(String to, String staffName, String accessUrl, String loginCode, String initialPassword);
}