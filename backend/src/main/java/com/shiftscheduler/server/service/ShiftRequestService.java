package com.shiftscheduler.server.service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shiftscheduler.server.api.ShiftRequestCreateRequest;
import com.shiftscheduler.server.api.ShiftRequestResponse;
import com.shiftscheduler.server.api.ShiftRequestUpdateRequest;
import com.shiftscheduler.server.domain.ShiftAssignment;
import com.shiftscheduler.server.domain.ShiftRequest;
import com.shiftscheduler.server.domain.ShiftRequestStatus;
import com.shiftscheduler.server.domain.ShiftType;
import com.shiftscheduler.server.domain.Staff;
import com.shiftscheduler.server.repository.ShiftAssignmentRepository;
import com.shiftscheduler.server.repository.ShiftRequestRepository;
import com.shiftscheduler.server.repository.ShiftTypeRepository;
import com.shiftscheduler.server.repository.StaffRepository;

@Service
public class ShiftRequestService {

  @Autowired
  private ShiftRequestRepository shiftRequestRepository;

  @Autowired
  private StaffRepository staffRepository;

  @Autowired
  private ShiftTypeRepository shiftTypeRepository;

  @Autowired
  private ShiftAssignmentRepository shiftAssignmentRepository;

  @Autowired
  private AccessControlService accessControlService;

  @Transactional
  public ShiftRequestResponse createShiftRequest(Long staffId, ShiftRequestCreateRequest request) {
    // Validate required fields
    if (request.getWorkDate() == null) {
      throw new IllegalArgumentException("勤務日は必須です。");
    }
    if (request.getWorkDate().isBefore(LocalDate.now())) {
      throw new IllegalArgumentException("過去の日付には申請できません。");
    }

    boolean isVacation = Boolean.TRUE.equals(request.getIsVacation());
    if (!isVacation && request.getDesiredShiftTypeId() == null) {
      throw new IllegalArgumentException("希望シフトタイプIDは必須です。");
    }

    Staff staff = staffRepository.findById(staffId)
        .orElseThrow(() -> new IllegalArgumentException("スタッフが見つかりません。"));

    ShiftType shiftType = null;
    if (!isVacation) {
      shiftType = shiftTypeRepository.findById(request.getDesiredShiftTypeId())
          .orElseThrow(() -> new IllegalArgumentException("シフトタイプが見つかりません。"));
    }

    // Create request
    ShiftRequest shiftRequest = new ShiftRequest();
    shiftRequest.setStaff(staff);
    shiftRequest.setWorkDate(request.getWorkDate());
    shiftRequest.setDesiredShiftType(shiftType);
    shiftRequest.setStatus(ShiftRequestStatus.DRAFT);
    shiftRequest.setCreatedAt(OffsetDateTime.now());
    shiftRequest.setUpdatedAt(OffsetDateTime.now());

    ShiftRequest saved = shiftRequestRepository.save(shiftRequest);
    return convertToResponse(saved);
  }

  @Transactional
  public ShiftRequestResponse updateShiftRequest(Long staffId, Long shiftRequestId, ShiftRequestUpdateRequest request) {
    ShiftRequest shiftRequest = shiftRequestRepository.findById(shiftRequestId)
        .orElseThrow(() -> new IllegalArgumentException("シフト要望が見つかりません。"));

    @SuppressWarnings("unused")
    Staff staff = staffRepository.findById(staffId)
        .orElseThrow(() -> new IllegalArgumentException("スタッフが見つかりません。"));

    // Only staff member can update their own request
    if (!shiftRequest.getStaff().getId().equals(staffId)) {
      throw new IllegalArgumentException("自分のシフト要望のみ更新できます。");
    }

    // Can only update if status is DRAFT
    if (!shiftRequest.getStatus().equals(ShiftRequestStatus.DRAFT)) {
      throw new IllegalArgumentException("下書き状態のみ更新可能です。");
    }

    assertRequestIsEditable(shiftRequest);

    // Update desired shift type if provided
    boolean isVacation = Boolean.TRUE.equals(request.getIsVacation());
    if (isVacation) {
      shiftRequest.setDesiredShiftType(null);
    } else if (request.getDesiredShiftTypeId() != null) {
      ShiftType shiftType = shiftTypeRepository.findById(request.getDesiredShiftTypeId())
          .orElseThrow(() -> new IllegalArgumentException("シフトタイプが見つかりません。"));
      shiftRequest.setDesiredShiftType(shiftType);
    }

    shiftRequest.setUpdatedAt(OffsetDateTime.now());
    ShiftRequest updated = shiftRequestRepository.save(shiftRequest);
    return convertToResponse(updated);
  }

