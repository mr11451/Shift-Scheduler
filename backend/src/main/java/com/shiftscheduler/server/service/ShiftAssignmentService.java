package com.shiftscheduler.server.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shiftscheduler.server.api.AutoShiftGenerationResultResponse;
import com.shiftscheduler.server.api.ShiftAssignmentCreateRequest;
import com.shiftscheduler.server.api.ShiftAssignmentResponse;
import com.shiftscheduler.server.api.ShiftAssignmentUpdateRequest;
import com.shiftscheduler.server.domain.ShiftAssignment;
import com.shiftscheduler.server.domain.ShiftRequest;
import com.shiftscheduler.server.domain.ShiftRequestStatus;
import com.shiftscheduler.server.domain.ShiftType;
import com.shiftscheduler.server.domain.Staff;
import com.shiftscheduler.server.repository.ShiftAssignmentRepository;
import com.shiftscheduler.server.repository.ShiftRequestRepository;
import com.shiftscheduler.server.repository.ShiftTypeRepository;
import com.shiftscheduler.server.repository.StaffRepository;

@Service
public class ShiftAssignmentService {

  private static final int MAX_AUTO_GENERATION_RETRIES = 10;

  @Autowired
  private ShiftAssignmentRepository shiftAssignmentRepository;

  @Autowired
  private StaffRepository staffRepository;

  @Autowired
  private ShiftTypeRepository shiftTypeRepository;

  @Autowired
  private ShiftRequestRepository shiftRequestRepository;

  @Autowired
  private SystemSettingService systemSettingService;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private AccessControlService accessControlService;

  // ===== CRUD =====

  @Transactional
  public ShiftAssignmentResponse createShiftAssignment(Long editorStaffId, ShiftAssignmentCreateRequest request) {
    if (request.getStaffId() == null) {
      throw new IllegalArgumentException("スタッフIDは必須です。");
    }
    if (request.getWorkDate() == null) {
      throw new IllegalArgumentException("勤務日は必須です。");
    }
    if (request.getShiftTypeId() == null) {
      throw new IllegalArgumentException("シフトタイプIDは必須です。");
    }

    Staff editor = staffRepository.findById(editorStaffId)
        .orElseThrow(() -> new IllegalArgumentException("編集者スタッフが見つかりません。"));
    Staff target = staffRepository.findById(request.getStaffId())
        .orElseThrow(() -> new IllegalArgumentException("対象スタッフが見つかりません。"));

    if (!accessControlService.canEditShift(editor, target)) {
      throw new IllegalArgumentException("このスタッフのシフトを編集する権限がありません。");
    }

    if (isConfirmedMonthAssignment(request.getWorkDate()) && !canOverrideConfirmedMonth(editor)) {
      throw new IllegalArgumentException("確定済み月のシフトは管理者のみ変更できます。");
    }

    ShiftType shiftType = shiftTypeRepository.findById(request.getShiftTypeId())
        .orElseThrow(() -> new IllegalArgumentException("シフトタイプが見つかりません。"));

    ShiftAssignment assignment = new ShiftAssignment();
    assignment.setStaff(target);
    assignment.setWorkDate(request.getWorkDate());
    assignment.setShiftType(shiftType);
    assignment.setNote(request.getNote());
    assignment.setUpdatedBy(editor);
    assignment.setUpdatedAt(OffsetDateTime.now());

    ShiftAssignment saved = shiftAssignmentRepository.save(assignment);
    return convertToResponse(saved);
  }

  @Transactional
  public ShiftAssignmentResponse updateShiftAssignment(Long editorStaffId, Long shiftAssignmentId, ShiftAssignmentUpdateRequest request) {
    ShiftAssignment assignment = shiftAssignmentRepository.findById(shiftAssignmentId)
        .orElseThrow(() -> new IllegalArgumentException("シフト割り当てが見つかりません。"));

    Staff editor = staffRepository.findById(editorStaffId)
        .orElseThrow(() -> new IllegalArgumentException("編集者スタッフが見つかりません。"));

    if (!accessControlService.canEditShift(editor, assignment.getStaff())) {
      throw new IllegalArgumentException("このシフトを編集する権限がありません。");
    }

    if (isConfirmedMonthAssignment(assignment.getWorkDate()) && !canOverrideConfirmedMonth(editor)) {
      throw new IllegalArgumentException("確定済み月のシフトは管理者のみ変更できます。");
    }

    if (request.getShiftTypeId() != null) {
      ShiftType shiftType = shiftTypeRepository.findById(request.getShiftTypeId())
          .orElseThrow(() -> new IllegalArgumentException("シフトタイプが見つかりません。"));
      assignment.setShiftType(shiftType);
    }

    if (request.getNote() != null) {
      assignment.setNote(request.getNote());
    }

    assignment.setUpdatedBy(editor);
    assignment.setUpdatedAt(OffsetDateTime.now());

    ShiftAssignment updated = shiftAssignmentRepository.save(assignment);
    return convertToResponse(updated);
  }

  public ShiftAssignmentResponse getShiftAssignmentById(Long shiftAssignmentId) {
    ShiftAssignment assignment = shiftAssignmentRepository.findById(shiftAssignmentId)
        .orElseThrow(() -> new IllegalArgumentException("シフト割り当てが見つかりません。"));
    return convertToResponse(assignment);
  }

  public List<ShiftAssignmentResponse> getShiftAssignmentsByStaffAndDateRange(Long staffId, LocalDate startDate, LocalDate endDate) {
    return shiftAssignmentRepository.findByStaffIdAndDateRange(staffId, startDate, endDate)
        .stream()
        .map(this::convertToResponse)
        .collect(Collectors.toList());
  }

  public List<ShiftAssignmentResponse> getShiftAssignmentsByGroupAndDateRange(Long groupId, LocalDate startDate, LocalDate endDate) {
    return shiftAssignmentRepository.findByGroupIdAndDateRange(groupId, startDate, endDate)
        .stream()
        .map(this::convertToResponse)
        .collect(Collectors.toList());
  }

  public List<ShiftAssignmentResponse> getShiftAssignmentsByDateRange(LocalDate startDate, LocalDate endDate) {
    return shiftAssignmentRepository.findByDateRange(startDate, endDate)
        .stream()
        .map(this::convertToResponse)
        .collect(Collectors.toList());
  }

  @Transactional
  public void deleteShiftAssignment(Long editorStaffId, Long shiftAssignmentId) {
    ShiftAssignment assignment = shiftAssignmentRepository.findById(shiftAssignmentId)
        .orElseThrow(() -> new IllegalArgumentException("シフト割り当てが見つかりません。"));

    Staff editor = staffRepository.findById(editorStaffId)
        .orElseThrow(() -> new IllegalArgumentException("編集者スタッフが見つかりません。"));

    if (!accessControlService.canEditShift(editor, assignment.getStaff())) {
      throw new IllegalArgumentException("このシフトを削除する権限がありません。");
    }

    if (isConfirmedMonthAssignment(assignment.getWorkDate()) && !canOverrideConfirmedMonth(editor)) {
      throw new IllegalArgumentException("確定済み月のシフトは管理者のみ変更できます。");
    }

    shiftAssignmentRepository.deleteById(shiftAssignmentId);
  }

