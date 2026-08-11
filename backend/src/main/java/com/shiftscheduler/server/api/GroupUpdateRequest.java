package com.shiftscheduler.server.api;

public class GroupUpdateRequest {

  private String groupCode;
  private String groupName;
  private Boolean isActive;

  public GroupUpdateRequest() {}

  public GroupUpdateRequest(String groupCode, String groupName, Boolean isActive) {
    this.groupCode = groupCode;
    this.groupName = groupName;
    this.isActive = isActive;
  }

  public String getGroupCode() {
    return groupCode;
  }

  public void setGroupCode(String groupCode) {
    this.groupCode = groupCode;
  }

  public String getGroupName() {
    return groupName;
  }

  public void setGroupName(String groupName) {
    this.groupName = groupName;
  }

  public Boolean getIsActive() {
    return isActive;
  }

  public void setIsActive(Boolean isActive) {
    this.isActive = isActive;
  }

  @Override
  public String toString() {
    return "GroupUpdateRequest{"
        + "groupCode='"
        + groupCode
        + '\''
        + ", groupName='"
        + groupName
        + '\''
        + ", isActive="
        + isActive
        + '}';
  }
}
