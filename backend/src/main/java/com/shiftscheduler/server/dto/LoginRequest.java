package com.shiftscheduler.server.dto;

public record LoginRequest(
    String staffCode,
    String password
) {}
