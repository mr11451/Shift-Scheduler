package com.shiftscheduler.server;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.shiftscheduler.server.domain.Staff;
import com.shiftscheduler.server.repository.StaffRepository;

@ExtendWith(MockitoExtension.class)
class InitializeTestPasswordsRunnerTests {

    @Mock
    private StaffRepository staffRepository;

    @InjectMocks
    private InitializeTestPasswordsRunner runner;

    @Test
    void run_clearsExistingLoginSessions() throws Exception {
        Staff loggedInStaff = new Staff();
        loggedInStaff.setLoginSessionId("active-session-id");
        loggedInStaff.setPasswordHash("existing-password-hash");

        when(staffRepository.findAll()).thenReturn(List.of(loggedInStaff));

        runner.run();

        verify(staffRepository).save(loggedInStaff);
    }

    @Test
    void run_doesNotSaveStaffWithoutSessionOrMissingPassword() throws Exception {
        Staff loggedOutStaff = new Staff();
        loggedOutStaff.setPasswordHash("existing-password-hash");

        when(staffRepository.findAll()).thenReturn(List.of(loggedOutStaff));

        runner.run();

        verify(staffRepository, never()).save(loggedOutStaff);
    }
}