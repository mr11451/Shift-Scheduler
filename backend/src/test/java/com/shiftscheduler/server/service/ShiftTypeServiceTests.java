package com.shiftscheduler.server.service;

import java.time.LocalTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.shiftscheduler.server.api.ShiftTypeCreateRequest;
import com.shiftscheduler.server.api.ShiftTypeResponse;
import com.shiftscheduler.server.api.ShiftTypeUpdateRequest;
import com.shiftscheduler.server.domain.RoleLevel;
import com.shiftscheduler.server.domain.ShiftType;
import com.shiftscheduler.server.domain.Staff;
import com.shiftscheduler.server.repository.ShiftTypeRepository;
import com.shiftscheduler.server.repository.StaffRepository;

@ExtendWith(MockitoExtension.class)
class ShiftTypeServiceTests {

    @Mock
    private ShiftTypeRepository shiftTypeRepository;

    @Mock
    private SystemSettingService systemSettingService;

    @Mock
    private StaffRepository staffRepository;

    @Spy
    private AccessControlService accessControlService = new AccessControlService();

    @InjectMocks
    private ShiftTypeService shiftTypeService;

    private Staff masterStaff() {
        Staff staff = new Staff();
        staff.setId(1L);
        staff.setStaffName("マスター");
        staff.setRoleLevel(RoleLevel.MASTER);
        return staff;
    }

    @Test
    void createShiftType_savesNewShiftTypeAndReturnsResponse() {
        Staff creator = masterStaff();
        ShiftTypeCreateRequest request = new ShiftTypeCreateRequest(
                "A",
                "早番",
                LocalTime.of(9, 0),
                LocalTime.of(18, 0),
                false,
                2
        );
        when(staffRepository.findById(1L)).thenReturn(Optional.of(creator));
        when(shiftTypeRepository.findByShiftCode("A")).thenReturn(Optional.empty());
        when(shiftTypeRepository.save(any(ShiftType.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ShiftTypeResponse response = shiftTypeService.createShiftType(1L, request);

        ArgumentCaptor<ShiftType> captor = ArgumentCaptor.forClass(ShiftType.class);
        verify(shiftTypeRepository).save(captor.capture());
        ShiftType saved = captor.getValue();

        assertThat(saved.getShiftCode()).isEqualTo("A");
        assertThat(saved.getShiftName()).isEqualTo("早番");
        assertThat(saved.getStartTime()).isEqualTo(LocalTime.of(9, 0));
        assertThat(saved.getEndTime()).isEqualTo(LocalTime.of(18, 0));
        assertThat(saved.getIsOffType()).isFalse();
        assertThat(saved.getIsActive()).isTrue();
        assertThat(saved.getSortOrder()).isEqualTo(2);
        assertThat(saved.getCreatedBy()).isEqualTo(creator);
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();

        assertThat(response.getShiftCode()).isEqualTo("A");
        assertThat(response.getShiftName()).isEqualTo("早番");
        assertThat(response.getSortOrder()).isEqualTo(2);
        assertThat(response.getIsActive()).isTrue();
        assertThat(response.getCreatedByStaffId()).isEqualTo(1L);
    }

    @Test
    void createShiftType_throwsWhenShiftCodeAlreadyExists() {
        when(staffRepository.findById(1L)).thenReturn(Optional.of(masterStaff()));
        ShiftTypeCreateRequest request = new ShiftTypeCreateRequest(
                "A",
                "早番",
                null,
                null,
                null,
                null
        );
        when(shiftTypeRepository.findByShiftCode("A")).thenReturn(Optional.of(new ShiftType()));

        assertThatThrownBy(() -> shiftTypeService.createShiftType(1L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("既に使用されています");
    }

    @Test
    void updateShiftType_updatesEditableFields() {
        ShiftType existing = new ShiftType();
        existing.setId(1L);
        existing.setShiftCode("A");
        existing.setShiftName("早番");
        existing.setStartTime(LocalTime.of(9, 0));
        existing.setEndTime(LocalTime.of(18, 0));
        existing.setIsOffType(false);
        existing.setIsActive(true);
        existing.setSortOrder(1);

        ShiftTypeUpdateRequest request = new ShiftTypeUpdateRequest(
                "B",
                "遅番",
                LocalTime.of(10, 0),
                LocalTime.of(19, 0),
                true,
                3,
                false
        );
        when(staffRepository.findById(1L)).thenReturn(Optional.of(masterStaff()));
        when(shiftTypeRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(shiftTypeRepository.findByShiftCode("B")).thenReturn(Optional.empty());
        when(shiftTypeRepository.save(any(ShiftType.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ShiftTypeResponse response = shiftTypeService.updateShiftType(1L, request, 1L);

        assertThat(response.getShiftCode()).isEqualTo("B");
        assertThat(response.getShiftName()).isEqualTo("遅番");
        assertThat(response.getStartTime()).isEqualTo(LocalTime.of(10, 0));
        assertThat(response.getEndTime()).isEqualTo(LocalTime.of(19, 0));
        assertThat(response.getIsOffType()).isTrue();
        assertThat(response.getSortOrder()).isEqualTo(3);
        assertThat(response.getIsActive()).isFalse();
        assertThat(existing.getUpdatedAt()).isNotNull();

        verify(shiftTypeRepository).save(existing);
    }

    @Test
    void updateShiftType_throwsWhenChiefDidNotCreateIt() {
        Staff otherChief = new Staff();
        otherChief.setId(2L);
        otherChief.setRoleLevel(RoleLevel.CHIEF);

        Staff creator = new Staff();
        creator.setId(3L);
        creator.setRoleLevel(RoleLevel.CHIEF);

        ShiftType existing = new ShiftType();
        existing.setId(1L);
        existing.setShiftCode("A");
        existing.setShiftName("早番");
        existing.setCreatedBy(creator);

        when(staffRepository.findById(2L)).thenReturn(Optional.of(otherChief));
        when(shiftTypeRepository.findById(1L)).thenReturn(Optional.of(existing));

        ShiftTypeUpdateRequest request = new ShiftTypeUpdateRequest("B", "遅番", null, null, null, null, null);

        assertThatThrownBy(() -> shiftTypeService.updateShiftType(1L, request, 2L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("権限");
    }

    @Test
    void deactivateAndReactivateShiftType_toggleActiveFlag() {
        ShiftType existing = new ShiftType();
        existing.setId(1L);
        existing.setShiftCode("A");
        existing.setShiftName("早番");
        existing.setIsActive(true);
        existing.setSortOrder(1);

        when(staffRepository.findById(1L)).thenReturn(Optional.of(masterStaff()));
        when(shiftTypeRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(shiftTypeRepository.save(any(ShiftType.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ShiftTypeResponse deactivated = shiftTypeService.deactivateShiftType(1L, 1L);
        ShiftTypeResponse reactivated = shiftTypeService.reactivateShiftType(1L, 1L);

        assertThat(deactivated.getIsActive()).isFalse();
        assertThat(reactivated.getIsActive()).isTrue();
        verify(systemSettingService).resetAutoShiftRequiredCount(1L);
        verify(shiftTypeRepository, org.mockito.Mockito.times(2)).save(existing);
    }

    @Test
    void shiftCodeExists_returnsRepositoryPresence() {
        when(shiftTypeRepository.findByShiftCode("A")).thenReturn(Optional.of(new ShiftType()));

        assertThat(shiftTypeService.shiftCodeExists("A")).isTrue();
    }
}
