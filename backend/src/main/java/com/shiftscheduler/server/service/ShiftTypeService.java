package com.shiftscheduler.server.service;

import com.shiftscheduler.server.api.ShiftTypeCreateRequest;
import com.shiftscheduler.server.api.ShiftTypeResponse;
import com.shiftscheduler.server.api.ShiftTypeUpdateRequest;
import com.shiftscheduler.server.domain.ShiftType;
import com.shiftscheduler.server.repository.ShiftTypeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ShiftTypeService {

  @Autowired
  private ShiftTypeRepository shiftTypeRepository;

  @Autowired
  private SystemSettingService systemSettingService;

  @Transactional
  public ShiftTypeResponse createShiftType(ShiftTypeCreateRequest request) {
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
    shiftType.setCreatedAt(OffsetDateTime.now());
    shiftType.setUpdatedAt(OffsetDateTime.now());

    ShiftType saved = shiftTypeRepository.save(shiftType);
    return convertToResponse(saved);
  }

  @Transactional
  public ShiftTypeResponse updateShiftType(Long shiftTypeId, ShiftTypeUpdateRequest request) {
    ShiftType shiftType =
        shiftTypeRepository
            .findById(shiftTypeId)
            .orElseThrow(
                () -> new IllegalArgumentException("シフトタイプが見つかりません: " + shiftTypeId));

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
  public ShiftTypeResponse deactivateShiftType(Long shiftTypeId) {
    ShiftType shiftType =
        shiftTypeRepository
            .findById(shiftTypeId)
            .orElseThrow(
                () -> new IllegalArgumentException("シフトタイプが見つかりません: " + shiftTypeId));

    shiftType.setIsActive(false);
    shiftType.setUpdatedAt(OffsetDateTime.now());

    ShiftType updated = shiftTypeRepository.save(shiftType);
    systemSettingService.resetAutoShiftRequiredCount(shiftTypeId);
    return convertToResponse(updated);
  }

  @Transactional
  public ShiftTypeResponse reactivateShiftType(Long shiftTypeId) {
    ShiftType shiftType =
        shiftTypeRepository
            .findById(shiftTypeId)
            .orElseThrow(
                () -> new IllegalArgumentException("シフトタイプが見つかりません: " + shiftTypeId));

    shiftType.setIsActive(true);
    shiftType.setUpdatedAt(OffsetDateTime.now());

    ShiftType updated = shiftTypeRepository.save(shiftType);
    return convertToResponse(updated);
  }

  public boolean shiftCodeExists(String shiftCode) {
    return shiftTypeRepository.findByShiftCode(shiftCode).isPresent();
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
    return response;
  }
}
