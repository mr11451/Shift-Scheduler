package com.shiftscheduler.server.api;

public class QualificationResponse {

  private Long id;
  private String qualificationName;
  private String description;
  private Boolean isActive;

  public QualificationResponse() {}

  public QualificationResponse(Long id, String qualificationName, String description, Boolean isActive) {
    this.id = id;
    this.qualificationName = qualificationName;
    this.description = description;
    this.isActive = isActive;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
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
    return "QualificationResponse{"
        + "id="
        + id
        + ", qualificationName='"
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
