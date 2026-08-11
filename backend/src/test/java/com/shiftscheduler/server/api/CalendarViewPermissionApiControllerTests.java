package com.shiftscheduler.server.api;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shiftscheduler.server.domain.CalendarViewPermission;
import com.shiftscheduler.server.domain.CalendarViewPermissionStatus;
import com.shiftscheduler.server.domain.Staff;
import com.shiftscheduler.server.service.CalendarViewPermissionService;
import com.shiftscheduler.server.util.JwtTokenUtil;
import java.util.List;
import java.util.Optional;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CalendarViewPermissionApiController.class)
@AutoConfigureMockMvc(addFilters = false)
class CalendarViewPermissionApiControllerTests {

    private static final String AUTH_HEADER = "Bearer " + JwtTokenUtil.generateToken(1L, "STF-00001", "MASTER");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CalendarViewPermissionService calendarViewPermissionService;

    @Test
    void getCalendarViewPermissionById_returnsOk() throws Exception {
        CalendarViewPermissionResponse response = new CalendarViewPermissionResponse();
        response.setId(1L);
        response.setRequesterStaffId(10L);
        response.setTargetStaffId(20L);
        response.setStatus(CalendarViewPermissionStatus.PENDING);
        when(calendarViewPermissionService.getCalendarViewPermissionById(1L)).thenReturn(response);

        mockMvc.perform(get("/api/calendar-view-permissions/1")
                .header("Authorization", AUTH_HEADER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requesterStaffId").value(10L));
    }

    @Test
    void getApprovedCalendarViewPermissionsForRequester_returnsNotFoundWhenEmpty() throws Exception {
        when(calendarViewPermissionService.getApprovedCalendarViewPermissionsForRequester(1L, 2L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/calendar-view-permissions/requester/1/target/2")
                .header("Authorization", AUTH_HEADER))
                .andExpect(status().isNotFound());
    }

    @Test
    void getApprovedTargetStaffIdsForRequester_returnsList() throws Exception {
        when(calendarViewPermissionService.getApprovedTargetStaffIdsForRequester(1L)).thenReturn(List.of(2L, 3L));

        mockMvc.perform(get("/api/calendar-view-permissions/requester/1/approved-targets")
                .header("Authorization", AUTH_HEADER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void createCalendarViewPermission_returnsCreated() throws Exception {
        CalendarViewPermissionResponse response = new CalendarViewPermissionResponse();
        response.setId(1L);
        response.setRequesterStaffId(1L);
        response.setTargetStaffId(2L);
        response.setStatus(CalendarViewPermissionStatus.PENDING);
        when(calendarViewPermissionService.createCalendarViewPermission(anyLong(), any(CalendarViewPermissionCreateRequest.class))).thenReturn(response);

        CalendarViewPermissionCreateRequest request = new CalendarViewPermissionCreateRequest(2L);

        mockMvc.perform(post("/api/calendar-view-permissions")
                .header("Authorization", AUTH_HEADER)
                .requestAttr("staffId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.targetStaffId").value(2L));
    }

    @Test
    void calendarViewPermissionEntity_setsAuditTimestampsOnPersist() {
        CalendarViewPermission permission = new CalendarViewPermission();
        permission.setRequesterStaff(new Staff());
        permission.setTargetStaff(new Staff());
        permission.prePersist();

        assertNotNull(permission.getRequestedAt());
        assertNotNull(permission.getCreatedAt());
        assertNotNull(permission.getUpdatedAt());
    }
}
