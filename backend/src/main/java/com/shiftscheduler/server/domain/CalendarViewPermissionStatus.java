package com.shiftscheduler.server.domain;

public enum CalendarViewPermissionStatus {
    PENDING("申請中"),
    APPROVED("許可"),
    REJECTED("却下"),
    CANCELED("キャンセル"),
    EXPIRED("失効");

    private final String displayName;

    CalendarViewPermissionStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
