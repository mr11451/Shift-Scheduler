package com.shiftscheduler.server.api;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shiftscheduler.server.service.QualificationService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(QualificationApiController.class)
class QualificationApiControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private QualificationService qualificationService;

    @Test
    void getAllQualifications_returnsList() throws Exception {
        QualificationResponse response = new QualificationResponse();
        response.setId(1L);
        response.setQualificationName("看護師");
        response.setIsActive(true);
        when(qualificationService.getAllQualifications()).thenReturn(List.of(response));

        mockMvc.perform(get("/api/qualifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].qualificationName").value("看護師"));
    }

    @Test
    void getQualificationById_returnsNotFoundWhenMissing() throws Exception {
        when(qualificationService.getQualificationById(99L)).thenThrow(new IllegalArgumentException("資格が見つかりません: 99"));

        mockMvc.perform(get("/api/qualifications/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void createQualification_returnsCreated() throws Exception {
        QualificationResponse response = new QualificationResponse();
        response.setId(1L);
        response.setQualificationName("看護師");
        response.setIsActive(true);
        when(qualificationService.createQualification(any(QualificationCreateRequest.class))).thenReturn(response);

        QualificationCreateRequest request = new QualificationCreateRequest();
        request.setQualificationName("看護師");

        mockMvc.perform(post("/api/qualifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.qualificationName").value("看護師"));
    }

    @Test
    void deleteQualification_returnsNoContent() throws Exception {
        doNothing().when(qualificationService).deactivateQualification(1L);

        mockMvc.perform(delete("/api/qualifications/1"))
                .andExpect(status().isNoContent());
    }
}
