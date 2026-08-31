package com.shiftscheduler.server.service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shiftscheduler.server.api.GroupCreateRequest;
import com.shiftscheduler.server.api.GroupResponse;
import com.shiftscheduler.server.api.GroupUpdateRequest;
import com.shiftscheduler.server.domain.Group;
import com.shiftscheduler.server.repository.GroupRepository;

@Service
public class GroupService {

  @Autowired
  private GroupRepository groupRepository;

  /**
   * Create a new group after validating required fields and code uniqueness.
   */
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

  /**
   * Update an existing group's editable fields, enforcing code uniqueness.
   */
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

  /**
   * Look up a group by ID.
   */
  public GroupResponse getGroupById(Long groupId) {
    Group group =
        groupRepository
            .findById(groupId)
            .orElseThrow(
                () -> new IllegalArgumentException("グループが見つかりません: " + groupId));
    return convertToResponse(group);
  }

  /**
   * Look up a group by its unique code.
   */
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

  /**
   * List every group regardless of active status.
   */
  public List<GroupResponse> getAllGroups() {
    return groupRepository.findAll()
        .stream()
        .map(this::convertToResponse)
        .collect(Collectors.toList());
  }

  /**
   * List only active groups.
   */
  public List<GroupResponse> getAllActiveGroups() {
    return groupRepository.findAllByIsActiveTrue()
        .stream()
        .map(this::convertToResponse)
        .collect(Collectors.toList());
  }

  /**
   * Soft-delete a group by flipping its active flag off.
   */
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

  /**
   * Restore a previously deactivated group.
   */
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

  /**
   * Check whether a group code is already in use.
   */
  public boolean groupCodeExists(String groupCode) {
    return groupRepository.findByGroupCode(groupCode).isPresent();
  }

  /**
   * Map the entity to its API response shape.
   */
  private GroupResponse convertToResponse(Group group) {
    GroupResponse response = new GroupResponse();
    response.setId(group.getId());
    response.setGroupCode(group.getGroupCode());
    response.setGroupName(group.getGroupName());
    response.setIsActive(group.getIsActive());
    return response;
  }
}
