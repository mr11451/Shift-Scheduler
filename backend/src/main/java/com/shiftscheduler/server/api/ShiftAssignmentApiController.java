package com.shiftscheduler.server.api;

import com.shiftscheduler.server.annotation.RequireRole;
import com.shiftscheduler.server.service.ShiftAssignmentService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;
import java.util.List;

@RestController
@RequestMapping("/api/shift-assignments")
public class ShiftAssignmentApiController {

  @Autowired
  private ShiftAssignmentService shiftAssignmentService;

  /**
   * GET /api/shift-assignments/{shiftAssignmentId} - Retrieve shift assignment by ID
   */
  @GetMapping("/{shiftAssignmentId}")
  public ResponseEntity<ShiftAssignmentResponse> getShiftAssignmentById(@PathVariable Long shiftAssignmentId) {
    try {
      ShiftAssignmentResponse assignment = shiftAssignmentService.getShiftAssignmentById(shiftAssignmentId);
      return ResponseEntity.ok(assignment);
    } catch (IllegalArgumentException e) {
      return ResponseEntity.notFound().build();
    }
  }

  /**
   * GET /api/shift-assignments/staff/{staffId} - Retrieve shift assignments for staff by date range
   * Query params: startDate, endDate (yyyy-MM-dd format)
   */
  @GetMapping("/staff/{staffId}")
  public ResponseEntity<?> getShiftAssignmentsByStaffAndDateRange(
      @PathVariable Long staffId,
      @RequestParam String startDate,
      @RequestParam String endDate) {
    try {
      LocalDate start = LocalDate.parse(startDate);
      LocalDate end = LocalDate.parse(endDate);
      List<ShiftAssignmentResponse> assignments = shiftAssignmentService.getShiftAssignmentsByStaffAndDateRange(staffId, start, end);
      return ResponseEntity.ok(assignments);
    } catch (Exception e) {
      return ResponseEntity.badRequest().body("エラー: " + e.getMessage());
    }
  }

  /**
   * GET /api/shift-assignments/group/{groupId} - Retrieve shift assignments for group by date range
   * Query params: startDate, endDate (yyyy-MM-dd format)
   */
  @GetMapping("/group/{groupId}")
  public ResponseEntity<?> getShiftAssignmentsByGroupAndDateRange(
      @PathVariable Long groupId,
      @RequestParam String startDate,
      @RequestParam String endDate) {
    try {
      LocalDate start = LocalDate.parse(startDate);
      LocalDate end = LocalDate.parse(endDate);
      List<ShiftAssignmentResponse> assignments = shiftAssignmentService.getShiftAssignmentsByGroupAndDateRange(groupId, start, end);
      return ResponseEntity.ok(assignments);
    } catch (Exception e) {
      return ResponseEntity.badRequest().body("エラー: " + e.getMessage());
    }
  }

  /**
   * GET /api/shift-assignments - Retrieve shift assignments by date range
   * Query params: startDate, endDate (yyyy-MM-dd format)
   */
  @GetMapping
  public ResponseEntity<?> getShiftAssignmentsByDateRange(
      @RequestParam String startDate,
      @RequestParam String endDate) {
    try {
      LocalDate start = LocalDate.parse(startDate);
      LocalDate end = LocalDate.parse(endDate);
      List<ShiftAssignmentResponse> assignments = shiftAssignmentService.getShiftAssignmentsByDateRange(start, end);
      return ResponseEntity.ok(assignments);
    } catch (Exception e) {
      return ResponseEntity.badRequest().body("エラー: " + e.getMessage());
    }
  }

