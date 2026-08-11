package com.shiftscheduler.server.api;

import java.time.LocalTime;

public class ShiftTypeResponse {

  private Long id;
  private String shiftCode;
  private String shiftName;
  private LocalTime startTime;
  private LocalTime endTime;
  private Boolean isOffType;
  private Boolean isActive;
  private Integer sortOrder;

  public ShiftTypeResponse() {}

  public ShiftTypeResponse(Long id, String shiftCode, String shiftName, LocalTime startTime, LocalTime endTime, Boolean isOffType, Boolean isActive, Integer sortOrder) {
    this.id = id;
    this.shiftCode = shiftCode;
    this.shiftName = shiftName;
    this.startTime = startTime;
    this.endTime = endTime;
    this.isOffType = isOffType;
    this.isActive = isActive;
    this.sortOrder = sortOrder;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
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

  public Boolean getIsActive() {
    return isActive;
  }

  public void setIsActive(Boolean isActive) {
    this.isActive = isActive;
  }

  public Integer getSortOrder() {
    return sortOrder;
  }

  public void setSortOrder(Integer sortOrder) {
    this.sortOrder = sortOrder;
  }

  @Override
  public String toString() {
    return "ShiftTypeResponse{"
        + "id="
        + id
        + ", shiftCode='"
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
        + ", isActive="
        + isActive
        + ", sortOrder="
        + sortOrder
        + '}';
  }
}
