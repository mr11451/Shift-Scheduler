package com.shiftscheduler.server.service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shiftscheduler.server.api.CalendarViewPermissionCreateRequest;
import com.shiftscheduler.server.api.CalendarViewPermissionResponse;
import com.shiftscheduler.server.domain.CalendarViewPermission;
import com.shiftscheduler.server.domain.CalendarViewPermissionStatus;
import com.shiftscheduler.server.domain.Staff;
import com.shiftscheduler.server.repository.CalendarViewPermissionRepository;
import com.shiftscheduler.server.repository.StaffRepository;

@Service
public class CalendarViewPermissionService {

  @Autowired
  private CalendarViewPermissionRepository calendarViewPermissionRepository;

  @Autowired
  private StaffRepository staffRepository;

  @Autowired
  private SystemSettingService systemSettingService;

  /**
   * Create a pending calendar-view request from a MEMBER to another staff in the same group.
   */
  @Transactional
  public CalendarViewPermissionResponse createCalendarViewPermission(Long requesterStaffId, CalendarViewPermissionCreateRequest request) {
    // Validate required fields
    if (request.getTargetStaffId() == null) {
      throw new IllegalArgumentException("対象スタッフIDは必須です。");
    }

    if (requesterStaffId.equals(request.getTargetStaffId())) {
      throw new IllegalArgumentException("自分自身にはカレンダー表示権を許可できません。");
    }

    Staff requester = staffRepository.findById(requesterStaffId)
        .orElseThrow(() -> new IllegalArgumentException("申請者スタッフが見つかりません。"));
    Staff target = staffRepository.findById(request.getTargetStaffId())
        .orElseThrow(() -> new IllegalArgumentException("対象スタッフが見つかりません。"));

    if (!Boolean.TRUE.equals(systemSettingService.getSystemSettingBooleanValue("calendarViewPermissionEnabled"))) {
      throw new IllegalArgumentException("メンバー間カレンダー閲覧機能が無効です。");
    }

    if (requester.getRoleLevel() != com.shiftscheduler.server.domain.RoleLevel.MEMBER
        || requester.getGroup() == null
        || target.getGroup() == null
        || !requester.getGroup().getId().equals(target.getGroup().getId())) {
      throw new IllegalArgumentException("同じグループのメンバーにのみ申請できます。");
    }

    CalendarViewPermission permission = new CalendarViewPermission();
    permission.setRequesterStaff(requester);
    permission.setTargetStaff(target);
    permission.setStatus(CalendarViewPermissionStatus.PENDING);
    permission.setRequestedAt(OffsetDateTime.now());

    CalendarViewPermission saved = calendarViewPermissionRepository.save(permission);
    return convertToResponse(saved);
  }

  /**
   * Approve a pending permission request; only the target staff may approve.
   */
  @Transactional
  public CalendarViewPermissionResponse approveCalendarViewPermission(Long approverStaffId, Long permissionId) {
    CalendarViewPermission permission = calendarViewPermissionRepository.findById(permissionId)
        .orElseThrow(() -> new IllegalArgumentException("権限申請が見つかりません。"));

    Staff approver = staffRepository.findById(approverStaffId)
        .orElseThrow(() -> new IllegalArgumentException("承認者スタッフが見つかりません。"));

    // Only target staff can approve
    if (!permission.getTargetStaff().getId().equals(approverStaffId)) {
      throw new IllegalArgumentException("対象者のみ承認できます。");
    }

    // Can only approve if status is PENDING
    if (!permission.getStatus().equals(CalendarViewPermissionStatus.PENDING)) {
      throw new IllegalArgumentException("申請中の状態のみ承認可能です。");
    }

    permission.setStatus(CalendarViewPermissionStatus.APPROVED);
    permission.setRespondedAt(OffsetDateTime.now());
    permission.setUpdatedAt(OffsetDateTime.now());

    CalendarViewPermission updated = calendarViewPermissionRepository.save(permission);
    return convertToResponse(updated);
  }

