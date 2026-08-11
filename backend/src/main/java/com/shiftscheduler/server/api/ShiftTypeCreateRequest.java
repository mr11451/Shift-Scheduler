package com.shiftscheduler.server.api;

import java.time.LocalTime;

public class ShiftTypeCreateRequest {

  private String shiftCode;
  private String shiftName;
  private LocalTime startTime;
  private LocalTime endTime;
  private Boolean isOffType;
  private Integer sortOrder;

  public ShiftTypeCreateRequest() {}

  public ShiftTypeCreateRequest(String shiftCode, String shiftName, LocalTime startTime, LocalTime endTime, Boolean isOffType, Integer sortOrder) {
    this.shiftCode = shiftCode;
    this.shiftName = shiftName;
    this.startTime = startTime;
    this.endTime = endTime;
    this.isOffType = isOffType;
    this.sortOrder = sortOrder;
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

  @Override
  public String toString() {
    return "ShiftTypeCreateRequest{"
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
        + '}';
  }
}