  @Transactional
  public ShiftRequestResponse submitShiftRequest(Long staffId, Long shiftRequestId) {
    ShiftRequest shiftRequest = shiftRequestRepository.findById(shiftRequestId)
        .orElseThrow(() -> new IllegalArgumentException("シフト要望が見つかりません。"));

    // Only staff member can submit their own request
    if (!shiftRequest.getStaff().getId().equals(staffId)) {
      throw new IllegalArgumentException("自分のシフト要望のみ提出できます。");
    }

    // Can only submit if status is DRAFT
    if (!shiftRequest.getStatus().equals(ShiftRequestStatus.DRAFT)) {
      throw new IllegalArgumentException("下書き状態のみ提出可能です。");
    }

    assertRequestIsEditable(shiftRequest);

    shiftRequest.setStatus(ShiftRequestStatus.SUBMITTED);
    shiftRequest.setSubmittedAt(OffsetDateTime.now());
    shiftRequest.setUpdatedAt(OffsetDateTime.now());

    ShiftRequest updated = shiftRequestRepository.save(shiftRequest);
    return convertToResponse(updated);
  }

  @Transactional
  public ShiftRequestResponse approveShiftRequest(Long editorStaffId, Long shiftRequestId) {
    ShiftRequest shiftRequest = shiftRequestRepository.findById(shiftRequestId)
        .orElseThrow(() -> new IllegalArgumentException("シフト要望が見つかりません。"));

    Staff editor = staffRepository.findById(editorStaffId)
        .orElseThrow(() -> new IllegalArgumentException("編集者スタッフが見つかりません。"));

    // Only MASTER or CHIEF can approve
    if (!accessControlService.isMaster(editor) && !accessControlService.isChief(editor)) {
      throw new IllegalArgumentException("マスターまたはチーフのみ承認できます。");
    }

    // Can only approve if status is SUBMITTED
    if (!shiftRequest.getStatus().equals(ShiftRequestStatus.SUBMITTED)) {
      throw new IllegalArgumentException("提出済み状態のみ承認可能です。");
    }

    shiftRequest.setStatus(ShiftRequestStatus.APPLIED);
    shiftRequest.setDecidedAt(OffsetDateTime.now());
    shiftRequest.setUpdatedAt(OffsetDateTime.now());

    ShiftRequest updated = shiftRequestRepository.save(shiftRequest);
    return convertToResponse(updated);
  }

  @Transactional
  public ShiftRequestResponse rejectShiftRequest(Long editorStaffId, Long shiftRequestId) {
    ShiftRequest shiftRequest = shiftRequestRepository.findById(shiftRequestId)
        .orElseThrow(() -> new IllegalArgumentException("シフト要望が見つかりません。"));

    Staff editor = staffRepository.findById(editorStaffId)
        .orElseThrow(() -> new IllegalArgumentException("編集者スタッフが見つかりません。"));

    // Only MASTER or CHIEF can reject
    if (!accessControlService.isMaster(editor) && !accessControlService.isChief(editor)) {
      throw new IllegalArgumentException("マスターまたはチーフのみ却下できます。");
    }

    // Can only reject if status is SUBMITTED
    if (!shiftRequest.getStatus().equals(ShiftRequestStatus.SUBMITTED)) {
      throw new IllegalArgumentException("提出済み状態のみ却下可能です。");
    }

    shiftRequest.setStatus(ShiftRequestStatus.REJECTED);
    shiftRequest.setDecidedAt(OffsetDateTime.now());
    shiftRequest.setUpdatedAt(OffsetDateTime.now());

    ShiftRequest updated = shiftRequestRepository.save(shiftRequest);
    return convertToResponse(updated);
  }

  @Transactional
  public void deleteShiftRequest(Long staffId, Long shiftRequestId) {
    ShiftRequest shiftRequest = shiftRequestRepository.findById(shiftRequestId)
        .orElseThrow(() -> new IllegalArgumentException("シフト要望が見つかりません。"));

    if (!shiftRequest.getStaff().getId().equals(staffId)) {
      throw new IllegalArgumentException("自分のシフト要望のみ削除できます。");
    }

    if (!shiftRequest.getStatus().equals(ShiftRequestStatus.DRAFT)) {
      throw new IllegalArgumentException("下書き状態のみ削除可能です。");
    }

    assertRequestIsEditable(shiftRequest);

    shiftRequestRepository.delete(shiftRequest);
  }

  private void assertRequestIsEditable(ShiftRequest shiftRequest) {
    if (shiftRequest.getWorkDate() != null && shiftRequest.getWorkDate().isBefore(LocalDate.now())) {
      throw new IllegalArgumentException("過去の日付の申請は操作できません。");
    }
  }

