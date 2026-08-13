package com.shiftscheduler.server.dto;

public record InitialLoginInformation(
    boolean emailSent,
    String message,
    String accessUrl,
    String loginCode,
    String initialPassword
) {}