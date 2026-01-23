package com.example.courtierprobackend.user.businesslayer;

import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailChangeServiceTest {
    @Mock
    private com.example.courtierprobackend.user.dataaccesslayer.EmailChangeTokenRepository tokenRepository;
    @Mock
    private com.example.courtierprobackend.user.dataaccesslayer.UserAccountRepository userAccountRepository;
    @Mock
    private com.example.courtierprobackend.email.EmailService emailService;
    @Mock
    private com.example.courtierprobackend.user.domainclientlayer.auth0.Auth0ManagementClient auth0ManagementClient;
    @Mock
    private com.example.courtierprobackend.transactions.datalayer.repositories.TransactionParticipantRepository transactionParticipantRepository;

    @InjectMocks
    private EmailChangeService emailChangeService;


    @Test
    void testInitiateEmailChange() {
        var user = new com.example.courtierprobackend.user.dataaccesslayer.UserAccount();
        java.util.UUID userId = java.util.UUID.randomUUID();
        user.setId(userId);
        String newEmail = "new@example.com";

        // Mock deleteByUserId and save
        doNothing().when(tokenRepository).deleteByUserId(userId);
        when(tokenRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // Mock emailService
        doNothing().when(emailService).sendEmailChangeConfirmation(eq(user), eq(newEmail), anyString());

        emailChangeService.initiateEmailChange(user, newEmail);

        verify(tokenRepository).deleteByUserId(userId);
        verify(tokenRepository).save(argThat(token ->
                token.getUserId().equals(userId)
                        && token.getNewEmail().equals(newEmail)
                        && token.getToken() != null
                        && token.getExpiresAt() != null
                        && !token.isUsed()
        ));
        verify(emailService).sendEmailChangeConfirmation(eq(user), eq(newEmail), anyString());
    }

    @Test
    void testConfirmEmailChange_tokenNotFound() {
        when(tokenRepository.findByTokenAndUsedFalse("tok")).thenReturn(java.util.Optional.empty());
        assertFalse(emailChangeService.confirmEmailChange("tok"));
    }

    @Test
    void testConfirmEmailChange_tokenExpired() {
        var token = new com.example.courtierprobackend.user.dataaccesslayer.EmailChangeToken();
        token.setExpiresAt(java.time.Instant.now().minusSeconds(60));
        token.setUserId(java.util.UUID.randomUUID());
        when(tokenRepository.findByTokenAndUsedFalse("tok")).thenReturn(java.util.Optional.of(token));
        assertFalse(emailChangeService.confirmEmailChange("tok"));
    }

    @Test
    void testConfirmEmailChange_userNotFound() {
        var token = new com.example.courtierprobackend.user.dataaccesslayer.EmailChangeToken();
        token.setExpiresAt(java.time.Instant.now().plusSeconds(60));
        java.util.UUID userId = java.util.UUID.randomUUID();
        token.setUserId(userId);
        when(tokenRepository.findByTokenAndUsedFalse("tok")).thenReturn(java.util.Optional.of(token));
        when(userAccountRepository.findById(userId)).thenReturn(java.util.Optional.empty());
        assertFalse(emailChangeService.confirmEmailChange("tok"));
    }

    @Test
    void testConfirmEmailChange_auth0UpdateAndSuccess() {
        var token = new com.example.courtierprobackend.user.dataaccesslayer.EmailChangeToken();
        token.setExpiresAt(java.time.Instant.now().plusSeconds(60));
        java.util.UUID userId = java.util.UUID.randomUUID();
        token.setUserId(userId);
        token.setNewEmail("new@email.com");
        when(tokenRepository.findByTokenAndUsedFalse("tok")).thenReturn(java.util.Optional.of(token));

        var user = new com.example.courtierprobackend.user.dataaccesslayer.UserAccount();
        user.setId(userId);
        user.setAuth0UserId("auth0|id");
        user.setEmail("old@email.com");
        user.setActive(false);
        when(userAccountRepository.findById(userId)).thenReturn(java.util.Optional.of(user));

        doNothing().when(auth0ManagementClient).updateUserEmail(eq("auth0|id"), eq("new@email.com"));
        when(userAccountRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(tokenRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        boolean result = emailChangeService.confirmEmailChange("tok");
        assertTrue(result);
        assertEquals("new@email.com", user.getEmail());
        assertTrue(user.isActive());
        verify(auth0ManagementClient).updateUserEmail(eq("auth0|id"), eq("new@email.com"));
        verify(userAccountRepository).save(user);
        verify(tokenRepository).save(token);
    }

    @Test
    void testConfirmEmailChange_noAuth0Id() {
        var token = new com.example.courtierprobackend.user.dataaccesslayer.EmailChangeToken();
        token.setExpiresAt(java.time.Instant.now().plusSeconds(60));
        java.util.UUID userId = java.util.UUID.randomUUID();
        token.setUserId(userId);
        token.setNewEmail("new@email.com");
        when(tokenRepository.findByTokenAndUsedFalse("tok")).thenReturn(java.util.Optional.of(token));

        var user = new com.example.courtierprobackend.user.dataaccesslayer.UserAccount();
        user.setId(userId);
        user.setAuth0UserId(null);
        user.setEmail("old@email.com");
        user.setActive(false);
        when(userAccountRepository.findById(userId)).thenReturn(java.util.Optional.of(user));

        when(userAccountRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(tokenRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        boolean result = emailChangeService.confirmEmailChange("tok");
        assertTrue(result);
        assertEquals("new@email.com", user.getEmail());
        assertTrue(user.isActive());
        verify(auth0ManagementClient, never()).updateUserEmail(anyString(), anyString());
        verify(userAccountRepository).save(user);
        verify(tokenRepository).save(token);
    }
}
