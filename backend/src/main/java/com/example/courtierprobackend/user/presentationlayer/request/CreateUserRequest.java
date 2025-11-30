package com.example.courtierprobackend.user.presentationlayer.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class CreateUserRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Email format is invalid")
    String email;

    @NotBlank(message = "First name is required")
    String firstName;

    @NotBlank(message = "Last name is required")
    String lastName;

    @NotBlank(message = "Role is required")
    @Pattern(
            regexp = "ADMIN|BROKER|CLIENT",
            message = "Role must be one of: ADMIN, BROKER, CLIENT"
    )
    String role;

    @NotBlank(message = "Preferred language is required")
    @Pattern(
            regexp = "en|fr",
            message = "Preferred language must be 'en' or 'fr'"
    )
    String preferredLanguage;
}
