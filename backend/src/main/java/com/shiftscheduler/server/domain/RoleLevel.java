package com.shiftscheduler.server.domain;

public enum RoleLevel {
    MEMBER("メンバー"),
    CHIEF("チーフ"),
    MASTER("マスター");

    private final String displayName;

    RoleLevel(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
