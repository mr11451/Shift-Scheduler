package com.shiftscheduler.server.domain;

public enum RoleLevel {
    MEMBER("メンバ"),
    CHIEF("チーフ"),
    MASTER("マスタ");

    private final String displayName;

    RoleLevel(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
