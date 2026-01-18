package com.example.courtierprobackend.user.presentationlayer.controller;

import com.example.courtierprobackend.common.exceptions.UnauthorizedException;
import com.example.courtierprobackend.security.UserContextFilter;
import com.example.courtierprobackend.user.dataaccesslayer.UserAccount;
import com.example.courtierprobackend.user.dataaccesslayer.UserAccountRepository;
import com.example.courtierprobackend.user.domainclientlayer.auth0.Auth0ManagementClient;
import com.example.courtierprobackend.user.mapper.UserMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CurrentUserControllerMfaTest {
    @Mock
    private UserAccountRepository userAccountRepository;
    @Mock
    private UserMapper userMapper;
    @Mock
    private Auth0ManagementClient auth0ManagementClient;
    @Mock
    private HttpServletRequest request;
    @Mock
    private Jwt jwt;
    @InjectMocks
    private CurrentUserController controller;

    @Test
    void getMfaStatus_WithInternalId_ReturnsMfaStatus() {
        UUID internalId = UUID.randomUUID();
        UserAccount account = new UserAccount();
        account.setAuth0UserId("auth0|testid");
        when(request.getAttribute(UserContextFilter.INTERNAL_USER_ID_ATTR)).thenReturn(internalId);
        when(userAccountRepository.findById(internalId)).thenReturn(Optional.of(account));
        when(auth0ManagementClient.isMfaEnabled("auth0|testid")).thenReturn(true);

        ResponseEntity<Map<String, Object>> result = controller.getMfaStatus(request, jwt);
        assertThat(result.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(result.getBody()).containsEntry("mfaEnabled", true);
    }

    @Test
    void getMfaStatus_WithJwtFallback_ReturnsMfaStatus() {
        when(request.getAttribute(UserContextFilter.INTERNAL_USER_ID_ATTR)).thenReturn(null);
        when(jwt.getClaimAsString("sub")).thenReturn("auth0|jwtid");
        when(auth0ManagementClient.isMfaEnabled("auth0|jwtid")).thenReturn(false);

        ResponseEntity<Map<String, Object>> result = controller.getMfaStatus(request, jwt);
        assertThat(result.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(result.getBody()).containsEntry("mfaEnabled", false);
    }

    @Test
    void getMfaStatus_NoAuth_ThrowsUnauthorized() {
        when(request.getAttribute(UserContextFilter.INTERNAL_USER_ID_ATTR)).thenReturn(null);
        when(jwt.getClaimAsString("sub")).thenReturn(null);
        assertThatThrownBy(() -> controller.getMfaStatus(request, jwt))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Authentication required");
    }
}
