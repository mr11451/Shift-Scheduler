package com.shiftscheduler.server.api;

import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.hasSize;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shiftscheduler.server.domain.RoleLevel;
import com.shiftscheduler.server.domain.Staff;
import com.shiftscheduler.server.dto.StaffCreateRequest;
import com.shiftscheduler.server.dto.StaffResponse;
import com.shiftscheduler.server.dto.StaffUpdateRequest;
import com.shiftscheduler.server.service.StaffService;
import com.shiftscheduler.server.util.JwtTokenUtil;

@WebMvcTest(StaffApiController.class)
@AutoConfigureMockMvc(addFilters = false)
class StaffApiControllerTests {

    private static final String AUTH_HEADER = "Bearer " + JwtTokenUtil.generateToken(1L, "STF-00001", "MASTER");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private StaffService staffService;

    @Test
    void listStaffs_returnsList() throws Exception {
        Staff staff = new Staff();
        staff.setId(1L);
        staff.setStaffCode("STF-00001");
        staff.setStaffName("山田太郎");
        staff.setRoleLevel(RoleLevel.MEMBER);
        staff.setIsActive(true);
        when(staffService.getAllActiveStaff()).thenReturn(List.of(staff));
        when(staffService.convertToResponse(staff)).thenReturn(toResponse(staff));

        mockMvc.perform(get("/api/staffs")
                        .header("Authorization", AUTH_HEADER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].staffCode").value("STF-00001"));
    }

    @Test
    void listStaffs_usesRequesterContextForFiltering() throws Exception {
        Staff staff = new Staff();
        staff.setId(2L);
        staff.setStaffCode("STF-00002");
        staff.setStaffName("佐藤花子");
        staff.setRoleLevel(RoleLevel.MEMBER);
        staff.setIsActive(true);
        when(staffService.getSelectableStaffsForRequester(7L)).thenReturn(List.of(staff));
        when(staffService.convertToResponse(staff)).thenReturn(toResponse(staff));

        mockMvc.perform(get("/api/staffs")
                        .header("Authorization", AUTH_HEADER)
                        .requestAttr("staffId", 7L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].staffCode").value("STF-00002"));
    }

    @Test
    void getStaff_returnsNotFoundWhenMissing() throws Exception {
        when(staffService.getStaffById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/staffs/99")
                        .header("Authorization", AUTH_HEADER))
                .andExpect(status().isNotFound());
    }

    @Test
    void createStaff_returnsCreated() throws Exception {
        Staff staff = new Staff();
        staff.setId(1L);
        staff.setStaffCode("STF-00001");
        staff.setStaffName("山田太郎");
        staff.setRoleLevel(RoleLevel.MEMBER);
        staff.setIsActive(true);
        when(staffService.createStaff(anyLong(), any(StaffCreateRequest.class))).thenReturn(staff);
        when(staffService.convertToResponse(staff)).thenReturn(toResponse(staff));

        StaffCreateRequest request = new StaffCreateRequest();
        request.setStaffName("山田太郎");
        request.setResponsibility("担当");
        request.setRoleLevel(RoleLevel.MEMBER);

        mockMvc.perform(post("/api/staffs")
                        .header("Authorization", AUTH_HEADER)
                        .requestAttr("staffId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.staffCode").value("STF-00001"));
    }

    @Test
    void updateStaff_returnsOk() throws Exception {
        Staff staff = new Staff();
        staff.setId(1L);
        staff.setStaffCode("STF-00001");
        staff.setStaffName("山田太郎");
        staff.setRoleLevel(RoleLevel.MASTER);
        staff.setIsActive(true);
        when(staffService.updateStaff(anyLong(), anyLong(), any(StaffUpdateRequest.class))).thenReturn(staff);
        when(staffService.convertToResponse(staff)).thenReturn(toResponse(staff));

        StaffUpdateRequest request = new StaffUpdateRequest();
        request.setStaffName("山田太郎");
        request.setResponsibility("担当");
        request.setRoleLevel(RoleLevel.MASTER);
        request.setIsActive(true);

        mockMvc.perform(put("/api/staffs/1")
                        .header("Authorization", AUTH_HEADER)
                        .requestAttr("staffId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.staffCode").value("STF-00001"));
    }

    @Test
    void deactivateStaff_returnsNoContent() throws Exception {
        Staff staff = new Staff();
        staff.setId(1L);
        staff.setStaffCode("STF-00001");
        staff.setStaffName("山田太郎");
        staff.setRoleLevel(RoleLevel.MEMBER);
        staff.setIsActive(false);
        when(staffService.deactivateStaff(1L)).thenReturn(staff);

        mockMvc.perform(delete("/api/staffs/1")
                        .header("Authorization", AUTH_HEADER))
                .andExpect(status().isNoContent());
    }

    private StaffResponse toResponse(Staff staff) {
        StaffResponse response = new StaffResponse();
        response.setId(staff.getId());
        response.setStaffCode(staff.getStaffCode());
        response.setStaffName(staff.getStaffName());
        response.setRoleLevel(staff.getRoleLevel());
        response.setIsActive(staff.getIsActive());
        return response;
    }
}
