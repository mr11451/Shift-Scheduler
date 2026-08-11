package com.shiftscheduler.server.api;

import com.shiftscheduler.server.service.GroupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/groups")
public class GroupApiController {

  @Autowired
  private GroupService groupService;

  /**
   * GET /api/groups - Retrieve all groups
   */
  @GetMapping
  public ResponseEntity<List<GroupResponse>> getAllGroups() {
    List<GroupResponse> groups = groupService.getAllGroups();
    return ResponseEntity.ok(groups);
  }

  /**
   * GET /api/groups/{groupId} - Retrieve group by ID
   */
  @GetMapping("/{groupId}")
  public ResponseEntity<GroupResponse> getGroupById(@PathVariable Long groupId) {
    try {
      GroupResponse group = groupService.getGroupById(groupId);
      return ResponseEntity.ok(group);
    } catch (IllegalArgumentException e) {
      return ResponseEntity.notFound().build();
    }
  }

  /**
   * POST /api/groups - Create new group
   */
  @PostMapping
  public ResponseEntity<?> createGroup(@RequestBody GroupCreateRequest request) {
    try {
      GroupResponse group = groupService.createGroup(request);
      return ResponseEntity.status(HttpStatus.CREATED).body(group);
    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest().body("エラー: " + e.getMessage());
    }
  }

  /**
   * PUT /api/groups/{groupId} - Update group
   */
  @PutMapping("/{groupId}")
  public ResponseEntity<?> updateGroup(@PathVariable Long groupId, @RequestBody GroupUpdateRequest request) {
    try {
      GroupResponse group = groupService.updateGroup(groupId, request);
      return ResponseEntity.ok(group);
    } catch (IllegalArgumentException e) {
      if (e.getMessage().contains("見つかりません")) {
        return ResponseEntity.notFound().build();
      }
      return ResponseEntity.badRequest().body("エラー: " + e.getMessage());
    }
  }

  /**
   * DELETE /api/groups/{groupId} - Deactivate group
   */
  @DeleteMapping("/{groupId}")
  public ResponseEntity<Void> deleteGroup(@PathVariable Long groupId) {
    try {
      groupService.deactivateGroup(groupId);
      return ResponseEntity.noContent().build();
    } catch (IllegalArgumentException e) {
      return ResponseEntity.notFound().build();
    }
  }

  /**
   * POST /api/groups/{groupId}/reactivate - Reactivate group
   */
  @PostMapping("/{groupId}/reactivate")
  public ResponseEntity<?> reactivateGroup(@PathVariable Long groupId) {
    try {
      GroupResponse group = groupService.reactivateGroup(groupId);
      return ResponseEntity.ok(group);
    } catch (IllegalArgumentException e) {
      return ResponseEntity.notFound().build();
    }
  }
}
