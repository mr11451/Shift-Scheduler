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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "calendar_view_permissions", indexes = {
    @Index(name = "idx_calendar_view_permissions_requester", columnList = "requester_staff_id"),
    @Index(name = "idx_calendar_view_permissions_target", columnList = "target_staff_id"),
    @Index(name = "idx_calendar_view_permissions_status", columnList = "status"),
    @Index(name = "idx_calendar_view_permissions_requester_target_status", columnList = "requester_staff_id,target_staff_id,status")
})
public class CalendarViewPermission {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_staff_id", nullable = false)
    private Staff requesterStaff;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_staff_id", nullable = false)
    private Staff targetStaff;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private CalendarViewPermissionStatus status = CalendarViewPermissionStatus.PENDING;

    @Column(nullable = false, updatable = false)
    private OffsetDateTime requestedAt;

    @Column
    private OffsetDateTime respondedAt;

    @Column
    private OffsetDateTime expiredAt;

    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(nullable = false)
    private OffsetDateTime updatedAt;

    // Constructors
    public CalendarViewPermission() {}

    public CalendarViewPermission(Staff requesterStaff, Staff targetStaff) {
        this.requesterStaff = requesterStaff;
        this.targetStaff = targetStaff;
        this.status = CalendarViewPermissionStatus.PENDING;
    }

    @PrePersist
    public void prePersist() {
        OffsetDateTime now = OffsetDateTime.now();
        if (requestedAt == null) {
            requestedAt = now;
        }
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    public void preUpdate() {
        if (updatedAt == null) {
            updatedAt = OffsetDateTime.now();
        } else {
            updatedAt = OffsetDateTime.now();
        }
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Staff getRequesterStaff() {
        return requesterStaff;
    }

    public void setRequesterStaff(Staff requesterStaff) {
        this.requesterStaff = requesterStaff;
    }

    public Staff getTargetStaff() {
        return targetStaff;
    }

    public void setTargetStaff(Staff targetStaff) {
        this.targetStaff = targetStaff;
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
