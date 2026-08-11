package com.shiftscheduler.server.api;

import com.shiftscheduler.server.service.QualificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/qualifications")
public class QualificationApiController {

  @Autowired
  private QualificationService qualificationService;

  /**
   * GET /api/qualifications - Retrieve all qualifications
   */
  @GetMapping
  public ResponseEntity<List<QualificationResponse>> getAllQualifications() {
    List<QualificationResponse> qualifications = qualificationService.getAllQualifications();
    return ResponseEntity.ok(qualifications);
  }

  /**
   * GET /api/qualifications/{qualificationId} - Retrieve qualification by ID
   */
  @GetMapping("/{qualificationId}")
  public ResponseEntity<QualificationResponse> getQualificationById(@PathVariable Long qualificationId) {
    try {
      QualificationResponse qualification = qualificationService.getQualificationById(qualificationId);
      return ResponseEntity.ok(qualification);
    } catch (IllegalArgumentException e) {
      return ResponseEntity.notFound().build();
    }
  }

  /**
   * POST /api/qualifications - Create new qualification
   */
  @PostMapping
  public ResponseEntity<?> createQualification(@RequestBody QualificationCreateRequest request) {
    try {
      QualificationResponse qualification = qualificationService.createQualification(request);
      return ResponseEntity.status(HttpStatus.CREATED).body(qualification);
    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest().body("エラー: " + e.getMessage());
    }
  }

  /**
   * PUT /api/qualifications/{qualificationId} - Update qualification
   */
  @PutMapping("/{qualificationId}")
  public ResponseEntity<?> updateQualification(@PathVariable Long qualificationId, @RequestBody QualificationUpdateRequest request) {
    try {
      QualificationResponse qualification = qualificationService.updateQualification(qualificationId, request);
      return ResponseEntity.ok(qualification);
    } catch (IllegalArgumentException e) {
      if (e.getMessage().contains("見つかりません")) {
        return ResponseEntity.notFound().build();
      }
      return ResponseEntity.badRequest().body("エラー: " + e.getMessage());
    }
  }

  /**
   * DELETE /api/qualifications/{qualificationId} - Deactivate qualification
   */
  @DeleteMapping("/{qualificationId}")
  public ResponseEntity<Void> deleteQualification(@PathVariable Long qualificationId) {
    try {
      qualificationService.deactivateQualification(qualificationId);
      return ResponseEntity.noContent().build();
    } catch (IllegalArgumentException e) {
      return ResponseEntity.notFound().build();
    }
  }

  /**
   * POST /api/qualifications/{qualificationId}/reactivate - Reactivate qualification
   */
  @PostMapping("/{qualificationId}/reactivate")
  public ResponseEntity<?> reactivateQualification(@PathVariable Long qualificationId) {
    try {
      QualificationResponse qualification = qualificationService.reactivateQualification(qualificationId);
      return ResponseEntity.ok(qualification);
    } catch (IllegalArgumentException e) {
      return ResponseEntity.notFound().build();
    }
  }
}
