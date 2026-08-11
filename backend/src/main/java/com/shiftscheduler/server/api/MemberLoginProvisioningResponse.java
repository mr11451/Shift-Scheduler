package com.shiftscheduler.server.api;

import com.shiftscheduler.server.domain.MemberLoginProvisioningStatus;
import java.time.OffsetDateTime;

public class MemberLoginProvisioningResponse {

  private Long id;
  private Long staffId;
  private String staffName;
  private String loginCode;
  private String accessUrl;
  private MemberLoginProvisioningStatus status;
  private OffsetDateTime issuedAt;
  private OffsetDateTime expiresAt;
  private OffsetDateTime sentAt;
  private String lastErrorMessage;

  public MemberLoginProvisioningResponse() {}

  public MemberLoginProvisioningResponse(Long id, Long staffId, String staffName, String loginCode, String accessUrl, MemberLoginProvisioningStatus status, OffsetDateTime issuedAt, OffsetDateTime expiresAt, OffsetDateTime sentAt, String lastErrorMessage) {
    this.id = id;
    this.staffId = staffId;
    this.staffName = staffName;
    this.loginCode = loginCode;
    this.accessUrl = accessUrl;
    this.status = status;
    this.issuedAt = issuedAt;
    this.expiresAt = expiresAt;
    this.sentAt = sentAt;
    this.lastErrorMessage = lastErrorMessage;
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

  public String getLoginCode() {
    return loginCode;
  }

  public void setLoginCode(String loginCode) {
    this.loginCode = loginCode;
  }

  public String getAccessUrl() {
    return accessUrl;
  }

  public void setAccessUrl(String accessUrl) {
    this.accessUrl = accessUrl;
  }

  public MemberLoginProvisioningStatus getStatus() {
    return status;
  }

  public void setStatus(MemberLoginProvisioningStatus status) {
    this.status = status;
  }

  public OffsetDateTime getIssuedAt() {
    return issuedAt;
  }

  public void setIssuedAt(OffsetDateTime issuedAt) {
    this.issuedAt = issuedAt;
  }

  public OffsetDateTime getExpiresAt() {
    return expiresAt;
  }

  public void setExpiresAt(OffsetDateTime expiresAt) {
    this.expiresAt = expiresAt;
  }

  public OffsetDateTime getSentAt() {
    return sentAt;
  }

  public void setSentAt(OffsetDateTime sentAt) {
    this.sentAt = sentAt;
  }

  public String getLastErrorMessage() {
    return lastErrorMessage;
  }

  public void setLastErrorMessage(String lastErrorMessage) {
    this.lastErrorMessage = lastErrorMessage;
  }

  @Override
  public String toString() {
    return "MemberLoginProvisioningResponse{"
        + "id="
        + id
        + ", staffId="
        + staffId
        + ", staffName='"
        + staffName
        + '\''
        + ", loginCode='"
        + loginCode
        + '\''
        + ", accessUrl='"
        + accessUrl
        + '\''
        + ", status="
        + status
        + ", issuedAt="
        + issuedAt
        + ", expiresAt="
        + expiresAt
        + ", sentAt="
        + sentAt
        + ", lastErrorMessage='"
        + lastErrorMessage
        + '\''
        + '}';
  }
}