  @Transactional
  public void deleteShiftAssignmentsByStaffAndDateRange(Long editorStaffId, Long staffId, LocalDate startDate, LocalDate endDate) {
    Staff editor = staffRepository.findById(editorStaffId)
        .orElseThrow(() -> new IllegalArgumentException("編集者スタッフが見つかりません。"));
    Staff target = staffRepository.findById(staffId)
        .orElseThrow(() -> new IllegalArgumentException("対象スタッフが見つかりません。"));

    if (!accessControlService.canEditShift(editor, target)) {
      throw new IllegalArgumentException("このスタッフのシフトを削除する権限がありません。");
    }

    shiftAssignmentRepository.deleteByStaffIdAndDateRange(staffId, startDate, endDate);
  }

  @Transactional
  public int clearMonthlyShiftAssignments(Long editorStaffId, int year, int month) {
    if (month < 1 || month > 12) {
      throw new IllegalArgumentException("月は1〜12の範囲で指定してください。");
    }

    Staff editor = staffRepository.findById(editorStaffId)
        .orElseThrow(() -> new IllegalArgumentException("編集者スタッフが見つかりません。"));

    boolean confirmedMonth = systemSettingService.isMonthConfirmed(year, month);
    if (confirmedMonth && !canClearConfirmedMonth(editor, year, month)) {
      throw new IllegalArgumentException("指定月は確定済みのためクリアできません。");
    }

    YearMonth targetMonth = YearMonth.of(year, month);
    LocalDate startDate = targetMonth.atDay(1);
    LocalDate endDate = targetMonth.atEndOfMonth();

    List<Long> editableStaffIds = staffRepository.findAllByIsActiveTrue().stream()
        .filter(staff -> accessControlService.canEditShift(editor, staff))
        .map(Staff::getId)
        .toList();

    if (editableStaffIds.isEmpty()) {
      return 0;
    }

    List<ShiftAssignment> existingAssignments =
        shiftAssignmentRepository.findByStaffIdInAndWorkDateBetween(editableStaffIds, startDate, endDate);
    int deletedCount = existingAssignments.size();

    if (deletedCount > 0) {
      shiftAssignmentRepository.deleteByStaffIdInAndWorkDateBetween(editableStaffIds, startDate, endDate);
    }

    List<ShiftRequest> requestsInTargetMonth = shiftRequestRepository.findByStaffIdInAndWorkDateBetween(editableStaffIds, startDate, endDate);
    if (!requestsInTargetMonth.isEmpty()) {
      for (ShiftRequest request : requestsInTargetMonth) {
        if (request.getStatus() == ShiftRequestStatus.APPLIED || request.getStatus() == ShiftRequestStatus.REJECTED) {
          request.setStatus(ShiftRequestStatus.SUBMITTED);
          request.setDecidedAt(null);
          request.setUpdatedAt(OffsetDateTime.now());
          shiftRequestRepository.save(request);
        }
      }
    }

    if (confirmedMonth) {
      systemSettingService.removeConfirmedMonth(editorStaffId, year, month);
    }

    return deletedCount;
  }

  // ===== 自動生成コンテキスト =====

  private static class AutoGenContext {
    Staff editor;
    YearMonth targetMonth;
    LocalDate startDate;
    LocalDate endDate;
    AutoShiftGenerationRules rules;

    List<Staff> editableStaffs;
    List<ShiftType> activeWorkShiftTypes;

    Map<Long, Staff> staffById;
    Map<Long, ShiftType> shiftTypeById;
    Map<Long, List<TimeBand>> ngTimeBandsByStaffId;
    Map<Long, Set<Long>> blockedShiftTypeIdsByStaffId;
    Map<Long, Set<Long>> preferredShiftTypeIdsByStaffId;

    List<ShiftRequest> requests;
    Map<String, ShiftRequest> requestByKey;
  }

  private static class ExistingState {
    Map<String, ShiftAssignment> assignmentByKey;
    Map<Long, Set<LocalDate>> workedDaysByStaff;
    Map<Long, Integer> monthlyWorkCount;
    Map<LocalDate, Map<Long, Integer>> dailyShiftCount;
  }

  @Transactional
  public AutoShiftGenerationResultResponse autoGenerateShiftAssignments(Long editorStaffId, int year, int month) {
    if (month < 1 || month > 12) {
      throw new IllegalArgumentException("月は1〜12の範囲で指定してください。");
    }

    if (systemSettingService.isMonthConfirmed(year, month)) {
      throw new IllegalArgumentException("指定月は確定済みのため自動生成できません。");
    }

    AutoGenContext ctx = buildContext(editorStaffId, year, month);
    ExistingState state = loadExistingState(ctx);

    if ("OVERWRITE".equalsIgnoreCase(ctx.rules.existingShiftHandling)) {
      List<Long> editableStaffIds = ctx.editableStaffs.stream().map(Staff::getId).toList();
      if (!editableStaffIds.isEmpty()) {
        shiftAssignmentRepository.deleteByStaffIdInAndWorkDateBetween(editableStaffIds, ctx.startDate, ctx.endDate);
      }
      state = loadExistingState(ctx);
    }

    if (ctx.activeWorkShiftTypes.isEmpty()) {
      throw new IllegalArgumentException("有効な勤務シフト種類が存在しません。");
    }

    int totalRequiredPerDay = calculateTotalRequiredPerDay(ctx.rules);
    if (ctx.editableStaffs.size() > 1 && totalRequiredPerDay > ctx.editableStaffs.size()) {
      throw new IllegalArgumentException("1日あたり必要人数の合計が対象スタッフ数を超えているため、自動生成できません。必要人数を減らしてください。");
    }

    GenerationAttemptResult bestAttempt = null;
    int attemptsPerformed = 0;

    for (int attempt = 0; attempt <= MAX_AUTO_GENERATION_RETRIES; attempt++) {
      attemptsPerformed = attempt + 1;

      GenerationAttemptResult attemptResult = runGenerationAttempt(ctx, state);

      if (bestAttempt == null || isBetterAttempt(attemptResult, bestAttempt)) {
        bestAttempt = attemptResult;
      }

      if (attemptResult.unassignedRequiredCount == 0 && attemptResult.unmetConditions.isEmpty()) {
        break;
      }
    }

    if (bestAttempt != null && !bestAttempt.generatedAssignments.isEmpty()) {
      List<ShiftAssignment> savedAssignments = shiftAssignmentRepository.saveAll(bestAttempt.generatedAssignments);
      if (savedAssignments != null && !savedAssignments.isEmpty()) {
        bestAttempt.generatedAssignments = savedAssignments;
      }
    }

    return buildResponse(ctx, bestAttempt, attemptsPerformed);
  }

