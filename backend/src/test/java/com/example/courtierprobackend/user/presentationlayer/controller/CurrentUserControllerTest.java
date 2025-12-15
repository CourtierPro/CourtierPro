package com.example.courtierprobackend.user.presentationlayer.controller;

import com.example.courtierprobackend.common.exceptions.NotFoundException;
import com.example.courtierprobackend.common.exceptions.UnauthorizedException;
import com.example.courtierprobackend.security.UserContextFilter;
import com.example.courtierprobackend.user.dataaccesslayer.UserAccount;
import com.example.courtierprobackend.user.dataaccesslayer.UserAccountRepository;
import com.example.courtierprobackend.user.mapper.UserMapper;
import com.example.courtierprobackend.user.presentationlayer.response.UserResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CurrentUserControllerTest {

    @Mock
    private UserAccountRepository userAccountRepository;
    @Mock
    private UserMapper userMapper;
    @Mock
    private HttpServletRequest request;
    @Mock
    private Jwt jwt;

    @InjectMocks
    private CurrentUserController controller;

    @Test
    void getCurrentUser_WithInternalId_ReturnsUser() {
        UUID internalId = UUID.randomUUID();
        UserAccount account = new UserAccount();
        UserResponse response = UserResponse.builder().build();

        when(request.getAttribute(UserContextFilter.INTERNAL_USER_ID_ATTR)).thenReturn(internalId);
        when(userAccountRepository.findById(internalId)).thenReturn(Optional.of(account));
        when(userMapper.toResponse(account)).thenReturn(response);

        ResponseEntity<UserResponse> result = controller.getCurrentUser(request, jwt);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isEqualTo(response);
    }

    @Test
    void getCurrentUser_WithInternalId_UserNotFound_ThrowsNotFound() {
        UUID internalId = UUID.randomUUID();
        when(request.getAttribute(UserContextFilter.INTERNAL_USER_ID_ATTR)).thenReturn(internalId);
        when(userAccountRepository.findById(internalId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> controller.getCurrentUser(request, jwt))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("User not found");
    }

    @Test
    void getCurrentUser_NoInternalId_WithJwt_FallbackSuccess() {
        String auth0Id = "auth0|123";
        UserAccount account = new UserAccount();
        UserResponse response = UserResponse.builder().build();

        when(request.getAttribute(UserContextFilter.INTERNAL_USER_ID_ATTR)).thenReturn(null);
        when(jwt.getClaimAsString("sub")).thenReturn(auth0Id);
        when(userAccountRepository.findByAuth0UserId(auth0Id)).thenReturn(Optional.of(account));
        when(userMapper.toResponse(account)).thenReturn(response);

        ResponseEntity<UserResponse> result = controller.getCurrentUser(request, jwt);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isEqualTo(response);
    }

    @Test
    void getCurrentUser_NoInternalId_WithJwt_FallbackUserNotFound_ThrowsNotFound() {
        String auth0Id = "auth0|123";
        when(request.getAttribute(UserContextFilter.INTERNAL_USER_ID_ATTR)).thenReturn(null);
        when(jwt.getClaimAsString("sub")).thenReturn(auth0Id);
        when(userAccountRepository.findByAuth0UserId(auth0Id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> controller.getCurrentUser(request, jwt))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("User not provisioned");
    }

    @Test
    void getCurrentUser_NoInternalId_NoJwt_ThrowsUnauthorized() {
        when(request.getAttribute(UserContextFilter.INTERNAL_USER_ID_ATTR)).thenReturn(null);
        
        // jwt is passed as null in this scenario (AuthenticationPrincipal can be null)
        assertThatThrownBy(() -> controller.getCurrentUser(request, null))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Authentication required");
    }
}
