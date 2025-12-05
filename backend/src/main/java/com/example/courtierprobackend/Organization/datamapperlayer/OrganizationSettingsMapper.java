package com.example.courtierprobackend.Organization.datamapperlayer;

import com.example.courtierprobackend.Organization.dataccesslayer.OrganizationSettings;
import com.example.courtierprobackend.Organization.presentationlayer.model.OrganizationSettingsResponseModel;
import org.springframework.stereotype.Component;

@Component
public class OrganizationSettingsMapper {

    public OrganizationSettingsResponseModel toResponseModel(OrganizationSettings entity) {

        if (entity == null) {
            return null;
        }

        return OrganizationSettingsResponseModel.builder()
                .id(entity.getId())
                .defaultLanguage(entity.getDefaultLanguage())
                .inviteSubjectEn(entity.getInviteSubjectEn())
                .inviteBodyEn(entity.getInviteBodyEn())
                .inviteSubjectFr(entity.getInviteSubjectFr())
                .inviteBodyFr(entity.getInviteBodyFr())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