  private AutoGenContext buildContext(Long editorStaffId, int year, int month) {
    Staff editor = staffRepository.findById(editorStaffId)
        .orElseThrow(() -> new IllegalArgumentException("編集者スタッフが見つかりません。"));

    YearMonth targetMonth = YearMonth.of(year, month);
    LocalDate startDate = targetMonth.atDay(1);
    LocalDate endDate = targetMonth.atEndOfMonth();

    AutoShiftGenerationRules rules =
        parseRules(systemSettingService.getSystemSettingTextValue("autoShiftGenerationRules"));

    List<Staff> editableStaffs = staffRepository.findAllByIsActiveTrue().stream()
        .filter(staff -> accessControlService.canEditShift(editor, staff))
        .sorted(Comparator.comparing(Staff::getStaffCode, Comparator.nullsLast(String::compareTo)))
        .collect(Collectors.toList());

    if (editableStaffs.isEmpty()) {
      throw new IllegalArgumentException("自動生成対象のスタッフが存在しません。");
    }

    List<ShiftType> activeWorkShiftTypes = shiftTypeRepository.findAllActiveWorkShifts();

    Map<Long, Staff> staffById =
        editableStaffs.stream().collect(Collectors.toMap(Staff::getId, Function.identity()));

    Map<Long, ShiftType> shiftTypeById =
        activeWorkShiftTypes.stream().collect(Collectors.toMap(ShiftType::getId, Function.identity()));

    Map<Long, List<TimeBand>> ngTimeBandsByStaffId = editableStaffs.stream()
        .collect(Collectors.toMap(Staff::getId, staff -> parseNgTimeBands(staff.getNgShiftTimeBands())));
    Map<Long, Set<Long>> blockedShiftTypeIdsByStaffId = editableStaffs.stream()
        .collect(Collectors.toMap(Staff::getId, staff -> parseBlockedShiftTypeIds(staff.getNgShiftTimeBands())));
    Map<Long, Set<Long>> preferredShiftTypeIdsByStaffId = new HashMap<>();
    editableStaffs.stream()
        .map(Staff::getId)
        .forEach(staffId -> preferredShiftTypeIdsByStaffId.put(staffId, parseBlockedShiftTypeIds(
            editableStaffs.stream()
                .filter(staff -> staff.getId().equals(staffId))
                .findFirst()
                .map(Staff::getPreferredShiftTimeBands)
                .orElse(null))));

    List<Long> staffIds = editableStaffs.stream().map(Staff::getId).toList();

    List<ShiftRequest> requests = shiftRequestRepository
        .findByStaffIdInAndWorkDateBetween(staffIds, startDate, endDate).stream()
        .filter(r -> r.getStatus() != ShiftRequestStatus.REJECTED)
        .collect(Collectors.toList());

    Map<String, ShiftRequest> requestByKey =
        requests.stream().collect(Collectors.toMap(this::requestKey, Function.identity(), (l, r) -> l));

    AutoGenContext ctx = new AutoGenContext();
    ctx.editor = editor;
    ctx.targetMonth = targetMonth;
    ctx.startDate = startDate;
    ctx.endDate = endDate;
    ctx.rules = rules;
    ctx.editableStaffs = editableStaffs;
    ctx.activeWorkShiftTypes = activeWorkShiftTypes;
    ctx.staffById = staffById;
    ctx.shiftTypeById = shiftTypeById;
    ctx.ngTimeBandsByStaffId = ngTimeBandsByStaffId;
    ctx.blockedShiftTypeIdsByStaffId = blockedShiftTypeIdsByStaffId;
    ctx.preferredShiftTypeIdsByStaffId = preferredShiftTypeIdsByStaffId;
    ctx.requests = requests;
    ctx.requestByKey = requestByKey;

    return ctx;
  }

  private ExistingState loadExistingState(AutoGenContext ctx) {
    List<Long> staffIds = ctx.editableStaffs.stream().map(Staff::getId).toList();

    List<ShiftAssignment> existingAssignments =
        shiftAssignmentRepository.findByStaffIdInAndWorkDateBetween(
            staffIds, ctx.startDate, ctx.endDate);

    Map<String, ShiftAssignment> assignmentByKey =
        existingAssignments.stream()
            .collect(Collectors.toMap(this::assignmentKey, Function.identity(), (l, r) -> l));

    Map<Long, Set<LocalDate>> workedDaysByStaff = new HashMap<>();
    Map<Long, Integer> monthlyWorkCount = new HashMap<>();
    Map<LocalDate, Map<Long, Integer>> dailyShiftCount = new HashMap<>();

    initializeStateMaps(staffIds, workedDaysByStaff, monthlyWorkCount);

    for (ShiftAssignment a : existingAssignments) {
      Long staffId = a.getStaff().getId();
      LocalDate date = a.getWorkDate();

      workedDaysByStaff.computeIfAbsent(staffId, unused -> new HashSet<>()).add(date);
      monthlyWorkCount.merge(staffId, 1, Integer::sum);
      incrementDailyShiftCount(dailyShiftCount, date, a.getShiftType().getId());
    }

    ExistingState state = new ExistingState();
    state.assignmentByKey = assignmentByKey;
    state.workedDaysByStaff = workedDaysByStaff;
    state.monthlyWorkCount = monthlyWorkCount;
    state.dailyShiftCount = dailyShiftCount;

    return state;
  }

  private GenerationAttemptResult runGenerationAttempt(
      AutoGenContext ctx,
      ExistingState baseState
  ) {
    Map<String, ShiftAssignment> assignmentByKey = new HashMap<>(baseState.assignmentByKey);
    Map<Long, Set<LocalDate>> workedDaysByStaff = cloneWorkedDaysByStaff(baseState.workedDaysByStaff);
    Map<Long, Integer> monthlyWorkCount = new HashMap<>(baseState.monthlyWorkCount);
    Map<LocalDate, Map<Long, Integer>> dailyShiftCount = cloneDailyShiftCount(baseState.dailyShiftCount);

    int unassignedRequiredCount = assignRequiredShifts(
        ctx.editor,
        ctx.requests,
        ctx.rules,
        ctx.staffById,
        ctx.shiftTypeById,
        ctx.ngTimeBandsByStaffId,
        ctx.blockedShiftTypeIdsByStaffId,
        ctx.requestByKey,
        assignmentByKey,
        workedDaysByStaff,
        monthlyWorkCount,
        dailyShiftCount
    );

    List<AutoShiftGenerationResultResponse.UnmetCondition> unmetConditions =
        fillRequiredCounts(
            ctx.editor,
            ctx.editableStaffs,
            ctx.activeWorkShiftTypes,
            ctx.rules,
            ctx.staffById,
            ctx.shiftTypeById,
            ctx.ngTimeBandsByStaffId,
            ctx.blockedShiftTypeIdsByStaffId,
            ctx.requestByKey,
            assignmentByKey,
            workedDaysByStaff,
            monthlyWorkCount,
            dailyShiftCount,
            ctx.startDate,
            ctx.endDate
        );

    List<ShiftAssignment> generatedAssignments = collectGeneratedAssignments(assignmentByKey);

    return new GenerationAttemptResult(generatedAssignments, unassignedRequiredCount, unmetConditions);
  }

