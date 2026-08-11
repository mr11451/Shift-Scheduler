package com.shiftscheduler.server.api;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shiftscheduler.server.domain.ShiftType;
import com.shiftscheduler.server.service.ShiftTypeService;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ShiftTypeApiController.class)
class ShiftTypeApiControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ShiftTypeService shiftTypeService;

    @Test
    void getAllShiftTypes_returnsList() throws Exception {
        ShiftTypeResponse response = new ShiftTypeResponse();
        response.setId(1L);
        response.setShiftCode("A");
        response.setShiftName("早番");
        response.setStartTime(LocalTime.of(9, 0));
        response.setEndTime(LocalTime.of(18, 0));
        response.setSortOrder(1);
        response.setIsActive(true);

        when(shiftTypeService.getAllShiftTypes()).thenReturn(List.of(response));

        mockMvc.perform(get("/api/shift-types"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].shiftCode").value("A"))
                .andExpect(jsonPath("$[0].shiftName").value("早番"))
                .andExpect(jsonPath("$[0].sortOrder").value(1));
    }

    @Test
    void getShiftTypeById_returnsNotFoundWhenMissing() throws Exception {
        when(shiftTypeService.getShiftTypeById(99L)).thenThrow(new IllegalArgumentException("シフトタイプが見つかりません: 99"));

        mockMvc.perform(get("/api/shift-types/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void createShiftType_returnsCreatedResponse() throws Exception {
        ShiftTypeResponse response = new ShiftTypeResponse();
        response.setId(1L);
        response.setShiftCode("A");
        response.setShiftName("早番");
        response.setSortOrder(1);
        response.setIsActive(true);

        when(shiftTypeService.createShiftType(any(ShiftTypeCreateRequest.class))).thenReturn(response);

        ShiftTypeCreateRequest request = new ShiftTypeCreateRequest(
                "A",
                "早番",
                LocalTime.of(9, 0),
                LocalTime.of(18, 0),
                false,
                1
        );

        mockMvc.perform(post("/api/shift-types")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.shiftCode").value("A"))
                .andExpect(jsonPath("$.shiftName").value("早番"));
    }

    @Test
    void updateShiftType_returnsOkResponse() throws Exception {
        ShiftTypeResponse response = new ShiftTypeResponse();
        response.setId(1L);
        response.setShiftCode("B");
        response.setShiftName("遅番");
        response.setSortOrder(2);
        response.setIsActive(false);

        when(shiftTypeService.updateShiftType(eq(1L), any(ShiftTypeUpdateRequest.class))).thenReturn(response);

        ShiftTypeUpdateRequest request = new ShiftTypeUpdateRequest(
                "B",
                "遅番",
                LocalTime.of(10, 0),
                LocalTime.of(19, 0),
                true,
                2,
                false
        );

        mockMvc.perform(put("/api/shift-types/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shiftCode").value("B"))
                .andExpect(jsonPath("$.shiftName").value("遅番"));
    }

    @Test
    void deleteShiftType_returnsNoContent() throws Exception {
                ShiftTypeResponse response = new ShiftTypeResponse();
                response.setId(1L);
                response.setShiftCode("A");
                response.setShiftName("早番");
                response.setIsActive(false);
                when(shiftTypeService.deactivateShiftType(1L)).thenReturn(response);

        mockMvc.perform(delete("/api/shift-types/1"))
                .andExpect(status().isNoContent());
    }
}
