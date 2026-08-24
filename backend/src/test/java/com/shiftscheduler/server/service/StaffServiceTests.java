package com.shiftscheduler.server.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.shiftscheduler.server.domain.Group;
import com.shiftscheduler.server.domain.RoleLevel;
import com.shiftscheduler.server.domain.Staff;
import com.shiftscheduler.server.dto.StaffCreateRequest;
import com.shiftscheduler.server.dto.StaffUpdateRequest;
import com.shiftscheduler.server.repository.CalendarViewPermissionRepository;
import com.shiftscheduler.server.repository.GroupRepository;
import com.shiftscheduler.server.repository.StaffRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StaffServiceTests {

    @Mock
    private StaffRepository staffRepository;

    @Mock
    private GroupRepository groupRepository;

    @Mock
    private CalendarViewPermissionRepository calendarViewPermissionRepository;

    @Mock
    private AccessControlService accessControlService;

    @Mock
    private SystemSettingService systemSettingService;

    @InjectMocks
    private StaffService staffService;

    @Test
    void getSelectableStaffsForRequester_returnsSelfAndApprovedTargetsForMember() {
        Staff requester = new Staff();
        requester.setId(1L);
        requester.setRoleLevel(RoleLevel.MEMBER);
        requester.setIsActive(true);

        Staff approvedTarget = new Staff();
        approvedTarget.setId(2L);
        approvedTarget.setIsActive(true);

        Staff otherStaff = new Staff();
        otherStaff.setId(3L);
        otherStaff.setIsActive(true);

        when(staffRepository.findById(1L)).thenReturn(Optional.of(requester));
        when(staffRepository.findAllByIsActiveTrue()).thenReturn(List.of(requester, approvedTarget, otherStaff));
        when(calendarViewPermissionRepository.findApprovedTargetStaffIds(1L, com.shiftscheduler.server.domain.CalendarViewPermissionStatus.APPROVED))
                .thenReturn(List.of(2L));
        when(systemSettingService.getSystemSettingBooleanValue("calendarViewPermissionEnabled")).thenReturn(true);

        List<Staff> result = staffService.getSelectableStaffsForRequester(1L);

        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(staff -> staff.getId().equals(1L)));
        assertTrue(result.stream().anyMatch(staff -> staff.getId().equals(2L)));
        assertTrue(result.stream().noneMatch(staff -> staff.getId().equals(3L)));
    }

    @Test
    void getSelectableStaffsForRequester_doesNotIncludeSameGroupStaffsForMemberWithoutApproval() {
        Group group = new Group();
        group.setId(100L);

        Staff requester = new Staff();
        requester.setId(1L);
        requester.setRoleLevel(RoleLevel.MEMBER);
        requester.setGroup(group);
        requester.setIsActive(true);

        Staff sameGroupStaff = new Staff();
        sameGroupStaff.setId(2L);
        sameGroupStaff.setGroup(group);
        sameGroupStaff.setIsActive(true);

        Staff approvedTarget = new Staff();
        approvedTarget.setId(3L);
        approvedTarget.setGroup(new Group());
        approvedTarget.getGroup().setId(200L);
        approvedTarget.setIsActive(true);

        Staff otherGroupStaff = new Staff();
        otherGroupStaff.setId(4L);
        otherGroupStaff.setGroup(new Group());
        otherGroupStaff.getGroup().setId(300L);
        otherGroupStaff.setIsActive(true);

        when(staffRepository.findById(1L)).thenReturn(Optional.of(requester));
        when(staffRepository.findAllByIsActiveTrue()).thenReturn(List.of(requester, sameGroupStaff, approvedTarget, otherGroupStaff));
        when(calendarViewPermissionRepository.findApprovedTargetStaffIds(1L, com.shiftscheduler.server.domain.CalendarViewPermissionStatus.APPROVED))
                .thenReturn(List.of(3L));
        when(systemSettingService.getSystemSettingBooleanValue("calendarViewPermissionEnabled")).thenReturn(true);

        List<Staff> result = staffService.getSelectableStaffsForRequester(1L);

        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(staff -> staff.getId().equals(1L)));
        assertTrue(result.stream().anyMatch(staff -> staff.getId().equals(3L)));
        assertTrue(result.stream().noneMatch(staff -> staff.getId().equals(2L)));
        assertTrue(result.stream().noneMatch(staff -> staff.getId().equals(4L)));
    }

    @Test
    void getCalendarViewPermissionTargets_includesSameGroupStaffWithAnyRoleWhenEnabled() {
        Group group = new Group();
        group.setId(100L);

        Staff requester = new Staff();
        requester.setId(1L);
        requester.setRoleLevel(RoleLevel.MEMBER);
        requester.setGroup(group);
        requester.setIsActive(true);

        Staff sameGroupMember = new Staff();
        sameGroupMember.setId(2L);
        sameGroupMember.setRoleLevel(RoleLevel.MEMBER);
        sameGroupMember.setGroup(group);
        sameGroupMember.setIsActive(true);

        Staff sameGroupChief = new Staff();
        sameGroupChief.setId(3L);
        sameGroupChief.setRoleLevel(RoleLevel.CHIEF);
        sameGroupChief.setGroup(group);
        sameGroupChief.setIsActive(true);

        Staff otherGroupMember = new Staff();
        otherGroupMember.setId(4L);
        otherGroupMember.setRoleLevel(RoleLevel.MEMBER);
        Group otherGroup = new Group();
        otherGroup.setId(200L);
        otherGroupMember.setGroup(otherGroup);
        otherGroupMember.setIsActive(true);

        when(staffRepository.findById(1L)).thenReturn(Optional.of(requester));
        when(staffRepository.findAllByIsActiveTrue()).thenReturn(List.of(requester, sameGroupMember, sameGroupChief, otherGroupMember));
        when(systemSettingService.getSystemSettingBooleanValue("calendarViewPermissionEnabled")).thenReturn(true);

        List<Staff> result = staffService.getCalendarViewPermissionTargets(1L);

        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(staff -> staff.getId().equals(2L)));
        assertTrue(result.stream().anyMatch(staff -> staff.getId().equals(3L)));
        assertTrue(result.stream().noneMatch(staff -> staff.getId().equals(4L)));
    }

    @Test
    void getCalendarViewPermissionTargets_returnsEmptyWhenFeatureIsDisabled() {
        Staff requester = new Staff();
        requester.setId(1L);
        requester.setRoleLevel(RoleLevel.MEMBER);

        when(staffRepository.findById(1L)).thenReturn(Optional.of(requester));
        when(systemSettingService.getSystemSettingBooleanValue("calendarViewPermissionEnabled")).thenReturn(false);

        List<Staff> result = staffService.getCalendarViewPermissionTargets(1L);

        assertTrue(result.isEmpty());
    }

    @Test
    void getSelectableStaffsForRequester_returnsAllActiveStaffForMaster() {
        Staff requester = new Staff();
        requester.setId(10L);
        requester.setRoleLevel(RoleLevel.MASTER);
        requester.setIsActive(true);

        Staff another = new Staff();
        another.setId(11L);
        another.setIsActive(true);

        when(staffRepository.findById(10L)).thenReturn(Optional.of(requester));
        when(staffRepository.findAllByIsActiveTrue()).thenReturn(List.of(requester, another));

        List<Staff> result = staffService.getSelectableStaffsForRequester(10L);

        assertEquals(2, result.size());
    }

    @Test
    void getSelectableStaffsForRequester_returnsSameGroupMembersForChiefWithoutPermission() {
        Group group = new Group();
        group.setId(100L);

        Staff requester = new Staff();
        requester.setId(20L);
        requester.setRoleLevel(RoleLevel.CHIEF);
        requester.setGroup(group);
        requester.setIsActive(true);

        Staff sameGroupMember = new Staff();
        sameGroupMember.setId(21L);
        sameGroupMember.setRoleLevel(RoleLevel.MEMBER);
        sameGroupMember.setGroup(group);
        sameGroupMember.setIsActive(true);

        Staff sameGroupChief = new Staff();
        sameGroupChief.setId(22L);
        sameGroupChief.setRoleLevel(RoleLevel.CHIEF);
        sameGroupChief.setGroup(group);
        sameGroupChief.setIsActive(true);

        Staff otherGroupMember = new Staff();
        otherGroupMember.setId(23L);
        otherGroupMember.setRoleLevel(RoleLevel.MEMBER);
        otherGroupMember.setGroup(new Group());
        otherGroupMember.getGroup().setId(200L);
        otherGroupMember.setIsActive(true);

        when(staffRepository.findById(20L)).thenReturn(Optional.of(requester));
        when(staffRepository.findAllByIsActiveTrue()).thenReturn(List.of(requester, sameGroupMember, sameGroupChief, otherGroupMember));
        when(accessControlService.canViewShift(requester, requester)).thenReturn(true);
        when(accessControlService.canViewShift(requester, sameGroupMember)).thenReturn(true);
        when(accessControlService.canViewShift(requester, sameGroupChief)).thenReturn(false);
        when(accessControlService.canViewShift(requester, otherGroupMember)).thenReturn(false);

        List<Staff> result = staffService.getSelectableStaffsForRequester(20L);

        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(staff -> staff.getId().equals(20L)));
        assertTrue(result.stream().anyMatch(staff -> staff.getId().equals(21L)));
        assertTrue(result.stream().noneMatch(staff -> staff.getId().equals(22L)));
        assertTrue(result.stream().noneMatch(staff -> staff.getId().equals(23L)));
    }

    @Test
    void createStaff_assignsGroupOnlyWhenUpdaterIsMaster() {
        Group group = new Group();
        group.setId(100L);
        group.setGroupName("Group A");

        Staff updater = new Staff();
        updater.setId(1L);
        updater.setRoleLevel(RoleLevel.MASTER);

        when(staffRepository.findById(1L)).thenReturn(Optional.of(updater));
        when(groupRepository.findById(100L)).thenReturn(Optional.of(group));
        when(staffRepository.count()).thenReturn(0L);
        when(staffRepository.save(org.mockito.ArgumentMatchers.any(Staff.class))).thenAnswer(invocation -> invocation.getArgument(0));

        StaffCreateRequest request = new StaffCreateRequest();
        request.setStaffName("新規スタッフ");
        request.setResponsibility("担当");
        request.setRoleLevel(RoleLevel.MEMBER);
        request.setGroupId(100L);

        Staff created = staffService.createStaff(1L, request);

        assertEquals(100L, created.getGroup().getId());
        verify(groupRepository).findById(100L);
    }

    @Test
    void updateStaff_doesNotChangeGroupWhenUpdaterIsNotMaster() {
        Group currentGroup = new Group();
        currentGroup.setId(200L);
        currentGroup.setGroupName("Current");

        Group targetGroup = new Group();
        targetGroup.setId(300L);
        targetGroup.setGroupName("Target");

        Staff updater = new Staff();
        updater.setId(1L);
        updater.setRoleLevel(RoleLevel.CHIEF);

        Staff staff = new Staff();
        staff.setId(2L);
        staff.setGroup(currentGroup);
        staff.setStaffName("既存スタッフ");
        staff.setRoleLevel(RoleLevel.MEMBER);
        staff.setIsActive(true);

        when(staffRepository.findById(1L)).thenReturn(Optional.of(updater));
        when(staffRepository.findById(2L)).thenReturn(Optional.of(staff));
        when(staffRepository.save(org.mockito.ArgumentMatchers.any(Staff.class))).thenAnswer(invocation -> invocation.getArgument(0));

        StaffUpdateRequest request = new StaffUpdateRequest();
        request.setStaffName("既存スタッフ");
        request.setResponsibility("担当");
        request.setRoleLevel(RoleLevel.MEMBER);
        request.setGroupId(300L);
        request.setIsActive(true);

        Staff updated = staffService.updateStaff(1L, 2L, request);

        assertEquals(200L, updated.getGroup().getId());
    }
}
