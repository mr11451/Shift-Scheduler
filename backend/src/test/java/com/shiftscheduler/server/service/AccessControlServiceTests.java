package com.shiftscheduler.server.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.shiftscheduler.server.domain.Group;
import com.shiftscheduler.server.domain.RoleLevel;
import com.shiftscheduler.server.domain.Staff;
import org.junit.jupiter.api.Test;

class AccessControlServiceTests {

    private final AccessControlService accessControlService = new AccessControlService();

    @Test
    void masterCanViewAnyStaff() {
        Staff master = createStaff(1L, RoleLevel.MASTER, null);
        Staff member = createStaff(2L, RoleLevel.MEMBER, null);

        assertTrue(accessControlService.canViewShift(master, member));
    }

    @Test
    void chiefCanViewSameGroupMembersWithoutExtraPermission() {
        Group group = new Group();
        group.setId(100L);

        Staff chief = createStaff(10L, RoleLevel.CHIEF, group);
        Staff sameGroupMember = createStaff(11L, RoleLevel.MEMBER, group);
        Staff otherGroupMember = createStaff(12L, RoleLevel.MEMBER, createGroup(200L));

        assertTrue(accessControlService.canViewShift(chief, sameGroupMember));
        assertFalse(accessControlService.canViewShift(chief, otherGroupMember));
    }

    @Test
    void memberCanOnlyViewThemselves() {
        Staff member = createStaff(20L, RoleLevel.MEMBER, null);
        Staff otherMember = createStaff(21L, RoleLevel.MEMBER, null);

        assertTrue(accessControlService.canViewShift(member, member));
        assertFalse(accessControlService.canViewShift(member, otherMember));
    }

    private Staff createStaff(Long id, RoleLevel roleLevel, Group group) {
        Staff staff = new Staff();
        staff.setId(id);
        staff.setRoleLevel(roleLevel);
        staff.setGroup(group);
        return staff;
    }

    private Group createGroup(Long id) {
        Group group = new Group();
        group.setId(id);
        return group;
    }
}
