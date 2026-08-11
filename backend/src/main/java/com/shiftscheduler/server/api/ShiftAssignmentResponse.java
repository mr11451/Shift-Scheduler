package com.shiftscheduler.server.api;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public class ShiftAssignmentResponse {

  private Long id;
  private Long staffId;
  private String staffName;
  private LocalDate workDate;
  private Long shiftTypeId;
  private String shiftCode;
  private String shiftName;
  private String note;
  private String updatedBy;
  private OffsetDateTime updatedAt;

  public ShiftAssignmentResponse() {}

  public ShiftAssignmentResponse(Long id, Long staffId, String staffName, LocalDate workDate, Long shiftTypeId, String shiftCode, String shiftName, String note, String updatedBy, OffsetDateTime updatedAt) {
    this.id = id;
    this.staffId = staffId;
    this.staffName = staffName;
    this.workDate = workDate;
    this.shiftTypeId = shiftTypeId;
    this.shiftCode = shiftCode;
    this.shiftName = shiftName;
    this.note = note;
    this.updatedBy = updatedBy;
    this.updatedAt = updatedAt;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Long getStaffId() {
    return staffId;
  }

  public void setStaffId(Long staffId) {
    this.staffId = staffId;
  }

  public String getStaffName() {
    return staffName;
  }

  public void setStaffName(String staffName) {
    this.staffName = staffName;
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

  public String getNote() {
    return note;
  }

  public void setNote(String note) {
    this.note = note;
  }

  public String getUpdatedBy() {
    return updatedBy;
  }

  public void setUpdatedBy(String updatedBy) {
    this.updatedBy = updatedBy;
  }

  public OffsetDateTime getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(OffsetDateTime updatedAt) {
    this.updatedAt = updatedAt;
  }

  @Override
  public String toString() {
    return "ShiftAssignmentResponse{"
        + "id="
        + id
        + ", staffId="
        + staffId
        + ", staffName='"
        + staffName
        + '\''
        + ", workDate="
        + workDate
        + ", shiftTypeId="
        + shiftTypeId
        + ", shiftCode='"
        + shiftCode
        + '\''
        + ", shiftName='"
        + shiftName
        + '\''
        + ", note='"
        + note
        + '\''
        + ", updatedBy='"
        + updatedBy
        + '\''
        + ", updatedAt="
        + updatedAt
        + '}';
  }
}
