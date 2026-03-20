package com.nexus.platform.integration;

import com.nexus.platform.dto.Result;
import com.nexus.platform.entity.User;
import com.nexus.platform.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebClient;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureWebClient
public class UserIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void testUserRegistration() throws Exception {
        String requestBody = objectMapper.writeValueAsString(
            new UserRegistrationRequest("testuser", "password123", "test@example.com")
        );

        mockMvc.perform(post("/api/v1/user/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(result -> {
                    result("$.code").value(0)
                    result("$.message").value("success")
                });
    }

    @Test
    public void testUserLogin() throws Exception {
        String requestBody = objectMapper.writeValueAsString(
            new UserLoginRequest("testuser", "password123")
        );

        mockMvc.perform(post("/api/v1/user/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(result -> {
                    result("$.code").value(0)
                    result("$.data").exists()
                });
    }

    @Test
    public void testInvalidLogin() throws Exception {
        String requestBody = objectMapper.writeValueAsString(
            new UserLoginRequest("testuser", "wrongpassword")
        );

        mockMvc.perform(post("/api/v1/user/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(result -> {
                    result("$.code").value(-1)
                    result("$.message").exists()
                });
    }

    record UserRegistrationRequest(String username, String password, String email) {}
    record UserLoginRequest(String username, String password) {}
}
