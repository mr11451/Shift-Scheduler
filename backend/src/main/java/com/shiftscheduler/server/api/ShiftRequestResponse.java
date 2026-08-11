package com.shiftscheduler.server.api;

import com.shiftscheduler.server.domain.ShiftRequestStatus;
import java.time.LocalDate;
import java.time.OffsetDateTime;

public class ShiftRequestResponse {

  private Long id;
  private Long staffId;
  private String staffName;
  private LocalDate workDate;
  private Long desiredShiftTypeId;
  private String desiredShiftCode;
  private String desiredShiftName;
  private ShiftRequestStatus status;
  private OffsetDateTime submittedAt;
  private OffsetDateTime decidedAt;

  public ShiftRequestResponse() {}

  public ShiftRequestResponse(Long id, Long staffId, String staffName, LocalDate workDate, Long desiredShiftTypeId, String desiredShiftCode, String desiredShiftName, ShiftRequestStatus status, OffsetDateTime submittedAt, OffsetDateTime decidedAt) {
    this.id = id;
    this.staffId = staffId;
    this.staffName = staffName;
    this.workDate = workDate;
    this.desiredShiftTypeId = desiredShiftTypeId;
    this.desiredShiftCode = desiredShiftCode;
    this.desiredShiftName = desiredShiftName;
    this.status = status;
    this.submittedAt = submittedAt;
    this.decidedAt = decidedAt;
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

  public Long getDesiredShiftTypeId() {
    return desiredShiftTypeId;
  }

  public void setDesiredShiftTypeId(Long desiredShiftTypeId) {
    this.desiredShiftTypeId = desiredShiftTypeId;
  }

  public String getDesiredShiftCode() {
    return desiredShiftCode;
  }

  public void setDesiredShiftCode(String desiredShiftCode) {
    this.desiredShiftCode = desiredShiftCode;
  }

  public String getDesiredShiftName() {
    return desiredShiftName;
  }

  public void setDesiredShiftName(String desiredShiftName) {
    this.desiredShiftName = desiredShiftName;
  }

  public ShiftRequestStatus getStatus() {
    return status;
  }

  public void setStatus(ShiftRequestStatus status) {
    this.status = status;
  }

  public OffsetDateTime getSubmittedAt() {
    return submittedAt;
  }

  public void setSubmittedAt(OffsetDateTime submittedAt) {
    this.submittedAt = submittedAt;
  }

  public OffsetDateTime getDecidedAt() {
    return decidedAt;
  }

  public void setDecidedAt(OffsetDateTime decidedAt) {
    this.decidedAt = decidedAt;
  }

  @Override
  public String toString() {
    return "ShiftRequestResponse{"
        + "id="
        + id
        + ", staffId="
        + staffId
        + ", staffName='"
        + staffName
        + '\''
        + ", workDate="
        + workDate
        + ", desiredShiftTypeId="
        + desiredShiftTypeId
        + ", desiredShiftCode='"
        + desiredShiftCode
        + '\''
        + ", desiredShiftName='"
        + desiredShiftName
        + '\''
        + ", status="
        + status
        + ", submittedAt="
        + submittedAt
        + ", decidedAt="
        + decidedAt
        + '}';
  }
}
