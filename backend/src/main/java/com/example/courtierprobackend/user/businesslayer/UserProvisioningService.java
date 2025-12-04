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

        //  create a user and assign it a role
        UserRole role = UserRole.valueOf(request.getRole());

        // Create user in Auth0 and get back userId|passwordSetupUrl
        String result = auth0ManagementClient.createUser(
                request.getEmail(),
                request.getFirstName(),
                request.getLastName(),
                role
        );

        // Parse the result
        String[] parts = result.split("\\|", 2);
        String auth0UserId = parts[0];
        String passwordSetupUrl = parts.length > 1 ? parts[1] : null;

        // Send email with password setup link
        if (passwordSetupUrl != null) {
            emailService.sendPasswordSetupEmail(request.getEmail(), passwordSetupUrl);
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