  private int assignRequiredShifts(
      Staff editor,
      List<ShiftRequest> requests,
      AutoShiftGenerationRules rules,
      Map<Long, Staff> staffById,
      Map<Long, ShiftType> shiftTypeById,
      Map<Long, List<TimeBand>> ngTimeBandsByStaffId,
      Map<Long, Set<Long>> blockedShiftTypeIdsByStaffId,
      Map<String, ShiftRequest> requestByKey,
      Map<String, ShiftAssignment> assignmentByKey,
      Map<Long, Set<LocalDate>> workedDaysByStaff,
      Map<Long, Integer> monthlyWorkCount,
      Map<LocalDate, Map<Long, Integer>> dailyShiftCount
  ) {
    if (!"REQUIRED".equalsIgnoreCase(rules.desiredShiftMode)) {
      return 0;
    }

    int unassigned = 0;

    List<ShiftRequest> sorted = new ArrayList<>(requests);
    sorted.sort(
        Comparator.comparing(ShiftRequest::getWorkDate)
            .thenComparing(r -> r.getStaff().getStaffCode(), Comparator.nullsLast(String::compareTo))
    );

    for (ShiftRequest req : sorted) {
      Long staffId = req.getStaff().getId();
      ShiftType desired = req.getDesiredShiftType();

      if (!staffById.containsKey(staffId)) {
        continue;
      }

      if (desired == null) {
        continue;
      }

      if (!shiftTypeById.containsKey(desired.getId())) {
        continue;
      }

      boolean ok = canAssignShift(
          staffId,
          req.getWorkDate(),
          desired.getId(),
          assignmentByKey,
          monthlyWorkCount,
          workedDaysByStaff,
          rules,
          shiftTypeById,
          ngTimeBandsByStaffId,
          blockedShiftTypeIdsByStaffId,
          requestByKey,
          true,
          false
      );

      if (!ok) {
        unassigned++;
        continue;
      }

      createAssignmentInMemory(editor, staffById.get(staffId), desired, req.getWorkDate(), assignmentByKey);
      monthlyWorkCount.merge(staffId, 1, Integer::sum);
      workedDaysByStaff.computeIfAbsent(staffId, unused -> new HashSet<>()).add(req.getWorkDate());
      incrementDailyShiftCount(dailyShiftCount, req.getWorkDate(), desired.getId());
    }

    return unassigned;
  }

  private List<AutoShiftGenerationResultResponse.UnmetCondition> fillRequiredCounts(
      Staff editor,
      List<Staff> editableStaffs,
      List<ShiftType> activeWorkShiftTypes,
      AutoShiftGenerationRules rules,
      Map<Long, Staff> staffById,
      Map<Long, ShiftType> shiftTypeById,
      Map<Long, List<TimeBand>> ngTimeBandsByStaffId,
      Map<Long, Set<Long>> blockedShiftTypeIdsByStaffId,
      Map<String, ShiftRequest> requestByKey,
      Map<String, ShiftAssignment> assignmentByKey,
      Map<Long, Set<LocalDate>> workedDaysByStaff,
      Map<Long, Integer> monthlyWorkCount,
      Map<LocalDate, Map<Long, Integer>> dailyShiftCount,
      LocalDate startDate,
      LocalDate endDate
  ) {
    List<AutoShiftGenerationResultResponse.UnmetCondition> unmet = new ArrayList<>();

    LocalDate current = startDate;
    while (!current.isAfter(endDate)) {
      final LocalDate currentDate = current;
      List<ShiftType> orderedShiftTypes = activeWorkShiftTypes.stream()
          .filter(shiftType -> Math.max(0, rules.requiredCounts.getOrDefault(shiftType.getId(), 0)) > 0)
          .sorted((left, right) -> {
            int leftRequired = Math.max(0, rules.requiredCounts.getOrDefault(left.getId(), 0));
            int rightRequired = Math.max(0, rules.requiredCounts.getOrDefault(right.getId(), 0));
            int requiredComparison = Integer.compare(rightRequired, leftRequired);
            if (requiredComparison != 0) {
              return requiredComparison;
            }
            int leftCandidates = countAssignableCandidates(
                editableStaffs,
                currentDate,
                left,
                assignmentByKey,
                monthlyWorkCount,
                workedDaysByStaff,
                rules,
                shiftTypeById,
                ngTimeBandsByStaffId,
                blockedShiftTypeIdsByStaffId,
                requestByKey,
                false,
                true);
            int rightCandidates = countAssignableCandidates(
                editableStaffs,
                currentDate,
                right,
                assignmentByKey,
                monthlyWorkCount,
                workedDaysByStaff,
                rules,
                shiftTypeById,
                ngTimeBandsByStaffId,
                blockedShiftTypeIdsByStaffId,
                requestByKey,
                false,
                true);
            return Integer.compare(leftCandidates, rightCandidates);
          })
          .toList();

      for (ShiftType shiftType : orderedShiftTypes) {
        int required = Math.max(0, rules.requiredCounts.getOrDefault(shiftType.getId(), 0));
        if (required <= 0) continue;

        if (hasVacationRequestsForDate(current, editableStaffs, requestByKey)) {
          continue;
        }

        int currentCount = dailyShiftCount
            .getOrDefault(current, Map.of())
            .getOrDefault(shiftType.getId(), 0);

        int retries = 0;
        int attempts = 0;

        while (currentCount < required && attempts < required * 20 + MAX_AUTO_GENERATION_RETRIES) {
          Long staffId = selectCandidateStaff(
              editableStaffs,
              current,
              shiftType,
              assignmentByKey,
              monthlyWorkCount,
              workedDaysByStaff,
              rules,
              shiftTypeById,
              ngTimeBandsByStaffId,
              blockedShiftTypeIdsByStaffId,
              requestByKey
          );

          if (staffId == null) {
            if (retries >= MAX_AUTO_GENERATION_RETRIES) break;
            retries++;
            attempts++;
            continue;
          }

          createAssignmentInMemory(editor, staffById.get(staffId), shiftType, current, assignmentByKey);
          monthlyWorkCount.merge(staffId, 1, Integer::sum);
          workedDaysByStaff.computeIfAbsent(staffId, unused -> new HashSet<>()).add(current);
          incrementDailyShiftCount(dailyShiftCount, current, shiftType.getId());
          currentCount++;
          retries = 0;
          attempts++;
        }

        if (currentCount < required) {
          unmet.add(buildUnmetCondition(current, shiftType, required, currentCount));
        }
      }

      current = current.plusDays(1);
    }

    return unmet;
  }

  private List<ShiftAssignment> collectGeneratedAssignments(Map<String, ShiftAssignment> assignmentByKey) {
    return assignmentByKey.values().stream()
        .filter(a -> a.getId() == null)
        .collect(Collectors.toList());
  }

  private AutoShiftGenerationResultResponse buildResponse(
      AutoGenContext ctx,
      GenerationAttemptResult bestAttempt,
      int attemptsPerformed
  ) {
    AutoShiftGenerationResultResponse response = new AutoShiftGenerationResultResponse();
    response.setYear(ctx.targetMonth.getYear());
    response.setMonth(ctx.targetMonth.getMonthValue());
    response.setStartDate(ctx.startDate);
    response.setEndDate(ctx.endDate);
    response.setTargetStaffCount(ctx.editableStaffs.size());
    response.setSkippedHolidayCount(0);
    response.setConsideredRequestCount(ctx.requests.size());
    response.setGeneratedCount(bestAttempt != null ? bestAttempt.generatedAssignments.size() : 0);
    response.setUnassignedRequiredCount(bestAttempt != null ? bestAttempt.unassignedRequiredCount : 0);
    response.setRetryCount(Math.max(0, attemptsPerformed - 1));
    response.setUnmetConditions(bestAttempt != null ? bestAttempt.unmetConditions : List.of());
    return response;
  }

  private int calculateTotalRequiredPerDay(AutoShiftGenerationRules rules) {
    return rules.requiredCounts.values().stream()
        .mapToInt(value -> Math.max(0, value))
        .sum();
  }

  // ===== スタッフ選択アルゴリズム =====

  private static class StaffSelectionScore {
    Staff staff;
    boolean requestMatch;
    boolean preferredBandMatch;
    int monthlyWorkCount;
    int recentWorkCount;
    int consecutiveDays;
    LocalDate lastWorkedDate;

