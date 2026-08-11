package com.shiftscheduler.server.api;

public class GroupCreateRequest {

  private String groupCode;
  private String groupName;

  public GroupCreateRequest() {}

  public GroupCreateRequest(String groupCode, String groupName) {
    this.groupCode = groupCode;
    this.groupName = groupName;
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

  @Override
  public String toString() {
    return "GroupCreateRequest{"
        + "groupCode='"
        + groupCode
        + '\''
        + ", groupName='"
        + groupName
        + '\''
        + '}';
  }
}
