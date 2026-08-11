package com.shiftscheduler.server.api;

public class CalendarViewPermissionUpdateRequest {

  private Long targetStaffId;

  public CalendarViewPermissionUpdateRequest() {}

  public CalendarViewPermissionUpdateRequest(Long targetStaffId) {
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
    return "CalendarViewPermissionUpdateRequest{"
        + "targetStaffId="
        + targetStaffId
        + '}';
  }
}
