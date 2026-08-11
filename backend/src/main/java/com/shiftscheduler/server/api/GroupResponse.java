package com.shiftscheduler.server.api;

public class GroupResponse {

  private Long id;
  private String groupCode;
  private String groupName;
  private Boolean isActive;

  public GroupResponse() {}

  public GroupResponse(Long id, String groupCode, String groupName, Boolean isActive) {
    this.id = id;
    this.groupCode = groupCode;
    this.groupName = groupName;
    this.isActive = isActive;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
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
    return "GroupResponse{"
        + "id="
        + id
        + ", groupCode='"
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
