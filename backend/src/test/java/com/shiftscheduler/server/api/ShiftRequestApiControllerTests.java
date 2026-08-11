package com.shiftscheduler.server.api;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shiftscheduler.server.domain.ShiftRequestStatus;
import com.shiftscheduler.server.service.ShiftRequestService;
import com.shiftscheduler.server.util.JwtTokenUtil;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ShiftRequestApiController.class)
@AutoConfigureMockMvc(addFilters = false)
class ShiftRequestApiControllerTests {

    private static final String AUTH_HEADER = "Bearer " + JwtTokenUtil.generateToken(10L, "STF-00010", "MASTER");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ShiftRequestService shiftRequestService;

    @Test
    void getShiftRequestById_returnsOk() throws Exception {
        ShiftRequestResponse response = new ShiftRequestResponse();
        response.setId(1L);
        response.setStaffId(10L);
        response.setDesiredShiftTypeId(20L);
        response.setDesiredShiftCode("A");
        response.setDesiredShiftName("早番");
        response.setStatus(ShiftRequestStatus.DRAFT);
        when(shiftRequestService.getShiftRequestById(1L)).thenReturn(response);

        mockMvc.perform(get("/api/shift-requests/1")
                .header("Authorization", AUTH_HEADER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.desiredShiftCode").value("A"));
    }

    @Test
    void getShiftRequestsByStaffStatusAndDateRange_returnsBadRequestForInvalidStatus() throws Exception {
        mockMvc.perform(get("/api/shift-requests/staff/1/status/INVALID")
                .header("Authorization", AUTH_HEADER)
                        .param("startDate", "2026-08-01")
                        .param("endDate", "2026-08-31"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createShiftRequest_returnsCreated() throws Exception {
        ShiftRequestResponse response = new ShiftRequestResponse();
        response.setId(1L);
        response.setStaffId(10L);
        response.setDesiredShiftTypeId(20L);
        response.setDesiredShiftCode("A");
        response.setDesiredShiftName("早番");
        response.setStatus(ShiftRequestStatus.DRAFT);
        when(shiftRequestService.createShiftRequest(anyLong(), any(ShiftRequestCreateRequest.class))).thenReturn(response);

        ShiftRequestCreateRequest request = new ShiftRequestCreateRequest();
        request.setWorkDate(LocalDate.of(2026, 8, 1));
        request.setDesiredShiftTypeId(20L);

        mockMvc.perform(post("/api/shift-requests")
                .header("Authorization", AUTH_HEADER)
                .requestAttr("staffId", 10L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.desiredShiftCode").value("A"));
    }

    @Test
    void submitShiftRequest_returnsOk() throws Exception {
        ShiftRequestResponse response = new ShiftRequestResponse();
        response.setId(1L);
        response.setStatus(ShiftRequestStatus.SUBMITTED);
        when(shiftRequestService.submitShiftRequest(10L, 1L)).thenReturn(response);

        mockMvc.perform(post("/api/shift-requests/1/submit")
                .header("Authorization", AUTH_HEADER)
                        .requestAttr("staffId", 10L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUBMITTED"));
    }
}
