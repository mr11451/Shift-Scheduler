package com.shiftscheduler.server.api;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.shiftscheduler.server.annotation.RequireRole;
import com.shiftscheduler.server.service.ShiftTypeService;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/shift-types")
public class ShiftTypeApiController {

  @Autowired
  private ShiftTypeService shiftTypeService;

  /**
   * GET /api/shift-types - Retrieve shift types. Pass active=true to only get active ones
   * (e.g. for choosing a shift type on a new assignment); omit to see the full history.
   */
  @GetMapping
  public ResponseEntity<List<ShiftTypeResponse>> getAllShiftTypes(@RequestParam(required = false) Boolean active) {
    List<ShiftTypeResponse> shiftTypes = Boolean.TRUE.equals(active)
        ? shiftTypeService.getAllActiveShiftTypes()
        : shiftTypeService.getAllShiftTypes();
    return ResponseEntity.ok(shiftTypes);
  }

  /**
   * GET /api/shift-types/active - Retrieve all active shift types
   */
  @GetMapping("/active")
  public ResponseEntity<List<ShiftTypeResponse>> getAllActiveShiftTypes() {
    List<ShiftTypeResponse> shiftTypes = shiftTypeService.getAllActiveShiftTypes();
    return ResponseEntity.ok(shiftTypes);
  }

  /**
   * GET /api/shift-types/{shiftTypeId} - Retrieve shift type by ID
   */
  @GetMapping("/{shiftTypeId}")
  public ResponseEntity<ShiftTypeResponse> getShiftTypeById(@PathVariable Long shiftTypeId) {
    try {
      ShiftTypeResponse shiftType = shiftTypeService.getShiftTypeById(shiftTypeId);
      return ResponseEntity.ok(shiftType);
    } catch (IllegalArgumentException e) {
      return ResponseEntity.notFound().build();
    }
  }

  /**
   * POST /api/shift-types - Create new shift type
   * Creator staff ID is resolved from JWT-authenticated request context.
   */
  @RequireRole(roles = {"MASTER", "CHIEF"})
  @PostMapping
  public ResponseEntity<?> createShiftType(@RequestBody ShiftTypeCreateRequest request, HttpServletRequest httpRequest) {
    try {
      Long creatorStaffId = getAuthenticatedStaffId(httpRequest);
      if (creatorStaffId == null) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("エラー: 認証が必要です。");
      }
      ShiftTypeResponse shiftType = shiftTypeService.createShiftType(creatorStaffId, request);
      return ResponseEntity.status(HttpStatus.CREATED).body(shiftType);
    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest().body("エラー: " + e.getMessage());
    }
  }

  /**
   * PUT /api/shift-types/{shiftTypeId} - Update shift type
   * MASTER can update any shift type; CHIEF can only update ones they created.
   */
  @RequireRole(roles = {"MASTER", "CHIEF"})
  @PutMapping("/{shiftTypeId}")
  public ResponseEntity<?> updateShiftType(@PathVariable Long shiftTypeId, @RequestBody ShiftTypeUpdateRequest request, HttpServletRequest httpRequest) {
    try {
      Long requesterStaffId = getAuthenticatedStaffId(httpRequest);
      if (requesterStaffId == null) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("エラー: 認証が必要です。");
      }
      ShiftTypeResponse shiftType = shiftTypeService.updateShiftType(shiftTypeId, request, requesterStaffId);
      return ResponseEntity.ok(shiftType);
    } catch (IllegalArgumentException e) {
      if (e.getMessage().contains("見つかりません")) {
        return ResponseEntity.notFound().build();
      }
      if (e.getMessage().contains("権限")) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body("エラー: " + e.getMessage());
      }
      return ResponseEntity.badRequest().body("エラー: " + e.getMessage());
    }
  }

  /**
   * DELETE /api/shift-types/{shiftTypeId} - Deactivate shift type
   * MASTER can deactivate any shift type; CHIEF can only deactivate ones they created.
   */
  @RequireRole(roles = {"MASTER", "CHIEF"})
  @DeleteMapping("/{shiftTypeId}")
  public ResponseEntity<?> deleteShiftType(@PathVariable Long shiftTypeId, HttpServletRequest httpRequest) {
    try {
      Long requesterStaffId = getAuthenticatedStaffId(httpRequest);
      if (requesterStaffId == null) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("エラー: 認証が必要です。");
      }
      shiftTypeService.deactivateShiftType(shiftTypeId, requesterStaffId);
      return ResponseEntity.noContent().build();
    } catch (IllegalArgumentException e) {
      if (e.getMessage().contains("見つかりません")) {
        return ResponseEntity.notFound().build();
      }
      if (e.getMessage().contains("権限")) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body("エラー: " + e.getMessage());
      }
      return ResponseEntity.badRequest().body("エラー: " + e.getMessage());
    }
  }

  /**
   * POST /api/shift-types/{shiftTypeId}/reactivate - Reactivate shift type
   * MASTER can reactivate any shift type; CHIEF can only reactivate ones they created.
   */
  @RequireRole(roles = {"MASTER", "CHIEF"})
  @PostMapping("/{shiftTypeId}/reactivate")
  public ResponseEntity<?> reactivateShiftType(@PathVariable Long shiftTypeId, HttpServletRequest httpRequest) {
    try {
      Long requesterStaffId = getAuthenticatedStaffId(httpRequest);
      if (requesterStaffId == null) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("エラー: 認証が必要です。");
      }
      ShiftTypeResponse shiftType = shiftTypeService.reactivateShiftType(shiftTypeId, requesterStaffId);
      return ResponseEntity.ok(shiftType);
    } catch (IllegalArgumentException e) {
      if (e.getMessage().contains("見つかりません")) {
        return ResponseEntity.notFound().build();
      }
      if (e.getMessage().contains("権限")) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body("エラー: " + e.getMessage());
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

