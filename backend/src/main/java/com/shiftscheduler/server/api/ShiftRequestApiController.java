package com.shiftscheduler.server.api;

import com.shiftscheduler.server.domain.ShiftRequestStatus;
import com.shiftscheduler.server.service.ShiftRequestService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/shift-requests")
public class ShiftRequestApiController {

  @Autowired
  private ShiftRequestService shiftRequestService;

  /**
   * GET /api/shift-requests/{shiftRequestId} - Retrieve shift request by ID
   */
  @GetMapping("/{shiftRequestId}")
  public ResponseEntity<ShiftRequestResponse> getShiftRequestById(@PathVariable Long shiftRequestId) {
    try {
      ShiftRequestResponse request = shiftRequestService.getShiftRequestById(shiftRequestId);
      return ResponseEntity.ok(request);
    } catch (IllegalArgumentException e) {
      return ResponseEntity.notFound().build();
    }
  }

  /**
   * GET /api/shift-requests/staff/{staffId} - Retrieve shift requests for staff by date range
   * Query params: startDate, endDate (yyyy-MM-dd format)
   */
  @GetMapping("/staff/{staffId}")
  public ResponseEntity<?> getShiftRequestsByStaffAndDateRange(
      @PathVariable Long staffId,
      @RequestParam String startDate,
      @RequestParam String endDate) {
    try {
      LocalDate start = LocalDate.parse(startDate);
      LocalDate end = LocalDate.parse(endDate);
      List<ShiftRequestResponse> requests = shiftRequestService.getShiftRequestsByStaffAndDateRange(staffId, start, end);
      return ResponseEntity.ok(requests);
    } catch (Exception e) {
      return ResponseEntity.badRequest().body("エラー: " + e.getMessage());
    }
  }

  /**
   * GET /api/shift-requests/staff/{staffId}/status/{status} - Retrieve shift requests for staff by status and date range
   * Query params: startDate, endDate (yyyy-MM-dd format)
   * Status: DRAFT, SUBMITTED, APPLIED, REJECTED
   */
  @GetMapping("/staff/{staffId}/status/{status}")
  public ResponseEntity<?> getShiftRequestsByStaffStatusAndDateRange(
      @PathVariable Long staffId,
      @PathVariable String status,
      @RequestParam String startDate,
      @RequestParam String endDate) {
    try {
      ShiftRequestStatus requestStatus = ShiftRequestStatus.valueOf(status);
      LocalDate start = LocalDate.parse(startDate);
      LocalDate end = LocalDate.parse(endDate);
      List<ShiftRequestResponse> requests = shiftRequestService.getShiftRequestsByStaffStatusAndDateRange(staffId, requestStatus, start, end);
      return ResponseEntity.ok(requests);
    } catch (Exception e) {
      return ResponseEntity.badRequest().body("エラー: " + e.getMessage());
    }
  }

  /**
   * GET /api/shift-requests/group/{groupId} - Retrieve shift requests for group by date range
   * Query params: startDate, endDate (yyyy-MM-dd format)
   */
  @GetMapping("/group/{groupId}")
  public ResponseEntity<?> getShiftRequestsByGroupAndDateRange(
      @PathVariable Long groupId,
      @RequestParam String startDate,
      @RequestParam String endDate) {
    try {
      LocalDate start = LocalDate.parse(startDate);
      LocalDate end = LocalDate.parse(endDate);
      List<ShiftRequestResponse> requests = shiftRequestService.getShiftRequestsByGroupAndDateRange(groupId, start, end);
      return ResponseEntity.ok(requests);
    } catch (Exception e) {
      return ResponseEntity.badRequest().body("エラー: " + e.getMessage());
    }
  }

  /**
   * GET /api/shift-requests/unreflected - Retrieve unreflected shift requests by date range
   * Query params: startDate, endDate (yyyy-MM-dd format)
   */
  @GetMapping("/unreflected")
  public ResponseEntity<?> getUnreflectedShiftRequestsByDateRange(
      @RequestParam String startDate,
      @RequestParam String endDate) {
    try {
      LocalDate start = LocalDate.parse(startDate);
      LocalDate end = LocalDate.parse(endDate);
      List<ShiftRequestResponse> requests = shiftRequestService.getUnreflectedShiftRequestsByDateRange(start, end);
      return ResponseEntity.ok(requests);
    } catch (Exception e) {
      return ResponseEntity.badRequest().body("エラー: " + e.getMessage());
    }
  }

