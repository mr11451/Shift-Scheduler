package com.shiftscheduler.server.api;

public class CalendarViewPermissionCreateRequest {

  private Long targetStaffId;

  public CalendarViewPermissionCreateRequest() {}

  public CalendarViewPermissionCreateRequest(Long targetStaffId) {
    this.targetStaffId = targetStaffId;
  }

  public Long getTargetStaffId() {
    return targetStaffId;
  }

  public void setTargetStaffId(Long targetStaffId) {
    this.targetStaffId = targetStaffId;
  }

  @Override
  public String toString() {
    return "CalendarViewPermissionCreateRequest{"
        + "targetStaffId="
        + targetStaffId
        + '}';
  }
}
