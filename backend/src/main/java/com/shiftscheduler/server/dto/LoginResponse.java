package com.shiftscheduler.server.dto;

public record LoginResponse(
    Long staffId,
    String staffCode,
    String staffName,
    String roleLevel,
    Long groupId,
    String token
) {}