  /**
   * POST /api/shift-requests - Create new shift request
   * Staff ID is resolved from JWT-authenticated request context.
   */
  @PostMapping
  public ResponseEntity<?> createShiftRequest(
      HttpServletRequest httpRequest,
      @RequestBody ShiftRequestCreateRequest request) {
    try {
      Long staffId = getAuthenticatedStaffId(httpRequest);
      if (staffId == null) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("エラー: 認証が必要です。");
      }
      ShiftRequestResponse response = shiftRequestService.createShiftRequest(staffId, request);
      return ResponseEntity.status(HttpStatus.CREATED).body(response);
    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest().body("エラー: " + e.getMessage());
    }
  }

  /**
   * PUT /api/shift-requests/{shiftRequestId} - Update shift request
   * Staff ID is resolved from JWT-authenticated request context.
   */
  @PutMapping("/{shiftRequestId}")
  public ResponseEntity<?> updateShiftRequest(
      @PathVariable Long shiftRequestId,
      HttpServletRequest httpRequest,
      @RequestBody ShiftRequestUpdateRequest request) {
    try {
      Long staffId = getAuthenticatedStaffId(httpRequest);
      if (staffId == null) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("エラー: 認証が必要です。");
      }
      ShiftRequestResponse response = shiftRequestService.updateShiftRequest(staffId, shiftRequestId, request);
      return ResponseEntity.ok(response);
    } catch (IllegalArgumentException e) {
      if (e.getMessage().contains("見つかりません")) {
        return ResponseEntity.notFound().build();
      }
      return ResponseEntity.badRequest().body("エラー: " + e.getMessage());
    }
  }

  /**
   * POST /api/shift-requests/{shiftRequestId}/submit - Submit shift request
   * Staff ID is resolved from JWT-authenticated request context.
   */
  @PostMapping("/{shiftRequestId}/submit")
  public ResponseEntity<?> submitShiftRequest(
      @PathVariable Long shiftRequestId,
      HttpServletRequest httpRequest) {
    try {
      Long staffId = getAuthenticatedStaffId(httpRequest);
      if (staffId == null) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("エラー: 認証が必要です。");
      }
      ShiftRequestResponse response = shiftRequestService.submitShiftRequest(staffId, shiftRequestId);
      return ResponseEntity.ok(response);
    } catch (IllegalArgumentException e) {
      if (e.getMessage().contains("見つかりません")) {
        return ResponseEntity.notFound().build();
      }
      return ResponseEntity.badRequest().body("エラー: " + e.getMessage());
    }
  }

  /**
   * POST /api/shift-requests/{shiftRequestId}/approve - Approve shift request
   * Editor staff ID is resolved from JWT-authenticated request context.
   */
  @PostMapping("/{shiftRequestId}/approve")
  public ResponseEntity<?> approveShiftRequest(
      @PathVariable Long shiftRequestId,
      HttpServletRequest httpRequest) {
    try {
      Long editorStaffId = getAuthenticatedStaffId(httpRequest);
      if (editorStaffId == null) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("エラー: 認証が必要です。");
      }
      ShiftRequestResponse response = shiftRequestService.approveShiftRequest(editorStaffId, shiftRequestId);
      return ResponseEntity.ok(response);
    } catch (IllegalArgumentException e) {
      if (e.getMessage().contains("見つかりません")) {
        return ResponseEntity.notFound().build();
      }
      return ResponseEntity.badRequest().body("エラー: " + e.getMessage());
    }
  }

  /**
   * POST /api/shift-requests/{shiftRequestId}/reject - Reject shift request
   * Editor staff ID is resolved from JWT-authenticated request context.
   */
  @PostMapping("/{shiftRequestId}/reject")
  public ResponseEntity<?> rejectShiftRequest(
      @PathVariable Long shiftRequestId,
      HttpServletRequest httpRequest) {
    try {
      Long editorStaffId = getAuthenticatedStaffId(httpRequest);
      if (editorStaffId == null) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("エラー: 認証が必要です。");
      }
      ShiftRequestResponse response = shiftRequestService.rejectShiftRequest(editorStaffId, shiftRequestId);
      return ResponseEntity.ok(response);
    } catch (IllegalArgumentException e) {
      if (e.getMessage().contains("見つかりません")) {
        return ResponseEntity.notFound().build();
      }
      return ResponseEntity.badRequest().body("エラー: " + e.getMessage());
    }
  }

  @DeleteMapping("/{shiftRequestId}")
  public ResponseEntity<?> deleteShiftRequest(
      @PathVariable Long shiftRequestId,
      HttpServletRequest httpRequest) {
    try {
      Long staffId = getAuthenticatedStaffId(httpRequest);
      if (staffId == null) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("エラー: 認証が必要です。");
      }
      shiftRequestService.deleteShiftRequest(staffId, shiftRequestId);
      return ResponseEntity.noContent().build();
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
