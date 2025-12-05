package com.example.courtierprobackend.user.businesslayer;

import com.example.courtierprobackend.Organization.businesslayer.OrganizationSettingsService;
import com.example.courtierprobackend.Organization.presentationlayer.model.OrganizationSettingsResponseModel;
import com.example.courtierprobackend.datamapperlayer.UserMapper;
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

    public UserProvisioningService(UserAccountRepository userAccountRepository,
                                   Auth0ManagementClient auth0ManagementClient,
                                   UserMapper userMapper,
                                   OrganizationSettingsService organizationSettingsService) {
        this.userAccountRepository = userAccountRepository;
        this.auth0ManagementClient = auth0ManagementClient;
        this.userMapper = userMapper;
        this.organizationSettingsService = organizationSettingsService;
    }

    public List<UserResponse> getAllUsers() {
        return userAccountRepository.findAll()
                .stream()
                .map(userMapper::toResponse)
                .toList();
    }

    public UserResponse createUser(CreateUserRequest request) {

        UserRole role = UserRole.valueOf(request.getRole());

        String auth0UserId = auth0ManagementClient.createUser(
                request.getEmail(),
                request.getFirstName(),
                request.getLastName(),
                role
        );

        // Read organization defaults
        OrganizationSettingsResponseModel settings = organizationSettingsService.getSettings();

        // Determine final language: request or org default
        String requestedLanguage = request.getPreferredLanguage();
        String effectiveLanguage =
                (requestedLanguage != null && !requestedLanguage.isBlank())
                        ? requestedLanguage
                        : settings.getDefaultLanguage();

        // Map request to entity
        UserAccount account = userMapper.toNewUserEntity(request, auth0UserId);

        // Apply language if missing on the mapped entity
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

        // Synchronize with Auth0: if active=false -> blocked=true
        auth0ManagementClient.setBlocked(account.getAuth0UserId(), !active);

        return userMapper.toResponse(saved);
    }
}
