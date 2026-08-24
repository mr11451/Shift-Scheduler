package com.shiftscheduler.server.service;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shiftscheduler.server.domain.Group;
import com.shiftscheduler.server.domain.MemberLoginProvisioning;
import com.shiftscheduler.server.domain.MemberLoginProvisioningStatus;
import com.shiftscheduler.server.domain.RoleLevel;
import com.shiftscheduler.server.domain.Staff;
import com.shiftscheduler.server.dto.InitialLoginInformation;
import com.shiftscheduler.server.dto.StaffCreateRequest;
import com.shiftscheduler.server.dto.StaffCreateResponse;
import com.shiftscheduler.server.dto.StaffResponse;
import com.shiftscheduler.server.dto.StaffUpdateRequest;
import com.shiftscheduler.server.repository.CalendarViewPermissionRepository;
import com.shiftscheduler.server.repository.GroupRepository;
import com.shiftscheduler.server.repository.MemberLoginProvisioningRepository;
import com.shiftscheduler.server.repository.StaffRepository;

@Service
@Transactional
public class StaffService {
    private final StaffRepository staffRepository;
    private final GroupRepository groupRepository;
    private final CalendarViewPermissionRepository calendarViewPermissionRepository;
    private final AccessControlService accessControlService;
    private final SystemSettingService systemSettingService;
    private final MemberLoginProvisioningRepository memberLoginProvisioningRepository;
    private final PasswordResetEmailService passwordResetEmailService;
    private final SecureRandom secureRandom = new SecureRandom();

    public StaffService(StaffRepository staffRepository, GroupRepository groupRepository,
                        CalendarViewPermissionRepository calendarViewPermissionRepository,
                        AccessControlService accessControlService, SystemSettingService systemSettingService,
                        MemberLoginProvisioningRepository memberLoginProvisioningRepository,
                        PasswordResetEmailService passwordResetEmailService) {
        this.staffRepository = staffRepository;
        this.groupRepository = groupRepository;
        this.calendarViewPermissionRepository = calendarViewPermissionRepository;
        this.accessControlService = accessControlService;
        this.systemSettingService = systemSettingService;
        this.memberLoginProvisioningRepository = memberLoginProvisioningRepository;
        this.passwordResetEmailService = passwordResetEmailService;
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
        staff.setNgShiftTypeIds(request.getNgShiftTypeIds());
        staff.setPreferredShiftTypeIds(request.getPreferredShiftTypeIds());
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

    public StaffCreateResponse createStaffWithInitialLogin(Long updaterStaffId, StaffCreateRequest request) {
        Staff staff = createStaff(updaterStaffId, request);
        InitialLoginInformation initialLoginInformation = createInitialLoginInformation(staff);
        return new StaffCreateResponse(convertToResponse(staff), initialLoginInformation);
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
        staff.setNgShiftTypeIds(request.getNgShiftTypeIds());
        staff.setPreferredShiftTypeIds(request.getPreferredShiftTypeIds());
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

        boolean calendarViewPermissionEnabled = Boolean.TRUE.equals(
            systemSettingService.getSystemSettingBooleanValue("calendarViewPermissionEnabled"));
        Set<Long> approvedTargetIds = new java.util.HashSet<>(
                calendarViewPermissionRepository.findApprovedTargetStaffIds(requesterStaffId,
                        com.shiftscheduler.server.domain.CalendarViewPermissionStatus.APPROVED));

        return allStaffs.stream()
            .filter(staff -> isViewableStaff(requester, staff, approvedTargetIds, calendarViewPermissionEnabled))
                .collect(Collectors.toList());
    }

    public List<Staff> getCalendarViewPermissionTargets(Long requesterStaffId) {
        Optional<Staff> requesterOpt = staffRepository.findById(requesterStaffId);
        if (requesterOpt.isEmpty() || requesterOpt.get().getRoleLevel() != RoleLevel.MEMBER
                || !Boolean.TRUE.equals(systemSettingService.getSystemSettingBooleanValue("calendarViewPermissionEnabled"))) {
            return List.of();
        }

        Staff requester = requesterOpt.get();
        return staffRepository.findAllByIsActiveTrue().stream()
                .filter(target -> isSameGroup(requester, target))
                .collect(Collectors.toList());
    }

        private boolean isViewableStaff(Staff requester, Staff targetStaff, Set<Long> approvedTargetIds,
                        boolean calendarViewPermissionEnabled) {
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
                || (calendarViewPermissionEnabled && approvedTargetIds.contains(targetStaff.getId()));
        }

        return false;
    }

