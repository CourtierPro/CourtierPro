package com.example.courtierprobackend.user.presentationlayer;

import com.example.courtierprobackend.user.businesslayer.UserProvisioningService;
import com.example.courtierprobackend.user.dataaccesslayer.UserRole;
import com.example.courtierprobackend.user.presentationlayer.controller.AdminUserController;
import com.example.courtierprobackend.user.presentationlayer.request.CreateUserRequest;
import com.example.courtierprobackend.user.presentationlayer.request.UpdateStatusRequest;
import com.example.courtierprobackend.user.presentationlayer.response.UserResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.mockito.Mockito;

/**
 * Integration test for AdminUserController.
 * Tests HTTP request/response handling with mocked service layer.
 */
@WebMvcTest(value = AdminUserController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminUserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserProvisioningService userProvisioningService;

    @TestConfiguration
    static class TestConfig {
        @Bean
        public UserProvisioningService userProvisioningService() {
            return Mockito.mock(UserProvisioningService.class);
        }
    }

    @BeforeEach
    void setUp() {
        reset(userProvisioningService);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getAllUsers_ReturnsUsersList() throws Exception {
        // Arrange
        UserResponse user1 = UserResponse.builder()
                .id(UUID.randomUUID().toString())
                .email("user1@test.com")
                .firstName("John")
                .lastName("Doe")
                .role("BROKER")
                .active(true)
                .preferredLanguage("en")
                .build();

        UserResponse user2 = UserResponse.builder()
                .id(UUID.randomUUID().toString())
                .email("user2@test.com")
                .firstName("Jane")
                .lastName("Smith")
                .role("ADMIN")
                .active(true)
                .preferredLanguage("fr")
                .build();

        when(userProvisioningService.getAllUsers()).thenReturn(List.of(user1, user2));

        // Act & Assert
        mockMvc.perform(get("/api/admin/users")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].email").value("user1@test.com"))
                .andExpect(jsonPath("$[1].email").value("user2@test.com"));

        verify(userProvisioningService).getAllUsers();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createUser_WithValidRequest_ReturnsCreated() throws Exception {
        // Arrange
        CreateUserRequest request = CreateUserRequest.builder()
                .email("newuser@test.com")
                .firstName("New")
                .lastName("User")
                .role("BROKER")
                .preferredLanguage("en")
                .build();

        UserResponse response = UserResponse.builder()
                .id(UUID.randomUUID().toString())
                .email("newuser@test.com")
                .firstName("New")
                .lastName("User")
                .role("BROKER")
                .active(true)
                .preferredLanguage("en")
                .build();

        when(userProvisioningService.createUser(any(CreateUserRequest.class))).thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/api/admin/users")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("newuser@test.com"))
                .andExpect(jsonPath("$.firstName").value("New"))
                .andExpect(jsonPath("$.lastName").value("User"))
                .andExpect(jsonPath("$.role").value("BROKER"));

        verify(userProvisioningService).createUser(any(CreateUserRequest.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateStatus_WithValidRequest_ReturnsOk() throws Exception {
        // Arrange
        UUID userId = UUID.randomUUID();
        UpdateStatusRequest request = UpdateStatusRequest.builder()
                .active(false)
                .build();

        UserResponse response = UserResponse.builder()
                .id(userId.toString())
                .email("user@test.com")
                .firstName("John")
                .lastName("Doe")
                .role("BROKER")
                .active(false)
                .preferredLanguage("en")
                .build();

        when(userProvisioningService.updateStatus(eq(userId), any(UpdateStatusRequest.class))).thenReturn(response);

        // Act & Assert
        mockMvc.perform(patch("/api/admin/users/{userId}/status", userId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId.toString()))
                .andExpect(jsonPath("$.active").value(false));

        verify(userProvisioningService).updateStatus(eq(userId), any(UpdateStatusRequest.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void triggerPasswordReset_WithValidUserId_ReturnsOk() throws Exception {
        // Arrange
        UUID userId = UUID.randomUUID();

        // Act & Assert
        mockMvc.perform(post("/api/admin/users/{userId}/password-reset", userId)
                        .with(csrf()))
                .andExpect(status().isOk());

        verify(userProvisioningService).triggerPasswordReset(userId);
    }

}
