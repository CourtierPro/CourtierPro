package com.example.courtierprobackend.user.presentationlayer.request;

import lombok.Data;

@Data
public class UpdateUserProfileRequest {
    private String email;
    private Boolean emailNotificationsEnabled;
    private Boolean inAppNotificationsEnabled;
    private String preferredLanguage;
}
