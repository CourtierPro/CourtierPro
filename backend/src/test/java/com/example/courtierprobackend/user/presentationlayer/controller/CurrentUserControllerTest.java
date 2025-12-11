package com.example.courtierprobackend.user.presentationlayer.controller;

import com.example.courtierprobackend.audit.loginaudit.businesslayer.LoginAuditService;
import com.example.courtierprobackend.security.UserContextFilter;
import com.example.courtierprobackend.user.dataaccesslayer.UserAccount;
import com.example.courtierprobackend.user.dataaccesslayer.UserAccountRepository;
import com.example.courtierprobackend.user.dataaccesslayer.UserRole;
import com.example.courtierprobackend.user.mapper.UserMapper;
import com.example.courtierprobackend.user.presentationlayer.response.UserResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CurrentUserControllerTest {

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private LoginAuditService loginAuditService;

    @InjectMocks
    private CurrentUserController controller;

    @Mock
    private HttpServletRequest request;

    @Mock
    private Jwt jwt;

    private UserAccount userAccount;
    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        userAccount = new UserAccount(
                "auth0|123",
                "test@example.com",
                "John",
                "Doe",
                UserRole.CLIENT,
                "en"
        );
        userAccount.setId(userId);
    }

    @Test
    void getCurrentUser_WithInternalId_ReturnsUserAndRecordsLogin() {
        // Arrange
        when(request.getAttribute(UserContextFilter.INTERNAL_USER_ID_ATTR)).thenReturn(userId);
        when(userAccountRepository.findById(userId)).thenReturn(Optional.of(userAccount));
        when(userMapper.toResponse(userAccount)).thenReturn(new UserResponse());
        
        when(request.getHeader("X-Forwarded-For")).thenReturn("1.2.3.4");
        when(request.getHeader("User-Agent")).thenReturn("Mozilla/5.0");

        // Act
        ResponseEntity<UserResponse> response = controller.getCurrentUser(request, jwt);

        // Assert
        assertThat(response.getStatusCodeValue()).isEqualTo(200);
        verify(loginAuditService).recordLoginEvent(
                eq("auth0|123"),
                eq("test@example.com"),
                eq("CLIENT"),
                eq("1.2.3.4"),
                eq("Mozilla/5.0")
        );
    }

    @Test
    void getCurrentUser_WithAuth0Id_ReturnsUserAndRecordsLogin() {
        // Arrange
        when(request.getAttribute(UserContextFilter.INTERNAL_USER_ID_ATTR)).thenReturn(null);
        when(jwt.getClaimAsString("sub")).thenReturn("auth0|123");
        when(userAccountRepository.findByAuth0UserId("auth0|123")).thenReturn(Optional.of(userAccount));
        when(userMapper.toResponse(userAccount)).thenReturn(new UserResponse());

        when(request.getRemoteAddr()).thenReturn("5.6.7.8");

        // Act
        ResponseEntity<UserResponse> response = controller.getCurrentUser(request, jwt);

        // Assert
        assertThat(response.getStatusCodeValue()).isEqualTo(200);
        verify(loginAuditService).recordLoginEvent(
                eq("auth0|123"),
                eq("test@example.com"),
                eq("CLIENT"),
                eq("5.6.7.8"),
                any()
        );
    }
}
