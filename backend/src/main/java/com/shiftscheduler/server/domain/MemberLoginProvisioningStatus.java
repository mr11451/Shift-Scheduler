package com.shiftscheduler.server.domain;

public enum MemberLoginProvisioningStatus {
    ISSUED("発行済"),
    SENT("送信済"),
    FAILED("送信失敗"),
    EXPIRED("期限切れ");

    private final String displayName;

    MemberLoginProvisioningStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
