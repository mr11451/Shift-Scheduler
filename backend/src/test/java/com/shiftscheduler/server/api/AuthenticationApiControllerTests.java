package com.shiftscheduler.server.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shiftscheduler.server.dto.LoginRequest;
import com.shiftscheduler.server.dto.LoginResponse;
import com.shiftscheduler.server.repository.StaffRepository;
import com.shiftscheduler.server.service.AuthenticationService;
import com.shiftscheduler.server.service.ActiveLoginException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@WebMvcTest(AuthenticationApiController.class)
class AuthenticationApiControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthenticationService authenticationService;

    @MockBean
    private StaffRepository staffRepository;

    @Test
    void login_returnsOkWhenCredentialsAreValid() throws Exception {
        LoginResponse response = new LoginResponse(1L, "STF-00001", "山田太郎", "MASTER", "token-value");
        when(authenticationService.login(any(LoginRequest.class))).thenReturn(response);

        LoginRequest request = new LoginRequest("STF-00001", "password123");

        mockMvc.perform(post("/api/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.staffCode").value("STF-00001"))
                .andExpect(jsonPath("$.staffName").value("山田太郎"))
                .andExpect(jsonPath("$.roleLevel").value("MASTER"))
                .andExpect(jsonPath("$.token").value("token-value"));
    }

    @Test
    void login_returnsUnauthorizedWhenServiceRejectsCredentials() throws Exception {
        when(authenticationService.login(any(LoginRequest.class)))
                .thenThrow(new IllegalArgumentException("スタッフコードまたはパスワードが正しくありません。"));

        LoginRequest request = new LoginRequest("STF-00001", "wrong-password");

        mockMvc.perform(post("/api/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_returnsConflictWhenStaffAlreadyHasActiveSession() throws Exception {
        when(authenticationService.login(any(LoginRequest.class))).thenThrow(new ActiveLoginException());

        LoginRequest request = new LoginRequest("STF-00001", "password123");

        mockMvc.perform(post("/api/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }
}
