package com.shiftscheduler.server.api;

import java.time.LocalDate;

public class ShiftRequestCreateRequest {

  private LocalDate workDate;
  private Long desiredShiftTypeId;
  private Boolean isVacation;

  public ShiftRequestCreateRequest() {}

  public ShiftRequestCreateRequest(LocalDate workDate, Long desiredShiftTypeId) {
    this.workDate = workDate;
    this.desiredShiftTypeId = desiredShiftTypeId;
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

  public Boolean getIsVacation() {
    return isVacation;
  }

  public void setIsVacation(Boolean isVacation) {
    this.isVacation = isVacation;
  }

  @Override
  public String toString() {
    return "ShiftRequestCreateRequest{"
        + "workDate="
        + workDate
        + ", desiredShiftTypeId="
        + desiredShiftTypeId
        + ", isVacation="
        + isVacation
        + '}';
  }
}
