package com.shiftscheduler.server.api;

import java.time.LocalDate;

public class ShiftAssignmentCreateRequest {

  private Long staffId;
  private LocalDate workDate;
  private Long shiftTypeId;
  private String note;

  public ShiftAssignmentCreateRequest() {}

  public ShiftAssignmentCreateRequest(Long staffId, LocalDate workDate, Long shiftTypeId, String note) {
    this.staffId = staffId;
    this.workDate = workDate;
    this.shiftTypeId = shiftTypeId;
    this.note = note;
  }

  public Long getStaffId() {
    return staffId;
  }

  public void setStaffId(Long staffId) {
    this.staffId = staffId;
  }

  public LocalDate getWorkDate() {
    return workDate;
  }

  public void setWorkDate(LocalDate workDate) {
    this.workDate = workDate;
  }

  public Long getShiftTypeId() {
    return shiftTypeId;
  }

  public void setShiftTypeId(Long shiftTypeId) {
    this.shiftTypeId = shiftTypeId;
  }

  public String getNote() {
    return note;
  }

  public void setNote(String note) {
    this.note = note;
  }

  @Override
  public String toString() {
    return "ShiftAssignmentCreateRequest{"
        + "staffId="
        + staffId
        + ", workDate="
        + workDate
        + ", shiftTypeId="
        + shiftTypeId
        + ", note='"
        + note
        + '\''
        + '}';
  }
}
