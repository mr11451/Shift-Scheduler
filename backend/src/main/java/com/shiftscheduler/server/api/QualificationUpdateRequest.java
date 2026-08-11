package com.shiftscheduler.server.api;

public class QualificationUpdateRequest {

  private String qualificationName;
  private String description;
  private Boolean isActive;

  public QualificationUpdateRequest() {}

  public QualificationUpdateRequest(String qualificationName, String description, Boolean isActive) {
    this.qualificationName = qualificationName;
    this.description = description;
    this.isActive = isActive;
  }

  public String getQualificationName() {
    return qualificationName;
  }

  public void setQualificationName(String qualificationName) {
    this.qualificationName = qualificationName;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public Boolean getIsActive() {
    return isActive;
  }

  public void setIsActive(Boolean isActive) {
    this.isActive = isActive;
  }

  @Override
  public String toString() {
    return "QualificationUpdateRequest{"
        + "qualificationName='"
        + qualificationName
        + '\''
        + ", description='"
        + description
        + '\''
        + ", isActive="
        + isActive
        + '}';
  }
}