    StaffSelectionScore(
        Staff staff,
        boolean requestMatch,
        boolean preferredBandMatch,
        int monthlyWorkCount,
        int recentWorkCount,
        int consecutiveDays,
        LocalDate lastWorkedDate
    ) {
      this.staff = staff;
      this.requestMatch = requestMatch;
      this.preferredBandMatch = preferredBandMatch;
      this.monthlyWorkCount = monthlyWorkCount;
      this.recentWorkCount = recentWorkCount;
      this.consecutiveDays = consecutiveDays;
      this.lastWorkedDate = lastWorkedDate;
    }
  }

  private Long selectCandidateStaff(
      List<Staff> editableStaffs,
      LocalDate workDate,
      ShiftType targetShiftType,
      Map<String, ShiftAssignment> assignmentByKey,
      Map<Long, Integer> monthlyWorkCount,
      Map<Long, Set<LocalDate>> workedDaysByStaff,
      AutoShiftGenerationRules rules,
      Map<Long, ShiftType> shiftTypeById,
      Map<Long, List<TimeBand>> ngTimeBandsByStaffId,
      Map<Long, Set<Long>> blockedShiftTypeIdsByStaffId,
      Map<String, ShiftRequest> requestByKey) {

    List<Staff> candidates = collectAssignableCandidates(
        editableStaffs,
        workDate,
        targetShiftType,
        assignmentByKey,
        monthlyWorkCount,
        workedDaysByStaff,
        rules,
        shiftTypeById,
        ngTimeBandsByStaffId,
        blockedShiftTypeIdsByStaffId,
        requestByKey,
        false,
        false);

    if (candidates.isEmpty()) {
      candidates = collectAssignableCandidates(
          editableStaffs,
          workDate,
          targetShiftType,
          assignmentByKey,
          monthlyWorkCount,
          workedDaysByStaff,
          rules,
          shiftTypeById,
          ngTimeBandsByStaffId,
          blockedShiftTypeIdsByStaffId,
          requestByKey,
          true,
          false);
    }

    if (candidates.isEmpty()) {
      candidates = collectAssignableCandidates(
          editableStaffs,
          workDate,
          targetShiftType,
          assignmentByKey,
          monthlyWorkCount,
          workedDaysByStaff,
          rules,
          shiftTypeById,
          ngTimeBandsByStaffId,
          blockedShiftTypeIdsByStaffId,
          requestByKey,
          true,
          true);
    }

    if (candidates.isEmpty()) {
      return null;
    }

    List<StaffSelectionScore> scoredCandidates = candidates.stream()
        .map(staff -> new StaffSelectionScore(
            staff,
            isRequestMatch(requestByKey, staff.getId(), workDate, targetShiftType.getId(), rules.desiredShiftMode),
            isPreferredShiftBandMatch(staff, targetShiftType.getId()),
            monthlyWorkCount.getOrDefault(staff.getId(), 0),
            recentWorkCount(workedDaysByStaff.getOrDefault(staff.getId(), Set.of()), workDate, 7),
            currentConsecutiveDays(workedDaysByStaff.getOrDefault(staff.getId(), Set.of()), workDate),
            lastWorkedDate(workedDaysByStaff.getOrDefault(staff.getId(), Set.of()), workDate)))
        .collect(Collectors.toList());

    return chooseWeightedRandomCandidate(scoredCandidates, workDate, rules).staff.getId();
  }

  private List<Staff> collectAssignableCandidates(
      List<Staff> editableStaffs,
      LocalDate workDate,
      ShiftType targetShiftType,
      Map<String, ShiftAssignment> assignmentByKey,
      Map<Long, Integer> monthlyWorkCount,
      Map<Long, Set<LocalDate>> workedDaysByStaff,
      AutoShiftGenerationRules rules,
      Map<Long, ShiftType> shiftTypeById,
      Map<Long, List<TimeBand>> ngTimeBandsByStaffId,
      Map<Long, Set<Long>> blockedShiftTypeIdsByStaffId,
      Map<String, ShiftRequest> requestByKey,
      boolean ignoreMinimumRestDays,
      boolean ignoreShiftGap) {
    return editableStaffs.stream()
        .filter(staff -> !isBlockedByNgShiftTimeBand(
            staff.getId(),
            targetShiftType.getId(),
            shiftTypeById,
            ngTimeBandsByStaffId,
            blockedShiftTypeIdsByStaffId))
        .filter(staff -> canAssignShift(
            staff.getId(),
            workDate,
            targetShiftType.getId(),
            assignmentByKey,
            monthlyWorkCount,
            workedDaysByStaff,
            rules,
            shiftTypeById,
            ngTimeBandsByStaffId,
            blockedShiftTypeIdsByStaffId,
            requestByKey,
            false,
            ignoreMinimumRestDays,
            ignoreShiftGap))
        .collect(Collectors.toList());
  }

  private int countAssignableCandidates(
      List<Staff> editableStaffs,
      LocalDate workDate,
      ShiftType targetShiftType,
      Map<String, ShiftAssignment> assignmentByKey,
      Map<Long, Integer> monthlyWorkCount,
      Map<Long, Set<LocalDate>> workedDaysByStaff,
      AutoShiftGenerationRules rules,
      Map<Long, ShiftType> shiftTypeById,
      Map<Long, List<TimeBand>> ngTimeBandsByStaffId,
      Map<Long, Set<Long>> blockedShiftTypeIdsByStaffId,
      Map<String, ShiftRequest> requestByKey,
      boolean ignoreMinimumRestDays,
      boolean ignoreShiftGap) {
    return collectAssignableCandidates(
        editableStaffs,
        workDate,
        targetShiftType,
        assignmentByKey,
        monthlyWorkCount,
        workedDaysByStaff,
        rules,
        shiftTypeById,
        ngTimeBandsByStaffId,
        blockedShiftTypeIdsByStaffId,
        requestByKey,
        ignoreMinimumRestDays,
        ignoreShiftGap).size();
  }

  private StaffSelectionScore chooseWeightedRandomCandidate(
      List<StaffSelectionScore> candidates,
      LocalDate workDate,
      AutoShiftGenerationRules rules) {
    if (candidates.isEmpty()) {
      return null;
    }

    StaffSelectionScore best = candidates.get(0);
    int bestWeight = calculateCandidateWeight(best, workDate, rules);

    for (int i = 1; i < candidates.size(); i++) {
      StaffSelectionScore candidate = candidates.get(i);
      int candidateWeight = calculateCandidateWeight(candidate, workDate, rules);

      int comparison = Integer.compare(candidateWeight, bestWeight);
      if (comparison > 0) {
        best = candidate;
        bestWeight = candidateWeight;
      } else if (comparison == 0) {
        int monthlyComparison = Integer.compare(candidate.monthlyWorkCount, best.monthlyWorkCount);
        if (monthlyComparison < 0) {
          best = candidate;
          bestWeight = candidateWeight;
        } else if (monthlyComparison == 0) {
          int recentComparison = Integer.compare(candidate.recentWorkCount, best.recentWorkCount);
          if (recentComparison < 0) {
            best = candidate;
            bestWeight = candidateWeight;
          } else if (recentComparison == 0) {
            int consecutiveComparison = Integer.compare(candidate.consecutiveDays, best.consecutiveDays);
            if (consecutiveComparison < 0) {
              best = candidate;
              bestWeight = candidateWeight;
            } else if (consecutiveComparison == 0) {
              String candidateCode = candidate.staff.getStaffCode() == null ? "" : candidate.staff.getStaffCode();
              String bestCode = best.staff.getStaffCode() == null ? "" : best.staff.getStaffCode();
              if (candidateCode.compareTo(bestCode) < 0) {
                best = candidate;
                bestWeight = candidateWeight;
              }
            }
          }
        }
      }
    }

    return best;
  }

