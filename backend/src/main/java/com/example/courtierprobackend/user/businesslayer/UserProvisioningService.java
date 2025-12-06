package com.example.courtierprobackend.user.businesslayer;

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
    private final EmailService emailService;

    public UserProvisioningService(UserAccountRepository userAccountRepository,
                                   Auth0ManagementClient auth0ManagementClient,
                                   UserMapper userMapper,
                                   EmailService emailService) {
        this.userAccountRepository = userAccountRepository;
        this.auth0ManagementClient = auth0ManagementClient;
        this.userMapper = userMapper;
        this.emailService = emailService;
    }

    public List<UserResponse> getAllUsers() {
        return userAccountRepository.findAll()
                .stream()
                .map(userMapper::toResponse)
                .toList();
    }

    public UserResponse createUser(CreateUserRequest request) {

        // Determine role
        UserRole role = UserRole.valueOf(request.getRole());

        // Create user in Auth0 and get back "auth0UserId|passwordSetupUrl"
        String result = auth0ManagementClient.createUser(
                request.getEmail(),
                request.getFirstName(),
                request.getLastName(),
                role
        );

        // --- Parse the result safely ---
        // We use the *last* '|' as separator so it works even if the auth0UserId contains a '|'
        String auth0UserId;
        String passwordSetupUrl = null;

        int separatorIndex = result.lastIndexOf('|');
        if (separatorIndex == -1) {
            // No separator – assume only the user id was returned
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
        // --- end parsing ---

        // Send email with password setup link (if we have one)
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

        // Create local user record
        UserAccount account = userMapper.toNewUserEntity(request, auth0UserId);
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
