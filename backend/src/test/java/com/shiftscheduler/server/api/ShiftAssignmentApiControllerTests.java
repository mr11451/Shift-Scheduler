package com.shiftscheduler.server.api;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shiftscheduler.server.service.ShiftAssignmentService;
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

@WebMvcTest(ShiftAssignmentApiController.class)
@AutoConfigureMockMvc(addFilters = false)
class ShiftAssignmentApiControllerTests {

    private static final String AUTH_HEADER = "Bearer " + JwtTokenUtil.generateToken(10L, "STF-00010", "MASTER");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ShiftAssignmentService shiftAssignmentService;

    @Test
    void getShiftAssignmentById_returnsOk() throws Exception {
        ShiftAssignmentResponse response = new ShiftAssignmentResponse();
        response.setId(1L);
        response.setStaffId(10L);
        response.setShiftTypeId(20L);
        response.setShiftCode("A");
        response.setShiftName("早番");
        when(shiftAssignmentService.getShiftAssignmentById(1L)).thenReturn(response);

        mockMvc.perform(get("/api/shift-assignments/1")
                        .header("Authorization", AUTH_HEADER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shiftCode").value("A"));
    }

    @Test
    void getShiftAssignmentsByStaffAndDateRange_returnsBadRequestForInvalidDate() throws Exception {
        mockMvc.perform(get("/api/shift-assignments/staff/1")
                .header("Authorization", AUTH_HEADER)
                        .param("startDate", "invalid")
                        .param("endDate", "2026-08-31"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createShiftAssignment_returnsCreated() throws Exception {
        ShiftAssignmentResponse response = new ShiftAssignmentResponse();
        response.setId(1L);
        response.setStaffId(10L);
        response.setShiftTypeId(20L);
        response.setShiftCode("A");
        response.setShiftName("早番");
        when(shiftAssignmentService.createShiftAssignment(anyLong(), any(ShiftAssignmentCreateRequest.class))).thenReturn(response);

        ShiftAssignmentCreateRequest request = new ShiftAssignmentCreateRequest();
        request.setStaffId(10L);
        request.setWorkDate(LocalDate.of(2026, 8, 1));
        request.setShiftTypeId(20L);

        mockMvc.perform(post("/api/shift-assignments")
                        .header("Authorization", AUTH_HEADER)
                .requestAttr("staffId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.shiftCode").value("A"));
    }

    @Test
    void deleteShiftAssignment_returnsNoContent() throws Exception {
        // Void method; no stubbing required.

        mockMvc.perform(delete("/api/shift-assignments/1")
                        .header("Authorization", AUTH_HEADER)
                        .requestAttr("staffId", 1L))
                .andExpect(status().isNoContent());
    }

        @Test
        void autoGenerateShiftAssignments_returnsOk() throws Exception {
                AutoShiftGenerationResultResponse response = new AutoShiftGenerationResultResponse();
                response.setYear(2026);
                response.setMonth(8);
                response.setGeneratedCount(42);
                when(shiftAssignmentService.autoGenerateShiftAssignments(anyLong(), anyInt(), anyInt())).thenReturn(response);

                mockMvc.perform(post("/api/shift-assignments/auto-generate")
                                                .header("Authorization", AUTH_HEADER)
                                                .requestAttr("staffId", 1L)
                                                .param("year", "2026")
                                                .param("month", "8"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.year").value(2026))
                                .andExpect(jsonPath("$.month").value(8))
                                .andExpect(jsonPath("$.generatedCount").value(42));
        }
}
