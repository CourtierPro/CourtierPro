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
    private com.example.courtierprobackend.user.businesslayer.EmailChangeService emailChangeService;

    @Test
    void confirmEmailChange_Success() {
        when(emailChangeService.confirmEmailChange("validtoken")).thenReturn(true);
        CurrentUserController controllerWithEmail = new CurrentUserController(userAccountRepository, userMapper, auth0ManagementClient, emailChangeService);
        ResponseEntity<String> result = controllerWithEmail.confirmEmailChange("validtoken");
        assertThat(result.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(result.getBody()).contains("confirmed");
    }

    @Test
    void confirmEmailChange_Failure() {
        when(emailChangeService.confirmEmailChange("badtoken")).thenReturn(false);
        CurrentUserController controllerWithEmail = new CurrentUserController(userAccountRepository, userMapper, auth0ManagementClient, emailChangeService);
        ResponseEntity<String> result = controllerWithEmail.confirmEmailChange("badtoken");
        assertThat(result.getStatusCode().is4xxClientError()).isTrue();
        assertThat(result.getBody()).contains("Invalid");
    }

    @Test
    void updateCurrentUser_EmailChangeFlow_TriggersEmailChangeService() {
        UUID internalId = UUID.randomUUID();
        UserAccount account = new UserAccount();
        account.setEmail("old@example.com");
        UserResponse response = UserResponse.builder().build();

        when(request.getAttribute(UserContextFilter.INTERNAL_USER_ID_ATTR)).thenReturn(internalId);
        when(userAccountRepository.findById(internalId)).thenReturn(Optional.of(account));
        when(userAccountRepository.save(account)).thenReturn(account);
        when(userMapper.toResponse(account)).thenReturn(response);


        com.example.courtierprobackend.user.presentationlayer.request.UpdateUserProfileRequest updateRequest = new com.example.courtierprobackend.user.presentationlayer.request.UpdateUserProfileRequest();
        updateRequest.setEmail("new@example.com");
        updateRequest.setPreferredLanguage("en");

        ResponseEntity<UserResponse> result = controller.updateCurrentUser(request, updateRequest);
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(emailChangeService).initiateEmailChange(account, "new@example.com");
        assertThat(account.isActive()).isFalse();
    }

    @Test
    void updateCurrentUser_NotificationPreferences() {
        UUID internalId = UUID.randomUUID();
        UserAccount account = new UserAccount();
        UserResponse response = UserResponse.builder().build();

        when(request.getAttribute(UserContextFilter.INTERNAL_USER_ID_ATTR)).thenReturn(internalId);
        when(userAccountRepository.findById(internalId)).thenReturn(Optional.of(account));
        when(userAccountRepository.save(account)).thenReturn(account);
        when(userMapper.toResponse(account)).thenReturn(response);


        com.example.courtierprobackend.user.presentationlayer.request.UpdateUserProfileRequest updateRequest = new com.example.courtierprobackend.user.presentationlayer.request.UpdateUserProfileRequest();
        updateRequest.setEmailNotificationsEnabled(true);
        updateRequest.setInAppNotificationsEnabled(false);
        updateRequest.setWeeklyDigestEnabled(true);
        updateRequest.setPreferredLanguage("en");

        ResponseEntity<UserResponse> result = controller.updateCurrentUser(request, updateRequest);
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(account.isEmailNotificationsEnabled()).isTrue();
        assertThat(account.isInAppNotificationsEnabled()).isFalse();
        assertThat(account.isWeeklyDigestEnabled()).isTrue();
    }

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

                com.example.courtierprobackend.user.presentationlayer.request.UpdateUserProfileRequest updateRequest = new com.example.courtierprobackend.user.presentationlayer.request.UpdateUserProfileRequest();
                updateRequest.setPreferredLanguage("en");

                ResponseEntity<UserResponse> result = controller.updateCurrentUser(request, updateRequest);

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

                com.example.courtierprobackend.user.presentationlayer.request.UpdateUserProfileRequest updateRequest = new com.example.courtierprobackend.user.presentationlayer.request.UpdateUserProfileRequest();
                updateRequest.setPreferredLanguage("fr");

                ResponseEntity<UserResponse> result = controller.updateCurrentUser(request, updateRequest);

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

                com.example.courtierprobackend.user.presentationlayer.request.UpdateUserProfileRequest updateRequest = new com.example.courtierprobackend.user.presentationlayer.request.UpdateUserProfileRequest();
                updateRequest.setPreferredLanguage("EN");

                controller.updateCurrentUser(request, updateRequest);

            assertThat(account.getPreferredLanguage()).isEqualTo("en");
        }

        @Test
        void updateCurrentUser_WithInvalidLanguage_ThrowsBadRequest() {
            UUID internalId = UUID.randomUUID();
            UserAccount account = new UserAccount();

            when(request.getAttribute(UserContextFilter.INTERNAL_USER_ID_ATTR)).thenReturn(internalId);
            when(userAccountRepository.findById(internalId)).thenReturn(Optional.of(account));

                com.example.courtierprobackend.user.presentationlayer.request.UpdateUserProfileRequest updateRequest = new com.example.courtierprobackend.user.presentationlayer.request.UpdateUserProfileRequest();
                updateRequest.setPreferredLanguage("de");

                assertThatThrownBy(() -> controller.updateCurrentUser(request, updateRequest))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Invalid language");
        }

        @Test
        void updateCurrentUser_WithNullLanguage_ThrowsBadRequest() {
            UUID internalId = UUID.randomUUID();
            UserAccount account = new UserAccount();

            when(request.getAttribute(UserContextFilter.INTERNAL_USER_ID_ATTR)).thenReturn(internalId);
            when(userAccountRepository.findById(internalId)).thenReturn(Optional.of(account));

                com.example.courtierprobackend.user.presentationlayer.request.UpdateUserProfileRequest updateRequest = new com.example.courtierprobackend.user.presentationlayer.request.UpdateUserProfileRequest();
                updateRequest.setPreferredLanguage(null);

                assertThatThrownBy(() -> controller.updateCurrentUser(request, updateRequest))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Invalid language");
        }

        @Test
        void updateCurrentUser_NoInternalId_ThrowsUnauthorized() {
            when(request.getAttribute(UserContextFilter.INTERNAL_USER_ID_ATTR)).thenReturn(null);

                com.example.courtierprobackend.user.presentationlayer.request.UpdateUserProfileRequest updateRequest = new com.example.courtierprobackend.user.presentationlayer.request.UpdateUserProfileRequest();
                updateRequest.setPreferredLanguage("en");

                assertThatThrownBy(() -> controller.updateCurrentUser(request, updateRequest))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessage("Authentication required");
        }

        @Test
        void updateCurrentUser_UserNotFound_ThrowsNotFound() {
            UUID internalId = UUID.randomUUID();
            when(request.getAttribute(UserContextFilter.INTERNAL_USER_ID_ATTR)).thenReturn(internalId);
            when(userAccountRepository.findById(internalId)).thenReturn(Optional.empty());

                com.example.courtierprobackend.user.presentationlayer.request.UpdateUserProfileRequest updateRequest = new com.example.courtierprobackend.user.presentationlayer.request.UpdateUserProfileRequest();
                updateRequest.setPreferredLanguage("en");

                assertThatThrownBy(() -> controller.updateCurrentUser(request, updateRequest))
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


            com.example.courtierprobackend.user.presentationlayer.request.UpdateUserProfileRequest updateRequest = new com.example.courtierprobackend.user.presentationlayer.request.UpdateUserProfileRequest();
            updateRequest.setPreferredLanguage("en");

            ResponseEntity<UserResponse> result = controller.updateCurrentUser(request, updateRequest);

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

                com.example.courtierprobackend.user.presentationlayer.request.UpdateUserProfileRequest updateRequest = new com.example.courtierprobackend.user.presentationlayer.request.UpdateUserProfileRequest();
                updateRequest.setPreferredLanguage("en");

                ResponseEntity<UserResponse> result = controller.updateCurrentUser(request, updateRequest);

            // Request should still succeed even if Auth0 sync fails
            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(account.getPreferredLanguage()).isEqualTo("en");
            verify(userAccountRepository).save(account);
            verify(auth0ManagementClient).updateUserLanguage("auth0|syncfail789", "en");
        }

        @Test
        void updateCurrentUser_PartialNotificationUpdates() {
            UUID internalId = UUID.randomUUID();
            UserAccount account = new UserAccount();
            account.setEmailNotificationsEnabled(true);
            account.setInAppNotificationsEnabled(true);
            account.setWeeklyDigestEnabled(false);
            UserResponse response = UserResponse.builder().build();

            when(request.getAttribute(UserContextFilter.INTERNAL_USER_ID_ATTR)).thenReturn(internalId);
            when(userAccountRepository.findById(internalId)).thenReturn(Optional.of(account));
            when(userAccountRepository.save(account)).thenReturn(account);
            when(userMapper.toResponse(account)).thenReturn(response);

            com.example.courtierprobackend.user.presentationlayer.request.UpdateUserProfileRequest updateRequest = new com.example.courtierprobackend.user.presentationlayer.request.UpdateUserProfileRequest();
            updateRequest.setEmailNotificationsEnabled(null);
            updateRequest.setInAppNotificationsEnabled(false);
            updateRequest.setWeeklyDigestEnabled(null);
            updateRequest.setPreferredLanguage("en");

            controller.updateCurrentUser(request, updateRequest);

            assertThat(account.isEmailNotificationsEnabled()).isTrue(); // Unchanged
            assertThat(account.isInAppNotificationsEnabled()).isFalse(); // Updated
            assertThat(account.isWeeklyDigestEnabled()).isFalse(); // Unchanged
        }
    }

    @Nested
    @DisplayName("GET /api/me/mfa-status")
    class GetMfaStatusTests {
        @Test
        void getMfaStatus_UserNotFoundInDb_FallbackToJwt() throws Exception {
            String auth0Id = "auth0|jwt_only";
            when(request.getAttribute(UserContextFilter.INTERNAL_USER_ID_ATTR)).thenReturn(UUID.randomUUID());
            when(userAccountRepository.findById(any(UUID.class))).thenReturn(Optional.empty());
            when(jwt.getClaimAsString("sub")).thenReturn(auth0Id);
            when(auth0ManagementClient.isMfaEnabled(auth0Id)).thenReturn(true);

            ResponseEntity<Map<String, Object>> result = controller.getMfaStatus(request, jwt);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody().get("mfaEnabled")).isEqualTo(true);
        }

        @Test
        void getMfaStatus_Auth0IdNotFound_ThrowsUnauthorized() {
            when(request.getAttribute(UserContextFilter.INTERNAL_USER_ID_ATTR)).thenReturn(null);
            
            assertThatThrownBy(() -> controller.getMfaStatus(request, null))
                    .isInstanceOf(UnauthorizedException.class);
        }

        @Test
        void getMfaStatus_Auth0SyncFails_ReturnsFalse() throws Exception {
            String auth0Id = "auth0|fail";
            UserAccount account = new UserAccount();
            account.setAuth0UserId(auth0Id);
            when(request.getAttribute(UserContextFilter.INTERNAL_USER_ID_ATTR)).thenReturn(UUID.randomUUID());
            when(userAccountRepository.findById(any(UUID.class))).thenReturn(Optional.of(account));
            when(auth0ManagementClient.isMfaEnabled(auth0Id)).thenThrow(new RuntimeException("API error"));

            ResponseEntity<Map<String, Object>> result = controller.getMfaStatus(request, jwt);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody().get("mfaEnabled")).isEqualTo(false);
        }
    }
}