  /**
   * POST /api/shift-assignments/auto-generate - Auto-generate monthly shift assignments
   * Query params: year, month
   */
  @RequireRole(roles = {"CHIEF", "MASTER"})
  @PostMapping(value = "/auto-generate", params = {"year", "month"})
  public ResponseEntity<?> autoGenerateShiftAssignments(
      HttpServletRequest httpRequest,
      @RequestParam int year,
      @RequestParam int month) {
    try {
      Long editorStaffId = getAuthenticatedStaffId(httpRequest);
      if (editorStaffId == null) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("エラー: 認証が必要です。");
      }
      AutoShiftGenerationResultResponse result = shiftAssignmentService.autoGenerateShiftAssignments(editorStaffId, year, month);
      return ResponseEntity.ok(result);
    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest().body("エラー: " + e.getMessage());
    }
  }

  /** POST /api/shift-assignments/auto-generate?date=yyyy-MM-dd - Generate for a closing-date period. */
  @RequireRole(roles = {"CHIEF", "MASTER"})
  @PostMapping(value = "/auto-generate", params = "date")
  public ResponseEntity<?> autoGenerateShiftAssignmentsForPeriod(
      HttpServletRequest httpRequest,
      @RequestParam String date) {
    try {
      Long editorStaffId = getAuthenticatedStaffId(httpRequest);
      if (editorStaffId == null) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("エラー: 認証が必要です。");
      }
      return ResponseEntity.ok(shiftAssignmentService.autoGenerateShiftAssignmentsForPeriod(editorStaffId, LocalDate.parse(date)));
    } catch (Exception e) {
      return ResponseEntity.badRequest().body("エラー: " + e.getMessage());
    }
  }

  /**
   * DELETE /api/shift-assignments/month - Clear monthly shift assignments
   * Query params: year, month
   * Shift requests are not modified by this endpoint.
   */
  @RequireRole(roles = {"CHIEF", "MASTER"})
  @DeleteMapping("/month")
  public ResponseEntity<?> clearMonthlyShiftAssignments(
      HttpServletRequest httpRequest,
      @RequestParam int year,
      @RequestParam int month) {
    try {
      Long editorStaffId = getAuthenticatedStaffId(httpRequest);
      if (editorStaffId == null) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("エラー: 認証が必要です。");
      }

      int deletedCount = shiftAssignmentService.clearMonthlyShiftAssignments(editorStaffId, year, month);
      return ResponseEntity.ok(Map.of(
          "year", year,
          "month", month,
          "deletedCount", deletedCount,
          "message", "シフト状態をクリアしました。申請データは保持されています。"));
    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest().body("エラー: " + e.getMessage());
    }
  }

  /** DELETE /api/shift-assignments/period?date=yyyy-MM-dd - Clear assignments for a closing-date period. */
  @RequireRole(roles = {"CHIEF", "MASTER"})
  @DeleteMapping("/period")
  public ResponseEntity<?> clearShiftAssignmentsForPeriod(HttpServletRequest httpRequest, @RequestParam String date) {
    try {
      Long editorStaffId = getAuthenticatedStaffId(httpRequest);
      if (editorStaffId == null) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("エラー: 認証が必要です。");
      }
      int deletedCount = shiftAssignmentService.clearShiftAssignmentsForPeriod(editorStaffId, LocalDate.parse(date));
      return ResponseEntity.ok(Map.of("deletedCount", deletedCount));
    } catch (Exception e) {
      return ResponseEntity.badRequest().body("エラー: " + e.getMessage());
    }
  }

  /**
   * POST /api/shift-assignments - Create new shift assignment
   * Editor staff ID is resolved from JWT-authenticated request context.
   */
  @RequireRole(roles = {"CHIEF", "MASTER"})
  @PostMapping
  public ResponseEntity<?> createShiftAssignment(
      HttpServletRequest httpRequest,
      @RequestBody ShiftAssignmentCreateRequest request) {
    try {
      Long editorStaffId = getAuthenticatedStaffId(httpRequest);
      if (editorStaffId == null) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("エラー: 認証が必要です。");
      }
      ShiftAssignmentResponse assignment = shiftAssignmentService.createShiftAssignment(editorStaffId, request);
      return ResponseEntity.status(HttpStatus.CREATED).body(assignment);
    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest().body("エラー: " + e.getMessage());
    }
  }

  /**
   * PUT /api/shift-assignments/{shiftAssignmentId} - Update shift assignment
   * Editor staff ID is resolved from JWT-authenticated request context.
   */
  @RequireRole(roles = {"CHIEF", "MASTER"})
  @PutMapping("/{shiftAssignmentId}")
  public ResponseEntity<?> updateShiftAssignment(
      @PathVariable Long shiftAssignmentId,
      HttpServletRequest httpRequest,
      @RequestBody ShiftAssignmentUpdateRequest request) {
    try {
      Long editorStaffId = getAuthenticatedStaffId(httpRequest);
      if (editorStaffId == null) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("エラー: 認証が必要です。");
      }
      ShiftAssignmentResponse assignment = shiftAssignmentService.updateShiftAssignment(editorStaffId, shiftAssignmentId, request);
      return ResponseEntity.ok(assignment);
    } catch (IllegalArgumentException e) {
      if (e.getMessage().contains("見つかりません")) {
        return ResponseEntity.notFound().build();
      }
      return ResponseEntity.badRequest().body("エラー: " + e.getMessage());
    }
  }

  /**
   * DELETE /api/shift-assignments/{shiftAssignmentId} - Delete shift assignment
   * Editor staff ID is resolved from JWT-authenticated request context.
   */
  @RequireRole(roles = {"CHIEF", "MASTER"})
  @DeleteMapping("/{shiftAssignmentId}")
  public ResponseEntity<?> deleteShiftAssignment(
      @PathVariable Long shiftAssignmentId,
      HttpServletRequest httpRequest) {
    try {
      Long editorStaffId = getAuthenticatedStaffId(httpRequest);
      if (editorStaffId == null) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("エラー: 認証が必要です。");
      }
      shiftAssignmentService.deleteShiftAssignment(editorStaffId, shiftAssignmentId);
      return ResponseEntity.noContent().build();
    } catch (IllegalArgumentException e) {
      if (e.getMessage().contains("見つかりません")) {
        return ResponseEntity.notFound().build();
      }
      return ResponseEntity.badRequest().body("エラー: " + e.getMessage());
    }
  }

  /**
   * DELETE /api/shift-assignments/staff/{staffId} - Delete shift assignments for staff by date range
   * Query params: startDate, endDate (yyyy-MM-dd format)
   * Editor staff ID is resolved from JWT-authenticated request context.
   */
  @RequireRole(roles = {"CHIEF", "MASTER"})
  @DeleteMapping("/staff/{staffId}")
  public ResponseEntity<?> deleteShiftAssignmentsByStaffAndDateRange(
      @PathVariable Long staffId,
      @RequestParam String startDate,
      @RequestParam String endDate,
      HttpServletRequest httpRequest) {
    try {
      Long editorStaffId = getAuthenticatedStaffId(httpRequest);
      if (editorStaffId == null) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("エラー: 認証が必要です。");
      }
      LocalDate start = LocalDate.parse(startDate);
      LocalDate end = LocalDate.parse(endDate);
      shiftAssignmentService.deleteShiftAssignmentsByStaffAndDateRange(editorStaffId, staffId, start, end);
      return ResponseEntity.noContent().build();
    } catch (Exception e) {
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
