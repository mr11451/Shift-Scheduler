package com.shiftscheduler.server.service;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shiftscheduler.server.api.AutoShiftGenerationResultResponse;
import com.shiftscheduler.server.domain.RoleLevel;
import com.shiftscheduler.server.domain.ShiftAssignment;
import com.shiftscheduler.server.domain.ShiftRequest;
import com.shiftscheduler.server.domain.ShiftRequestStatus;
import com.shiftscheduler.server.domain.ShiftType;
import com.shiftscheduler.server.domain.Staff;
import com.shiftscheduler.server.repository.ShiftAssignmentRepository;
import com.shiftscheduler.server.repository.ShiftRequestRepository;
import com.shiftscheduler.server.repository.ShiftTypeRepository;
import com.shiftscheduler.server.repository.StaffRepository;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ShiftAssignmentServiceTests {

    @Mock
    private ShiftAssignmentRepository shiftAssignmentRepository;

    @Mock
    private StaffRepository staffRepository;

    @Mock
    private ShiftTypeRepository shiftTypeRepository;

    @Mock
    private ShiftRequestRepository shiftRequestRepository;

    @Mock
    private SystemSettingService systemSettingService;

    @Mock
    private AccessControlService accessControlService;

    @InjectMocks
    private ShiftAssignmentService shiftAssignmentService;

    private void configureObjectMapper() {
        ReflectionTestUtils.setField(shiftAssignmentService, "objectMapper", new ObjectMapper());
    }

    @Test
    void clearMonthlyShiftAssignments_removesConfirmedMonthStatus() {
        Staff editor = new Staff();
        editor.setId(1L);
        editor.setStaffCode("STF-00001");
        editor.setStaffName("管理者");
        editor.setRoleLevel(RoleLevel.MASTER);
        editor.setIsActive(true);

        when(staffRepository.findById(1L)).thenReturn(Optional.of(editor));
        when(accessControlService.canEditShift(any(Staff.class), any(Staff.class))).thenReturn(true);
        when(accessControlService.isMaster(editor)).thenReturn(true);
        when(accessControlService.isChief(editor)).thenReturn(false);
        when(staffRepository.findAllByIsActiveTrue()).thenReturn(List.of(editor));
        when(shiftAssignmentRepository.findByStaffIdInAndWorkDateBetween(any(), any(), any())).thenReturn(List.of());
        when(systemSettingService.isMonthConfirmed(2026, 9)).thenReturn(true);
        when(systemSettingService.isMonthConfirmed(2026, 10)).thenReturn(true);

        int deletedCount = shiftAssignmentService.clearMonthlyShiftAssignments(1L, 2026, 10);

        assertEquals(0, deletedCount);
        verify(systemSettingService).removeConfirmedMonth(1L, 2026, 10);
    }

    @Test
    void clearMonthlyShiftAssignments_resetsExistingRequestsToSubmittedStatus() {
        Staff editor = new Staff();
        editor.setId(1L);
        editor.setStaffCode("STF-00001");
        editor.setStaffName("管理者");
        editor.setRoleLevel(RoleLevel.MASTER);
        editor.setIsActive(true);

        Staff targetStaff = new Staff();
        targetStaff.setId(2L);
        targetStaff.setStaffCode("STF-00002");
        targetStaff.setStaffName("対象者");
        targetStaff.setRoleLevel(RoleLevel.MEMBER);
        targetStaff.setIsActive(true);

        ShiftRequest request = new ShiftRequest();
        request.setId(10L);
        request.setStaff(targetStaff);
        request.setWorkDate(LocalDate.of(2026, 10, 5));
        request.setStatus(ShiftRequestStatus.APPLIED);
        request.setSubmittedAt(OffsetDateTime.parse("2026-09-01T10:00:00+09:00"));
        request.setDecidedAt(OffsetDateTime.parse("2026-09-02T10:00:00+09:00"));

        when(staffRepository.findById(1L)).thenReturn(Optional.of(editor));
        when(accessControlService.canEditShift(any(Staff.class), any(Staff.class))).thenReturn(true);
        when(staffRepository.findAllByIsActiveTrue()).thenReturn(List.of(editor, targetStaff));
        when(shiftAssignmentRepository.findByStaffIdInAndWorkDateBetween(any(), any(), any())).thenReturn(List.of());
        when(shiftRequestRepository.findByStaffIdInAndWorkDateBetween(any(), any(), any())).thenReturn(List.of(request));
        when(shiftRequestRepository.save(any(ShiftRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(systemSettingService.isMonthConfirmed(2026, 10)).thenReturn(false);

        int deletedCount = shiftAssignmentService.clearMonthlyShiftAssignments(1L, 2026, 10);

        assertEquals(0, deletedCount);
        assertEquals(ShiftRequestStatus.SUBMITTED, request.getStatus());
        assertEquals(true, request.getDecidedAt() == null);
        verify(shiftRequestRepository).save(request);
    }

    @Test
    void autoGenerateShiftAssignments_processesLastDayOfMonthEvenWhenHolidayIsConfigured() throws Exception {
        Staff editor = new Staff();
        editor.setId(1L);
        editor.setStaffCode("STF-00001");
        editor.setStaffName("管理者");
        editor.setRoleLevel(RoleLevel.MASTER);
        editor.setIsActive(true);

        ShiftType workShift = new ShiftType();
        workShift.setId(10L);
        workShift.setShiftCode("A");
        workShift.setShiftName("早番");
        workShift.setStartTime(LocalTime.of(9, 0));
        workShift.setEndTime(LocalTime.of(18, 0));
        workShift.setIsActive(true);

        when(staffRepository.findById(1L)).thenReturn(Optional.of(editor));
        when(staffRepository.findAllByIsActiveTrue()).thenReturn(List.of(editor));
        when(accessControlService.canEditShift(any(Staff.class), any(Staff.class))).thenReturn(true);
        when(shiftTypeRepository.findAllActiveWorkShifts()).thenReturn(List.of(workShift));
        when(shiftAssignmentRepository.findByStaffIdInAndWorkDateBetween(any(), any(), any())).thenReturn(List.of());
        when(shiftRequestRepository.findByStaffIdInAndWorkDateBetween(any(), any(), any())).thenReturn(List.of());
        when(systemSettingService.isMonthConfirmed(2026, 8)).thenReturn(false);
        when(systemSettingService.getSystemSettingTextValue("autoShiftGenerationRules")).thenReturn(
                "{\"requiredCounts\":{\"10\":1},\"monthlyMaxWorkdaysMode\":\"FIXED\",\"monthlyMaxWorkdays\":40,\"maxConsecutiveWorkdays\":40,\"minimumRestDays\":0,\"desiredShiftMode\":\"IGNORE\",\"existingShiftHandling\":\"ONLY_EMPTY\"}");

        AutoShiftGenerationResultResponse result = shiftAssignmentService.autoGenerateShiftAssignments(1L, 2026, 8);

        assertEquals(31, result.getGeneratedCount());
        assertEquals(LocalDate.of(2026, 8, 1), result.getStartDate());
        assertEquals(LocalDate.of(2026, 8, 31), result.getEndDate());

        ArgumentCaptor<List<ShiftAssignment>> captor = ArgumentCaptor.forClass(List.class);
        verify(shiftAssignmentRepository).saveAll(captor.capture());
        List<ShiftAssignment> savedAssignments = captor.getValue();
        assertEquals(31, savedAssignments.size());
        assertTrue(savedAssignments.stream().anyMatch(assignment -> assignment.getWorkDate().equals(LocalDate.of(2026, 8, 31))));
    }

    @Test
    void autoGenerateShiftAssignments_overwritesExistingAssignmentsWhenConfigured() throws Exception {
        Staff editor = new Staff();
        editor.setId(1L);
        editor.setStaffCode("STF-00001");
        editor.setStaffName("管理者");
        editor.setRoleLevel(RoleLevel.MASTER);
        editor.setIsActive(true);

        Staff targetStaff = createStaff(2L, "STF-00002", null);

        ShiftType workShift = new ShiftType();
        workShift.setId(10L);
        workShift.setShiftCode("A");
        workShift.setShiftName("早番");
        workShift.setStartTime(LocalTime.of(9, 0));
        workShift.setEndTime(LocalTime.of(18, 0));
        workShift.setIsActive(true);

        ShiftAssignment existingAssignment = new ShiftAssignment();
        existingAssignment.setId(99L);
        existingAssignment.setStaff(targetStaff);
        existingAssignment.setWorkDate(LocalDate.of(2026, 9, 1));
        existingAssignment.setShiftType(workShift);
        existingAssignment.setUpdatedBy(editor);
        existingAssignment.setUpdatedAt(OffsetDateTime.now());

        when(staffRepository.findById(1L)).thenReturn(Optional.of(editor));
        when(staffRepository.findAllByIsActiveTrue()).thenReturn(List.of(editor, targetStaff));
        when(accessControlService.canEditShift(any(Staff.class), any(Staff.class))).thenReturn(true);
        when(shiftTypeRepository.findAllActiveWorkShifts()).thenReturn(List.of(workShift));
        when(shiftAssignmentRepository.findByStaffIdInAndWorkDateBetween(any(), any(), any())).thenReturn(List.of(existingAssignment));
        when(shiftRequestRepository.findByStaffIdInAndWorkDateBetween(any(), any(), any())).thenReturn(List.of());
        when(shiftAssignmentRepository.saveAll(any(List.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(systemSettingService.isMonthConfirmed(2026, 9)).thenReturn(false);
        when(systemSettingService.getSystemSettingTextValue("autoShiftGenerationRules")).thenReturn(
                "{\"requiredCounts\":{\"10\":1},\"monthlyMaxWorkdaysMode\":\"FIXED\",\"monthlyMaxWorkdays\":40,\"maxConsecutiveWorkdays\":40,\"minimumRestDays\":0,\"desiredShiftMode\":\"IGNORE\",\"existingShiftHandling\":\"OVERWRITE\"}");

        AutoShiftGenerationResultResponse result = shiftAssignmentService.autoGenerateShiftAssignments(1L, 2026, 9);

        assertTrue(result.getGeneratedCount() > 0);
        verify(shiftAssignmentRepository).deleteByStaffIdInAndWorkDateBetween(any(), any(), any());
    }

    @Test
    void resolveMonthlyMaxWorkdays_usesCalculatedValueForTargetMonth() {
        ShiftAssignmentService.AutoShiftGenerationRules rules = new ShiftAssignmentService.AutoShiftGenerationRules();
        rules.monthlyMaxWorkdaysMode = "CALCULATED";
        rules.monthlyMaxWorkdays = 0;

        int maxForAugust = ReflectionTestUtils.invokeMethod(shiftAssignmentService, "resolveMonthlyMaxWorkdays", rules, LocalDate.of(2026, 8, 15));
        int maxForSeptember = ReflectionTestUtils.invokeMethod(shiftAssignmentService, "resolveMonthlyMaxWorkdays", rules, LocalDate.of(2026, 9, 15));
        int maxForFebruary = ReflectionTestUtils.invokeMethod(shiftAssignmentService, "resolveMonthlyMaxWorkdays", rules, LocalDate.of(2026, 2, 15));

        assertEquals(24, maxForAugust);
        assertEquals(23, maxForSeptember);
        assertEquals(21, maxForFebruary);
    }

    @Test
    void autoGenerateShiftAssignments_respectsCalculatedMonthlyMaxWorkdaysAsUpperBound() throws Exception {
        Staff editor = new Staff();
        editor.setId(1L);
        editor.setStaffCode("STF-00001");
        editor.setStaffName("管理者");
        editor.setRoleLevel(RoleLevel.MASTER);
        editor.setIsActive(true);

        Staff staffA = createStaff(2L, "STF-00002", null);
        Staff staffB = createStaff(3L, "STF-00003", null);

        ShiftType workShift = new ShiftType();
        workShift.setId(10L);
        workShift.setShiftCode("A");
        workShift.setShiftName("早番");
        workShift.setStartTime(LocalTime.of(9, 0));
        workShift.setEndTime(LocalTime.of(18, 0));
        workShift.setIsActive(true);

        when(staffRepository.findById(1L)).thenReturn(Optional.of(editor));
        when(staffRepository.findAllByIsActiveTrue()).thenReturn(List.of(editor, staffA, staffB));
        when(accessControlService.canEditShift(any(Staff.class), any(Staff.class))).thenReturn(true);
        when(shiftTypeRepository.findAllActiveWorkShifts()).thenReturn(List.of(workShift));
        when(shiftAssignmentRepository.findByStaffIdInAndWorkDateBetween(any(), any(), any())).thenReturn(List.of());
        when(shiftRequestRepository.findByStaffIdInAndWorkDateBetween(any(), any(), any())).thenReturn(List.of());
        when(systemSettingService.isMonthConfirmed(2026, 8)).thenReturn(false);
        when(systemSettingService.getSystemSettingTextValue("autoShiftGenerationRules")).thenReturn(
                "{\"requiredCounts\":{\"10\":1},\"monthlyMaxWorkdaysMode\":\"CALCULATED\",\"monthlyMaxWorkdays\":0,\"maxConsecutiveWorkdays\":40,\"minimumRestDays\":0,\"desiredShiftMode\":\"IGNORE\",\"existingShiftHandling\":\"ONLY_EMPTY\"}");

        AutoShiftGenerationResultResponse result = shiftAssignmentService.autoGenerateShiftAssignments(1L, 2026, 8);

        assertEquals(31, result.getGeneratedCount());
        Map<Long, Integer> countsByStaff = new HashMap<>();

        ArgumentCaptor<List<ShiftAssignment>> captor = ArgumentCaptor.forClass(List.class);
        verify(shiftAssignmentRepository).saveAll(captor.capture());
        List<ShiftAssignment> generated = captor.getValue();
        assertEquals(31, generated.size());

        for (ShiftAssignment assignment : generated) {
            countsByStaff.merge(assignment.getStaff().getId(), 1, Integer::sum);
        }

        for (Integer count : countsByStaff.values()) {
            assertTrue(count <= 23);
        }
    }

    @Test
    void autoGenerateShiftAssignments_prioritizesRequiredCountsOverMonthlyCap() throws Exception {
        Staff editor = new Staff();
        editor.setId(1L);
        editor.setStaffCode("STF-00001");
        editor.setStaffName("管理者");
        editor.setRoleLevel(RoleLevel.MASTER);
        editor.setIsActive(true);

        Staff staffA = createStaff(2L, "STF-00002", null);
        Staff staffB = createStaff(3L, "STF-00003", null);

        ShiftType workShift = new ShiftType();
        workShift.setId(10L);
        workShift.setShiftCode("A");
        workShift.setShiftName("早番");
        workShift.setStartTime(LocalTime.of(9, 0));
        workShift.setEndTime(LocalTime.of(18, 0));
        workShift.setIsActive(true);

        when(staffRepository.findById(1L)).thenReturn(Optional.of(editor));
        when(staffRepository.findAllByIsActiveTrue()).thenReturn(List.of(editor, staffA, staffB));
        when(accessControlService.canEditShift(any(Staff.class), any(Staff.class))).thenReturn(true);
        when(shiftTypeRepository.findAllActiveWorkShifts()).thenReturn(List.of(workShift));
        when(shiftAssignmentRepository.findByStaffIdInAndWorkDateBetween(any(), any(), any())).thenReturn(List.of());
        when(shiftRequestRepository.findByStaffIdInAndWorkDateBetween(any(), any(), any())).thenReturn(List.of());
        when(systemSettingService.isMonthConfirmed(2026, 8)).thenReturn(false);
        when(systemSettingService.getSystemSettingTextValue("autoShiftGenerationRules")).thenReturn(
                "{\"requiredCounts\":{\"10\":1},\"monthlyMaxWorkdaysMode\":\"FIXED\",\"monthlyMaxWorkdays\":1,\"maxConsecutiveWorkdays\":40,\"minimumRestDays\":0,\"desiredShiftMode\":\"IGNORE\",\"existingShiftHandling\":\"ONLY_EMPTY\"}");

        AutoShiftGenerationResultResponse result = shiftAssignmentService.autoGenerateShiftAssignments(1L, 2026, 8);

        assertEquals(31, result.getGeneratedCount());
        assertEquals(0, result.getUnassignedRequiredCount());
        assertEquals(31, result.getGeneratedCount());
        assertEquals(31, result.getGeneratedCount());
    }

    @Test
    void autoGenerateShiftAssignments_usesConfiguredCalculatedMonthlyMaxWorkdaysFormula() throws Exception {
        Staff editor = new Staff();
        editor.setId(1L);
        editor.setStaffCode("STF-00001");
        editor.setStaffName("管理者");
        editor.setRoleLevel(RoleLevel.MASTER);
        editor.setIsActive(true);

        ShiftType workShift = new ShiftType();
        workShift.setId(10L);
        workShift.setShiftCode("A");
        workShift.setShiftName("早番");
        workShift.setStartTime(LocalTime.of(9, 0));
        workShift.setEndTime(LocalTime.of(18, 0));
        workShift.setIsActive(true);

        when(staffRepository.findById(1L)).thenReturn(Optional.of(editor));
        when(staffRepository.findAllByIsActiveTrue()).thenReturn(List.of(editor));
        when(accessControlService.canEditShift(any(Staff.class), any(Staff.class))).thenReturn(true);
        when(shiftTypeRepository.findAllActiveWorkShifts()).thenReturn(List.of(workShift));
        when(shiftAssignmentRepository.findByStaffIdInAndWorkDateBetween(any(), any(), any())).thenReturn(List.of());
        when(shiftRequestRepository.findByStaffIdInAndWorkDateBetween(any(), any(), any())).thenReturn(List.of());
        when(systemSettingService.isMonthConfirmed(2026, 8)).thenReturn(false);
        when(systemSettingService.getSystemSettingTextValue("autoShiftGenerationRules")).thenReturn(
                "{\"requiredCounts\":{\"10\":1},\"monthlyMaxWorkdaysMode\":\"CALCULATED\",\"monthlyMaxWorkdays\":0,\"maxConsecutiveWorkdays\":40,\"minimumRestDays\":0,\"desiredShiftMode\":\"IGNORE\",\"existingShiftHandling\":\"ONLY_EMPTY\"}");

        AutoShiftGenerationResultResponse result = shiftAssignmentService.autoGenerateShiftAssignments(1L, 2026, 8);

        assertEquals(31, result.getGeneratedCount());
    }

    @Test
    void autoGenerateShiftAssignments_prefersLessRecentlyWorkedStaffWhenCountsAreTied() throws Exception {
        Staff editor = new Staff();
        editor.setId(99L);
        editor.setStaffCode("STF-99999");
        editor.setStaffName("管理者");
        editor.setRoleLevel(RoleLevel.MASTER);
        editor.setIsActive(true);

        Staff staffA = createStaff(1L, "STF-00001", null);
        Staff staffB = createStaff(2L, "STF-00002", null);
        Staff staffC = createStaff(3L, "STF-00003", "{\"shiftTypeIds\":[10]}");

        ShiftType workShift = new ShiftType();
        workShift.setId(10L);
        workShift.setShiftCode("A");
        workShift.setShiftName("早番");
        workShift.setStartTime(LocalTime.of(9, 0));
        workShift.setEndTime(LocalTime.of(18, 0));
        workShift.setIsActive(true);

        when(staffRepository.findById(99L)).thenReturn(Optional.of(editor));
        when(staffRepository.findAllByIsActiveTrue()).thenReturn(List.of(editor, staffA, staffB, staffC));
        when(accessControlService.canEditShift(editor, editor)).thenReturn(false);
        when(accessControlService.canEditShift(editor, staffA)).thenReturn(true);
        when(accessControlService.canEditShift(editor, staffB)).thenReturn(true);
        when(accessControlService.canEditShift(editor, staffC)).thenReturn(true);
        when(shiftTypeRepository.findAllActiveWorkShifts()).thenReturn(List.of(workShift));
        when(shiftAssignmentRepository.findByStaffIdInAndWorkDateBetween(any(), any(), any())).thenReturn(List.of(
                existingAssignment(1L, staffA, LocalDate.of(2026, 8, 1), workShift, editor),
                existingAssignment(2L, staffB, LocalDate.of(2026, 8, 2), workShift, editor),
                existingAssignment(3L, staffB, LocalDate.of(2026, 8, 3), workShift, editor),
                existingAssignment(4L, staffA, LocalDate.of(2026, 8, 4), workShift, editor),
                existingAssignment(5L, staffC, LocalDate.of(2026, 8, 5), workShift, editor)
        ));
        when(shiftRequestRepository.findByStaffIdInAndWorkDateBetween(any(), any(), any())).thenReturn(List.of());
        when(systemSettingService.isMonthConfirmed(2026, 8)).thenReturn(false);
        when(systemSettingService.getSystemSettingTextValue("autoShiftGenerationRules")).thenReturn(
                "{\"requiredCounts\":{\"10\":1},\"monthlyMaxWorkdaysMode\":\"FIXED\",\"monthlyMaxWorkdays\":40,\"maxConsecutiveWorkdays\":40,\"minimumRestDays\":0,\"desiredShiftMode\":\"IGNORE\",\"existingShiftHandling\":\"ONLY_EMPTY\"}");

        AutoShiftGenerationResultResponse result = shiftAssignmentService.autoGenerateShiftAssignments(99L, 2026, 8);

        assertEquals(26, result.getGeneratedCount());

        ArgumentCaptor<List<ShiftAssignment>> captor = ArgumentCaptor.forClass(List.class);
        verify(shiftAssignmentRepository).saveAll(captor.capture());
        ShiftAssignment generatedOnSixth = captor.getValue().stream()
                .filter(assignment -> LocalDate.of(2026, 8, 6).equals(assignment.getWorkDate()))
                .findFirst()
                .orElseThrow();

        assertEquals(1L, generatedOnSixth.getStaff().getId());
    }

    @Test
    void autoGenerateShiftAssignments_relaxesMinimumRestWhenOtherwiseNoOneCanWork() throws Exception {
        Staff editor = new Staff();
        editor.setId(50L);
        editor.setStaffCode("STF-99950");
        editor.setStaffName("管理者");
        editor.setRoleLevel(RoleLevel.MASTER);
        editor.setIsActive(true);

        Staff staffA = createStaff(1L, "STF-00001", null);
        Staff staffB = createStaff(2L, "STF-00002", null);

        ShiftType workShift = new ShiftType();
        workShift.setId(10L);
        workShift.setShiftCode("A");
        workShift.setShiftName("早番");
        workShift.setStartTime(LocalTime.of(9, 0));
        workShift.setEndTime(LocalTime.of(18, 0));
        workShift.setIsActive(true);

        when(staffRepository.findById(50L)).thenReturn(Optional.of(editor));
        when(staffRepository.findAllByIsActiveTrue()).thenReturn(List.of(editor, staffA, staffB));
        when(accessControlService.canEditShift(editor, editor)).thenReturn(false);
        when(accessControlService.canEditShift(editor, staffA)).thenReturn(true);
        when(accessControlService.canEditShift(editor, staffB)).thenReturn(true);
        when(shiftTypeRepository.findAllActiveWorkShifts()).thenReturn(List.of(workShift));
        when(shiftAssignmentRepository.findByStaffIdInAndWorkDateBetween(any(), any(), any())).thenReturn(List.of(
                existingAssignment(1L, staffA, LocalDate.of(2026, 8, 1), workShift, editor),
                existingAssignment(2L, staffB, LocalDate.of(2026, 8, 1), workShift, editor)
        ));
        when(shiftRequestRepository.findByStaffIdInAndWorkDateBetween(any(), any(), any())).thenReturn(List.of());
        when(systemSettingService.isMonthConfirmed(2026, 8)).thenReturn(false);
        when(systemSettingService.getSystemSettingTextValue("autoShiftGenerationRules")).thenReturn(
                "{\"requiredCounts\":{\"10\":1},\"monthlyMaxWorkdaysMode\":\"FIXED\",\"monthlyMaxWorkdays\":40,\"maxConsecutiveWorkdays\":40,\"minimumRestDays\":2,\"desiredShiftMode\":\"IGNORE\",\"existingShiftHandling\":\"ONLY_EMPTY\"}");

        shiftAssignmentService.autoGenerateShiftAssignments(50L, 2026, 8);

        ArgumentCaptor<List<ShiftAssignment>> captor = ArgumentCaptor.forClass(List.class);
        verify(shiftAssignmentRepository).saveAll(captor.capture());
        assertTrue(captor.getValue().stream().anyMatch(assignment -> LocalDate.of(2026, 8, 3).equals(assignment.getWorkDate())));
    }

    @Test
    void autoGenerateShiftAssignments_skipsVacationRequests() throws Exception {
        Staff editor = new Staff();
        editor.setId(100L);
        editor.setStaffCode("STF-999100");
        editor.setStaffName("管理者");
        editor.setRoleLevel(RoleLevel.MASTER);
        editor.setIsActive(true);

        Staff staff = createStaff(1L, "STF-00001", null);

        ShiftType workShift = new ShiftType();
        workShift.setId(10L);
        workShift.setShiftCode("A");
        workShift.setShiftName("早番");
        workShift.setStartTime(LocalTime.of(9, 0));
        workShift.setEndTime(LocalTime.of(18, 0));
        workShift.setIsActive(true);

        ShiftRequest vacationRequest = new ShiftRequest();
        vacationRequest.setId(200L);
        vacationRequest.setStaff(staff);
        vacationRequest.setWorkDate(LocalDate.of(2026, 8, 1));
        vacationRequest.setStatus(ShiftRequestStatus.SUBMITTED);
        vacationRequest.setDesiredShiftType(null);

        when(staffRepository.findById(100L)).thenReturn(Optional.of(editor));
        when(staffRepository.findAllByIsActiveTrue()).thenReturn(List.of(editor, staff));
        when(accessControlService.canEditShift(editor, editor)).thenReturn(false);
        when(accessControlService.canEditShift(editor, staff)).thenReturn(true);
        when(shiftTypeRepository.findAllActiveWorkShifts()).thenReturn(List.of(workShift));
        when(shiftAssignmentRepository.findByStaffIdInAndWorkDateBetween(any(), any(), any())).thenReturn(List.of());
        when(shiftRequestRepository.findByStaffIdInAndWorkDateBetween(any(), any(), any())).thenReturn(List.of(vacationRequest));
        when(systemSettingService.isMonthConfirmed(2026, 8)).thenReturn(false);
        when(systemSettingService.getSystemSettingTextValue("autoShiftGenerationRules")).thenReturn(
                "{\"requiredCounts\":{\"10\":1},\"monthlyMaxWorkdaysMode\":\"FIXED\",\"monthlyMaxWorkdays\":40,\"maxConsecutiveWorkdays\":40,\"minimumRestDays\":0,\"desiredShiftMode\":\"REQUIRED\",\"existingShiftHandling\":\"ONLY_EMPTY\"}");

        AutoShiftGenerationResultResponse result = shiftAssignmentService.autoGenerateShiftAssignments(100L, 2026, 8);

        assertEquals(30, result.getGeneratedCount());
        assertTrue(result.getUnmetConditions().isEmpty());
        assertEquals(0, result.getUnassignedRequiredCount());

        ArgumentCaptor<List<ShiftAssignment>> captor = ArgumentCaptor.forClass(List.class);
        verify(shiftAssignmentRepository).saveAll(captor.capture());
        assertTrue(captor.getValue().stream().noneMatch(assignment -> LocalDate.of(2026, 8, 1).equals(assignment.getWorkDate()) && staff.getId().equals(assignment.getStaff().getId())));
    }

    @Test
    void calculateCandidateWeight_givesOnlySmallBonusForRequestMatch() throws Exception {
        var scoreClass = Class.forName("com.shiftscheduler.server.service.ShiftAssignmentService$StaffSelectionScore");
        Constructor<?> constructor = scoreClass.getDeclaredConstructor(
                Staff.class,
                boolean.class,
                boolean.class,
                int.class,
                int.class,
                int.class,
                LocalDate.class);
        constructor.setAccessible(true);

        Object withoutRequest = constructor.newInstance(null, false, false, 0, 0, 0, null);
        Object withRequest = constructor.newInstance(null, true, false, 0, 0, 0, null);

        ShiftAssignmentService.AutoShiftGenerationRules rules = new ShiftAssignmentService.AutoShiftGenerationRules();
        rules.monthlyMaxWorkdaysMode = "FIXED";
        rules.monthlyMaxWorkdays = 40;
        rules.maxConsecutiveWorkdays = 40;
        rules.minimumRestDays = 0;
        rules.minimumShiftGapHours = 0;

        int baseWeight = ReflectionTestUtils.invokeMethod(shiftAssignmentService, "calculateCandidateWeight", withoutRequest, LocalDate.of(2026, 8, 15), rules);
        int requestWeight = ReflectionTestUtils.invokeMethod(shiftAssignmentService, "calculateCandidateWeight", withRequest, LocalDate.of(2026, 8, 15), rules);

        assertEquals(10, requestWeight - baseWeight);
    }

    @Test
    void calculateCandidateWeight_givesSmallBonusForPreferredShiftBandMatch() throws Exception {
        var scoreClass = Class.forName("com.shiftscheduler.server.service.ShiftAssignmentService$StaffSelectionScore");
        Constructor<?> constructor = scoreClass.getDeclaredConstructor(
                Staff.class,
                boolean.class,
                boolean.class,
                int.class,
                int.class,
                int.class,
                LocalDate.class);
        constructor.setAccessible(true);

        Object withoutPreferredBand = constructor.newInstance(null, false, false, 0, 0, 0, null);
        Object withPreferredBand = constructor.newInstance(null, false, false, 0, 0, 0, null);

        var preferredBandField = scoreClass.getDeclaredField("preferredBandMatch");
        preferredBandField.setAccessible(true);
        preferredBandField.setBoolean(withPreferredBand, true);

        ShiftAssignmentService.AutoShiftGenerationRules rules = new ShiftAssignmentService.AutoShiftGenerationRules();
        rules.monthlyMaxWorkdaysMode = "FIXED";
        rules.monthlyMaxWorkdays = 40;
        rules.maxConsecutiveWorkdays = 40;
        rules.minimumRestDays = 0;
        rules.minimumShiftGapHours = 0;

        int baseWeight = ReflectionTestUtils.invokeMethod(shiftAssignmentService, "calculateCandidateWeight", withoutPreferredBand, LocalDate.of(2026, 8, 15), rules);
        int preferredWeight = ReflectionTestUtils.invokeMethod(shiftAssignmentService, "calculateCandidateWeight", withPreferredBand, LocalDate.of(2026, 8, 15), rules);

        assertEquals(5, preferredWeight - baseWeight);
    }

    @Test
    void autoGenerateShiftAssignments_fillsRequiredCountEvenWhenShiftGapWouldOtherwiseBlock() throws Exception {
        Staff editor = new Staff();
        editor.setId(62L);
        editor.setStaffCode("STF-99962");
        editor.setStaffName("管理者");
        editor.setRoleLevel(RoleLevel.MASTER);
        editor.setIsActive(true);

        Staff staff = createStaff(1L, "STF-00001", null);

        ShiftType workShift = new ShiftType();
        workShift.setId(10L);
        workShift.setShiftCode("A");
        workShift.setShiftName("早番");
        workShift.setStartTime(LocalTime.of(9, 0));
        workShift.setEndTime(LocalTime.of(18, 0));
        workShift.setIsActive(true);

        when(staffRepository.findById(62L)).thenReturn(Optional.of(editor));
        when(staffRepository.findAllByIsActiveTrue()).thenReturn(List.of(editor, staff));
        when(accessControlService.canEditShift(editor, editor)).thenReturn(false);
        when(accessControlService.canEditShift(editor, staff)).thenReturn(true);
        when(shiftTypeRepository.findAllActiveWorkShifts()).thenReturn(List.of(workShift));
        when(shiftAssignmentRepository.findByStaffIdInAndWorkDateBetween(any(), any(), any())).thenReturn(List.of(
                existingAssignment(100L, staff, LocalDate.of(2026, 8, 10), workShift, editor)
        ));
        when(shiftRequestRepository.findByStaffIdInAndWorkDateBetween(any(), any(), any())).thenReturn(List.of());
        when(systemSettingService.isMonthConfirmed(2026, 8)).thenReturn(false);
        when(systemSettingService.getSystemSettingTextValue("autoShiftGenerationRules")).thenReturn(
                "{\"requiredCounts\":{\"10\":1},\"monthlyMaxWorkdaysMode\":\"FIXED\",\"monthlyMaxWorkdays\":40,\"maxConsecutiveWorkdays\":40,\"minimumRestDays\":0,\"minimumShiftGapHours\":16,\"desiredShiftMode\":\"IGNORE\",\"existingShiftHandling\":\"ONLY_EMPTY\"}");

        AutoShiftGenerationResultResponse result = shiftAssignmentService.autoGenerateShiftAssignments(62L, 2026, 8);

        assertTrue(result.getGeneratedCount() > 0);
        assertEquals(0, result.getUnassignedRequiredCount());
        assertTrue(result.getUnmetConditions().isEmpty());

        ArgumentCaptor<List<ShiftAssignment>> captor = ArgumentCaptor.forClass(List.class);
        verify(shiftAssignmentRepository).saveAll(captor.capture());
        assertTrue(captor.getValue().stream().anyMatch(assignment -> LocalDate.of(2026, 8, 11).equals(assignment.getWorkDate())));
    }

    @Test
    void autoGenerateShiftAssignments_respectsBlockedShiftTypes() throws Exception {
        Staff editor = new Staff();
        editor.setId(60L);
        editor.setStaffCode("STF-99960");
        editor.setStaffName("管理者");
        editor.setRoleLevel(RoleLevel.MASTER);
        editor.setIsActive(true);

        Staff blockedStaff = createStaff(1L, "STF-00001", "{\"shiftTypeIds\":[10]}");

        ShiftType workShift = new ShiftType();
        workShift.setId(10L);
        workShift.setShiftCode("A");
        workShift.setShiftName("早番");
        workShift.setStartTime(LocalTime.of(10, 0));
        workShift.setEndTime(LocalTime.of(12, 0));
        workShift.setIsActive(true);

        when(staffRepository.findById(60L)).thenReturn(Optional.of(editor));
        when(staffRepository.findAllByIsActiveTrue()).thenReturn(List.of(editor, blockedStaff));
        when(accessControlService.canEditShift(editor, editor)).thenReturn(false);
        when(accessControlService.canEditShift(editor, blockedStaff)).thenReturn(true);
        when(shiftTypeRepository.findAllActiveWorkShifts()).thenReturn(List.of(workShift));
        when(shiftAssignmentRepository.findByStaffIdInAndWorkDateBetween(any(), any(), any())).thenReturn(List.of());
        when(shiftRequestRepository.findByStaffIdInAndWorkDateBetween(any(), any(), any())).thenReturn(List.of());
        when(systemSettingService.isMonthConfirmed(2026, 8)).thenReturn(false);
        when(systemSettingService.getSystemSettingTextValue("autoShiftGenerationRules")).thenReturn(
                "{\"requiredCounts\":{\"10\":1},\"monthlyMaxWorkdaysMode\":\"FIXED\",\"monthlyMaxWorkdays\":40,\"maxConsecutiveWorkdays\":40,\"minimumRestDays\":0,\"desiredShiftMode\":\"IGNORE\",\"existingShiftHandling\":\"ONLY_EMPTY\"}");

        AutoShiftGenerationResultResponse result = shiftAssignmentService.autoGenerateShiftAssignments(60L, 2026, 8);

        assertEquals(0, result.getGeneratedCount());
        assertTrue(!result.getUnmetConditions().isEmpty());
        assertEquals("早番", result.getUnmetConditions().get(0).getShiftTypeName());
    }

    @Test
    void autoGenerateShiftAssignments_prioritizesBlockedShiftTypesOverOtherRules() throws Exception {
        Staff editor = new Staff();
        editor.setId(61L);
        editor.setStaffCode("STF-99961");
        editor.setStaffName("管理者");
        editor.setRoleLevel(RoleLevel.MASTER);
        editor.setIsActive(true);

        Staff blockedStaff = createStaff(1L, "STF-00001", "{\"shiftTypeIds\":[10]}");
        Staff allowedStaff = createStaff(2L, "STF-00002", null);

        ShiftType workShift = new ShiftType();
        workShift.setId(10L);
        workShift.setShiftCode("A");
        workShift.setShiftName("早番");
        workShift.setStartTime(LocalTime.of(9, 0));
        workShift.setEndTime(LocalTime.of(18, 0));
        workShift.setIsActive(true);

        when(staffRepository.findById(61L)).thenReturn(Optional.of(editor));
        when(staffRepository.findAllByIsActiveTrue()).thenReturn(List.of(editor, blockedStaff, allowedStaff));
        when(accessControlService.canEditShift(editor, editor)).thenReturn(false);
        when(accessControlService.canEditShift(editor, blockedStaff)).thenReturn(true);
        when(accessControlService.canEditShift(editor, allowedStaff)).thenReturn(true);
        when(shiftTypeRepository.findAllActiveWorkShifts()).thenReturn(List.of(workShift));
        when(shiftAssignmentRepository.findByStaffIdInAndWorkDateBetween(any(), any(), any())).thenReturn(List.of());
        when(shiftRequestRepository.findByStaffIdInAndWorkDateBetween(any(), any(), any())).thenReturn(List.of());
        when(systemSettingService.isMonthConfirmed(2026, 8)).thenReturn(false);
        when(systemSettingService.getSystemSettingTextValue("autoShiftGenerationRules")).thenReturn(
                "{\"requiredCounts\":{\"10\":1},\"monthlyMaxWorkdaysMode\":\"FIXED\",\"monthlyMaxWorkdays\":40,\"maxConsecutiveWorkdays\":40,\"minimumRestDays\":0,\"desiredShiftMode\":\"IGNORE\",\"existingShiftHandling\":\"ONLY_EMPTY\"}");

        AutoShiftGenerationResultResponse result = shiftAssignmentService.autoGenerateShiftAssignments(61L, 2026, 8);

        assertEquals(31, result.getGeneratedCount());

        ArgumentCaptor<List<ShiftAssignment>> captor = ArgumentCaptor.forClass(List.class);
        verify(shiftAssignmentRepository).saveAll(captor.capture());
        assertTrue(captor.getValue().stream().anyMatch(assignment -> allowedStaff.getId().equals(assignment.getStaff().getId())));
    }

        @Test
        void autoGenerateShiftAssignments_blocksByNgShiftOrWeekday() throws Exception {
            configureObjectMapper();

        Staff editor = new Staff();
        editor.setId(62L);
        editor.setStaffCode("STF-99962");
        editor.setStaffName("管理者");
        editor.setRoleLevel(RoleLevel.MASTER);
        editor.setIsActive(true);

        Staff blockedStaff = createStaff(1L, "STF-00001", "{\"shiftTypeIds\":[10],\"weekdayIds\":[6]}");

        ShiftType workShift = new ShiftType();
        workShift.setId(10L);
        workShift.setShiftCode("A");
        workShift.setShiftName("早番");
        workShift.setStartTime(LocalTime.of(9, 0));
        workShift.setEndTime(LocalTime.of(18, 0));
        workShift.setIsActive(true);

        when(staffRepository.findById(62L)).thenReturn(Optional.of(editor));
        when(staffRepository.findAllByIsActiveTrue()).thenReturn(List.of(editor, blockedStaff));
        when(accessControlService.canEditShift(editor, editor)).thenReturn(false);
        when(accessControlService.canEditShift(editor, blockedStaff)).thenReturn(true);
        when(shiftTypeRepository.findAllActiveWorkShifts()).thenReturn(List.of(workShift));
        when(shiftAssignmentRepository.findByStaffIdInAndWorkDateBetween(any(), any(), any())).thenReturn(List.of());
        when(shiftRequestRepository.findByStaffIdInAndWorkDateBetween(any(), any(), any())).thenReturn(List.of());
        when(systemSettingService.isMonthConfirmed(2026, 8)).thenReturn(false);
        when(systemSettingService.getSystemSettingTextValue("autoShiftGenerationRules")).thenReturn(
            "{\"requiredCounts\":{\"10\":1},\"monthlyMaxWorkdaysMode\":\"FIXED\",\"monthlyMaxWorkdays\":40,\"maxConsecutiveWorkdays\":40,\"minimumRestDays\":0,\"desiredShiftMode\":\"IGNORE\",\"existingShiftHandling\":\"ONLY_EMPTY\"}");

        AutoShiftGenerationResultResponse result = shiftAssignmentService.autoGenerateShiftAssignments(62L, 2026, 8);

        assertEquals(0, result.getGeneratedCount());
        assertEquals(31, result.getUnassignedRequiredCount());
        }

        @Test
        void autoGenerateShiftAssignments_prefersMatchingWeekdayPreferenceOnGenerationDay() throws Exception {
            configureObjectMapper();

        Staff editor = new Staff();
        editor.setId(63L);
        editor.setStaffCode("STF-99963");
        editor.setStaffName("管理者");
        editor.setRoleLevel(RoleLevel.MASTER);
        editor.setIsActive(true);

        Staff preferredStaff = createStaff(1L, "STF-00001", null);
        preferredStaff.setPreferredShiftTypeIds("{\"shiftTypeIds\":[10],\"weekdayIds\":[6]}");
        Staff otherStaff = createStaff(2L, "STF-00002", null);

        ShiftType workShift = new ShiftType();
        workShift.setId(10L);
        workShift.setShiftCode("A");
        workShift.setShiftName("早番");
        workShift.setStartTime(LocalTime.of(9, 0));
        workShift.setEndTime(LocalTime.of(18, 0));
        workShift.setIsActive(true);

        when(staffRepository.findById(63L)).thenReturn(Optional.of(editor));
        when(staffRepository.findAllByIsActiveTrue()).thenReturn(List.of(editor, preferredStaff, otherStaff));
        when(accessControlService.canEditShift(editor, editor)).thenReturn(false);
        when(accessControlService.canEditShift(editor, preferredStaff)).thenReturn(true);
        when(accessControlService.canEditShift(editor, otherStaff)).thenReturn(true);
        when(shiftTypeRepository.findAllActiveWorkShifts()).thenReturn(List.of(workShift));
        when(shiftAssignmentRepository.findByStaffIdInAndWorkDateBetween(any(), any(), any())).thenReturn(List.of());
        when(shiftRequestRepository.findByStaffIdInAndWorkDateBetween(any(), any(), any())).thenReturn(List.of());
        when(systemSettingService.isMonthConfirmed(2026, 8)).thenReturn(false);
        when(systemSettingService.getSystemSettingTextValue("autoShiftGenerationRules")).thenReturn(
            "{\"requiredCounts\":{\"10\":1},\"monthlyMaxWorkdaysMode\":\"FIXED\",\"monthlyMaxWorkdays\":40,\"maxConsecutiveWorkdays\":40,\"minimumRestDays\":0,\"desiredShiftMode\":\"IGNORE\",\"existingShiftHandling\":\"ONLY_EMPTY\"}");

        AutoShiftGenerationResultResponse result = shiftAssignmentService.autoGenerateShiftAssignments(63L, 2026, 8);

        assertTrue(result.getGeneratedCount() > 0);

        ArgumentCaptor<List<ShiftAssignment>> captor = ArgumentCaptor.forClass(List.class);
        verify(shiftAssignmentRepository).saveAll(captor.capture());
        ShiftAssignment firstDayAssignment = captor.getValue().stream()
            .filter(assignment -> LocalDate.of(2026, 8, 1).equals(assignment.getWorkDate()))
            .findFirst()
            .orElseThrow();
        assertEquals(preferredStaff.getId(), firstDayAssignment.getStaff().getId());
        }

    @Test
    void autoGenerateShiftAssignments_respectsNgWeekdayWithStringAndSingularJsonFormats() throws Exception {
        configureObjectMapper();

        Staff editor = new Staff();
        editor.setId(64L);
        editor.setStaffCode("STF-99964");
        editor.setStaffName("管理者");
        editor.setRoleLevel(RoleLevel.MASTER);
        editor.setIsActive(true);

        Staff blockedStaff = createStaff(1L, "STF-00001", "{\"shiftTypeId\":\"10\",\"weekdayIds\":[\"6\"]}");
        Staff otherStaff = createStaff(2L, "STF-00002", null);

        ShiftType workShift = new ShiftType();
        workShift.setId(10L);
        workShift.setShiftCode("A");
        workShift.setShiftName("早番");
        workShift.setStartTime(LocalTime.of(9, 0));
        workShift.setEndTime(LocalTime.of(18, 0));
        workShift.setIsActive(true);

        when(staffRepository.findById(64L)).thenReturn(Optional.of(editor));
        when(staffRepository.findAllByIsActiveTrue()).thenReturn(List.of(editor, blockedStaff, otherStaff));
        when(accessControlService.canEditShift(editor, editor)).thenReturn(false);
        when(accessControlService.canEditShift(editor, blockedStaff)).thenReturn(true);
        when(accessControlService.canEditShift(editor, otherStaff)).thenReturn(true);
        when(shiftTypeRepository.findAllActiveWorkShifts()).thenReturn(List.of(workShift));
        when(shiftAssignmentRepository.findByStaffIdInAndWorkDateBetween(any(), any(), any())).thenReturn(List.of());
        when(shiftRequestRepository.findByStaffIdInAndWorkDateBetween(any(), any(), any())).thenReturn(List.of());
        when(systemSettingService.isMonthConfirmed(2026, 8)).thenReturn(false);
        when(systemSettingService.getSystemSettingTextValue("autoShiftGenerationRules")).thenReturn(
            "{\"requiredCounts\":{\"10\":1},\"monthlyMaxWorkdaysMode\":\"FIXED\",\"monthlyMaxWorkdays\":40,\"maxConsecutiveWorkdays\":40,\"minimumRestDays\":0,\"desiredShiftMode\":\"IGNORE\",\"existingShiftHandling\":\"ONLY_EMPTY\"}");

        AutoShiftGenerationResultResponse result = shiftAssignmentService.autoGenerateShiftAssignments(64L, 2026, 8);
        assertTrue(result.getGeneratedCount() > 0);

        ArgumentCaptor<List<ShiftAssignment>> captor = ArgumentCaptor.forClass(List.class);
        verify(shiftAssignmentRepository).saveAll(captor.capture());

        boolean blockedAssignedOnSaturday = captor.getValue().stream()
            .anyMatch(assignment -> assignment.getStaff().getId().equals(blockedStaff.getId())
                && assignment.getWorkDate().getDayOfWeek().getValue() % 7 == 6);
        assertFalse(blockedAssignedOnSaturday);
    }

    @Test
    void autoGenerateShiftAssignments_respectsNgWeekdayWithWeekDayIdsAlias() throws Exception {
        configureObjectMapper();

        Staff editor = new Staff();
        editor.setId(65L);
        editor.setStaffCode("STF-99965");
        editor.setStaffName("管理者");
        editor.setRoleLevel(RoleLevel.MASTER);
        editor.setIsActive(true);

        Staff blockedStaff = createStaff(1L, "STF-00001", "{\"shiftTypeIds\":[10],\"weekDayIds\":[6]}");
        Staff otherStaff = createStaff(2L, "STF-00002", null);

        ShiftType workShift = new ShiftType();
        workShift.setId(10L);
        workShift.setShiftCode("A");
        workShift.setShiftName("早番");
        workShift.setStartTime(LocalTime.of(9, 0));
        workShift.setEndTime(LocalTime.of(18, 0));
        workShift.setIsActive(true);

        when(staffRepository.findById(65L)).thenReturn(Optional.of(editor));
        when(staffRepository.findAllByIsActiveTrue()).thenReturn(List.of(editor, blockedStaff, otherStaff));
        when(accessControlService.canEditShift(editor, editor)).thenReturn(false);
        when(accessControlService.canEditShift(editor, blockedStaff)).thenReturn(true);
        when(accessControlService.canEditShift(editor, otherStaff)).thenReturn(true);
        when(shiftTypeRepository.findAllActiveWorkShifts()).thenReturn(List.of(workShift));
        when(shiftAssignmentRepository.findByStaffIdInAndWorkDateBetween(any(), any(), any())).thenReturn(List.of());
        when(shiftRequestRepository.findByStaffIdInAndWorkDateBetween(any(), any(), any())).thenReturn(List.of());
        when(systemSettingService.isMonthConfirmed(2026, 8)).thenReturn(false);
        when(systemSettingService.getSystemSettingTextValue("autoShiftGenerationRules")).thenReturn(
            "{\"requiredCounts\":{\"10\":1},\"monthlyMaxWorkdaysMode\":\"FIXED\",\"monthlyMaxWorkdays\":40,\"maxConsecutiveWorkdays\":40,\"minimumRestDays\":0,\"desiredShiftMode\":\"IGNORE\",\"existingShiftHandling\":\"ONLY_EMPTY\"}");

        AutoShiftGenerationResultResponse result = shiftAssignmentService.autoGenerateShiftAssignments(65L, 2026, 8);
        assertTrue(result.getGeneratedCount() > 0);

        ArgumentCaptor<List<ShiftAssignment>> captor = ArgumentCaptor.forClass(List.class);
        verify(shiftAssignmentRepository).saveAll(captor.capture());

        boolean blockedAssignedOnSaturday = captor.getValue().stream()
            .anyMatch(assignment -> assignment.getStaff().getId().equals(blockedStaff.getId())
                && assignment.getWorkDate().getDayOfWeek().getValue() % 7 == 6);
        assertFalse(blockedAssignedOnSaturday);
    }

    @Test
    void autoGenerateShiftAssignments_reportsUnmetConditionsAfterRetries() throws Exception {
        Staff editor = new Staff();
        editor.setId(60L);
        editor.setStaffCode("STF-99960");
        editor.setStaffName("管理者");
        editor.setRoleLevel(RoleLevel.MASTER);
        editor.setIsActive(true);

        Staff blockedStaff = createStaff(1L, "STF-00001", "{\"shiftTypeIds\":[10]}");

        ShiftType workShift = new ShiftType();
        workShift.setId(10L);
        workShift.setShiftCode("A");
        workShift.setShiftName("早番");
        workShift.setStartTime(LocalTime.of(9, 0));
        workShift.setEndTime(LocalTime.of(18, 0));
        workShift.setIsActive(true);

        when(staffRepository.findById(60L)).thenReturn(Optional.of(editor));
        when(staffRepository.findAllByIsActiveTrue()).thenReturn(List.of(editor, blockedStaff));
        when(accessControlService.canEditShift(editor, editor)).thenReturn(false);
        when(accessControlService.canEditShift(editor, blockedStaff)).thenReturn(true);
        when(shiftTypeRepository.findAllActiveWorkShifts()).thenReturn(List.of(workShift));
        when(shiftAssignmentRepository.findByStaffIdInAndWorkDateBetween(any(), any(), any())).thenReturn(List.of());
        when(shiftRequestRepository.findByStaffIdInAndWorkDateBetween(any(), any(), any())).thenReturn(List.of());
        when(systemSettingService.isMonthConfirmed(2026, 8)).thenReturn(false);
        when(systemSettingService.getSystemSettingTextValue("autoShiftGenerationRules")).thenReturn(
                "{\"requiredCounts\":{\"10\":1},\"monthlyMaxWorkdaysMode\":\"FIXED\",\"monthlyMaxWorkdays\":40,\"maxConsecutiveWorkdays\":40,\"minimumRestDays\":0,\"desiredShiftMode\":\"IGNORE\",\"existingShiftHandling\":\"ONLY_EMPTY\"}");

        AutoShiftGenerationResultResponse result = shiftAssignmentService.autoGenerateShiftAssignments(60L, 2026, 8);

        assertEquals(0, result.getGeneratedCount());
        assertTrue(result.getRetryCount() >= 0);
        assertTrue(!result.getUnmetConditions().isEmpty());
        assertEquals("早番", result.getUnmetConditions().get(0).getShiftTypeName());
    }

    @Test
    void updateShiftAssignment_rejectsConfirmedMonthForNonAdmin() {
        Staff editor = new Staff();
        editor.setId(80L);
        editor.setStaffCode("STF-99980");
        editor.setStaffName("チーフ");
        editor.setRoleLevel(RoleLevel.MEMBER);
        editor.setIsActive(true);

        Staff targetStaff = new Staff();
        targetStaff.setId(81L);
        targetStaff.setStaffCode("STF-00081");
        targetStaff.setStaffName("メンバー");
        targetStaff.setRoleLevel(RoleLevel.MEMBER);
        targetStaff.setIsActive(true);

        ShiftAssignment assignment = new ShiftAssignment();
        assignment.setId(200L);
        assignment.setStaff(targetStaff);
        assignment.setWorkDate(LocalDate.of(2026, 8, 10));
        ShiftType currentShift = new ShiftType();
        currentShift.setId(11L);
        currentShift.setShiftCode("A");
        currentShift.setShiftName("現行");
        assignment.setShiftType(currentShift);

        when(staffRepository.findById(80L)).thenReturn(Optional.of(editor));
        when(staffRepository.findById(81L)).thenReturn(Optional.of(targetStaff));
        when(shiftAssignmentRepository.findById(200L)).thenReturn(Optional.of(assignment));
        when(accessControlService.canEditShift(editor, targetStaff)).thenReturn(true);
        when(accessControlService.isMaster(editor)).thenReturn(false);
        when(accessControlService.isChief(editor)).thenReturn(false);
        when(systemSettingService.isMonthConfirmed(2026, 8)).thenReturn(true);
        when(shiftAssignmentRepository.save(any(ShiftAssignment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> shiftAssignmentService.updateShiftAssignment(80L, 200L, new com.shiftscheduler.server.api.ShiftAssignmentUpdateRequest()));

        assertTrue(exception.getMessage().contains("管理者"));
    }

    @Test
    void updateShiftAssignment_allowsConfirmedMonthForAdmin() {
        Staff editor = new Staff();
        editor.setId(82L);
        editor.setStaffCode("STF-99982");
        editor.setStaffName("マスター");
        editor.setRoleLevel(RoleLevel.MASTER);
        editor.setIsActive(true);

        Staff targetStaff = new Staff();
        targetStaff.setId(83L);
        targetStaff.setStaffCode("STF-00083");
        targetStaff.setStaffName("メンバー");
        targetStaff.setRoleLevel(RoleLevel.MEMBER);
        targetStaff.setIsActive(true);

        ShiftAssignment assignment = new ShiftAssignment();
        assignment.setId(201L);
        assignment.setStaff(targetStaff);
        assignment.setWorkDate(LocalDate.of(2026, 8, 10));
        ShiftType currentShift = new ShiftType();
        currentShift.setId(12L);
        currentShift.setShiftCode("A");
        currentShift.setShiftName("現行");
        assignment.setShiftType(currentShift);

        when(staffRepository.findById(82L)).thenReturn(Optional.of(editor));
        when(staffRepository.findById(83L)).thenReturn(Optional.of(targetStaff));
        when(shiftAssignmentRepository.findById(201L)).thenReturn(Optional.of(assignment));
        when(accessControlService.canEditShift(editor, targetStaff)).thenReturn(true);
        when(accessControlService.isMaster(editor)).thenReturn(true);
        when(accessControlService.isChief(editor)).thenReturn(false);
        when(systemSettingService.isMonthConfirmed(2026, 8)).thenReturn(true);
        ShiftType replacementShift = new ShiftType();
        replacementShift.setId(13L);
        replacementShift.setShiftName("置換");
        when(shiftTypeRepository.findById(13L)).thenReturn(Optional.of(replacementShift));
        when(shiftAssignmentRepository.save(any(ShiftAssignment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var request = new com.shiftscheduler.server.api.ShiftAssignmentUpdateRequest();
        request.setShiftTypeId(13L);
        shiftAssignmentService.updateShiftAssignment(82L, 201L, request);

        ArgumentCaptor<ShiftAssignment> captor = ArgumentCaptor.forClass(ShiftAssignment.class);
        verify(shiftAssignmentRepository).save(captor.capture());
        assertEquals(13L, captor.getValue().getShiftType().getId());
    }

    @Test
    void autoGenerateShiftAssignments_fillsDailyRequiredCountEvenWhenRequestsConflict() throws Exception {
        Staff editor = new Staff();
        editor.setId(70L);
        editor.setStaffCode("STF-99970");
        editor.setStaffName("管理者");
        editor.setRoleLevel(RoleLevel.MASTER);
        editor.setIsActive(true);

        Staff staffA = createStaff(1L, "STF-00001", null);
        Staff staffB = createStaff(2L, "STF-00002", null);

        ShiftType workShift = new ShiftType();
        workShift.setId(10L);
        workShift.setShiftCode("A");
        workShift.setShiftName("早番");
        workShift.setStartTime(LocalTime.of(9, 0));
        workShift.setEndTime(LocalTime.of(18, 0));
        workShift.setIsActive(true);

        ShiftType otherShift = new ShiftType();
        otherShift.setId(20L);
        otherShift.setShiftCode("B");
        otherShift.setShiftName("遅番");
        otherShift.setStartTime(LocalTime.of(13, 0));
        otherShift.setEndTime(LocalTime.of(22, 0));
        otherShift.setIsActive(true);

        ShiftRequest conflictingRequest = new ShiftRequest();
        conflictingRequest.setId(100L);
        conflictingRequest.setStaff(staffA);
        conflictingRequest.setWorkDate(LocalDate.of(2026, 8, 1));
        conflictingRequest.setStatus(ShiftRequestStatus.SUBMITTED);
        conflictingRequest.setDesiredShiftType(otherShift);

        when(staffRepository.findById(70L)).thenReturn(Optional.of(editor));
        when(staffRepository.findAllByIsActiveTrue()).thenReturn(List.of(editor, staffA, staffB));
        when(accessControlService.canEditShift(editor, editor)).thenReturn(false);
        when(accessControlService.canEditShift(editor, staffA)).thenReturn(true);
        when(accessControlService.canEditShift(editor, staffB)).thenReturn(true);
        when(shiftTypeRepository.findAllActiveWorkShifts()).thenReturn(List.of(workShift, otherShift));
        when(shiftAssignmentRepository.findByStaffIdInAndWorkDateBetween(any(), any(), any())).thenReturn(List.of());
        when(shiftRequestRepository.findByStaffIdInAndWorkDateBetween(any(), any(), any())).thenReturn(List.of(conflictingRequest));
        when(systemSettingService.isMonthConfirmed(2026, 8)).thenReturn(false);
        when(systemSettingService.getSystemSettingTextValue("autoShiftGenerationRules")).thenReturn(
                "{\"requiredCounts\":{\"10\":1},\"monthlyMaxWorkdaysMode\":\"FIXED\",\"monthlyMaxWorkdays\":40,\"maxConsecutiveWorkdays\":40,\"minimumRestDays\":0,\"desiredShiftMode\":\"REQUIRED\",\"existingShiftHandling\":\"ONLY_EMPTY\"}");

        AutoShiftGenerationResultResponse result = shiftAssignmentService.autoGenerateShiftAssignments(70L, 2026, 8);

        assertEquals(32, result.getGeneratedCount());
        assertTrue(result.getUnmetConditions().isEmpty());
    }

    @Test
    void calculateCandidateWeight_prefersStaffWithFewerMonthlyWorkdays() throws Exception {
        ShiftAssignmentService.AutoShiftGenerationRules rules = new ShiftAssignmentService.AutoShiftGenerationRules();
        rules.monthlyMaxWorkdaysMode = "FIXED";
        rules.monthlyMaxWorkdays = 40;

        Object lowerCountCandidate = createSelectionScore(0, 0, 0, LocalDate.of(2026, 8, 1));
        Object higherCountCandidate = createSelectionScore(2, 0, 0, LocalDate.of(2026, 8, 1));

        Method method = ShiftAssignmentService.class.getDeclaredMethod(
                "calculateCandidateWeight",
                Class.forName("com.shiftscheduler.server.service.ShiftAssignmentService$StaffSelectionScore"),
                LocalDate.class,
                ShiftAssignmentService.AutoShiftGenerationRules.class);
        method.setAccessible(true);

        int lowerCountWeight = (int) method.invoke(shiftAssignmentService, lowerCountCandidate, LocalDate.of(2026, 8, 2), rules);
        int higherCountWeight = (int) method.invoke(shiftAssignmentService, higherCountCandidate, LocalDate.of(2026, 8, 2), rules);

        assertTrue(lowerCountWeight > higherCountWeight);
    }

    @Test
    void autoGenerateShiftAssignments_allowsDailyRequiredTotalEqualToStaffCount() throws Exception {
        Staff editor = new Staff();
        editor.setId(70L);
        editor.setStaffCode("STF-99970");
        editor.setStaffName("管理者");
        editor.setRoleLevel(RoleLevel.MASTER);
        editor.setIsActive(true);

        Staff staffA = createStaff(1L, "STF-00001", null);
        Staff staffB = createStaff(2L, "STF-00002", null);

        ShiftType workShift = new ShiftType();
        workShift.setId(10L);
        workShift.setShiftCode("A");
        workShift.setShiftName("早番");
        workShift.setStartTime(LocalTime.of(9, 0));
        workShift.setEndTime(LocalTime.of(18, 0));
        workShift.setIsActive(true);

        when(staffRepository.findById(70L)).thenReturn(Optional.of(editor));
        when(staffRepository.findAllByIsActiveTrue()).thenReturn(List.of(editor, staffA, staffB));
        when(accessControlService.canEditShift(editor, editor)).thenReturn(false);
        when(accessControlService.canEditShift(editor, staffA)).thenReturn(true);
        when(accessControlService.canEditShift(editor, staffB)).thenReturn(true);
        when(shiftTypeRepository.findAllActiveWorkShifts()).thenReturn(List.of(workShift));
        when(shiftAssignmentRepository.findByStaffIdInAndWorkDateBetween(any(), any(), any())).thenReturn(List.of());
        when(shiftRequestRepository.findByStaffIdInAndWorkDateBetween(any(), any(), any())).thenReturn(List.of());
        when(systemSettingService.isMonthConfirmed(2026, 8)).thenReturn(false);
        when(systemSettingService.getSystemSettingTextValue("autoShiftGenerationRules")).thenReturn(
                "{\"requiredCounts\":{\"10\":2},\"monthlyMaxWorkdaysMode\":\"FIXED\",\"monthlyMaxWorkdays\":40,\"maxConsecutiveWorkdays\":40,\"minimumRestDays\":0,\"desiredShiftMode\":\"IGNORE\",\"existingShiftHandling\":\"ONLY_EMPTY\"}");

        AutoShiftGenerationResultResponse result = shiftAssignmentService.autoGenerateShiftAssignments(70L, 2026, 8);

        assertEquals(62, result.getGeneratedCount());
        assertEquals(0, result.getUnassignedRequiredCount());
    }

    private Object createSelectionScore(int monthlyWorkCount, int recentWorkCount, int consecutiveDays, LocalDate lastWorkedDate) throws Exception {
        Class<?> scoreClass = Class.forName("com.shiftscheduler.server.service.ShiftAssignmentService$StaffSelectionScore");
        Constructor<?> constructor = scoreClass.getDeclaredConstructor(
                Staff.class,
                boolean.class,
                boolean.class,
                int.class,
                int.class,
                int.class,
                LocalDate.class);
        constructor.setAccessible(true);
        return constructor.newInstance(new Staff(), false, false, monthlyWorkCount, recentWorkCount, consecutiveDays, lastWorkedDate);
    }

    private Staff createStaff(Long id, String staffCode, String ngShiftTypeIds) {
        Staff staff = new Staff();
        staff.setId(id);
        staff.setStaffCode(staffCode);
        staff.setStaffName(staffCode);
        staff.setRoleLevel(RoleLevel.MEMBER);
        staff.setIsActive(true);
        staff.setNgShiftTypeIds(ngShiftTypeIds);
        return staff;
    }

    private ShiftAssignment existingAssignment(Long id, Staff staff, LocalDate workDate, ShiftType shiftType, Staff updatedBy) {
        ShiftAssignment assignment = new ShiftAssignment();
        assignment.setId(id);
        assignment.setStaff(staff);
        assignment.setWorkDate(workDate);
        assignment.setShiftType(shiftType);
        assignment.setUpdatedBy(updatedBy);
        return assignment;
    }
}
