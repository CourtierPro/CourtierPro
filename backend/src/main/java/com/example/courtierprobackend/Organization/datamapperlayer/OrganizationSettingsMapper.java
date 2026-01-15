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
                .documentSubmittedSubjectEn(entity.getDocumentSubmittedSubjectEn())
                .documentSubmittedBodyEn(entity.getDocumentSubmittedBodyEn())
                .documentSubmittedSubjectFr(entity.getDocumentSubmittedSubjectFr())
                .documentSubmittedBodyFr(entity.getDocumentSubmittedBodyFr())
                .documentRequestedSubjectEn(entity.getDocumentRequestedSubjectEn())
                .documentRequestedBodyEn(entity.getDocumentRequestedBodyEn())
                .documentRequestedSubjectFr(entity.getDocumentRequestedSubjectFr())
                .documentRequestedBodyFr(entity.getDocumentRequestedBodyFr())
                .documentReviewSubjectEn(entity.getDocumentReviewSubjectEn())
                .documentReviewBodyEn(entity.getDocumentReviewBodyEn())
                .documentReviewSubjectFr(entity.getDocumentReviewSubjectFr())
                .documentReviewBodyFr(entity.getDocumentReviewBodyFr())
                .stageUpdateSubjectEn(entity.getStageUpdateSubjectEn())
                .stageUpdateBodyEn(entity.getStageUpdateBodyEn())
                .stageUpdateSubjectFr(entity.getStageUpdateSubjectFr())
                .stageUpdateBodyFr(entity.getStageUpdateBodyFr())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
