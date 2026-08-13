package com.shiftscheduler.server.dto;

import com.shiftscheduler.server.domain.RoleLevel;

public class StaffCreateRequest {
    private String staffName;
    private String email;
    private String phone;
    private String ngShiftTypeIds;
    private String preferredShiftTypeIds;
    private String responsibility;
    private RoleLevel roleLevel;
    private Long groupId;

    // Constructors
    public StaffCreateRequest() {}

    // Getters and Setters
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
}
