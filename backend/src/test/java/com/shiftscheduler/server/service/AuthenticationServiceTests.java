package com.shiftscheduler.server.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.shiftscheduler.server.domain.RoleLevel;
import com.shiftscheduler.server.domain.Staff;
import com.shiftscheduler.server.dto.LoginRequest;
import com.shiftscheduler.server.dto.LoginResponse;
import com.shiftscheduler.server.repository.StaffRepository;
import com.shiftscheduler.server.util.PasswordUtil;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTests {

    @Mock
    private StaffRepository staffRepository;

    @InjectMocks
    private AuthenticationService authenticationService;

    @Test
    void login_returnsTokenWhenCredentialsAreValid() {
        Staff staff = new Staff();
        staff.setId(1L);
        staff.setStaffCode("STF-00001");
        staff.setStaffName("山田太郎");
        staff.setRoleLevel(RoleLevel.MASTER);
        staff.setIsActive(true);
        staff.setPasswordHash(PasswordUtil.hashPassword("password123"));

        when(staffRepository.findByStaffCode("STF-00001")).thenReturn(Optional.of(staff));

        LoginResponse response = authenticationService.login(new LoginRequest("STF-00001", "password123"));

        assertThat(response.staffId()).isEqualTo(1L);
        assertThat(response.staffCode()).isEqualTo("STF-00001");
        assertThat(response.staffName()).isEqualTo("山田太郎");
        assertThat(response.roleLevel()).isEqualTo("MASTER");
        assertThat(response.token()).isNotBlank();
    }

    @Test
    void login_rejectsInactiveStaff() {
        Staff staff = new Staff();
        staff.setId(1L);
        staff.setStaffCode("STF-00001");
        staff.setStaffName("山田太郎");
        staff.setRoleLevel(RoleLevel.MASTER);
        staff.setIsActive(false);
        staff.setPasswordHash(PasswordUtil.hashPassword("password123"));

        when(staffRepository.findByStaffCode("STF-00001")).thenReturn(Optional.of(staff));

        assertThatThrownBy(() -> authenticationService.login(new LoginRequest("STF-00001", "password123")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("無効");
    }

    @Test
    void login_rejectsInvalidPassword() {
        Staff staff = new Staff();
        staff.setId(1L);
        staff.setStaffCode("STF-00001");
        staff.setStaffName("山田太郎");
        staff.setRoleLevel(RoleLevel.MASTER);
        staff.setIsActive(true);
        staff.setPasswordHash(PasswordUtil.hashPassword("password123"));

        when(staffRepository.findByStaffCode(anyString())).thenReturn(Optional.of(staff));

        assertThatThrownBy(() -> authenticationService.login(new LoginRequest("STF-00001", "wrong-password")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("正しくありません");
    }
}
