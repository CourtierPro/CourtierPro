package com.example.courtierprobackend.user.presentationlayer.controller;

import com.example.courtierprobackend.common.exceptions.BadRequestException;
import com.example.courtierprobackend.common.exceptions.NotFoundException;
import com.example.courtierprobackend.common.exceptions.UnauthorizedException;
import com.example.courtierprobackend.security.UserContextFilter;
import com.example.courtierprobackend.user.dataaccesslayer.UserAccount;
import com.example.courtierprobackend.user.dataaccesslayer.UserAccountRepository;
import com.example.courtierprobackend.user.domainclientlayer.auth0.Auth0ManagementClient;
import com.example.courtierprobackend.user.mapper.UserMapper;
import com.example.courtierprobackend.user.presentationlayer.response.UserResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.HashMap;
import java.util.Map;
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
    @Mock
    private Auth0ManagementClient auth0ManagementClient;

    @InjectMocks
    private CurrentUserController controller;

    @Nested
    @DisplayName("GET /api/me")
    class GetCurrentUserTests {

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

            assertThatThrownBy(() -> controller.getCurrentUser(request, null))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessage("Authentication required");
        }
    }

    @Nested
    @DisplayName("PATCH /api/me")
    class UpdateCurrentUserTests {

        @Test
        void updateCurrentUser_WithValidLanguageEn_UpdatesAndReturnsUser() {
            UUID internalId = UUID.randomUUID();
            UserAccount account = new UserAccount();
            account.setPreferredLanguage("fr");
            account.setAuth0UserId("auth0|test123");
            UserResponse response = UserResponse.builder().preferredLanguage("en").build();

            when(request.getAttribute(UserContextFilter.INTERNAL_USER_ID_ATTR)).thenReturn(internalId);
            when(userAccountRepository.findById(internalId)).thenReturn(Optional.of(account));
            when(userAccountRepository.save(account)).thenReturn(account);
            when(userMapper.toResponse(account)).thenReturn(response);

            Map<String, Object> updates = new HashMap<>();
            updates.put("preferredLanguage", "en");

            ResponseEntity<UserResponse> result = controller.updateCurrentUser(request, updates);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(account.getPreferredLanguage()).isEqualTo("en");
            verify(userAccountRepository).save(account);
            verify(auth0ManagementClient).updateUserLanguage("auth0|test123", "en");
        }

        @Test
        void updateCurrentUser_WithValidLanguageFr_UpdatesAndReturnsUser() {
            UUID internalId = UUID.randomUUID();
            UserAccount account = new UserAccount();
            account.setPreferredLanguage("en");
            account.setAuth0UserId("auth0|testfr456");
            UserResponse response = UserResponse.builder().preferredLanguage("fr").build();

            when(request.getAttribute(UserContextFilter.INTERNAL_USER_ID_ATTR)).thenReturn(internalId);
            when(userAccountRepository.findById(internalId)).thenReturn(Optional.of(account));
            when(userAccountRepository.save(account)).thenReturn(account);
            when(userMapper.toResponse(account)).thenReturn(response);

            Map<String, Object> updates = new HashMap<>();
            updates.put("preferredLanguage", "fr");

            ResponseEntity<UserResponse> result = controller.updateCurrentUser(request, updates);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(account.getPreferredLanguage()).isEqualTo("fr");
            verify(auth0ManagementClient).updateUserLanguage("auth0|testfr456", "fr");
        }

        @Test
        void updateCurrentUser_WithUppercaseLanguage_NormalizesToLowercase() {
            UUID internalId = UUID.randomUUID();
            UserAccount account = new UserAccount();
            UserResponse response = UserResponse.builder().build();

            when(request.getAttribute(UserContextFilter.INTERNAL_USER_ID_ATTR)).thenReturn(internalId);
            when(userAccountRepository.findById(internalId)).thenReturn(Optional.of(account));
            when(userAccountRepository.save(account)).thenReturn(account);
            when(userMapper.toResponse(account)).thenReturn(response);

            Map<String, Object> updates = new HashMap<>();
            updates.put("preferredLanguage", "EN");

            controller.updateCurrentUser(request, updates);

            assertThat(account.getPreferredLanguage()).isEqualTo("en");
        }

        @Test
        void updateCurrentUser_WithInvalidLanguage_ThrowsBadRequest() {
            UUID internalId = UUID.randomUUID();
            UserAccount account = new UserAccount();

            when(request.getAttribute(UserContextFilter.INTERNAL_USER_ID_ATTR)).thenReturn(internalId);
            when(userAccountRepository.findById(internalId)).thenReturn(Optional.of(account));

            Map<String, Object> updates = new HashMap<>();
            updates.put("preferredLanguage", "de");

            assertThatThrownBy(() -> controller.updateCurrentUser(request, updates))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Invalid language");
        }

        @Test
        void updateCurrentUser_WithNullLanguage_ThrowsBadRequest() {
            UUID internalId = UUID.randomUUID();
            UserAccount account = new UserAccount();

            when(request.getAttribute(UserContextFilter.INTERNAL_USER_ID_ATTR)).thenReturn(internalId);
            when(userAccountRepository.findById(internalId)).thenReturn(Optional.of(account));

            Map<String, Object> updates = new HashMap<>();
            updates.put("preferredLanguage", null);

            assertThatThrownBy(() -> controller.updateCurrentUser(request, updates))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Invalid language");
        }

        @Test
        void updateCurrentUser_NoInternalId_ThrowsUnauthorized() {
            when(request.getAttribute(UserContextFilter.INTERNAL_USER_ID_ATTR)).thenReturn(null);

            Map<String, Object> updates = new HashMap<>();
            updates.put("preferredLanguage", "en");

            assertThatThrownBy(() -> controller.updateCurrentUser(request, updates))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessage("Authentication required");
        }

        @Test
        void updateCurrentUser_UserNotFound_ThrowsNotFound() {
            UUID internalId = UUID.randomUUID();
            when(request.getAttribute(UserContextFilter.INTERNAL_USER_ID_ATTR)).thenReturn(internalId);
            when(userAccountRepository.findById(internalId)).thenReturn(Optional.empty());

            Map<String, Object> updates = new HashMap<>();
            updates.put("preferredLanguage", "en");

            assertThatThrownBy(() -> controller.updateCurrentUser(request, updates))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessage("User not found");
        }

        @Test
        void updateCurrentUser_WithEmptyUpdates_SavesWithoutChanges() {
            UUID internalId = UUID.randomUUID();
            UserAccount account = new UserAccount();
            account.setPreferredLanguage("en");
            UserResponse response = UserResponse.builder().build();

            when(request.getAttribute(UserContextFilter.INTERNAL_USER_ID_ATTR)).thenReturn(internalId);
            when(userAccountRepository.findById(internalId)).thenReturn(Optional.of(account));
            when(userAccountRepository.save(account)).thenReturn(account);
            when(userMapper.toResponse(account)).thenReturn(response);

            Map<String, Object> updates = new HashMap<>();

            ResponseEntity<UserResponse> result = controller.updateCurrentUser(request, updates);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(account.getPreferredLanguage()).isEqualTo("en");
            verify(userAccountRepository).save(account);
        }

        @Test
        void updateCurrentUser_Auth0SyncFails_StillSucceeds() {
            UUID internalId = UUID.randomUUID();
            UserAccount account = new UserAccount();
            account.setPreferredLanguage("fr");
            account.setAuth0UserId("auth0|syncfail789");
            UserResponse response = UserResponse.builder().preferredLanguage("en").build();

            when(request.getAttribute(UserContextFilter.INTERNAL_USER_ID_ATTR)).thenReturn(internalId);
            when(userAccountRepository.findById(internalId)).thenReturn(Optional.of(account));
            when(userAccountRepository.save(account)).thenReturn(account);
            when(userMapper.toResponse(account)).thenReturn(response);
            doThrow(new RuntimeException("Auth0 API error")).when(auth0ManagementClient)
                    .updateUserLanguage(anyString(), anyString());

            Map<String, Object> updates = new HashMap<>();
            updates.put("preferredLanguage", "en");

            ResponseEntity<UserResponse> result = controller.updateCurrentUser(request, updates);

            // Request should still succeed even if Auth0 sync fails
            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(account.getPreferredLanguage()).isEqualTo("en");
            verify(userAccountRepository).save(account);
            verify(auth0ManagementClient).updateUserLanguage("auth0|syncfail789", "en");
        }
    }
}

