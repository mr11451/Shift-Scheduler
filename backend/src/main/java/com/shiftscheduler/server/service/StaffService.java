package com.shiftscheduler.server.service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shiftscheduler.server.domain.Group;
import com.shiftscheduler.server.domain.RoleLevel;
import com.shiftscheduler.server.domain.Staff;
import com.shiftscheduler.server.dto.StaffCreateRequest;
import com.shiftscheduler.server.dto.StaffResponse;
import com.shiftscheduler.server.dto.StaffUpdateRequest;
import com.shiftscheduler.server.repository.CalendarViewPermissionRepository;
import com.shiftscheduler.server.repository.GroupRepository;
import com.shiftscheduler.server.repository.StaffRepository;

@Service
@Transactional
public class StaffService {
    private final StaffRepository staffRepository;
    private final GroupRepository groupRepository;
    private final CalendarViewPermissionRepository calendarViewPermissionRepository;
    private final AccessControlService accessControlService;

    public StaffService(StaffRepository staffRepository, GroupRepository groupRepository,
                        CalendarViewPermissionRepository calendarViewPermissionRepository,
                        AccessControlService accessControlService) {
        this.staffRepository = staffRepository;
        this.groupRepository = groupRepository;
        this.calendarViewPermissionRepository = calendarViewPermissionRepository;
        this.accessControlService = accessControlService;
    }

    /**
     * Create a new staff member
     */
    public Staff createStaff(Long updaterStaffId, StaffCreateRequest request) {
        Staff updater = staffRepository.findById(updaterStaffId)
                .orElseThrow(() -> new IllegalArgumentException("更新者スタッフが見つかりません。"));

        Staff staff = new Staff();
        staff.setStaffCode(generateStaffCode());
        staff.setStaffName(request.getStaffName());
        staff.setEmail(request.getEmail());
        staff.setPhone(request.getPhone());
        staff.setNgShiftTimeBands(request.getNgShiftTimeBands());
        staff.setPreferredShiftTimeBands(request.getPreferredShiftTimeBands());
        staff.setResponsibility(request.getResponsibility() == null || request.getResponsibility().isBlank()
                ? "未設定" : request.getResponsibility());
        staff.setRoleLevel(request.getRoleLevel());
        staff.setIsActive(true);

        // Only the logged-in MASTER may assign a group.
        if (accessControlService.isMaster(updater) && request.getGroupId() != null) {
            Optional<Group> group = groupRepository.findById(request.getGroupId());
            if (group.isPresent()) {
                staff.setGroup(group.get());
            }
        }

        staff.setCreatedAt(OffsetDateTime.now());
        staff.setUpdatedAt(OffsetDateTime.now());

        return staffRepository.save(staff);
    }

    /**
     * Update an existing staff member
     */
    public Staff updateStaff(Long updaterStaffId, Long staffId, StaffUpdateRequest request) {
        Staff updater = staffRepository.findById(updaterStaffId)
                .orElseThrow(() -> new IllegalArgumentException("更新者スタッフが見つかりません。"));
        Staff staff = staffRepository.findById(staffId).orElseThrow(() -> new IllegalArgumentException("Staff not found"));

        staff.setStaffName(request.getStaffName());
        staff.setEmail(request.getEmail());
        staff.setPhone(request.getPhone());
        staff.setNgShiftTimeBands(request.getNgShiftTimeBands());
        staff.setPreferredShiftTimeBands(request.getPreferredShiftTimeBands());
        staff.setResponsibility(request.getResponsibility() == null || request.getResponsibility().isBlank()
                ? "未設定" : request.getResponsibility());
        staff.setIsActive(request.getIsActive());

        // Only the logged-in MASTER may change the group.
        if (accessControlService.isMaster(updater)) {
            if (request.getGroupId() != null) {
                Optional<Group> group = groupRepository.findById(request.getGroupId());
                if (group.isPresent()) {
                    staff.setGroup(group.get());
                }
            } else {
                staff.setGroup(null);
            }
        }

        staff.setUpdatedAt(OffsetDateTime.now());

        return staffRepository.save(staff);
    }

    /**
     * Get staff by ID
     */
    public Optional<Staff> getStaffById(Long staffId) {
        return staffRepository.findById(staffId);
    }