  private int calculateCandidateWeight(StaffSelectionScore c, LocalDate workDate, AutoShiftGenerationRules rules) {
    int weight = 0;

    // 希望シフトは低優先の参考情報として扱い、NGシフトのような制約よりは弱くする。
    if (c.requestMatch) {
      weight += 10;
    }

    if (c.preferredBandMatch) {
      weight += 5;
    }

    int monthlyMax = resolveMonthlyMaxWorkdays(rules, workDate);
    int remainingCapacity = monthlyMax > 0
        ? Math.max(1, monthlyMax - c.monthlyWorkCount)
        : 10;
    weight += Math.max(1, remainingCapacity) * 3;

    int monthlyBalancePenalty = monthlyMax > 0
        ? Math.max(0, c.monthlyWorkCount - (monthlyMax / 2))
        : 0;
    weight -= monthlyBalancePenalty * 4;

    int recentSlack = Math.max(1, 7 - c.recentWorkCount);
    weight += recentSlack * 2;

    int consecutiveSlack = Math.max(1, 5 - c.consecutiveDays);
    weight += consecutiveSlack * 2;

    if (c.lastWorkedDate == null) {
      weight += 10;
    } else {
      long daysSinceLast = ChronoUnit.DAYS.between(c.lastWorkedDate, workDate);
      weight += Math.max(1, (int) daysSinceLast / 2);
    }

    return weight;
  }

  // ===== ヘルパー =====

  private Map<Long, Set<LocalDate>> cloneWorkedDaysByStaff(Map<Long, Set<LocalDate>> source) {
    Map<Long, Set<LocalDate>> clone = new HashMap<>();
    for (Map.Entry<Long, Set<LocalDate>> entry : source.entrySet()) {
      clone.put(entry.getKey(), new HashSet<>(entry.getValue()));
    }
    return clone;
  }

  private Map<LocalDate, Map<Long, Integer>> cloneDailyShiftCount(Map<LocalDate, Map<Long, Integer>> source) {
    Map<LocalDate, Map<Long, Integer>> clone = new HashMap<>();
    for (Map.Entry<LocalDate, Map<Long, Integer>> entry : source.entrySet()) {
      clone.put(entry.getKey(), new HashMap<>(entry.getValue()));
    }
    return clone;
  }

  private AutoShiftGenerationResultResponse.UnmetCondition buildUnmetCondition(
      LocalDate workDate,
      ShiftType shiftType,
      int requiredCount,
      int assignedCount) {
    AutoShiftGenerationResultResponse.UnmetCondition condition = new AutoShiftGenerationResultResponse.UnmetCondition();
    condition.setWorkDate(workDate);
    condition.setShiftTypeId(shiftType.getId());
    condition.setShiftTypeName(shiftType.getShiftName());
    condition.setRequiredCount(requiredCount);
    condition.setAssignedCount(assignedCount);
    condition.setShortageCount(Math.max(0, requiredCount - assignedCount));
    condition.setReason("勤務可能な候補スタッフが見つかりませんでした。");
    return condition;
  }

  private void initializeStateMaps(List<Long> staffIds, Map<Long, Set<LocalDate>> workedDaysByStaff, Map<Long, Integer> monthlyWorkCount) {
    for (Long staffId : staffIds) {
      workedDaysByStaff.put(staffId, new HashSet<>());
      monthlyWorkCount.put(staffId, 0);
    }
  }

  private boolean canAssignShift(
      Long staffId,
      LocalDate workDate,
      Long shiftTypeId,
      Map<String, ShiftAssignment> assignmentByKey,
      @SuppressWarnings("unused") Map<Long, Integer> monthlyWorkCount,
      Map<Long, Set<LocalDate>> workedDaysByStaff,
      AutoShiftGenerationRules rules,
      Map<Long, ShiftType> shiftTypeById,
      Map<Long, List<TimeBand>> ngTimeBandsByStaffId,
      Map<Long, Set<Long>> blockedShiftTypeIdsByStaffId,
      Map<String, ShiftRequest> requestByKey,
      boolean forceRequested,
      boolean ignoreMinimumRestDays) {
    return canAssignShift(
        staffId,
        workDate,
        shiftTypeId,
        assignmentByKey,
        monthlyWorkCount,
        workedDaysByStaff,
        rules,
        shiftTypeById,
        ngTimeBandsByStaffId,
        blockedShiftTypeIdsByStaffId,
        requestByKey,
        forceRequested,
        ignoreMinimumRestDays,
        false);
  }

  private boolean canAssignShift(
      Long staffId,
      LocalDate workDate,
      Long shiftTypeId,
      Map<String, ShiftAssignment> assignmentByKey,
      @SuppressWarnings("unused") Map<Long, Integer> monthlyWorkCount,
      Map<Long, Set<LocalDate>> workedDaysByStaff,
      AutoShiftGenerationRules rules,
      Map<Long, ShiftType> shiftTypeById,
      Map<Long, List<TimeBand>> ngTimeBandsByStaffId,
      Map<Long, Set<Long>> blockedShiftTypeIdsByStaffId,
      Map<String, ShiftRequest> requestByKey,
      boolean forceRequested,
      boolean ignoreMinimumRestDays,
      boolean ignoreShiftGap) {
    if (isBlockedByNgShiftTimeBand(staffId, shiftTypeId, shiftTypeById, ngTimeBandsByStaffId, blockedShiftTypeIdsByStaffId)) {
      return false;
    }

    if (assignmentByKey.containsKey(staffId + "-" + workDate)) {
      return false;
    }

    int maxConsecutive = rules.maxConsecutiveWorkdays;
    if (maxConsecutive > 0 && wouldExceedConsecutiveLimit(workedDaysByStaff.getOrDefault(staffId, Set.of()), workDate, maxConsecutive)) {
      return false;
    }

    int minimumRestDays = rules.minimumRestDays;
    if (!ignoreMinimumRestDays
        && minimumRestDays > 0
        && violatesMinimumRest(workedDaysByStaff.getOrDefault(staffId, Set.of()), workDate, minimumRestDays)) {
      return false;
    }

    if (!ignoreShiftGap && violatesMinimumShiftGap(rules, workDate, shiftTypeId, shiftTypeById, assignmentByKey, workedDaysByStaff, staffId)) {
      return false;
    }

    ShiftRequest request = requestByKey.get(staffId + "-" + workDate);
    if (request == null) {
      return true;
    }

    Long requestedShiftTypeId = request.getDesiredShiftType() != null ? request.getDesiredShiftType().getId() : null;
    if (requestedShiftTypeId == null) {
      return true;
    }

    if (forceRequested) {
      return requestedShiftTypeId.equals(shiftTypeId);
    }

    // 申請は弱いヒントとして扱い、必要人数の充足を妨げないようにする。
    return true;
  }

  private boolean hasVacationRequestsForDate(
      LocalDate workDate,
      List<Staff> editableStaffs,
      Map<String, ShiftRequest> requestByKey) {
    if (workDate == null || editableStaffs == null || editableStaffs.isEmpty()) {
      return false;
    }

    for (Staff staff : editableStaffs) {
      ShiftRequest request = requestByKey.get(staff.getId() + "-" + workDate);
      if (request != null && request.getDesiredShiftType() == null) {
        return true;
      }
    }
    return false;
  }

