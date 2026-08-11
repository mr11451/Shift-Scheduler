package com.shiftscheduler.server.api;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shiftscheduler.server.service.GroupService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(GroupApiController.class)
class GroupApiControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private GroupService groupService;

    @Test
    void getAllGroups_returnsList() throws Exception {
        GroupResponse response = new GroupResponse();
        response.setId(1L);
        response.setGroupCode("G1");
        response.setGroupName("第1グループ");
        response.setIsActive(true);
        when(groupService.getAllGroups()).thenReturn(List.of(response));

        mockMvc.perform(get("/api/groups"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].groupCode").value("G1"))
                .andExpect(jsonPath("$[0].groupName").value("第1グループ"));
    }

    @Test
    void getGroupById_returnsNotFoundWhenMissing() throws Exception {
        when(groupService.getGroupById(99L)).thenThrow(new IllegalArgumentException("グループが見つかりません: 99"));

        mockMvc.perform(get("/api/groups/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void createGroup_returnsCreated() throws Exception {
        GroupResponse response = new GroupResponse();
        response.setId(1L);
        response.setGroupCode("G1");
        response.setGroupName("第1グループ");
        response.setIsActive(true);
        when(groupService.createGroup(any(GroupCreateRequest.class))).thenReturn(response);

        GroupCreateRequest request = new GroupCreateRequest();
        request.setGroupCode("G1");
        request.setGroupName("第1グループ");

        mockMvc.perform(post("/api/groups")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.groupCode").value("G1"));
    }

    @Test
    void deleteGroup_returnsNoContent() throws Exception {
        doNothing().when(groupService).deactivateGroup(1L);

        mockMvc.perform(delete("/api/groups/1"))
                .andExpect(status().isNoContent());
    }
}
