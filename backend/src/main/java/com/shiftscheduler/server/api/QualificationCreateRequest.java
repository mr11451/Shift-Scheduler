package com.shiftscheduler.server.api;

public class QualificationCreateRequest {

  private String qualificationName;
  private String description;

  public QualificationCreateRequest() {}

  public QualificationCreateRequest(String qualificationName, String description) {
    this.qualificationName = qualificationName;
    this.description = description;
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

  @Override
  public String toString() {
    return "QualificationCreateRequest{"
        + "qualificationName='"
        + qualificationName
        + '\''
        + ", description='"
        + description
        + '\''
        + '}';
  }
}
