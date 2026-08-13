package com.shiftscheduler.server.service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.anyString;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.shiftscheduler.server.domain.PasswordResetToken;
import com.shiftscheduler.server.domain.RoleLevel;
import com.shiftscheduler.server.domain.Staff;
import com.shiftscheduler.server.dto.LoginRequest;
import com.shiftscheduler.server.dto.LoginResponse;
import com.shiftscheduler.server.dto.PasswordResetRequestResponse;
import com.shiftscheduler.server.repository.PasswordResetTokenRepository;
import com.shiftscheduler.server.repository.StaffRepository;
import com.shiftscheduler.server.util.PasswordUtil;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTests {

    @Mock
    private StaffRepository staffRepository;

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Mock
    private PasswordResetEmailService passwordResetEmailService;

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

    @Test
    void requestPasswordReset_issuesOneHourTokenAndSendsEmail() {
        Staff staff = new Staff();
        staff.setId(1L);
        staff.setStaffName("山田太郎");
        staff.setEmail("taro@example.com");
        staff.setIsActive(true);
        when(staffRepository.findById(1L)).thenReturn(Optional.of(staff));
        when(passwordResetTokenRepository.findByStaffIdAndUsedAtIsNull(1L)).thenReturn(List.of());
        when(passwordResetEmailService.isAvailable()).thenReturn(true);
        ReflectionTestUtils.setField(authenticationService, "passwordResetBaseUrl", "https://example.com/password-reset");

        OffsetDateTime before = OffsetDateTime.now();
        authenticationService.requestPasswordReset(1L);
        OffsetDateTime after = OffsetDateTime.now();

        ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(passwordResetTokenRepository).save(tokenCaptor.capture());
        PasswordResetToken token = tokenCaptor.getValue();
        assertThat(token.getStaff()).isSameAs(staff);
        assertThat(token.getTokenHash()).isNotBlank();
        assertThat(token.getVerificationCodeHash()).isNotBlank();
        assertThat(token.getExpiresAt()).isBetween(before.plusMinutes(59), after.plusHours(1));
        verify(passwordResetEmailService).send(
            anyString(),
            anyString(),
            org.mockito.ArgumentMatchers.startsWith("https://example.com/password-reset/1/"),
            org.mockito.ArgumentMatchers.matches("\\d{6}")
        );
    }

    @Test
    void requestPasswordReset_returnsCredentialsWhenEmailIsUnavailable() {
        Staff staff = new Staff();
        staff.setId(1L);
        staff.setStaffName("山田太郎");
        staff.setEmail("taro@example.com");
        staff.setIsActive(true);
        when(staffRepository.findById(1L)).thenReturn(Optional.of(staff));
        when(passwordResetTokenRepository.findByStaffIdAndUsedAtIsNull(1L)).thenReturn(List.of());
        when(passwordResetEmailService.isAvailable()).thenReturn(false);
        ReflectionTestUtils.setField(authenticationService, "passwordResetBaseUrl", "https://example.com/password-reset");

        PasswordResetRequestResponse response = authenticationService.requestPasswordReset(1L);

        assertThat(response.emailSent()).isFalse();
        assertThat(response.accessUrl()).startsWith("https://example.com/password-reset/1/");
        assertThat(response.verificationCode()).matches("\\d{6}");
        verify(passwordResetTokenRepository).save(org.mockito.ArgumentMatchers.any(PasswordResetToken.class));
    }
}