  private int resolveMonthlyMaxWorkdays(AutoShiftGenerationRules rules, LocalDate date) {
    if (rules == null) {
      return 0;
    }

    if ("CALCULATED".equalsIgnoreCase(rules.monthlyMaxWorkdaysMode)) {
      YearMonth yearMonth = YearMonth.from(date);
      int daysInMonth = yearMonth.lengthOfMonth();
      double calculated = (daysInMonth / 7.0) * 5.0 + 1.0;
      return (int) Math.ceil(calculated);
    }

    return rules.monthlyMaxWorkdays <= 0 ? 0 : rules.monthlyMaxWorkdays;
  }

  private boolean wouldExceedConsecutiveLimit(Set<LocalDate> workedDays, LocalDate targetDate, int maxConsecutive) {
    LocalDate prev = targetDate.minusDays(1);
    int count = 0;
    while (workedDays.contains(prev)) {
      count++;
      prev = prev.minusDays(1);
    }
    return count >= maxConsecutive;
  }

  private boolean violatesMinimumRest(Set<LocalDate> workedDays, LocalDate targetDate, int minimumRestDays) {
    for (int i = 1; i <= minimumRestDays; i++) {
      if (workedDays.contains(targetDate.minusDays(i))) {
        return true;
      }
    }
    return false;
  }

  private boolean violatesMinimumShiftGap(
      AutoShiftGenerationRules rules,
      LocalDate targetDate,
      Long targetShiftTypeId,
      Map<Long, ShiftType> shiftTypeById,
      Map<String, ShiftAssignment> assignmentByKey,
      Map<Long, Set<LocalDate>> workedDaysByStaff,
      Long staffId) {
    if (rules == null || rules.minimumShiftGapHours <= 0) {
      return false;
    }

    ShiftType targetShiftType = shiftTypeById.get(targetShiftTypeId);
    if (targetShiftType == null || targetShiftType.getStartTime() == null) {
      return false;
    }

    Set<LocalDate> workedDays = workedDaysByStaff.getOrDefault(staffId, Set.of());
    for (LocalDate previousDate : workedDays) {
      if (!previousDate.isBefore(targetDate) || previousDate.isEqual(targetDate)) {
        continue;
      }

      ShiftAssignment previousAssignment = assignmentByKey.get(staffId + "-" + previousDate);
      if (previousAssignment == null || previousAssignment.getShiftType() == null || previousAssignment.getShiftType().getEndTime() == null) {
        continue;
      }

      LocalTime previousEnd = previousAssignment.getShiftType().getEndTime();
      LocalTime targetStart = targetShiftType.getStartTime();
      long gapHours = ChronoUnit.HOURS.between(previousEnd, targetStart);
      if (gapHours < rules.minimumShiftGapHours) {
        return true;
      }
    }

    return false;
  }

  private boolean isBlockedByNgShiftTimeBand(
      Long staffId,
      Long shiftTypeId,
      Map<Long, ShiftType> shiftTypeById,
      Map<Long, List<TimeBand>> ngTimeBandsByStaffId,
      Map<Long, Set<Long>> blockedShiftTypeIdsByStaffId
  ) {
    Set<Long> blockedShiftTypeIds = blockedShiftTypeIdsByStaffId.getOrDefault(staffId, Set.of());
    if (blockedShiftTypeIds.contains(shiftTypeId)) {
      return true;
    }

    List<TimeBand> ngBands = ngTimeBandsByStaffId.getOrDefault(staffId, List.of());
    ShiftType shiftType = shiftTypeById.get(shiftTypeId);
    if (shiftType == null) return false;

    LocalTime start = shiftType.getStartTime();
    LocalTime end = shiftType.getEndTime();

    for (TimeBand band : ngBands) {
      if (band.overlaps(start, end)) {
        return true;
      }
    }
    return false;
  }

  private int recentWorkCount(Set<LocalDate> workedDays, LocalDate targetDate, int days) {
    int count = 0;
    for (int i = 1; i <= days; i++) {
      if (workedDays.contains(targetDate.minusDays(i))) {
        count++;
      }
    }
    return count;
  }

  private int currentConsecutiveDays(Set<LocalDate> workedDays, LocalDate targetDate) {
    int count = 0;
    LocalDate prev = targetDate.minusDays(1);
    while (workedDays.contains(prev)) {
      count++;
      prev = prev.minusDays(1);
    }
    return count;
  }

  private LocalDate lastWorkedDate(Set<LocalDate> workedDays, LocalDate targetDate) {
    LocalDate prev = targetDate.minusDays(1);
    while (workedDays.contains(prev)) {
      return prev;
    }
    return null;
  }

  private boolean isRequestMatch(
      Map<String, ShiftRequest> requestByKey,
      Long staffId,
      LocalDate workDate,
      Long shiftTypeId,
      @SuppressWarnings("unused") String desiredShiftMode
  ) {
    ShiftRequest req = requestByKey.get(staffId + "-" + workDate);
    if (req == null || req.getDesiredShiftType() == null) return false;
    return req.getDesiredShiftType().getId().equals(shiftTypeId);
  }

  private boolean isPreferredShiftBandMatch(Staff staff, Long shiftTypeId) {
    if (staff == null || shiftTypeId == null) {
      return false;
    }
    return staff.getPreferredShiftTimeBands() != null
        && !staff.getPreferredShiftTimeBands().isBlank()
        && parseBlockedShiftTypeIds(staff.getPreferredShiftTimeBands()).contains(shiftTypeId);
  }

  private void createAssignmentInMemory(
      Staff editor,
      Staff staff,
      ShiftType shiftType,
      LocalDate workDate,
      Map<String, ShiftAssignment> assignmentByKey
  ) {
    ShiftAssignment a = new ShiftAssignment();
    a.setStaff(staff);
    a.setShiftType(shiftType);
    a.setWorkDate(workDate);
    a.setUpdatedBy(editor);
    a.setUpdatedAt(OffsetDateTime.now());
    assignmentByKey.put(staff.getId() + "-" + workDate, a);
  }

  private void incrementDailyShiftCount(
      Map<LocalDate, Map<Long, Integer>> dailyShiftCount,
      LocalDate date,
      Long shiftTypeId
  ) {
    dailyShiftCount
        .computeIfAbsent(date, unused -> new HashMap<>())
        .merge(shiftTypeId, 1, Integer::sum);
  }

  private String assignmentKey(ShiftAssignment a) {
    return a.getStaff().getId() + "-" + a.getWorkDate();
  }

  private String requestKey(ShiftRequest r) {
    return r.getStaff().getId() + "-" + r.getWorkDate();
  }

  private boolean isBetterAttempt(GenerationAttemptResult candidate, GenerationAttemptResult currentBest) {
    int candidateShortage = candidate.totalShortageCount();
    int bestShortage = currentBest.totalShortageCount();
    if (candidateShortage != bestShortage) {
      return candidateShortage < bestShortage;
    }
    if (candidate.unassignedRequiredCount != currentBest.unassignedRequiredCount) {
      return candidate.unassignedRequiredCount < currentBest.unassignedRequiredCount;
    }
    return candidate.generatedAssignments.size() > currentBest.generatedAssignments.size();
  }