    private boolean isSameGroup(Staff requester, Staff targetStaff) {
        return requester.getGroup() != null
                && targetStaff.getGroup() != null
                && requester.getGroup().getId().equals(targetStaff.getGroup().getId())
                && !requester.getId().equals(targetStaff.getId());
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

    private InitialLoginInformation createInitialLoginInformation(Staff staff) {
        if (staff.getRoleLevel() != RoleLevel.MEMBER) {
            return null;
        }

        String initialPassword = generateInitialPassword();
        String accessUrl = buildInitialLoginAccessUrl();
        staff.setPasswordHash(com.shiftscheduler.server.util.PasswordUtil.hashPassword(initialPassword));

        OffsetDateTime now = OffsetDateTime.now();
        MemberLoginProvisioning provisioning = new MemberLoginProvisioning();
        provisioning.setStaff(staff);
        provisioning.setLoginCode(staff.getStaffCode());
        provisioning.setInitialPasswordHash(staff.getPasswordHash());
        provisioning.setAccessUrl(accessUrl);
        provisioning.setIssuedAt(now);
        provisioning.setExpiresAt(now.plusDays(7));
        provisioning.setCreatedAt(now);
        provisioning.setUpdatedAt(now);

        boolean notificationEnabled = Boolean.TRUE.equals(
                systemSettingService.getSystemSettingBooleanValue("memberLoginNotificationEnabled"));
        String fallbackMessage = "メール送信が無効のため、初回ログイン情報を表示します。";
        boolean deliveryFailed = false;

        if (notificationEnabled && staff.getEmail() != null && !staff.getEmail().isBlank() && passwordResetEmailService.isAvailable()) {
            try {
                passwordResetEmailService.sendInitialLogin(
                        staff.getEmail(), staff.getStaffName(), accessUrl, staff.getStaffCode(), initialPassword);
                provisioning.setStatus(MemberLoginProvisioningStatus.SENT);
                provisioning.setSentAt(now);
                memberLoginProvisioningRepository.save(provisioning);
                return new InitialLoginInformation(true, "初回ログイン情報をメールで送信しました。", null, null, null);
            } catch (RuntimeException e) {
                provisioning.setStatus(MemberLoginProvisioningStatus.FAILED);
                provisioning.setLastErrorMessage(e.getMessage());
                fallbackMessage = "メールを送信できなかったため、初回ログイン情報を表示します。";
                deliveryFailed = true;
            }
        } else if (staff.getEmail() == null || staff.getEmail().isBlank()) {
            fallbackMessage = "メールアドレスが未登録のため、初回ログイン情報を表示します。";
        }

        if (!deliveryFailed) {
            provisioning.setStatus(MemberLoginProvisioningStatus.ISSUED);
        }
        memberLoginProvisioningRepository.save(provisioning);
        return new InitialLoginInformation(false, fallbackMessage, accessUrl, staff.getStaffCode(), initialPassword);
    }

    private String buildInitialLoginAccessUrl() {
        String baseUrl = systemSettingService.getSystemSettingTextValue("memberLoginNotificationBaseUrl");
        if (baseUrl == null || baseUrl.isBlank()) {
            return "/login";
        }
        return baseUrl.replaceAll("/+$", "") + "/login";
    }

    private String generateInitialPassword() {
        byte[] bytes = new byte[12];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
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
        response.setNgShiftTypeIds(staff.getNgShiftTypeIds());
        response.setPreferredShiftTypeIds(staff.getPreferredShiftTypeIds());
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
