package com.shiftscheduler.server.service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shiftscheduler.server.api.ShiftTypeCreateRequest;
import com.shiftscheduler.server.api.ShiftTypeResponse;
import com.shiftscheduler.server.api.ShiftTypeUpdateRequest;
import com.shiftscheduler.server.domain.ShiftType;
import com.shiftscheduler.server.domain.Staff;
import com.shiftscheduler.server.repository.ShiftTypeRepository;
import com.shiftscheduler.server.repository.StaffRepository;

@Service
public class ShiftTypeService {

  @Autowired
  private ShiftTypeRepository shiftTypeRepository;

  @Autowired
  private SystemSettingService systemSettingService;

  @Autowired
  private StaffRepository staffRepository;

  @Autowired
  private AccessControlService accessControlService;

  @Transactional
  public ShiftTypeResponse createShiftType(Long creatorStaffId, ShiftTypeCreateRequest request) {
    Staff creator = staffRepository.findById(creatorStaffId)
        .orElseThrow(() -> new IllegalArgumentException("作成者スタッフが見つかりません。"));

    // Validate required fields
    if (request.getShiftCode() == null || request.getShiftCode().trim().isEmpty()) {
      throw new IllegalArgumentException("シフトコードは必須です。");
    }
    if (request.getShiftName() == null || request.getShiftName().trim().isEmpty()) {
      throw new IllegalArgumentException("シフト名は必須です。");
    }

    // Check for duplicate shift code
    Optional<ShiftType> existing = shiftTypeRepository.findByShiftCode(request.getShiftCode());
    if (existing.isPresent()) {
      throw new IllegalArgumentException("このシフトコードは既に使用されています。");
    }

    ShiftType shiftType = new ShiftType();
    shiftType.setShiftCode(request.getShiftCode().trim());
    shiftType.setShiftName(request.getShiftName().trim());
    shiftType.setStartTime(request.getStartTime());
    shiftType.setEndTime(request.getEndTime());
    shiftType.setIsOffType(request.getIsOffType() != null ? request.getIsOffType() : false);
    shiftType.setIsActive(true);
    shiftType.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0);
    shiftType.setCreatedBy(creator);
    shiftType.setCreatedAt(OffsetDateTime.now());
    shiftType.setUpdatedAt(OffsetDateTime.now());

