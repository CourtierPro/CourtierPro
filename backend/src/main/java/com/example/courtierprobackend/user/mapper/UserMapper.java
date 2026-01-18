package com.example.courtierprobackend.user.mapper;

import com.example.courtierprobackend.user.dataaccesslayer.UserAccount;
import com.example.courtierprobackend.user.dataaccesslayer.UserRole;
import com.example.courtierprobackend.user.presentationlayer.request.CreateUserRequest;
import com.example.courtierprobackend.user.presentationlayer.response.UserResponse;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {


    public UserAccount toNewUserEntity(CreateUserRequest request, String auth0UserId) {
        UserRole role = UserRole.valueOf(request.getRole()); // "BROKER" -> UserRole.BROKER

        return new UserAccount(
                auth0UserId,
                request.getEmail(),
                request.getFirstName(),
                request.getLastName(),
                role,
                request.getPreferredLanguage()
        );
    }


    public UserResponse toResponse(UserAccount account) {
        return UserResponse.builder()
                .id(account.getId().toString())  // Internal UUID - used for all database operations
                .email(account.getEmail())
                .firstName(account.getFirstName())
                .lastName(account.getLastName())
                .role(account.getRole().name())
                .active(account.isActive())
                .preferredLanguage(account.getPreferredLanguage())
                .emailNotificationsEnabled(account.isEmailNotificationsEnabled())
                .inAppNotificationsEnabled(account.isInAppNotificationsEnabled())
                .build();
    }
}
