package com.shiftscheduler.server.service;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shiftscheduler.server.api.SystemSettingResponse;
import com.shiftscheduler.server.domain.RoleLevel;
import com.shiftscheduler.server.domain.Staff;
import com.shiftscheduler.server.domain.SystemSetting;
import com.shiftscheduler.server.repository.StaffRepository;
import com.shiftscheduler.server.repository.SystemSettingRepository;

@ExtendWith(MockitoExtension.class)
class SystemSettingServiceTests {

    @Mock
    private SystemSettingRepository systemSettingRepository;

    @Mock
    private StaffRepository staffRepository;

    @Mock
    private AccessControlService accessControlService;

    @Mock
    private ObjectMapper objectMapper;

    private SystemSettingService systemSettingService;

    private void initializeService() {
        systemSettingService = new SystemSettingService();
        ReflectionTestUtils.setField(systemSettingService, "systemSettingRepository", systemSettingRepository);
        ReflectionTestUtils.setField(systemSettingService, "staffRepository", staffRepository);
        ReflectionTestUtils.setField(systemSettingService, "accessControlService", accessControlService);
        ReflectionTestUtils.setField(systemSettingService, "objectMapper", objectMapper);
    }

    @Test
    void updateSystemSettingText_savesHolidayWeekdaysAndCanBeLoadedAgain() {
        initializeService();

        Staff updater = new Staff();
        updater.setId(1L);
        updater.setStaffName("管理者");
        updater.setRoleLevel(RoleLevel.MASTER);

        when(staffRepository.findById(1L)).thenReturn(Optional.of(updater));
        when(accessControlService.isMaster(updater)).thenReturn(true);
        when(systemSettingRepository.findBySettingKey("holidayWeekdays"))
                .thenReturn(Optional.empty(), Optional.of(createTextSetting("holidayWeekdays", "0,6", updater)));
        when(systemSettingRepository.save(any(SystemSetting.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SystemSettingResponse response = systemSettingService.updateSystemSettingText(1L, "holidayWeekdays", "0,6");
        String loadedValue = systemSettingService.getSystemSettingTextValue("holidayWeekdays");

        assertEquals("holidayWeekdays", response.getSettingKey());
        assertEquals("0,6", response.getSettingValueText());
        assertEquals("0,6", loadedValue);
        verify(systemSettingRepository).save(any(SystemSetting.class));
    }

    @Test
    void updateSystemSettingText_rejectsNonMasterUpdater() {
        initializeService();

        Staff updater = new Staff();
        updater.setId(2L);
        updater.setRoleLevel(RoleLevel.CHIEF);

        when(staffRepository.findById(2L)).thenReturn(Optional.of(updater));
        when(accessControlService.isMaster(updater)).thenReturn(false);

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> systemSettingService.updateSystemSettingText(2L, "holidayWeekdays", "0,6")
        );

        assertTrue(error.getMessage().contains("マスターのみシステム設定を更新できます。"));
    }

    @Test
    void getShiftPeriod_usesConfiguredClosingDayAcrossMonths() {
        initializeService();
        when(systemSettingRepository.findBySettingKey("closingDay"))
                .thenReturn(Optional.of(createTextSetting("closingDay", "25", null)));

        SystemSettingService.ShiftPeriod period = systemSettingService.getShiftPeriod(LocalDate.of(2026, 9, 2));

        assertEquals(LocalDate.of(2026, 8, 26), period.startDate());
        assertEquals(LocalDate.of(2026, 9, 25), period.endDate());
        assertEquals("2026-09-25", period.key());
    }

    @Test
    void updateSystemSettingText_rejectsClosingDayOutsideTheValidRange() {
        initializeService();
        Staff updater = new Staff();
        updater.setId(1L);
        when(staffRepository.findById(1L)).thenReturn(Optional.of(updater));

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> systemSettingService.updateSystemSettingText(1L, "closingDay", "32")
        );

        assertTrue(error.getMessage().contains("締め日は1〜31の範囲"));
    }

    private SystemSetting createTextSetting(String settingKey, String value, Staff updater) {
        SystemSetting setting = new SystemSetting();
        setting.setSettingKey(settingKey);
        setting.setSettingValueText(value);
        setting.setUpdatedBy(updater);
        return setting;
    }
}