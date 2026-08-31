package com.shiftscheduler.server.service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shiftscheduler.server.api.QualificationCreateRequest;
import com.shiftscheduler.server.api.QualificationResponse;
import com.shiftscheduler.server.api.QualificationUpdateRequest;
import com.shiftscheduler.server.domain.Qualification;
import com.shiftscheduler.server.repository.QualificationRepository;

@Service
public class QualificationService {

  @Autowired
  private QualificationRepository qualificationRepository;

  /**
   * Create a new qualification after validating the name and uniqueness.
   */
  @Transactional
  public QualificationResponse createQualification(QualificationCreateRequest request) {
    // Validate required fields
    if (request.getQualificationName() == null || request.getQualificationName().trim().isEmpty()) {
      throw new IllegalArgumentException("資格名は必須です。");
    }

    // Check for duplicate qualification name
    Optional<Qualification> existing = qualificationRepository.findByQualificationName(request.getQualificationName());
    if (existing.isPresent()) {
      throw new IllegalArgumentException("この資格名は既に使用されています。");
    }

    Qualification qualification = new Qualification();
    qualification.setQualificationName(request.getQualificationName().trim());
    qualification.setDescription(request.getDescription() != null ? request.getDescription().trim() : null);
    qualification.setIsActive(true);
    qualification.setCreatedAt(OffsetDateTime.now());
    qualification.setUpdatedAt(OffsetDateTime.now());

    Qualification saved = qualificationRepository.save(qualification);
    return convertToResponse(saved);
  }

  /**
   * Update an existing qualification's editable fields, enforcing name uniqueness.
   */
  @Transactional
  public QualificationResponse updateQualification(Long qualificationId, QualificationUpdateRequest request) {
    Qualification qualification =
        qualificationRepository
            .findById(qualificationId)
            .orElseThrow(
                () -> new IllegalArgumentException("資格が見つかりません: " + qualificationId));

    // Update qualification name if provided
    if (request.getQualificationName() != null && !request.getQualificationName().trim().isEmpty()) {
      String newName = request.getQualificationName().trim();
      if (!newName.equals(qualification.getQualificationName())) {
        // Check for duplicate
        Optional<Qualification> duplicate = qualificationRepository.findByQualificationName(newName);
        if (duplicate.isPresent()) {
          throw new IllegalArgumentException("この資格名は既に使用されています。");
        }
        qualification.setQualificationName(newName);
      }
    }

    // Update description if provided
    if (request.getDescription() != null) {
      qualification.setDescription(request.getDescription().trim());
    }

    // Update active status if provided
    if (request.getIsActive() != null) {
      qualification.setIsActive(request.getIsActive());
    }

    qualification.setUpdatedAt(OffsetDateTime.now());
    Qualification updated = qualificationRepository.save(qualification);
    return convertToResponse(updated);
  }

  /**
   * Look up a qualification by ID.
   */
  public QualificationResponse getQualificationById(Long qualificationId) {
    Qualification qualification =
        qualificationRepository
            .findById(qualificationId)
            .orElseThrow(
                () -> new IllegalArgumentException("資格が見つかりません: " + qualificationId));
    return convertToResponse(qualification);
  }

  /**
   * Look up a qualification by its unique name.
   */
  public QualificationResponse getQualificationByName(String qualificationName) {
    Qualification qualification =
        qualificationRepository
            .findByQualificationName(qualificationName)
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "資格が見つかりません: " + qualificationName));
    return convertToResponse(qualification);
  }

  /**
   * List every qualification regardless of active status.
   */
  public List<QualificationResponse> getAllQualifications() {
    return qualificationRepository.findAll()
        .stream()
        .map(this::convertToResponse)
        .collect(Collectors.toList());
  }

  /**
   * List only active qualifications.
   */
  public List<QualificationResponse> getAllActiveQualifications() {
    return qualificationRepository.findAllActive()
        .stream()
        .map(this::convertToResponse)
        .collect(Collectors.toList());
  }

  /**
   * Soft-delete a qualification by flipping its active flag off.
   */
  @Transactional
  public QualificationResponse deactivateQualification(Long qualificationId) {
    Qualification qualification =
        qualificationRepository
            .findById(qualificationId)
            .orElseThrow(
                () -> new IllegalArgumentException("資格が見つかりません: " + qualificationId));

    qualification.setIsActive(false);
    qualification.setUpdatedAt(OffsetDateTime.now());

    Qualification updated = qualificationRepository.save(qualification);
    return convertToResponse(updated);
  }

  /**
   * Restore a previously deactivated qualification.
   */
  @Transactional
  public QualificationResponse reactivateQualification(Long qualificationId) {
    Qualification qualification =
        qualificationRepository
            .findById(qualificationId)
            .orElseThrow(
                () -> new IllegalArgumentException("資格が見つかりません: " + qualificationId));

    qualification.setIsActive(true);
    qualification.setUpdatedAt(OffsetDateTime.now());

    Qualification updated = qualificationRepository.save(qualification);
    return convertToResponse(updated);
  }

  /**
   * Check whether a qualification name is already in use.
   */
  public boolean qualificationNameExists(String qualificationName) {
    return qualificationRepository.findByQualificationName(qualificationName).isPresent();
  }

  /**
   * Map the entity to its API response shape.
   */
  private QualificationResponse convertToResponse(Qualification qualification) {
    QualificationResponse response = new QualificationResponse();
    response.setId(qualification.getId());
    response.setQualificationName(qualification.getQualificationName());
    response.setDescription(qualification.getDescription());
    response.setIsActive(qualification.getIsActive());
    return response;
  }
}
