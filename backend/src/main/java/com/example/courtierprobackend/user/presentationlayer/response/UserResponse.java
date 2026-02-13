package com.example.courtierprobackend.user.presentationlayer.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class UserResponse {

    String id;
    String email;
    String firstName;
    String lastName;
    String role;
    boolean active;
    String preferredLanguage;

    boolean emailNotificationsEnabled;
    boolean inAppNotificationsEnabled;
    boolean weeklyDigestEnabled;
}
