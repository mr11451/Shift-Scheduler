package com.shiftscheduler.server.api;

import com.shiftscheduler.server.domain.CalendarViewPermissionStatus;
import com.shiftscheduler.server.service.CalendarViewPermissionService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/calendar-view-permissions")
public class CalendarViewPermissionApiController {

  @Autowired
  private CalendarViewPermissionService calendarViewPermissionService;

  /**
   * GET /api/calendar-view-permissions/{permissionId} - Retrieve permission by ID
   */
  @GetMapping("/{permissionId}")
  public ResponseEntity<CalendarViewPermissionResponse> getCalendarViewPermissionById(@PathVariable Long permissionId) {
    try {
      CalendarViewPermissionResponse permission = calendarViewPermissionService.getCalendarViewPermissionById(permissionId);
      return ResponseEntity.ok(permission);
    } catch (IllegalArgumentException e) {
      return ResponseEntity.notFound().build();
    }
  }

  /**
   * GET /api/calendar-view-permissions/requester/{requesterStaffId}/target/{targetStaffId} - Retrieve approved permissions
   */
  @GetMapping("/requester/{requesterStaffId}/target/{targetStaffId}")
  public ResponseEntity<?> getApprovedCalendarViewPermissionsForRequester(
      @PathVariable Long requesterStaffId,
      @PathVariable Long targetStaffId) {
    var permissions = calendarViewPermissionService.getApprovedCalendarViewPermissionsForRequester(requesterStaffId, targetStaffId);
    return permissions.map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }

  /**
   * GET /api/calendar-view-permissions/requester/{requesterStaffId}/approved-targets - Retrieve approved target staffs
   */
  @GetMapping("/requester/{requesterStaffId}/approved-targets")
  public ResponseEntity<List<Long>> getApprovedTargetStaffIdsForRequester(
      @PathVariable Long requesterStaffId) {
    List<Long> permissions = calendarViewPermissionService.getApprovedTargetStaffIdsForRequester(requesterStaffId);
    return ResponseEntity.ok(permissions);
  }

  /**
   * GET /api/calendar-view-permissions/requester/{requesterStaffId}/status/{status} - Retrieve permissions by status
   */
  @GetMapping("/requester/{requesterStaffId}/status/{status}")
  public ResponseEntity<?> getCalendarViewPermissionsByRequesterAndStatus(
      @PathVariable Long requesterStaffId,
      @PathVariable String status) {
    try {
      CalendarViewPermissionStatus permissionStatus = CalendarViewPermissionStatus.valueOf(status);
      List<CalendarViewPermissionResponse> permissions = calendarViewPermissionService.getCalendarViewPermissionsByRequesterAndStatus(requesterStaffId, permissionStatus);
      return ResponseEntity.ok(permissions);
    } catch (Exception e) {
      return ResponseEntity.badRequest().body("エラー: " + e.getMessage());
    }
  }

  /**
   * GET /api/calendar-view-permissions/target/{targetStaffId}/status/{status} - Retrieve permissions for target staff by status
   */
  @GetMapping("/target/{targetStaffId}/status/{status}")
  public ResponseEntity<?> getCalendarViewPermissionsByTargetAndStatus(
      @PathVariable Long targetStaffId,
      @PathVariable String status) {
    try {
      CalendarViewPermissionStatus permissionStatus = CalendarViewPermissionStatus.valueOf(status);
      List<CalendarViewPermissionResponse> permissions = calendarViewPermissionService.getCalendarViewPermissionsByTargetAndStatus(targetStaffId, permissionStatus);
      return ResponseEntity.ok(permissions);
    } catch (Exception e) {
      return ResponseEntity.badRequest().body("エラー: " + e.getMessage());
    }
  }

  /**
   * POST /api/calendar-view-permissions - Create new permission request
   * Requester staff ID is resolved from JWT-authenticated request context.
   */
  @PostMapping
  public ResponseEntity<?> createCalendarViewPermission(
      HttpServletRequest httpRequest,
      @RequestBody CalendarViewPermissionCreateRequest request) {
    try {
      Long requesterStaffId = getAuthenticatedStaffId(httpRequest);
      if (requesterStaffId == null) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("エラー: 認証が必要です。");
      }
      CalendarViewPermissionResponse permission = calendarViewPermissionService.createCalendarViewPermission(requesterStaffId, request);
      return ResponseEntity.status(HttpStatus.CREATED).body(permission);
    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest().body("エラー: " + e.getMessage());
    }
  }

  /**
   * POST /api/calendar-view-permissions/{permissionId}/approve - Approve permission
   * Approver staff ID is resolved from JWT-authenticated request context.
   */
  @PostMapping("/{permissionId}/approve")
  public ResponseEntity<?> approveCalendarViewPermission(
      @PathVariable Long permissionId,
      HttpServletRequest httpRequest) {
    try {
      Long approverStaffId = getAuthenticatedStaffId(httpRequest);
      if (approverStaffId == null) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("エラー: 認証が必要です。");
      }
      CalendarViewPermissionResponse permission = calendarViewPermissionService.approveCalendarViewPermission(approverStaffId, permissionId);
      return ResponseEntity.ok(permission);
    } catch (IllegalArgumentException e) {
      if (e.getMessage().contains("見つかりません")) {
        return ResponseEntity.notFound().build();
      }
      return ResponseEntity.badRequest().body("エラー: " + e.getMessage());
    }
  }

  /**
   * POST /api/calendar-view-permissions/{permissionId}/reject - Reject permission
   * Rejecter staff ID is resolved from JWT-authenticated request context.
   */
  @PostMapping("/{permissionId}/reject")
  public ResponseEntity<?> rejectCalendarViewPermission(
      @PathVariable Long permissionId,
      HttpServletRequest httpRequest) {
    try {
      Long rejecterStaffId = getAuthenticatedStaffId(httpRequest);
      if (rejecterStaffId == null) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("エラー: 認証が必要です。");
      }
      CalendarViewPermissionResponse permission = calendarViewPermissionService.rejectCalendarViewPermission(rejecterStaffId, permissionId);
      return ResponseEntity.ok(permission);
    } catch (IllegalArgumentException e) {
      if (e.getMessage().contains("見つかりません")) {
        return ResponseEntity.notFound().build();
      }
      return ResponseEntity.badRequest().body("エラー: " + e.getMessage());
    }
  }

  /**
   * POST /api/calendar-view-permissions/{permissionId}/cancel - Cancel permission request
   * Requester staff ID is resolved from JWT-authenticated request context.
   */
  @PostMapping("/{permissionId}/cancel")
  public ResponseEntity<?> cancelCalendarViewPermission(
      @PathVariable Long permissionId,
      HttpServletRequest httpRequest) {
    try {
      Long requesterStaffId = getAuthenticatedStaffId(httpRequest);
      if (requesterStaffId == null) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("エラー: 認証が必要です。");
      }
      CalendarViewPermissionResponse permission = calendarViewPermissionService.cancelCalendarViewPermission(requesterStaffId, permissionId);
      return ResponseEntity.ok(permission);
    } catch (IllegalArgumentException e) {
      if (e.getMessage().contains("見つかりません")) {
        return ResponseEntity.notFound().build();
      }
      return ResponseEntity.badRequest().body("エラー: " + e.getMessage());
    }
  }

  private Long getAuthenticatedStaffId(HttpServletRequest httpRequest) {
    Object staffId = httpRequest.getAttribute("staffId");
    if (staffId instanceof Number number) {
      return number.longValue();
    }
    return null;
  }
}