    /**
     * Get staff by staff code
     */
    public Optional<Staff> getStaffByCode(String staffCode) {
        return staffRepository.findByStaffCode(staffCode);
    }

    /**
     * Get all active staff
     */
    public List<Staff> getAllActiveStaff() {
        return staffRepository.findAllByIsActiveTrue();
    }

    /**
     * Get all staff in a group
     */
    public List<Staff> getStaffByGroup(Long groupId) {
        return staffRepository.findAllActiveByGroupId(groupId);
    }

    public List<Staff> getSelectableStaffsForRequester(Long requesterStaffId) {
        Optional<Staff> requesterOpt = staffRepository.findById(requesterStaffId);
        if (requesterOpt.isEmpty()) {
            return List.of();
        }

        Staff requester = requesterOpt.get();
        List<Staff> allStaffs = staffRepository.findAllByIsActiveTrue();

        if (requester.getRoleLevel() == RoleLevel.MASTER) {
            return allStaffs;
        }

        Set<Long> approvedTargetIds = new java.util.HashSet<>(
                calendarViewPermissionRepository.findApprovedTargetStaffIds(requesterStaffId,
                        com.shiftscheduler.server.domain.CalendarViewPermissionStatus.APPROVED));

        return allStaffs.stream()
                .filter(staff -> isViewableStaff(requester, staff, approvedTargetIds))
                .collect(Collectors.toList());
    }

    private boolean isViewableStaff(Staff requester, Staff targetStaff, Set<Long> approvedTargetIds) {
        if (requester == null || targetStaff == null) {
            return false;
        }

        if (requester.getRoleLevel() == RoleLevel.MASTER) {
            return true;
        }

        if (requester.getRoleLevel() == RoleLevel.CHIEF) {
            return accessControlService.canViewShift(requester, targetStaff);
        }

        if (requester.getRoleLevel() == RoleLevel.MEMBER) {
            return requester.getId().equals(targetStaff.getId())
                    || approvedTargetIds.contains(targetStaff.getId());
        }

        return false;
    }

    /**
     * Get all staff by role level
     */
    public List<Staff> getStaffByRole(RoleLevel roleLevel) {
        return staffRepository.findAllActiveByRoleLevel(roleLevel);
    }

    /**
     * Generate a unique staff code
     */
    private String generateStaffCode() {
        // Implementation for generating staff code like STF-00001
        long count = staffRepository.count() + 1;
        return String.format("STF-%05d", count);
    }

    /**
     * Check if staff code exists
     */
    public boolean staffCodeExists(String staffCode) {
        return staffRepository.findByStaffCode(staffCode).isPresent();
    }

    /**
     * Check if email exists (for registration)
     */
    public boolean emailExists(String email) {
        return staffRepository.findByEmail(email).isPresent();
    }

    /**
     * Convert Staff to StaffResponse DTO
     */
    public StaffResponse convertToResponse(Staff staff) {
        StaffResponse response = new StaffResponse();
        response.setId(staff.getId());
        response.setStaffCode(staff.getStaffCode());
        response.setStaffName(staff.getStaffName());
        response.setEmail(staff.getEmail());
        response.setPhone(staff.getPhone());
        response.setNgShiftTimeBands(staff.getNgShiftTimeBands());
        response.setPreferredShiftTimeBands(staff.getPreferredShiftTimeBands());
        response.setResponsibility(staff.getResponsibility());
        response.setRoleLevel(staff.getRoleLevel());
        if (staff.getGroup() != null) {
            response.setGroupId(staff.getGroup().getId());
            response.setGroupName(staff.getGroup().getGroupName());
        }
        response.setIsActive(staff.getIsActive());
        return response;
    }

    /**
     * Deactivate a staff member
     */
    public Staff deactivateStaff(Long staffId) {
        Staff staff = staffRepository.findById(staffId).orElseThrow(() -> new IllegalArgumentException("Staff not found"));
        staff.setIsActive(false);
        staff.setUpdatedAt(OffsetDateTime.now());
        return staffRepository.save(staff);
    }

    /**
     * Reactivate a staff member
     */
    public Staff reactivateStaff(Long staffId) {
        Staff staff = staffRepository.findById(staffId).orElseThrow(() -> new IllegalArgumentException("Staff not found"));
        staff.setIsActive(true);
        staff.setUpdatedAt(OffsetDateTime.now());
        return staffRepository.save(staff);
    }
}
