package com.example.courtierprobackend.user.presentationlayer.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

import java.util.UUID;

@Value
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class UserResponse {

    UUID id;
    String email;
    String firstName;
    String lastName;
    String role;
    boolean active;
    String preferredLanguage;
}
