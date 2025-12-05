package com.example.courtierprobackend.user.businesslayer;

import com.example.courtierprobackend.Organization.businesslayer.OrganizationSettingsService;
import com.example.courtierprobackend.Organization.presentationlayer.model.OrganizationSettingsResponseModel;
import com.example.courtierprobackend.datamapperlayer.UserMapper;
import com.example.courtierprobackend.email.EmailService;
import com.example.courtierprobackend.user.dataaccesslayer.UserAccount;
import com.example.courtierprobackend.user.dataaccesslayer.UserAccountRepository;
import com.example.courtierprobackend.user.dataaccesslayer.UserRole;
import com.example.courtierprobackend.user.domainclientlayer.auth0.Auth0ManagementClient;
import com.example.courtierprobackend.user.presentationlayer.request.CreateUserRequest;
import com.example.courtierprobackend.user.presentationlayer.request.UpdateStatusRequest;
import com.example.courtierprobackend.user.presentationlayer.response.UserResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
public class UserProvisioningService {

    private static final Logger logger =
            LoggerFactory.getLogger(UserProvisioningService.class);

    private final UserAccountRepository userAccountRepository;
    private final Auth0ManagementClient auth0ManagementClient;
    private final UserMapper userMapper;
    private final OrganizationSettingsService organizationSettingsService;
    private final EmailService emailService;

    public UserProvisioningService(UserAccountRepository userAccountRepository,
                                   Auth0ManagementClient auth0ManagementClient,
                                   UserMapper userMapper,
                                   OrganizationSettingsService organizationSettingsService,
                                   EmailService emailService) {
        this.userAccountRepository = userAccountRepository;
        this.auth0ManagementClient = auth0ManagementClient;
        this.userMapper = userMapper;
        this.organizationSettingsService = organizationSettingsService;
        this.emailService = emailService;
    }

    public List<UserResponse> getAllUsers() {
        return userAccountRepository.findAll()
                .stream()
                .map(userMapper::toResponse)
                .toList();
    }

    public UserResponse createUser(CreateUserRequest request) {

        // Déterminer le rôle
        UserRole role = UserRole.valueOf(request.getRole());

        // Créer l'utilisateur dans Auth0 et récupérer "auth0UserId|passwordSetupUrl"
        String result = auth0ManagementClient.createUser(
                request.getEmail(),
                request.getFirstName(),
                request.getLastName(),
                role
        );

        // --- Parse du résultat ---
        String auth0UserId;
        String passwordSetupUrl = null;

        int separatorIndex = result.lastIndexOf('|');
        if (separatorIndex == -1) {
            // Pas de separator – on assume que seulement le user id est retourné
            auth0UserId = result;
            logger.warn(
                    "Auth0 createUser result did not contain a password setup URL separator '|'. Value was: {}",
                    result
            );
        } else {
            auth0UserId = result.substring(0, separatorIndex);

            if (separatorIndex < result.length() - 1) {
                passwordSetupUrl = result.substring(separatorIndex + 1);
            }
        }
        // --- fin parse ---

        // Lire les paramètres d'organisation (pour la langue par défaut)
        OrganizationSettingsResponseModel settings = organizationSettingsService.getSettings();

        String requestedLanguage = request.getPreferredLanguage();
        String effectiveLanguage =
                (requestedLanguage != null && !requestedLanguage.isBlank())
                        ? requestedLanguage
                        : settings.getDefaultLanguage();

        // Envoyer le courriel avec le lien de setup de mot de passe (si présent)
        if (passwordSetupUrl != null) {
            boolean emailSent = emailService.sendPasswordSetupEmail(
                    request.getEmail(),
                    passwordSetupUrl
            );

            if (!emailSent) {
                logger.warn(
                        "User created (Auth0 id: {}) but password setup email could not be sent to {}",
                        auth0UserId,
                        request.getEmail()
                );
            }
        }

        // Créer l'entité locale
        UserAccount account = userMapper.toNewUserEntity(request, auth0UserId);

        // Appliquer la langue si manquante sur l'entité mappée
        if (account.getPreferredLanguage() == null || account.getPreferredLanguage().isBlank()) {
            account.setPreferredLanguage(effectiveLanguage);
        }

        UserAccount saved = userAccountRepository.save(account);

        return userMapper.toResponse(saved);
    }

    public UserResponse updateStatus(UUID userId, UpdateStatusRequest request) {
        UserAccount account = userAccountRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User with id " + userId + " not found"
                ));

        boolean active = request.getActive();
        account.setActive(active);
        UserAccount saved = userAccountRepository.save(account);

        // Synchroniser avec Auth0: si active=false -> blocked=true
        auth0ManagementClient.setBlocked(account.getAuth0UserId(), !active);

        return userMapper.toResponse(saved);
    }
}
