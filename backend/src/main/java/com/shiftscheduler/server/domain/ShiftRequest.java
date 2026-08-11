package com.shiftscheduler.server.domain;

import java.time.LocalDate;
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
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "shift_requests", indexes = {
    @Index(name = "idx_shift_requests_staff_id", columnList = "staff_id"),
    @Index(name = "idx_shift_requests_work_date", columnList = "work_date"),
    @Index(name = "idx_shift_requests_status", columnList = "status"),
    @Index(name = "idx_shift_requests_staff_status", columnList = "staff_id,status")
}, uniqueConstraints = {
    @UniqueConstraint(columnNames = {"staff_id", "work_date"})
})
public class ShiftRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "staff_id", nullable = false)
    private Staff staff;

    @Column(nullable = false)
    private LocalDate workDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "desired_shift_type_id")
    private ShiftType desiredShiftType;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private ShiftRequestStatus status = ShiftRequestStatus.DRAFT;

    @Column
    private OffsetDateTime submittedAt;

    @Column
    private OffsetDateTime decidedAt;

    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(nullable = false)
    private OffsetDateTime updatedAt;

    // Constructors
    public ShiftRequest() {}

    public ShiftRequest(Staff staff, LocalDate workDate, ShiftType desiredShiftType) {
        this.staff = staff;
        this.workDate = workDate;
        this.desiredShiftType = desiredShiftType;
        this.status = ShiftRequestStatus.DRAFT;
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

    public LocalDate getWorkDate() {
        return workDate;
    }

    public void setWorkDate(LocalDate workDate) {
        this.workDate = workDate;
    }

    public ShiftType getDesiredShiftType() {
        return desiredShiftType;
    }

    public void setDesiredShiftType(ShiftType desiredShiftType) {
        this.desiredShiftType = desiredShiftType;
    }

    public ShiftRequestStatus getStatus() {
        return status;
    }

    public void setStatus(ShiftRequestStatus status) {
        this.status = status;
    }

    public OffsetDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(OffsetDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }

    public OffsetDateTime getDecidedAt() {
        return decidedAt;
    }

    public void setDecidedAt(OffsetDateTime decidedAt) {
        this.decidedAt = decidedAt;
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
