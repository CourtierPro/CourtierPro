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
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
public class UserProvisioningService {

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

        // Determine role from request
        UserRole role = UserRole.valueOf(request.getRole());

        // Load organization settings (default language, templates, etc.)
        OrganizationSettingsResponseModel settings = organizationSettingsService.getSettings();

        // Effective language: user choice > org default > "en"
        String effectiveLanguage = request.getPreferredLanguage();
        if (effectiveLanguage == null || effectiveLanguage.isBlank()) {
            effectiveLanguage = settings.getDefaultLanguage();
        }
        if (effectiveLanguage == null || effectiveLanguage.isBlank()) {
            effectiveLanguage = "en";
        }

        // Create user in Auth0 and get back "auth0UserId|passwordSetupUrl"
        String result = auth0ManagementClient.createUser(
                request.getEmail(),
                request.getFirstName(),
                request.getLastName(),
                role,
                effectiveLanguage
        );

        // We use the *last* '|' as separator so it works even if the auth0UserId contains a '|'
        String auth0UserId;
        String passwordSetupUrl = null;

        int separatorIndex = result.lastIndexOf('|');
        if (separatorIndex == -1) {
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

        // Send invitation email if we have a password setup URL
        if (passwordSetupUrl != null) {
            boolean emailSent = emailService.sendPasswordSetupEmail(
                    request.getEmail(),
                    passwordSetupUrl,
                    effectiveLanguage
            );

            if (!emailSent) {
                logger.warn(
                        "User created (Auth0 id: {}) but password setup email could not be sent to {}",
                        auth0UserId,
                        request.getEmail()
                );
            }
        }

        // Create local user record
        UserAccount account = userMapper.toNewUserEntity(request, auth0UserId);

        // Persist preferred language if missing on the entity
        if (account.getPreferredLanguage() == null || account.getPreferredLanguage().isBlank()) {
            account.setPreferredLanguage(effectiveLanguage);
        }

        UserAccount saved = userAccountRepository.save(account);

        return userMapper.toResponse(saved);
    }


    public UserResponse updateStatus(UUID userId, UpdateStatusRequest request) {
        UserAccount account = userAccountRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "User with id " + userId + " not found"));

        boolean active = request.getActive();
        account.setActive(active);
        UserAccount saved = userAccountRepository.save(account);

        // Sync with Auth0: active=false -> blocked=true
        auth0ManagementClient.setBlocked(account.getAuth0UserId(), !active);

        return userMapper.toResponse(saved);
    }
}
