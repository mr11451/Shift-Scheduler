package com.shiftscheduler.server.domain;

import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "member_login_provisionings", indexes = {
    @Index(name = "idx_member_login_provisionings_staff_id", columnList = "staff_id"),
    @Index(name = "idx_member_login_provisionings_login_code", columnList = "login_code"),
    @Index(name = "idx_member_login_provisionings_status", columnList = "status"),
    @Index(name = "idx_member_login_provisionings_expires_at", columnList = "expires_at")
})
public class MemberLoginProvisioning {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "staff_id", nullable = false, unique = true)
    private Staff staff;

    @Column(nullable = false, length = 64)
    private String loginCode;

    @Column(nullable = false, length = 255)
    private String initialPasswordHash;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String accessUrl;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private MemberLoginProvisioningStatus status = MemberLoginProvisioningStatus.ISSUED;

    @Column(nullable = false, updatable = false)
    private OffsetDateTime issuedAt;

    @Column(nullable = false)
    private OffsetDateTime expiresAt;

    @Column
    private OffsetDateTime sentAt;

    @Column(columnDefinition = "TEXT")
    private String lastErrorMessage;

    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(nullable = false)
    private OffsetDateTime updatedAt;

    // Constructors
    public MemberLoginProvisioning() {}

    public MemberLoginProvisioning(Staff staff, String loginCode, String initialPasswordHash, String accessUrl, OffsetDateTime expiresAt) {
        this.staff = staff;
        this.loginCode = loginCode;
        this.initialPasswordHash = initialPasswordHash;
        this.accessUrl = accessUrl;
        this.expiresAt = expiresAt;
        this.status = MemberLoginProvisioningStatus.ISSUED;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Staff getStaff() {
        return staff;
    }

    public void setStaff(Staff staff) {
        this.staff = staff;
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

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
