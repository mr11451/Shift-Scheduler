package com.shiftscheduler.server.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.shiftscheduler.server.api.ShiftTypeCreateRequest;
import com.shiftscheduler.server.api.ShiftTypeResponse;
import com.shiftscheduler.server.api.ShiftTypeUpdateRequest;
import com.shiftscheduler.server.domain.ShiftType;
import com.shiftscheduler.server.repository.ShiftTypeRepository;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ShiftTypeServiceTests {

    @Mock
    private ShiftTypeRepository shiftTypeRepository;

    @Mock
    private SystemSettingService systemSettingService;

    @InjectMocks
    private ShiftTypeService shiftTypeService;

    @Test
    void createShiftType_savesNewShiftTypeAndReturnsResponse() {
        ShiftTypeCreateRequest request = new ShiftTypeCreateRequest(
                "A",
                "早番",
                LocalTime.of(9, 0),
                LocalTime.of(18, 0),
                false,
                2
        );
        when(shiftTypeRepository.findByShiftCode("A")).thenReturn(Optional.empty());
        when(shiftTypeRepository.save(any(ShiftType.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ShiftTypeResponse response = shiftTypeService.createShiftType(request);

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
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();

        assertThat(response.getShiftCode()).isEqualTo("A");
        assertThat(response.getShiftName()).isEqualTo("早番");
        assertThat(response.getSortOrder()).isEqualTo(2);
        assertThat(response.getIsActive()).isTrue();
    }

    @Test
    void createShiftType_throwsWhenShiftCodeAlreadyExists() {
        ShiftTypeCreateRequest request = new ShiftTypeCreateRequest(
                "A",
                "早番",
                null,
                null,
                null,
                null
        );
        when(shiftTypeRepository.findByShiftCode("A")).thenReturn(Optional.of(new ShiftType()));

        assertThatThrownBy(() -> shiftTypeService.createShiftType(request))
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
        when(shiftTypeRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(shiftTypeRepository.findByShiftCode("B")).thenReturn(Optional.empty());
        when(shiftTypeRepository.save(any(ShiftType.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ShiftTypeResponse response = shiftTypeService.updateShiftType(1L, request);

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
    void deactivateAndReactivateShiftType_toggleActiveFlag() {
        ShiftType existing = new ShiftType();
        existing.setId(1L);
        existing.setShiftCode("A");
        existing.setShiftName("早番");
        existing.setIsActive(true);
        existing.setSortOrder(1);

        when(shiftTypeRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(shiftTypeRepository.save(any(ShiftType.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ShiftTypeResponse deactivated = shiftTypeService.deactivateShiftType(1L);
        ShiftTypeResponse reactivated = shiftTypeService.reactivateShiftType(1L);

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