  /**
   * Reject a pending permission request; only the target staff may reject.
   */
  @Transactional
  public CalendarViewPermissionResponse rejectCalendarViewPermission(Long rejecterStaffId, Long permissionId) {
    CalendarViewPermission permission = calendarViewPermissionRepository.findById(permissionId)
        .orElseThrow(() -> new IllegalArgumentException("権限申請が見つかりません。"));

    Staff rejecter = staffRepository.findById(rejecterStaffId)
        .orElseThrow(() -> new IllegalArgumentException("却下者スタッフが見つかりません。"));

    // Only target staff can reject
    if (!permission.getTargetStaff().getId().equals(rejecterStaffId)) {
      throw new IllegalArgumentException("対象者のみ却下できます。");
    }

    // Can only reject if status is PENDING
    if (!permission.getStatus().equals(CalendarViewPermissionStatus.PENDING)) {
      throw new IllegalArgumentException("申請中の状態のみ却下可能です。");
    }

    permission.setStatus(CalendarViewPermissionStatus.REJECTED);
    permission.setRespondedAt(OffsetDateTime.now());
    permission.setUpdatedAt(OffsetDateTime.now());

    CalendarViewPermission updated = calendarViewPermissionRepository.save(permission);
    return convertToResponse(updated);
  }

  /**
   * Cancel a pending permission request; only the original requester may cancel.
   */
  @Transactional
  public CalendarViewPermissionResponse cancelCalendarViewPermission(Long requesterStaffId, Long permissionId) {
    CalendarViewPermission permission = calendarViewPermissionRepository.findById(permissionId)
        .orElseThrow(() -> new IllegalArgumentException("権限申請が見つかりません。"));

    // Only requester can cancel
    if (!permission.getRequesterStaff().getId().equals(requesterStaffId)) {
      throw new IllegalArgumentException("申請者のみキャンセルできます。");
    }

    // Can only cancel if status is PENDING
    if (!permission.getStatus().equals(CalendarViewPermissionStatus.PENDING)) {
      throw new IllegalArgumentException("申請中の状態のみキャンセル可能です。");
    }

    permission.setStatus(CalendarViewPermissionStatus.CANCELED);
    permission.setRespondedAt(OffsetDateTime.now());
    permission.setUpdatedAt(OffsetDateTime.now());

    CalendarViewPermission updated = calendarViewPermissionRepository.save(permission);
    return convertToResponse(updated);
  }

  /**
   * Look up a single permission request by ID.
   */
  public CalendarViewPermissionResponse getCalendarViewPermissionById(Long permissionId) {
    CalendarViewPermission permission = calendarViewPermissionRepository.findById(permissionId)
        .orElseThrow(() -> new IllegalArgumentException("権限申請が見つかりません。"));
    return convertToResponse(permission);
  }

  /**
   * Get the approved permission (if any) allowing the requester to view the target's calendar.
   */
  public Optional<CalendarViewPermissionResponse> getApprovedCalendarViewPermissionsForRequester(Long requesterStaffId, Long targetStaffId) {
    return calendarViewPermissionRepository.findApprovedPermission(requesterStaffId, targetStaffId, CalendarViewPermissionStatus.APPROVED)
        .map(this::convertToResponse);
  }

  /**
   * List staff IDs whose calendars the requester has approved access to.
   */
  public List<Long> getApprovedTargetStaffIdsForRequester(Long requesterStaffId) {
    return calendarViewPermissionRepository.findApprovedTargetStaffIds(requesterStaffId, CalendarViewPermissionStatus.APPROVED);
  }

  /**
   * List permission requests made by the given requester, filtered by status.
   */
  public List<CalendarViewPermissionResponse> getCalendarViewPermissionsByRequesterAndStatus(Long requesterStaffId, CalendarViewPermissionStatus status) {
    return calendarViewPermissionRepository.findByRequesterAndStatus(requesterStaffId, status)
        .stream()
        .map(this::convertToResponse)
        .collect(Collectors.toList());
  }

  /**
   * List permission requests directed at the given target staff, filtered by status.
   */
  public List<CalendarViewPermissionResponse> getCalendarViewPermissionsByTargetAndStatus(Long targetStaffId, CalendarViewPermissionStatus status) {
    return calendarViewPermissionRepository.findByTargetStaffIdAndStatus(targetStaffId, status)
        .stream()
        .map(this::convertToResponse)
        .collect(Collectors.toList());
  }

  /**
   * Map the entity to its API response shape.
   */
  private CalendarViewPermissionResponse convertToResponse(CalendarViewPermission permission) {
    CalendarViewPermissionResponse response = new CalendarViewPermissionResponse();
    response.setId(permission.getId());
    response.setRequesterStaffId(permission.getRequesterStaff().getId());
    response.setRequesterStaffName(permission.getRequesterStaff().getStaffName());
    response.setTargetStaffId(permission.getTargetStaff().getId());
    response.setTargetStaffName(permission.getTargetStaff().getStaffName());
    response.setStatus(permission.getStatus());
    response.setRequestedAt(permission.getRequestedAt());
    response.setRespondedAt(permission.getRespondedAt());
    response.setExpiredAt(permission.getExpiredAt());
    return response;
  }
}
