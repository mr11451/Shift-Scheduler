package com.shiftscheduler.server.api;

import java.time.OffsetDateTime;

public class SystemSettingResponse {

  private String settingKey;
  private Boolean settingValueBoolean;
  private String settingValueText;
  private String updatedBy;
  private OffsetDateTime updatedAt;

  public SystemSettingResponse() {}

  public SystemSettingResponse(String settingKey, Boolean settingValueBoolean, String settingValueText, String updatedBy, OffsetDateTime updatedAt) {
    this.settingKey = settingKey;
    this.settingValueBoolean = settingValueBoolean;
    this.settingValueText = settingValueText;
    this.updatedBy = updatedBy;
    this.updatedAt = updatedAt;
  }

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

  public String getUpdatedBy() {
    return updatedBy;
  }

  public void setUpdatedBy(String updatedBy) {
    this.updatedBy = updatedBy;
  }

  public OffsetDateTime getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(OffsetDateTime updatedAt) {
    this.updatedAt = updatedAt;
  }

  @Override
  public String toString() {
    return "SystemSettingResponse{"
        + "settingKey='"
        + settingKey
        + '\''
        + ", settingValueBoolean="
        + settingValueBoolean
        + ", settingValueText='"
        + settingValueText
        + '\''
        + ", updatedBy='"
        + updatedBy
        + '\''
        + ", updatedAt="
        + updatedAt
        + '}';
  }
}
