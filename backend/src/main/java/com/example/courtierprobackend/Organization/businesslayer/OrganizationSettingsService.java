// backend/src/main/java/com/example/courtierprobackend/Organization/businesslayer/OrganizationSettingsService.java
package com.example.courtierprobackend.Organization.businesslayer;

import com.example.courtierprobackend.Organization.presentationlayer.model.OrganizationSettingsResponseModel;
import com.example.courtierprobackend.Organization.presentationlayer.model.UpdateOrganizationSettingsRequestModel;

public interface OrganizationSettingsService {

    OrganizationSettingsResponseModel getSettings();

    OrganizationSettingsResponseModel updateSettings(
            UpdateOrganizationSettingsRequestModel request
    );
}
