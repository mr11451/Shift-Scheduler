package com.shiftscheduler.server.domain;

import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "system_settings")
public class SystemSetting {
    @Id
    @Column(length = 100)
    private String settingKey;

    @Column
    private Boolean settingValueBoolean;

    @Column(columnDefinition = "TEXT")
    private String settingValueText;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by_staff_id")
    private Staff updatedBy;

    @Column(nullable = false)
    private OffsetDateTime updatedAt;

    // Constructors
    public SystemSetting() {}

    public SystemSetting(String settingKey) {
        this.settingKey = settingKey;
    }

    public SystemSetting(String settingKey, Boolean settingValueBoolean) {
        this.settingKey = settingKey;
        this.settingValueBoolean = settingValueBoolean;
    }

    public SystemSetting(String settingKey, String settingValueText) {
        this.settingKey = settingKey;
        this.settingValueText = settingValueText;
    }

    // Getters and Setters
    public String getSettingKey() {
        return settingKey;
    }

    public void setSettingKey(String settingKey) {
        this.settingKey = settingKey;
    }

    public Boolean getSettingValueBoolean() {
        return settingValueBoolean;
    }

    public void setSettingValueBoolean(Boolean settingValueBoolean) {
        this.settingValueBoolean = settingValueBoolean;
    }

    public String getSettingValueText() {
        return settingValueText;
    }

    public void setSettingValueText(String settingValueText) {
        this.settingValueText = settingValueText;
    }

    public Staff getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(Staff updatedBy) {
        this.updatedBy = updatedBy;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
