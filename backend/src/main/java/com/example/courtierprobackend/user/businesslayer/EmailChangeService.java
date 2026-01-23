package com.example.courtierprobackend.user.businesslayer;

import org.springframework.transaction.annotation.Transactional;
import com.example.courtierprobackend.user.dataaccesslayer.EmailChangeToken;
import com.example.courtierprobackend.user.dataaccesslayer.EmailChangeTokenRepository;
import com.example.courtierprobackend.user.dataaccesslayer.UserAccount;
import com.example.courtierprobackend.user.dataaccesslayer.UserAccountRepository;
import com.example.courtierprobackend.email.EmailService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class EmailChangeService {
    private final EmailChangeTokenRepository tokenRepository;
    private final UserAccountRepository userAccountRepository;
    private final EmailService emailService;
    private final com.example.courtierprobackend.transactions.datalayer.repositories.TransactionParticipantRepository transactionParticipantRepository;
    private final com.example.courtierprobackend.user.domainclientlayer.auth0.Auth0ManagementClient auth0ManagementClient;

    @Value("${app.emailChangeTokenExpiryMinutes:30}")
    private int tokenExpiryMinutes;

    public EmailChangeService(EmailChangeTokenRepository tokenRepository,
                              UserAccountRepository userAccountRepository,
                              EmailService emailService,
                              com.example.courtierprobackend.user.domainclientlayer.auth0.Auth0ManagementClient auth0ManagementClient,
                              com.example.courtierprobackend.transactions.datalayer.repositories.TransactionParticipantRepository transactionParticipantRepository) {
        this.tokenRepository = tokenRepository;
        this.userAccountRepository = userAccountRepository;
        this.emailService = emailService;
        this.auth0ManagementClient = auth0ManagementClient;
        this.transactionParticipantRepository = transactionParticipantRepository;
    }

    @Transactional
    public void initiateEmailChange(UserAccount user, String newEmail) {
        // Remove any existing tokens for this user
        tokenRepository.deleteByUserId(user.getId());
        // Generate token
        String token = UUID.randomUUID().toString();
        EmailChangeToken changeToken = new EmailChangeToken();
        changeToken.setUserId(user.getId());
        changeToken.setNewEmail(newEmail);
        changeToken.setToken(token);
        changeToken.setExpiresAt(Instant.now().plusSeconds(tokenExpiryMinutes * 60L));
        tokenRepository.save(changeToken);
        // Send confirmation email
        emailService.sendEmailChangeConfirmation(user, newEmail, token);
    }

    public boolean confirmEmailChange(String token) {
        Optional<EmailChangeToken> tokenOpt = tokenRepository.findByTokenAndUsedFalse(token);
        if (tokenOpt.isEmpty()) return false;
        EmailChangeToken changeToken = tokenOpt.get();
        if (changeToken.getExpiresAt().isBefore(Instant.now())) return false;
        UserAccount user = userAccountRepository.findById(changeToken.getUserId()).orElse(null);
        if (user == null) return false;
        // Update email in Auth0 first
        if (user.getAuth0UserId() != null && !user.getAuth0UserId().isBlank()) {
            auth0ManagementClient.updateUserEmail(user.getAuth0UserId(), changeToken.getNewEmail());
        }
        
        // Update all transaction participants that match the old email
        String oldEmail = user.getEmail();
        if (oldEmail != null && !oldEmail.isBlank()) {
           java.util.List<com.example.courtierprobackend.transactions.datalayer.TransactionParticipant> participants = transactionParticipantRepository.findByEmailIgnoreCase(oldEmail);
           for (com.example.courtierprobackend.transactions.datalayer.TransactionParticipant p : participants) {
               p.setEmail(changeToken.getNewEmail());
           }
           transactionParticipantRepository.saveAll(participants);
        }

        user.setEmail(changeToken.getNewEmail());
        user.setActive(true);
        userAccountRepository.save(user);
        changeToken.setUsed(true);
        tokenRepository.save(changeToken);
        return true;
    }
}
