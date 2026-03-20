package com.nexus.platform.integration;

import com.nexus.platform.dto.Result;
import com.nexus.platform.entity.Game;
import com.nexus.platform.service.GameService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebClient;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureWebClient
public class GameIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void testGameUpload() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "game.zip".getBytes(),
            "application/zip"
        );

        mockMvc.perform(multipart("/api/v1/game/upload")
                .file(file)
                .param("name", "Test Game")
                .param("description", "Test Description")
                .param("developerId", "1"))
                .andExpect(status().isOk())
                .andExpect(result -> {
                    result("$.code").value(0)
                    result("$.data.name").value("Test Game")
                });
    }

    @Test
    public void testGetGameList() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/v1/game/list"))
                .andExpect(status().isOk())
                .andExpect(result -> {
                    result("$.code").value(0)
                    result("$.data").isArray()
                });
    }

    @Test
    public void testGetGameByAppId() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/v1/game/wx1234567890abcdef"))
                .andExpect(status().isOk())
                .andExpect(result -> {
                    result("$.code").value(0)
                    result("$.data.appId").value("wx1234567890abcdef")
                });
    }

    @Test
    public void testApproveGame() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/game/approve/1"))
                .andExpect(status().isOk())
                .andExpect(result -> {
                    result("$.code").value(0)
                });
    }

    @Test
    public void testRejectGame() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/game/reject/1"))
                .andExpect(status().isOk())
                .andExpect(result -> {
                    result("$.code").value(0)
                });
    }
}
