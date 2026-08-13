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
import jakarta.persistence.Table;

@Entity
@Table(name = "staffs", indexes = {
    @Index(name = "idx_staffs_staff_code", columnList = "staff_code"),
    @Index(name = "idx_staffs_email", columnList = "email"),
    @Index(name = "idx_staffs_group_id", columnList = "group_id"),
    @Index(name = "idx_staffs_role_level", columnList = "role_level"),
    @Index(name = "idx_staffs_is_active", columnList = "is_active")
})
public class Staff {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 50)
    private String staffCode;

    @Column(nullable = false, length = 100)
    private String staffName;

    @Column(length = 255)
    private String email;

    @Column(length = 20)
    private String phone;

    @Column(nullable = false, length = 100)
    private String responsibility;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private RoleLevel roleLevel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id")
    private Group group;

    @Column(nullable = false)
    private Boolean isActive = true;

    @Column(length = 255)
    private String passwordHash;

    @Column
    private OffsetDateTime passwordChangedAt;

    @Column(name = "ng_shift_time_bands", length = 1000)
    private String ngShiftTypeIds;

    @Column(name = "preferred_shift_time_bands", length = 1000)
    private String preferredShiftTypeIds;

    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(nullable = false)
    private OffsetDateTime updatedAt;

    // Constructors
    public Staff() {}

    public Staff(String staffCode, String staffName, String responsibility, RoleLevel roleLevel) {
        this.staffCode = staffCode;
        this.staffName = staffName;
        this.responsibility = responsibility;
        this.roleLevel = roleLevel;
        this.isActive = true;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getStaffCode() {
        return staffCode;
    }

    public void setStaffCode(String staffCode) {
        this.staffCode = staffCode;
    }

    public String getStaffName() {
        return staffName;
    }

    public void setStaffName(String staffName) {
        this.staffName = staffName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getResponsibility() {
        return responsibility;
    }

    public void setResponsibility(String responsibility) {
        this.responsibility = responsibility;
    }

    public RoleLevel getRoleLevel() {
        return roleLevel;
    }

    public void setRoleLevel(RoleLevel roleLevel) {
        this.roleLevel = roleLevel;
    }

    public Group getGroup() {
        return group;
    }

    public void setGroup(Group group) {
        this.group = group;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public OffsetDateTime getPasswordChangedAt() {
        return passwordChangedAt;
    }

    public void setPasswordChangedAt(OffsetDateTime passwordChangedAt) {
        this.passwordChangedAt = passwordChangedAt;
    }

    public String getNgShiftTypeIds() {
        return ngShiftTypeIds;
    }

    public void setNgShiftTypeIds(String ngShiftTypeIds) {
        this.ngShiftTypeIds = ngShiftTypeIds;
    }

    public String getPreferredShiftTypeIds() {
        return preferredShiftTypeIds;
    }

    public void setPreferredShiftTypeIds(String preferredShiftTypeIds) {
        this.preferredShiftTypeIds = preferredShiftTypeIds;
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
