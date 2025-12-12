package com.example.courtierprobackend.notifications.presentationlayer;

import com.example.courtierprobackend.notifications.businesslayer.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private NotificationController notificationController;

    private MockMvc mockMvc;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.standaloneSetup(notificationController)
                .setCustomArgumentResolvers(new HandlerMethodArgumentResolver() {
                    @Override
                    public boolean supportsParameter(MethodParameter parameter) {
                        return parameter.getParameterType().equals(Jwt.class);
                    }

                    @Override
                    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                            NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {
                        return Jwt.withTokenValue("token")
                                .header("alg", "none")
                                .claim("sub", "auth0|123")
                                .build();
                    }
                })
                .build();
    }

    @Test
    void getUserNotifications_shouldReturnList() throws Exception {
        // Arrange
        String userId = "auth0|123";
        NotificationResponseDTO dto = new NotificationResponseDTO();
        dto.setPublicId(UUID.randomUUID().toString());
        dto.setTitle("Test Notification");

        when(notificationService.getUserNotifications(userId)).thenReturn(List.of(dto));

        // Act & Assert
        mockMvc.perform(get("/api/v1/notifications")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Test Notification"));

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
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.read").value(true));

        verify(notificationService).markAsRead(publicId);
    }
}
