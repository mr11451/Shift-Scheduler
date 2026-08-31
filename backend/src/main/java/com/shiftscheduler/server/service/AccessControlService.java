package com.shiftscheduler.server.service;

import org.springframework.stereotype.Service;

import com.shiftscheduler.server.domain.RoleLevel;
import com.shiftscheduler.server.domain.Staff;

@Service
public class AccessControlService {
    
    /**
     * Check if the editor can view the target staff's shift
     */
    public boolean canViewShift(Staff editor, Staff targetStaff) {
        if (editor == null || targetStaff == null) {
            return false;
        }

        // MASTER can view any staff
        if (editor.getRoleLevel() == RoleLevel.MASTER) {
            return true;
        }

        // MEMBER can only view their own
        if (editor.getRoleLevel() == RoleLevel.MEMBER) {
            return editor.getId().equals(targetStaff.getId());
        }

        // CHIEF can view their own and members in the same group
        if (editor.getRoleLevel() == RoleLevel.CHIEF) {
            if (editor.getId().equals(targetStaff.getId())) {
                return true;
            }
            if (isSameGroupManagedRole(targetStaff) && isSameGroup(editor, targetStaff)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Check if the editor can edit the target staff's shift
     */
    public boolean canEditShift(Staff editor, Staff targetStaff) {
        if (editor == null || targetStaff == null) {
            return false;
        }

        // MEMBER cannot edit shifts
        if (editor.getRoleLevel() == RoleLevel.MEMBER) {
            return false;
        }

        // MASTER can edit any staff's shift
        if (editor.getRoleLevel() == RoleLevel.MASTER) {
            return true;
        }

        // CHIEF can edit members in the same group, and MASTER staff who belong to the same group
        if (editor.getRoleLevel() == RoleLevel.CHIEF) {
            if (isSameGroupManagedRole(targetStaff) && isSameGroup(editor, targetStaff)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Check if the editor can view the staff's desired shifts
     */
    public boolean canViewDesiredShifts(Staff editor, Staff targetStaff) {
        return canViewShift(editor, targetStaff);
    }

    /**
     * Check if the editor can edit the staff's desired shifts
     */
    public boolean canEditDesiredShifts(Staff editor, Staff targetStaff) {
        if (editor == null || targetStaff == null) {
            return false;
        }

        // Only the member themselves can edit their desired shifts
        if (editor.getId().equals(targetStaff.getId()) && editor.getRoleLevel() == RoleLevel.MEMBER) {
            return true;
        }

        // MASTER can edit any member's desired shifts
        if (editor.getRoleLevel() == RoleLevel.MASTER) {
            return true;
        }

        return false;
    }

    /**
     * Check if the editor is MASTER
     */
    public boolean isMaster(Staff editor) {
        return editor != null && editor.getRoleLevel() == RoleLevel.MASTER;
    }

    /**
     * Check if the editor is CHIEF
     */
    public boolean isChief(Staff editor) {
        return editor != null && editor.getRoleLevel() == RoleLevel.CHIEF;
    }

    /**
     * Check if the editor is MEMBER
     */
    public boolean isMember(Staff editor) {
        return editor != null && editor.getRoleLevel() == RoleLevel.MEMBER;
    }

    private boolean isSameGroup(Staff editor, Staff targetStaff) {
        if (editor == null || targetStaff == null) {
            return false;
        }
        if (editor.getGroup() == null || targetStaff.getGroup() == null) {
            return false;
        }
        return editor.getGroup().getId() != null
                && editor.getGroup().getId().equals(targetStaff.getGroup().getId());
    }

    /**
     * A MASTER/CHIEF counts as a same-group target only when they belong to a group;
     * a MEMBER always counts regardless of group membership checks done elsewhere.
     */
    private boolean isSameGroupManagedRole(Staff targetStaff) {
        if (targetStaff.getRoleLevel() == RoleLevel.MEMBER) {
            return true;
        }
        return (targetStaff.getRoleLevel() == RoleLevel.MASTER || targetStaff.getRoleLevel() == RoleLevel.CHIEF)
                && targetStaff.getGroup() != null;
    }
}
