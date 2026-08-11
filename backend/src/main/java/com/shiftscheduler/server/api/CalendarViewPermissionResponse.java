package com.shiftscheduler.server.api;

import com.shiftscheduler.server.domain.CalendarViewPermissionStatus;
import java.time.OffsetDateTime;

public class CalendarViewPermissionResponse {

  private Long id;
  private Long requesterStaffId;
  private String requesterStaffName;
  private Long targetStaffId;
  private String targetStaffName;
  private CalendarViewPermissionStatus status;
  private OffsetDateTime requestedAt;
  private OffsetDateTime respondedAt;
  private OffsetDateTime expiredAt;

  public CalendarViewPermissionResponse() {}

  public CalendarViewPermissionResponse(Long id, Long requesterStaffId, String requesterStaffName, Long targetStaffId, String targetStaffName, CalendarViewPermissionStatus status, OffsetDateTime requestedAt, OffsetDateTime respondedAt, OffsetDateTime expiredAt) {
    this.id = id;
    this.requesterStaffId = requesterStaffId;
    this.requesterStaffName = requesterStaffName;
    this.targetStaffId = targetStaffId;
    this.targetStaffName = targetStaffName;
    this.status = status;
    this.requestedAt = requestedAt;
    this.respondedAt = respondedAt;
    this.expiredAt = expiredAt;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Long getRequesterStaffId() {
    return requesterStaffId;
  }

  public void setRequesterStaffId(Long requesterStaffId) {
    this.requesterStaffId = requesterStaffId;
  }

  public String getRequesterStaffName() {
    return requesterStaffName;
  }

  public void setRequesterStaffName(String requesterStaffName) {
    this.requesterStaffName = requesterStaffName;
  }

  public Long getTargetStaffId() {
    return targetStaffId;
  }

  public void setTargetStaffId(Long targetStaffId) {
    this.targetStaffId = targetStaffId;
  }

  public String getTargetStaffName() {
    return targetStaffName;
  }

  public void setTargetStaffName(String targetStaffName) {
    this.targetStaffName = targetStaffName;
  }

  public CalendarViewPermissionStatus getStatus() {
    return status;
  }

  public void setStatus(CalendarViewPermissionStatus status) {
    this.status = status;
  }

  public OffsetDateTime getRequestedAt() {
    return requestedAt;
  }

  public void setRequestedAt(OffsetDateTime requestedAt) {
    this.requestedAt = requestedAt;
  }

  public OffsetDateTime getRespondedAt() {
    return respondedAt;
  }

  public void setRespondedAt(OffsetDateTime respondedAt) {
    this.respondedAt = respondedAt;
  }

  public OffsetDateTime getExpiredAt() {
    return expiredAt;
  }

  public void setExpiredAt(OffsetDateTime expiredAt) {
    this.expiredAt = expiredAt;
  }

  @Override
  public String toString() {
    return "CalendarViewPermissionResponse{"
        + "id="
        + id
        + ", requesterStaffId="
        + requesterStaffId
        + ", requesterStaffName='"
        + requesterStaffName
        + '\''
        + ", targetStaffId="
        + targetStaffId
        + ", targetStaffName='"
        + targetStaffName
        + '\''
        + ", status="
        + status
        + ", requestedAt="
        + requestedAt
        + ", respondedAt="
        + respondedAt
        + ", expiredAt="
        + expiredAt
        + '}';
  }
}
