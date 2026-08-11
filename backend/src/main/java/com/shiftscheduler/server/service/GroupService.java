package com.shiftscheduler.server.service;

import com.shiftscheduler.server.api.GroupCreateRequest;
import com.shiftscheduler.server.api.GroupResponse;
import com.shiftscheduler.server.api.GroupUpdateRequest;
import com.shiftscheduler.server.domain.Group;
import com.shiftscheduler.server.repository.GroupRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class GroupService {

  @Autowired
  private GroupRepository groupRepository;

  @Transactional
  public GroupResponse createGroup(GroupCreateRequest request) {
    // Validate required fields
    if (request.getGroupCode() == null || request.getGroupCode().trim().isEmpty()) {
      throw new IllegalArgumentException("グループコードは必須です。");
    }
    if (request.getGroupName() == null || request.getGroupName().trim().isEmpty()) {
      throw new IllegalArgumentException("グループ名は必須です。");
    }

    // Check for duplicate group code
    Optional<Group> existing = groupRepository.findByGroupCode(request.getGroupCode());
    if (existing.isPresent()) {
      throw new IllegalArgumentException("このグループコードは既に使用されています。");
    }

    Group group = new Group();
    group.setGroupCode(request.getGroupCode().trim());
    group.setGroupName(request.getGroupName().trim());
    group.setIsActive(true);
    group.setCreatedAt(OffsetDateTime.now());
    group.setUpdatedAt(OffsetDateTime.now());

    Group saved = groupRepository.save(group);
    return convertToResponse(saved);
  }

  @Transactional
  public GroupResponse updateGroup(Long groupId, GroupUpdateRequest request) {
    Group group =
        groupRepository
            .findById(groupId)
            .orElseThrow(
                () -> new IllegalArgumentException("グループが見つかりません: " + groupId));

    // Update group code if provided
    if (request.getGroupCode() != null && !request.getGroupCode().trim().isEmpty()) {
      String newCode = request.getGroupCode().trim();
      if (!newCode.equals(group.getGroupCode())) {
        // Check for duplicate
        Optional<Group> duplicate = groupRepository.findByGroupCode(newCode);
        if (duplicate.isPresent()) {
          throw new IllegalArgumentException("このグループコードは既に使用されています。");
        }
        group.setGroupCode(newCode);
      }
    }

    // Update group name if provided
    if (request.getGroupName() != null && !request.getGroupName().trim().isEmpty()) {
      group.setGroupName(request.getGroupName().trim());
    }

    // Update active status if provided
    if (request.getIsActive() != null) {
      group.setIsActive(request.getIsActive());
    }

    group.setUpdatedAt(OffsetDateTime.now());
    Group updated = groupRepository.save(group);
    return convertToResponse(updated);
  }

  public GroupResponse getGroupById(Long groupId) {
    Group group =
        groupRepository
            .findById(groupId)
            .orElseThrow(
                () -> new IllegalArgumentException("グループが見つかりません: " + groupId));
    return convertToResponse(group);
  }

  public GroupResponse getGroupByCode(String groupCode) {
    Group group =
        groupRepository
            .findByGroupCode(groupCode)
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "グループが見つかりません: " + groupCode));
    return convertToResponse(group);
  }

  public List<GroupResponse> getAllGroups() {
    return groupRepository.findAll()
        .stream()
        .map(this::convertToResponse)
        .collect(Collectors.toList());
  }

  public List<GroupResponse> getAllActiveGroups() {
    return groupRepository.findAllByIsActiveTrue()
        .stream()
        .map(this::convertToResponse)
        .collect(Collectors.toList());
  }

  @Transactional
  public GroupResponse deactivateGroup(Long groupId) {
    Group group =
        groupRepository
            .findById(groupId)
            .orElseThrow(
                () -> new IllegalArgumentException("グループが見つかりません: " + groupId));

    group.setIsActive(false);
    group.setUpdatedAt(OffsetDateTime.now());

    Group updated = groupRepository.save(group);
    return convertToResponse(updated);
  }

  @Transactional
  public GroupResponse reactivateGroup(Long groupId) {
    Group group =
        groupRepository
            .findById(groupId)
            .orElseThrow(
                () -> new IllegalArgumentException("グループが見つかりません: " + groupId));

    group.setIsActive(true);
    group.setUpdatedAt(OffsetDateTime.now());

    Group updated = groupRepository.save(group);
    return convertToResponse(updated);
  }

  public boolean groupCodeExists(String groupCode) {
    return groupRepository.findByGroupCode(groupCode).isPresent();
  }

  private GroupResponse convertToResponse(Group group) {
    GroupResponse response = new GroupResponse();
    response.setId(group.getId());
    response.setGroupCode(group.getGroupCode());
    response.setGroupName(group.getGroupName());
    response.setIsActive(group.getIsActive());
    return response;
  }
}
