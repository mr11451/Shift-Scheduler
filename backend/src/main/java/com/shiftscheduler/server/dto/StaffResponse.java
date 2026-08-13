package com.shiftscheduler.server.dto;

import com.shiftscheduler.server.domain.RoleLevel;

public class StaffResponse {
    private Long id;
    private String staffCode;
    private String staffName;
    private String email;
    private String phone;
    private String ngShiftTypeIds;
    private String preferredShiftTypeIds;
    private String responsibility;
    private RoleLevel roleLevel;
    private Long groupId;
    private String groupName;
    private Boolean isActive;

    // Constructors
    public StaffResponse() {}

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

    public Long getGroupId() {
        return groupId;
    }

    public void setGroupId(Long groupId) {
        this.groupId = groupId;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
}
