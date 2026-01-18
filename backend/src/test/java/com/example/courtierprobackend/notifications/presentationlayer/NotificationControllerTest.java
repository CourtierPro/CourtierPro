package com.example.courtierprobackend.notifications.presentationlayer;

import com.example.courtierprobackend.notifications.businesslayer.NotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

import com.example.courtierprobackend.security.UserContextFilter;

@WebMvcTest(NotificationController.class)
@EnableMethodSecurity
class NotificationControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private NotificationService notificationService;

        @MockBean
        private UserContextFilter userContextFilter;

    @org.junit.jupiter.api.BeforeEach
    void setup() throws Exception {
        org.mockito.Mockito.doAnswer(invocation -> {
            jakarta.servlet.FilterChain chain = invocation.getArgument(2);
            chain.doFilter(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(userContextFilter).doFilter(any(), any(), any());
    }

    @Test
    void getUserNotifications_shouldReturnList() throws Exception {
        // Arrange
        String userId = "auth0|123";
        NotificationResponseDTO dto = new NotificationResponseDTO();
        dto.setPublicId(UUID.randomUUID().toString());
        dto.setTitle("Test Notification");
        dto.setCreatedAt(java.time.LocalDateTime.of(2023, 1, 1, 12, 0, 0));

        when(notificationService.getUserNotifications(anyString())).thenReturn(List.of(dto));

        // Act & Assert
        mockMvc.perform(get("/api/v1/notifications")
                .with(jwt().jwt(jwt -> jwt.subject(userId)))
                .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Test Notification"))
                .andExpect(jsonPath("$[0].publicId").value(dto.getPublicId()))
                .andExpect(jsonPath("$[0].createdAt").value("2023-01-01T12:00:00"));

        verify(notificationService).getUserNotifications(userId);
    }

    @Test
    void markAsRead_shouldReturnUpdatedNotification() throws Exception {
        // Arrange
        String publicId = UUID.randomUUID().toString();
        NotificationResponseDTO dto = new NotificationResponseDTO();
        dto.setPublicId(publicId);
        dto.setRead(true);

        when(notificationService.markAsRead(publicId)).thenReturn(dto);

        // Act & Assert
        mockMvc.perform(put("/api/v1/notifications/" + publicId + "/read")
                .with(jwt().jwt(jwt -> jwt.subject("auth0|123")))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.read").value(true));

        verify(notificationService).markAsRead(publicId);
    }

    @Test
    void markAsRead_withInvalidUuid_shouldReturnBadRequest() throws Exception {
        // Arrange
        String invalidId = "invalid-uuid-format";

        // Act & Assert
        mockMvc.perform(put("/api/v1/notifications/" + invalidId + "/read")
                .with(jwt().jwt(jwt -> jwt.subject("auth0|123")))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getUserNotifications_whenEmpty_shouldReturnEmptyList() throws Exception {
        // Arrange
        String userId = "auth0|123";
        when(notificationService.getUserNotifications(userId)).thenReturn(List.of());

        // Act & Assert
        mockMvc.perform(get("/api/v1/notifications")
                .with(jwt().jwt(jwt -> jwt.subject(userId)))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void sendBroadcast_shouldReturnOk_whenAdmin() throws Exception {
        BroadcastRequestDTO request = new BroadcastRequestDTO("Title", "Message");

        mockMvc.perform(post("/api/v1/notifications/broadcast")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))
                        .jwt(jwt -> jwt.subject("auth0|admin")))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(notificationService).sendBroadcast(any(BroadcastRequestDTO.class), any());
    }

    @Test
    void sendBroadcast_shouldReturnForbidden_whenNotAdmin() throws Exception {
        BroadcastRequestDTO request = new BroadcastRequestDTO("Title", "Message");

        mockMvc.perform(post("/api/v1/notifications/broadcast")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER")).jwt(jwt -> jwt.subject("auth0|user")))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(request)))
                .andExpect(status().isForbidden());

        verify(notificationService, org.mockito.Mockito.never()).sendBroadcast(any(), any());
    }

    @Test
    void sendBroadcast_withEmptyTitle_shouldReturnBadRequest() throws Exception {
        BroadcastRequestDTO request = new BroadcastRequestDTO("", "Message");

        mockMvc.perform(post("/api/v1/notifications/broadcast")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))
                        .jwt(jwt -> jwt.subject("auth0|admin")))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void sendBroadcast_withEmptyMessage_shouldReturnBadRequest() throws Exception {
        BroadcastRequestDTO request = new BroadcastRequestDTO("Title", "");

        mockMvc.perform(post("/api/v1/notifications/broadcast")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))
                        .jwt(jwt -> jwt.subject("auth0|admin")))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void sendBroadcast_withTitleTooLong_shouldReturnBadRequest() throws Exception {
        String longTitle = "a".repeat(101);
        BroadcastRequestDTO request = new BroadcastRequestDTO(longTitle, "Message");

        mockMvc.perform(post("/api/v1/notifications/broadcast")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))
                        .jwt(jwt -> jwt.subject("auth0|admin")))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void sendBroadcast_withMessageTooLong_shouldReturnBadRequest() throws Exception {
        String longMessage = "a".repeat(501);
        BroadcastRequestDTO request = new BroadcastRequestDTO("Title", longMessage);

        mockMvc.perform(post("/api/v1/notifications/broadcast")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))
                        .jwt(jwt -> jwt.subject("auth0|admin")))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
