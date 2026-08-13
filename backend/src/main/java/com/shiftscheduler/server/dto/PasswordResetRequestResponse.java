package com.shiftscheduler.server.dto;

public record PasswordResetRequestResponse(
    boolean emailSent,
    String message,
    String accessUrl,
    String verificationCode
) {}