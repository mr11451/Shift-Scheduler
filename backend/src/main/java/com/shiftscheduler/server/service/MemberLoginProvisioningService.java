package com.shiftscheduler.server.service;

import com.shiftscheduler.server.api.MemberLoginProvisioningCreateRequest;
import com.shiftscheduler.server.api.MemberLoginProvisioningResponse;
import com.shiftscheduler.server.api.MemberLoginProvisioningUpdateRequest;
import com.shiftscheduler.server.domain.MemberLoginProvisioning;
import com.shiftscheduler.server.domain.MemberLoginProvisioningStatus;
import com.shiftscheduler.server.domain.Staff;
import com.shiftscheduler.server.repository.MemberLoginProvisioningRepository;
import com.shiftscheduler.server.repository.StaffRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MemberLoginProvisioningService {

  @Autowired
  private MemberLoginProvisioningRepository memberLoginProvisioningRepository;

  @Autowired
  private StaffRepository staffRepository;

  @Transactional
  public MemberLoginProvisioningResponse createMemberLoginProvisioning(MemberLoginProvisioningCreateRequest request) {
    // Validate required fields
    if (request.getStaffId() == null) {
      throw new IllegalArgumentException("スタッフIDは必須です。");
    }
    if (request.getLoginCode() == null || request.getLoginCode().trim().isEmpty()) {
      throw new IllegalArgumentException("ログインコードは必須です。");
    }
    if (request.getInitialPasswordHash() == null || request.getInitialPasswordHash().trim().isEmpty()) {
      throw new IllegalArgumentException("初期パスワードハッシュは必須です。");
    }
    if (request.getAccessUrl() == null || request.getAccessUrl().trim().isEmpty()) {
      throw new IllegalArgumentException("アクセスURLは必須です。");
    }

    Staff staff = staffRepository.findById(request.getStaffId())
        .orElseThrow(() -> new IllegalArgumentException("スタッフが見つかりません。"));

    MemberLoginProvisioning provisioning = new MemberLoginProvisioning();
    provisioning.setStaff(staff);
    provisioning.setLoginCode(request.getLoginCode().trim());
    provisioning.setInitialPasswordHash(request.getInitialPasswordHash().trim());
    provisioning.setAccessUrl(request.getAccessUrl().trim());
    provisioning.setStatus(MemberLoginProvisioningStatus.ISSUED);
    provisioning.setIssuedAt(OffsetDateTime.now());
    provisioning.setExpiresAt(request.getExpiresAt());

    MemberLoginProvisioning saved = memberLoginProvisioningRepository.save(provisioning);
    return convertToResponse(saved);
  }

  @Transactional
  public MemberLoginProvisioningResponse updateMemberLoginProvisioning(Long provisioningId, MemberLoginProvisioningUpdateRequest request) {
    MemberLoginProvisioning provisioning = memberLoginProvisioningRepository.findById(provisioningId)
        .orElseThrow(() -> new IllegalArgumentException("プロビジョニング情報が見つかりません。"));

    // Can only update if status is ISSUED
    if (!provisioning.getStatus().equals(MemberLoginProvisioningStatus.ISSUED)) {
      throw new IllegalArgumentException("発行済み状態のみ更新可能です。");
    }

    // Update fields if provided
    if (request.getLoginCode() != null && !request.getLoginCode().trim().isEmpty()) {
      provisioning.setLoginCode(request.getLoginCode().trim());
    }
    if (request.getInitialPasswordHash() != null && !request.getInitialPasswordHash().trim().isEmpty()) {
      provisioning.setInitialPasswordHash(request.getInitialPasswordHash().trim());
    }
    if (request.getAccessUrl() != null && !request.getAccessUrl().trim().isEmpty()) {
      provisioning.setAccessUrl(request.getAccessUrl().trim());
    }
    if (request.getExpiresAt() != null) {
      provisioning.setExpiresAt(request.getExpiresAt());
    }

    MemberLoginProvisioning updated = memberLoginProvisioningRepository.save(provisioning);
    return convertToResponse(updated);
  }

  @Transactional
  public MemberLoginProvisioningResponse markAsSent(Long provisioningId) {
    MemberLoginProvisioning provisioning = memberLoginProvisioningRepository.findById(provisioningId)
        .orElseThrow(() -> new IllegalArgumentException("プロビジョニング情報が見つかりません。"));

    // Can only mark as sent if status is ISSUED
    if (!provisioning.getStatus().equals(MemberLoginProvisioningStatus.ISSUED)) {
      throw new IllegalArgumentException("発行済み状態のみ送信マーク可能です。");
    }

    provisioning.setStatus(MemberLoginProvisioningStatus.SENT);
    provisioning.setSentAt(OffsetDateTime.now());

    MemberLoginProvisioning updated = memberLoginProvisioningRepository.save(provisioning);
    return convertToResponse(updated);
  }

  @Transactional
  public MemberLoginProvisioningResponse markAsFailed(Long provisioningId, String errorMessage) {
    MemberLoginProvisioning provisioning = memberLoginProvisioningRepository.findById(provisioningId)
        .orElseThrow(() -> new IllegalArgumentException("プロビジョニング情報が見つかりません。"));

    provisioning.setStatus(MemberLoginProvisioningStatus.FAILED);
    provisioning.setLastErrorMessage(errorMessage);

    MemberLoginProvisioning updated = memberLoginProvisioningRepository.save(provisioning);
    return convertToResponse(updated);
  }

  public MemberLoginProvisioningResponse getMemberLoginProvisioningById(Long provisioningId) {
    MemberLoginProvisioning provisioning = memberLoginProvisioningRepository.findById(provisioningId)
        .orElseThrow(() -> new IllegalArgumentException("プロビジョニング情報が見つかりません。"));
    return convertToResponse(provisioning);
  }

  public MemberLoginProvisioningResponse getMemberLoginProvisioningByStaffId(Long staffId) {
    MemberLoginProvisioning provisioning = memberLoginProvisioningRepository.findByStaffId(staffId)
        .orElseThrow(() -> new IllegalArgumentException("プロビジョニング情報が見つかりません。"));
    return convertToResponse(provisioning);
  }

  public MemberLoginProvisioningResponse getMemberLoginProvisioningByLoginCode(String loginCode) {
    MemberLoginProvisioning provisioning = memberLoginProvisioningRepository.findByLoginCode(loginCode)
        .orElseThrow(() -> new IllegalArgumentException("プロビジョニング情報が見つかりません。"));
    return convertToResponse(provisioning);
  }

  public List<MemberLoginProvisioningResponse> getMemberLoginProvisioningByStatus(MemberLoginProvisioningStatus status) {
    return memberLoginProvisioningRepository.findByStatus(status)
        .stream()
        .map(this::convertToResponse)
        .collect(Collectors.toList());
  }

  private MemberLoginProvisioningResponse convertToResponse(MemberLoginProvisioning provisioning) {
    MemberLoginProvisioningResponse response = new MemberLoginProvisioningResponse();
    response.setId(provisioning.getId());
    response.setStaffId(provisioning.getStaff().getId());
    response.setStaffName(provisioning.getStaff().getStaffName());
    response.setLoginCode(provisioning.getLoginCode());
    response.setAccessUrl(provisioning.getAccessUrl());
    response.setStatus(provisioning.getStatus());
    response.setIssuedAt(provisioning.getIssuedAt());
    response.setExpiresAt(provisioning.getExpiresAt());
    response.setSentAt(provisioning.getSentAt());
    response.setLastErrorMessage(provisioning.getLastErrorMessage());
    return response;
  }
}
