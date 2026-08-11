package com.shiftscheduler.server.domain;

public enum ShiftRequestStatus {
    DRAFT("下書き"),
    SUBMITTED("提出済"),
    APPLIED("反映済"),
    REJECTED("不採用");

    private final String displayName;

    ShiftRequestStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
