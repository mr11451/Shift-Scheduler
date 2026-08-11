package com.shiftscheduler.server.domain;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "shift_assignments", indexes = {
    @Index(name = "idx_shift_assignments_staff_id", columnList = "staff_id"),
    @Index(name = "idx_shift_assignments_work_date", columnList = "work_date"),
    @Index(name = "idx_shift_assignments_shift_type_id", columnList = "shift_type_id"),
    @Index(name = "idx_shift_assignments_staff_work_date", columnList = "staff_id,work_date")
}, uniqueConstraints = {
    @UniqueConstraint(columnNames = {"staff_id", "work_date"})
})
public class ShiftAssignment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "staff_id", nullable = false)
    private Staff staff;

    @Column(nullable = false)
    private LocalDate workDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shift_type_id", nullable = false)
    private ShiftType shiftType;

    @Column(length = 255)
    private String note;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by_staff_id", nullable = false)
    private Staff updatedBy;

    @Column(nullable = false)
    private OffsetDateTime updatedAt;

    // Constructors
    public ShiftAssignment() {}

    public ShiftAssignment(Staff staff, LocalDate workDate, ShiftType shiftType, Staff updatedBy) {
        this.staff = staff;
        this.workDate = workDate;
        this.shiftType = shiftType;
        this.updatedBy = updatedBy;
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

    public ShiftType getShiftType() {
        return shiftType;
    }

    public void setShiftType(ShiftType shiftType) {
        this.shiftType = shiftType;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public Staff getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(Staff updatedBy) {
        this.updatedBy = updatedBy;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
