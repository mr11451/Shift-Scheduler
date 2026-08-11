package com.shiftscheduler.server.api;

public class ShiftAssignmentUpdateRequest {

  private Long shiftTypeId;
  private String note;

  public ShiftAssignmentUpdateRequest() {}

  public ShiftAssignmentUpdateRequest(Long shiftTypeId, String note) {
    this.shiftTypeId = shiftTypeId;
    this.note = note;
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
    return "ShiftAssignmentUpdateRequest{"
        + "shiftTypeId="
        + shiftTypeId
        + ", note='"
        + note
        + '\''
        + '}';
  }
}
