package com.shiftscheduler.server.api;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class AutoShiftGenerationResultResponse {

  private int year;
  private int month;
  private LocalDate startDate;
  private LocalDate endDate;
  private int targetStaffCount;
  private int generatedCount;
  private int skippedHolidayCount;
  private int consideredRequestCount;
  private int unassignedRequiredCount;
  private int retryCount;
  private List<UnmetCondition> unmetConditions = new ArrayList<>();

  public int getYear() {
    return year;
  }

  public void setYear(int year) {
    this.year = year;
  }

  public int getMonth() {
    return month;
  }

  public void setMonth(int month) {
    this.month = month;
  }

  public LocalDate getStartDate() {
    return startDate;
  }

  public void setStartDate(LocalDate startDate) {
    this.startDate = startDate;
  }

  public LocalDate getEndDate() {
    return endDate;
  }

  public void setEndDate(LocalDate endDate) {
    this.endDate = endDate;
  }

  public int getTargetStaffCount() {
    return targetStaffCount;
  }

  public void setTargetStaffCount(int targetStaffCount) {
    this.targetStaffCount = targetStaffCount;
  }

  public int getGeneratedCount() {
    return generatedCount;
  }

  public void setGeneratedCount(int generatedCount) {
    this.generatedCount = generatedCount;
  }

  public int getSkippedHolidayCount() {
    return skippedHolidayCount;
  }

  public void setSkippedHolidayCount(int skippedHolidayCount) {
    this.skippedHolidayCount = skippedHolidayCount;
  }

  public int getConsideredRequestCount() {
    return consideredRequestCount;
  }

  public void setConsideredRequestCount(int consideredRequestCount) {
    this.consideredRequestCount = consideredRequestCount;
  }

  public int getUnassignedRequiredCount() {
    return unassignedRequiredCount;
  }

  public void setUnassignedRequiredCount(int unassignedRequiredCount) {
    this.unassignedRequiredCount = unassignedRequiredCount;
  }

  public int getRetryCount() {
    return retryCount;
  }

  public void setRetryCount(int retryCount) {
    this.retryCount = retryCount;
  }

  public List<UnmetCondition> getUnmetConditions() {
    return unmetConditions;
  }

  public void setUnmetConditions(List<UnmetCondition> unmetConditions) {
    this.unmetConditions = unmetConditions != null ? unmetConditions : new ArrayList<>();
  }

  public static class UnmetCondition {
    private LocalDate workDate;
    private Long shiftTypeId;
    private String shiftTypeName;
    private int requiredCount;
    private int assignedCount;
    private int shortageCount;
    private String reason;

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

    public String getShiftTypeName() {
      return shiftTypeName;
    }

    public void setShiftTypeName(String shiftTypeName) {
      this.shiftTypeName = shiftTypeName;
    }

    public int getRequiredCount() {
      return requiredCount;
    }

    public void setRequiredCount(int requiredCount) {
      this.requiredCount = requiredCount;
    }

    public int getAssignedCount() {
      return assignedCount;
    }

    public void setAssignedCount(int assignedCount) {
      this.assignedCount = assignedCount;
    }

    public int getShortageCount() {
      return shortageCount;
    }

    public void setShortageCount(int shortageCount) {
      this.shortageCount = shortageCount;
    }

    public String getReason() {
      return reason;
    }

    public void setReason(String reason) {
      this.reason = reason;
    }
  }
}