  private boolean canOverrideConfirmedMonth(Staff editor) {
    return accessControlService.isMaster(editor) || accessControlService.isChief(editor);
  }

  private boolean canClearConfirmedMonth(Staff editor, int year, int month) {
    if (!canOverrideConfirmedMonth(editor)) {
      return false;
    }

    YearMonth targetMonth = YearMonth.of(year, month);
    YearMonth currentMonth = YearMonth.from(LocalDate.now());
    return targetMonth.isAfter(currentMonth);
  }

  private boolean isConfirmedMonthAssignment(LocalDate workDate) {
    if (workDate == null) {
      return false;
    }
    return systemSettingService.isMonthConfirmed(workDate.getYear(), workDate.getMonthValue());
  }

  private ShiftAssignmentResponse convertToResponse(ShiftAssignment a) {
    ShiftAssignmentResponse r = new ShiftAssignmentResponse();
    r.setId(a.getId());
    r.setStaffId(a.getStaff().getId());
    r.setStaffName(a.getStaff().getStaffName());
    r.setShiftTypeId(a.getShiftType().getId());
    r.setShiftCode(a.getShiftType().getShiftCode());
    r.setShiftName(a.getShiftType().getShiftName());
    r.setWorkDate(a.getWorkDate());
    r.setNote(a.getNote());
    return r;
  }

  // ===== ルール・モデル =====

  public static class AutoShiftGenerationRules {
    public Map<Long, Integer> requiredCounts = new HashMap<>();
    public String desiredShiftMode;
    public String existingShiftHandling;
    public String monthlyMaxWorkdaysMode;
    public int monthlyMaxWorkdays;
    public int maxConsecutiveWorkdays;
    public int minimumRestDays;
    public int minimumShiftGapHours;
  }

  public static class TimeBand {
    private final LocalTime start;
    private final LocalTime end;

    public TimeBand(LocalTime start, LocalTime end) {
      this.start = start;
      this.end = end;
    }

    public boolean overlaps(LocalTime s, LocalTime e) {
      return !(e.isBefore(start) || s.isAfter(end));
    }
  }

  public static class GenerationAttemptResult {
    public List<ShiftAssignment> generatedAssignments;
    public int unassignedRequiredCount;
    public List<AutoShiftGenerationResultResponse.UnmetCondition> unmetConditions;

    public GenerationAttemptResult(
        List<ShiftAssignment> generatedAssignments,
        int unassignedRequiredCount,
        List<AutoShiftGenerationResultResponse.UnmetCondition> unmetConditions
    ) {
      this.generatedAssignments = generatedAssignments;
      this.unassignedRequiredCount = unassignedRequiredCount;
      this.unmetConditions = unmetConditions;
    }

    public int totalShortageCount() {
      return unmetConditions.stream()
          .mapToInt(AutoShiftGenerationResultResponse.UnmetCondition::getShortageCount)
          .sum();
    }
  }

  // ===== ルールパース（簡易版） =====

  private AutoShiftGenerationRules parseRules(String json) {
    AutoShiftGenerationRules rules = new AutoShiftGenerationRules();
    if (json == null || json.isBlank()) {
      return rules;
    }
    try {
      JsonNode root = objectMapper.readTree(json);
      rules.desiredShiftMode = root.path("desiredShiftMode").asText(null);
      rules.existingShiftHandling = root.path("existingShiftHandling").asText(null);
      rules.monthlyMaxWorkdaysMode = root.path("monthlyMaxWorkdaysMode").asText("FIXED");
      rules.monthlyMaxWorkdays = root.path("monthlyMaxWorkdays").asInt(0);
      rules.maxConsecutiveWorkdays = root.path("maxConsecutiveWorkdays").asInt(0);
      rules.minimumRestDays = root.path("minimumRestDays").asInt(0);
      rules.minimumShiftGapHours = root.path("minimumShiftGapHours").asInt(0);

      JsonNode requiredNode = root.path("requiredCounts");
      if (requiredNode.isObject()) {
        Iterator<String> fieldNames = requiredNode.fieldNames();
        while (fieldNames.hasNext()) {
          String key = fieldNames.next();
          long shiftTypeId = Long.parseLong(key);
          int count = requiredNode.path(key).asInt(0);
          rules.requiredCounts.put(shiftTypeId, count);
        }
      }
    } catch (Exception e) {
      // ルールが壊れていても、とりあえずデフォルトで動くようにする
    }
    return rules;
  }

  private List<TimeBand> parseNgTimeBands(String input) {
    List<TimeBand> bands = new ArrayList<>();
    if (input == null || input.isBlank()) {
      return bands;
    }

    String trimmed = input.trim();
    if (trimmed.startsWith("[") || trimmed.startsWith("{")) {
      try {
        JsonNode root = objectMapper.readTree(trimmed);
        if (root.isArray()) {
          for (JsonNode node : root) {
            if (node.isObject() && node.has("shiftTypeId")) {
              continue;
            }
            LocalTime start = LocalTime.parse(node.path("start").asText());
            LocalTime end = LocalTime.parse(node.path("end").asText());
            bands.add(new TimeBand(start, end));
          }
        }
      } catch (Exception e) {
        // 壊れていても無視
      }
      return bands;
    }

    String[] entries = trimmed.split("[\\r\\n,;]+");
    for (String entry : entries) {
      String token = entry.trim();
      if (token.isBlank()) {
        continue;
      }

      int separator = token.indexOf('-');
      if (separator < 0) {
        separator = token.indexOf('−');
      }
      if (separator < 0) {
        continue;
      }

      String startText = token.substring(0, separator).trim();
      String endText = token.substring(separator + 1).trim();
      if (startText.isBlank() || endText.isBlank()) {
        continue;
      }

      try {
        LocalTime start = LocalTime.parse(startText);
        LocalTime end = LocalTime.parse(endText);
        bands.add(new TimeBand(start, end));
      } catch (Exception ignored) {
        // 壊れていても無視
      }
    }

    return bands;
  }

  private Set<Long> parseBlockedShiftTypeIds(String input) {
    Set<Long> blockedIds = new HashSet<>();
    if (input == null || input.isBlank()) {
      return blockedIds;
    }

    String trimmed = input.trim();
    if (trimmed.startsWith("[") || trimmed.startsWith("{")) {
      try {
        JsonNode root = objectMapper.readTree(trimmed);
        if (root.isArray()) {
          for (JsonNode node : root) {
            if (node.isNumber()) {
              blockedIds.add(node.longValue());
            } else if (node.isObject()) {
              JsonNode idNode = node.path("shiftTypeId");
              if (idNode.isNumber()) {
                blockedIds.add(idNode.longValue());
              }
            }
          }
          return blockedIds;
        }
        if (root.isObject()) {
          JsonNode idsNode = root.path("shiftTypeIds");
          if (idsNode.isArray()) {
            for (JsonNode node : idsNode) {
              if (node.isNumber()) {
                blockedIds.add(node.longValue());
              }
            }
          }
          return blockedIds;
        }
      } catch (Exception ignored) {
        // 壊れていても無視
      }
      return blockedIds;
    }

    String[] entries = trimmed.split("[\\r\\n,;]+");
    for (String entry : entries) {
      String token = entry.trim();
      if (token.isBlank()) {
        continue;
      }
      try {
        blockedIds.add(Long.parseLong(token));
      } catch (NumberFormatException ignored) {
        // 時刻帯入力は無視して後方互換とする
      }
    }

    return blockedIds;
  }
}
