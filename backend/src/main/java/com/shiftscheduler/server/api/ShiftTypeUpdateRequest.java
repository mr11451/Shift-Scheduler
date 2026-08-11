package com.shiftscheduler.server.api;

import java.time.LocalTime;

public class ShiftTypeUpdateRequest {

  private String shiftCode;
  private String shiftName;
  private LocalTime startTime;
  private LocalTime endTime;
  private Boolean isOffType;
  private Integer sortOrder;
  private Boolean isActive;

  public ShiftTypeUpdateRequest() {}

  public ShiftTypeUpdateRequest(String shiftCode, String shiftName, LocalTime startTime, LocalTime endTime, Boolean isOffType, Integer sortOrder, Boolean isActive) {
    this.shiftCode = shiftCode;
    this.shiftName = shiftName;
    this.startTime = startTime;
    this.endTime = endTime;
    this.isOffType = isOffType;
    this.sortOrder = sortOrder;
    this.isActive = isActive;
  }

  public String getShiftCode() {
    return shiftCode;
  }

  public void setShiftCode(String shiftCode) {
    this.shiftCode = shiftCode;
  }

  public String getShiftName() {
    return shiftName;
  }

  public void setShiftName(String shiftName) {
    this.shiftName = shiftName;
  }

  public LocalTime getStartTime() {
    return startTime;
  }

  public void setStartTime(LocalTime startTime) {
    this.startTime = startTime;
  }

  public LocalTime getEndTime() {
    return endTime;
  }

  public void setEndTime(LocalTime endTime) {
    this.endTime = endTime;
  }

  public Boolean getIsOffType() {
    return isOffType;
  }

  public void setIsOffType(Boolean isOffType) {
    this.isOffType = isOffType;
  }

  public Integer getSortOrder() {
    return sortOrder;
  }

  public void setSortOrder(Integer sortOrder) {
    this.sortOrder = sortOrder;
  }

  public Boolean getIsActive() {
    return isActive;
  }

  public void setIsActive(Boolean isActive) {
    this.isActive = isActive;
  }

  @Override
  public String toString() {
    return "ShiftTypeUpdateRequest{"
        + "shiftCode='"
        + shiftCode
        + '\''
        + ", shiftName='"
        + shiftName
        + '\''
        + ", startTime="
        + startTime
        + ", endTime="
        + endTime
        + ", isOffType="
        + isOffType
        + ", sortOrder="
        + sortOrder
        + ", isActive="
        + isActive
        + '}';
  }
}