  /**
   * 月確定時に、その月の提出済み申請を確定シフトと照合し、一致すれば反映済み、
   * 一致しなければ不採用に遷移させる。休暇申請はシフトが割り当てられていない状態を一致とみなす。
   */
  @Transactional
  public void reconcileShiftRequestsForMonth(LocalDate startDate, LocalDate endDate) {
    List<ShiftRequest> submittedRequests = shiftRequestRepository.findUnreflectedByDateRange(startDate, endDate, ShiftRequestStatus.SUBMITTED);
    if (submittedRequests.isEmpty()) {
      return;
    }

    Map<String, ShiftAssignment> assignmentByKey = shiftAssignmentRepository.findByDateRange(startDate, endDate)
        .stream()
        .collect(Collectors.toMap(
            assignment -> assignment.getStaff().getId() + "-" + assignment.getWorkDate(),
            assignment -> assignment,
            (first, second) -> first));

    OffsetDateTime now = OffsetDateTime.now();
    for (ShiftRequest request : submittedRequests) {
      ShiftAssignment assignment = assignmentByKey.get(request.getStaff().getId() + "-" + request.getWorkDate());
      boolean matches;
      if (request.getDesiredShiftType() == null) {
        matches = assignment == null;
      } else {
        matches = assignment != null
            && assignment.getShiftType() != null
            && request.getDesiredShiftType().getId().equals(assignment.getShiftType().getId());
      }

      request.setStatus(matches ? ShiftRequestStatus.APPLIED : ShiftRequestStatus.REJECTED);
      request.setDecidedAt(now);
      request.setUpdatedAt(now);
    }

    shiftRequestRepository.saveAll(submittedRequests);
  }

  public ShiftRequestResponse getShiftRequestById(Long shiftRequestId) {
    ShiftRequest shiftRequest = shiftRequestRepository.findById(shiftRequestId)
        .orElseThrow(() -> new IllegalArgumentException("シフト要望が見つかりません。"));
    return convertToResponse(shiftRequest);
  }

  public List<ShiftRequestResponse> getShiftRequestsByStaffAndDateRange(Long staffId, LocalDate startDate, LocalDate endDate) {
    return shiftRequestRepository.findByStaffIdAndDateRange(staffId, startDate, endDate)
        .stream()
        .map(this::convertToResponse)
        .collect(Collectors.toList());
  }

  public List<ShiftRequestResponse> getShiftRequestsByStaffStatusAndDateRange(Long staffId, ShiftRequestStatus status, LocalDate startDate, LocalDate endDate) {
    return shiftRequestRepository.findByStaffIdAndDateRangeAndStatus(staffId, startDate, endDate, status)
        .stream()
        .map(this::convertToResponse)
        .collect(Collectors.toList());
  }

  public List<ShiftRequestResponse> getShiftRequestsByGroupAndDateRange(Long groupId, LocalDate startDate, LocalDate endDate) {
    return shiftRequestRepository.findByGroupIdAndDateRange(groupId, startDate, endDate)
        .stream()
        .map(this::convertToResponse)
        .collect(Collectors.toList());
  }

  public List<ShiftRequestResponse> getUnreflectedShiftRequestsByDateRange(LocalDate startDate, LocalDate endDate) {
    return shiftRequestRepository.findUnreflectedByDateRange(startDate, endDate, ShiftRequestStatus.SUBMITTED)
        .stream()
        .map(this::convertToResponse)
        .collect(Collectors.toList());
  }

  private ShiftRequestResponse convertToResponse(ShiftRequest shiftRequest) {
    ShiftRequestResponse response = new ShiftRequestResponse();
    response.setId(shiftRequest.getId());
    response.setStaffId(shiftRequest.getStaff().getId());
    response.setStaffName(shiftRequest.getStaff().getStaffName());
    response.setWorkDate(shiftRequest.getWorkDate());
    if (shiftRequest.getDesiredShiftType() != null) {
      response.setDesiredShiftTypeId(shiftRequest.getDesiredShiftType().getId());
      response.setDesiredShiftCode(shiftRequest.getDesiredShiftType().getShiftCode());
      response.setDesiredShiftName(shiftRequest.getDesiredShiftType().getShiftName());
    } else {
      response.setDesiredShiftTypeId(null);
      response.setDesiredShiftCode(null);
      response.setDesiredShiftName("休暇");
    }
    response.setStatus(shiftRequest.getStatus());
    response.setSubmittedAt(shiftRequest.getSubmittedAt());
    response.setDecidedAt(shiftRequest.getDecidedAt());
    return response;
  }
}
