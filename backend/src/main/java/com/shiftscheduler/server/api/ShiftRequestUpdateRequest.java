package com.shiftscheduler.server.api;

public class ShiftRequestUpdateRequest {

  private Long desiredShiftTypeId;
  private Boolean isVacation;

  public ShiftRequestUpdateRequest() {}

  public ShiftRequestUpdateRequest(Long desiredShiftTypeId) {
    this.desiredShiftTypeId = desiredShiftTypeId;
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
    return "ShiftRequestUpdateRequest{"
        + "desiredShiftTypeId="
        + desiredShiftTypeId
        + ", isVacation="
        + isVacation
        + '}';
  }
}
