package com.shiftscheduler.server.api;

import java.time.OffsetDateTime;

public class MemberLoginProvisioningCreateRequest {

  private Long staffId;
  private String loginCode;
  private String initialPasswordHash;
  private String accessUrl;
  private OffsetDateTime expiresAt;

  public MemberLoginProvisioningCreateRequest() {}

  public MemberLoginProvisioningCreateRequest(Long staffId, String loginCode, String initialPasswordHash, String accessUrl, OffsetDateTime expiresAt) {
    this.staffId = staffId;
    this.loginCode = loginCode;
    this.initialPasswordHash = initialPasswordHash;
    this.accessUrl = accessUrl;
    this.expiresAt = expiresAt;
  }

  public Long getStaffId() {
    return staffId;
  }

  public void setStaffId(Long staffId) {
    this.staffId = staffId;
  }

  public String getLoginCode() {
    return loginCode;
  }

  public void setLoginCode(String loginCode) {
    this.loginCode = loginCode;
  }

  public String getInitialPasswordHash() {
    return initialPasswordHash;
  }

  public void setInitialPasswordHash(String initialPasswordHash) {
    this.initialPasswordHash = initialPasswordHash;
  }

  public String getAccessUrl() {
    return accessUrl;
  }

  public void setAccessUrl(String accessUrl) {
    this.accessUrl = accessUrl;
  }

  public OffsetDateTime getExpiresAt() {
    return expiresAt;
  }

  public void setExpiresAt(OffsetDateTime expiresAt) {
    this.expiresAt = expiresAt;
  }

  @Override
  public String toString() {
    return "MemberLoginProvisioningCreateRequest{"
        + "staffId="
        + staffId
        + ", loginCode='"
        + loginCode
        + '\''
        + ", initialPasswordHash='"
        + initialPasswordHash
        + '\''
        + ", accessUrl='"
        + accessUrl
        + '\''
        + ", expiresAt="
        + expiresAt
        + '}';
  }
}