    ShiftType saved = shiftTypeRepository.save(shiftType);
    return convertToResponse(saved);
  }

  @Transactional
  public ShiftTypeResponse updateShiftType(Long shiftTypeId, ShiftTypeUpdateRequest request, Long requesterStaffId) {
    ShiftType shiftType =
        shiftTypeRepository
            .findById(shiftTypeId)
            .orElseThrow(
                () -> new IllegalArgumentException("シフトタイプが見つかりません: " + shiftTypeId));

    ensureCanManage(shiftType, requesterStaffId);

    // Update shift code if provided
    if (request.getShiftCode() != null && !request.getShiftCode().trim().isEmpty()) {
      String newCode = request.getShiftCode().trim();
      if (!newCode.equals(shiftType.getShiftCode())) {
        // Check for duplicate
        Optional<ShiftType> duplicate = shiftTypeRepository.findByShiftCode(newCode);
        if (duplicate.isPresent()) {
          throw new IllegalArgumentException("このシフトコードは既に使用されています。");
        }
        shiftType.setShiftCode(newCode);
      }
    }

    // Update shift name if provided
    if (request.getShiftName() != null && !request.getShiftName().trim().isEmpty()) {
      shiftType.setShiftName(request.getShiftName().trim());
    }

    // Update times if provided
    if (request.getStartTime() != null) {
      shiftType.setStartTime(request.getStartTime());
    }
    if (request.getEndTime() != null) {
      shiftType.setEndTime(request.getEndTime());
    }

    // Update is off type if provided
    if (request.getIsOffType() != null) {
      shiftType.setIsOffType(request.getIsOffType());
    }

    // Update sort order if provided
    if (request.getSortOrder() != null) {
      shiftType.setSortOrder(request.getSortOrder());
    }

    // Update active status if provided
    if (request.getIsActive() != null) {
      shiftType.setIsActive(request.getIsActive());
    }

    shiftType.setUpdatedAt(OffsetDateTime.now());
    ShiftType updated = shiftTypeRepository.save(shiftType);
    return convertToResponse(updated);
  }

  public ShiftTypeResponse getShiftTypeById(Long shiftTypeId) {
    ShiftType shiftType =
        shiftTypeRepository
            .findById(shiftTypeId)
            .orElseThrow(
                () -> new IllegalArgumentException("シフトタイプが見つかりません: " + shiftTypeId));
    return convertToResponse(shiftType);
  }

  public ShiftTypeResponse getShiftTypeByCode(String shiftCode) {
    ShiftType shiftType =
        shiftTypeRepository
            .findByShiftCode(shiftCode)
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "シフトタイプが見つかりません: " + shiftCode));
    return convertToResponse(shiftType);
  }

  public List<ShiftTypeResponse> getAllShiftTypes() {
    return shiftTypeRepository.findAll()
        .stream()
        .map(this::convertToResponse)
        .collect(Collectors.toList());
  }

  public List<ShiftTypeResponse> getAllActiveShiftTypes() {
    return shiftTypeRepository.findAllActive()
        .stream()
        .map(this::convertToResponse)
        .collect(Collectors.toList());
  }

  public List<ShiftTypeResponse> getAllActiveWorkShifts() {
    return shiftTypeRepository.findAllActiveWorkShifts()
        .stream()
        .map(this::convertToResponse)
        .collect(Collectors.toList());
  }

  @Transactional
  public ShiftTypeResponse deactivateShiftType(Long shiftTypeId, Long requesterStaffId) {
    ShiftType shiftType =
        shiftTypeRepository
            .findById(shiftTypeId)
            .orElseThrow(
                () -> new IllegalArgumentException("シフトタイプが見つかりません: " + shiftTypeId));

    ensureCanManage(shiftType, requesterStaffId);

    shiftType.setIsActive(false);
    shiftType.setUpdatedAt(OffsetDateTime.now());

    ShiftType updated = shiftTypeRepository.save(shiftType);
    systemSettingService.resetAutoShiftRequiredCount(shiftTypeId);
    return convertToResponse(updated);
  }

  @Transactional
  public ShiftTypeResponse reactivateShiftType(Long shiftTypeId, Long requesterStaffId) {
    ShiftType shiftType =
        shiftTypeRepository
            .findById(shiftTypeId)
            .orElseThrow(
                () -> new IllegalArgumentException("シフトタイプが見つかりません: " + shiftTypeId));

    ensureCanManage(shiftType, requesterStaffId);

    shiftType.setIsActive(true);
    shiftType.setUpdatedAt(OffsetDateTime.now());

    ShiftType updated = shiftTypeRepository.save(shiftType);
    return convertToResponse(updated);
  }

  public boolean shiftCodeExists(String shiftCode) {
    return shiftTypeRepository.findByShiftCode(shiftCode).isPresent();
  }

  /**
   * MASTER can manage any shift type; CHIEF can only manage the ones they created.
   */
  private void ensureCanManage(ShiftType shiftType, Long requesterStaffId) {
    Staff requester = staffRepository.findById(requesterStaffId)
        .orElseThrow(() -> new IllegalArgumentException("更新者スタッフが見つかりません。"));

    if (accessControlService.isMaster(requester)) {
      return;
    }

    if (accessControlService.isChief(requester)
        && shiftType.getCreatedBy() != null
        && shiftType.getCreatedBy().getId().equals(requester.getId())) {
      return;
    }

    throw new IllegalArgumentException("この操作を行う権限がありません。");
  }

  private ShiftTypeResponse convertToResponse(ShiftType shiftType) {
    ShiftTypeResponse response = new ShiftTypeResponse();
    response.setId(shiftType.getId());
    response.setShiftCode(shiftType.getShiftCode());
    response.setShiftName(shiftType.getShiftName());
    response.setStartTime(shiftType.getStartTime());
    response.setEndTime(shiftType.getEndTime());
    response.setIsOffType(shiftType.getIsOffType());
    response.setIsActive(shiftType.getIsActive());
    response.setSortOrder(shiftType.getSortOrder());
    if (shiftType.getCreatedBy() != null) {
      response.setCreatedByStaffId(shiftType.getCreatedBy().getId());
      response.setCreatedByStaffName(shiftType.getCreatedBy().getStaffName());
    }
    return response;
  }
}
