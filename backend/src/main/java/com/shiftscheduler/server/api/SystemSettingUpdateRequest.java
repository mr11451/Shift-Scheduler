package com.shiftscheduler.server.api;

public class SystemSettingUpdateRequest {

  private String settingKey;
  private Boolean settingValueBoolean;
  private String settingValueText;

  public SystemSettingUpdateRequest() {}

  public SystemSettingUpdateRequest(String settingKey, Boolean settingValueBoolean, String settingValueText) {
    this.settingKey = settingKey;
    this.settingValueBoolean = settingValueBoolean;
    this.settingValueText = settingValueText;
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

  @Override
  public String toString() {
    return "SystemSettingUpdateRequest{"
        + "settingKey='"
        + settingKey
        + '\''
        + ", settingValueBoolean="
        + settingValueBoolean
        + ", settingValueText='"
        + settingValueText
        + '\''
        + '}';
  }
}
