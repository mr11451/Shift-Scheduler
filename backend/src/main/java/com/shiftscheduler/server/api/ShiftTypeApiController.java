package com.shiftscheduler.server.api;

import com.shiftscheduler.server.annotation.RequireRole;
import com.shiftscheduler.server.service.ShiftTypeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/shift-types")
public class ShiftTypeApiController {

  @Autowired
  private ShiftTypeService shiftTypeService;

  /**
   * GET /api/shift-types - Retrieve all shift types
   */
  @GetMapping
  public ResponseEntity<List<ShiftTypeResponse>> getAllShiftTypes() {
    List<ShiftTypeResponse> shiftTypes = shiftTypeService.getAllShiftTypes();
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
   */
  @RequireRole(roles = {"MASTER"})
  @PostMapping
  public ResponseEntity<?> createShiftType(@RequestBody ShiftTypeCreateRequest request) {
    try {
      ShiftTypeResponse shiftType = shiftTypeService.createShiftType(request);
      return ResponseEntity.status(HttpStatus.CREATED).body(shiftType);
    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest().body("エラー: " + e.getMessage());
    }
  }

  /**
   * PUT /api/shift-types/{shiftTypeId} - Update shift type
   */
  @RequireRole(roles = {"MASTER"})
  @PutMapping("/{shiftTypeId}")
  public ResponseEntity<?> updateShiftType(@PathVariable Long shiftTypeId, @RequestBody ShiftTypeUpdateRequest request) {
    try {
      ShiftTypeResponse shiftType = shiftTypeService.updateShiftType(shiftTypeId, request);
      return ResponseEntity.ok(shiftType);
    } catch (IllegalArgumentException e) {
      if (e.getMessage().contains("見つかりません")) {
        return ResponseEntity.notFound().build();
      }
      return ResponseEntity.badRequest().body("エラー: " + e.getMessage());
    }
  }

  /**
   * DELETE /api/shift-types/{shiftTypeId} - Deactivate shift type
   */  @RequireRole(roles = {"MASTER"})  @DeleteMapping("/{shiftTypeId}")
  public ResponseEntity<Void> deleteShiftType(@PathVariable Long shiftTypeId) {
    try {
      shiftTypeService.deactivateShiftType(shiftTypeId);
      return ResponseEntity.noContent().build();
    } catch (IllegalArgumentException e) {
      return ResponseEntity.notFound().build();
    }
  }

  /**
   * POST /api/shift-types/{shiftTypeId}/reactivate - Reactivate shift type
   */
  @RequireRole(roles = {"MASTER"})
  @PostMapping("/{shiftTypeId}/reactivate")
  public ResponseEntity<?> reactivateShiftType(@PathVariable Long shiftTypeId) {
    try {
      ShiftTypeResponse shiftType = shiftTypeService.reactivateShiftType(shiftTypeId);
      return ResponseEntity.ok(shiftType);
    } catch (IllegalArgumentException e) {
      return ResponseEntity.notFound().build();
    }
  }
}
