package com.example.courtierprobackend.Organization.datamapperlayer;

import com.example.courtierprobackend.Organization.dataccesslayer.OrganizationSettings;
import com.example.courtierprobackend.Organization.presentationlayer.model.OrganizationSettingsResponseModel;
import com.example.courtierprobackend.Organization.presentationlayer.model.UpdateOrganizationSettingsRequestModel;
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
                .documentSignatureRequestedSubjectEn(entity.getDocumentSignatureRequestedSubjectEn())
                .documentSignatureRequestedBodyEn(entity.getDocumentSignatureRequestedBodyEn())
                .documentSignatureRequestedSubjectFr(entity.getDocumentSignatureRequestedSubjectFr())
                .documentSignatureRequestedBodyFr(entity.getDocumentSignatureRequestedBodyFr())
                .documentReviewSubjectEn(entity.getDocumentReviewSubjectEn())
                .documentReviewBodyEn(entity.getDocumentReviewBodyEn())
                .documentReviewSubjectFr(entity.getDocumentReviewSubjectFr())
                .documentReviewBodyFr(entity.getDocumentReviewBodyFr())
                .stageUpdateSubjectEn(entity.getStageUpdateSubjectEn())
                .stageUpdateBodyEn(entity.getStageUpdateBodyEn())
                .stageUpdateSubjectFr(entity.getStageUpdateSubjectFr())
                .stageUpdateBodyFr(entity.getStageUpdateBodyFr())
                .propertyOfferMadeSubjectEn(entity.getPropertyOfferMadeSubjectEn())
                .propertyOfferMadeBodyEn(entity.getPropertyOfferMadeBodyEn())
                .propertyOfferMadeSubjectFr(entity.getPropertyOfferMadeSubjectFr())
                .propertyOfferMadeBodyFr(entity.getPropertyOfferMadeBodyFr())
                .propertyOfferStatusSubjectEn(entity.getPropertyOfferStatusSubjectEn())
                .propertyOfferStatusBodyEn(entity.getPropertyOfferStatusBodyEn())
                .propertyOfferStatusSubjectFr(entity.getPropertyOfferStatusSubjectFr())
                .propertyOfferStatusBodyFr(entity.getPropertyOfferStatusBodyFr())
                .offerReceivedSubjectEn(entity.getOfferReceivedSubjectEn())
                .offerReceivedBodyEn(entity.getOfferReceivedBodyEn())
                .offerReceivedSubjectFr(entity.getOfferReceivedSubjectFr())
                .offerReceivedBodyFr(entity.getOfferReceivedBodyFr())
                .offerStatusSubjectEn(entity.getOfferStatusSubjectEn())
                .offerStatusBodyEn(entity.getOfferStatusBodyEn())
                .offerStatusSubjectFr(entity.getOfferStatusSubjectFr())
                .offerStatusBodyFr(entity.getOfferStatusBodyFr())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    /**
     * Copy all updatable fields from request into the existing settings entity.
     */
    public void updateEntityFromRequest(UpdateOrganizationSettingsRequestModel request, OrganizationSettings settings) {
        if (request == null || settings == null) {
            return;
        }

        settings.setDefaultLanguage(request.getDefaultLanguage());

        settings.setInviteSubjectEn(request.getInviteSubjectEn());
        settings.setInviteBodyEn(request.getInviteBodyEn());
        settings.setInviteSubjectFr(request.getInviteSubjectFr());
        settings.setInviteBodyFr(request.getInviteBodyFr());

        settings.setDocumentSubmittedSubjectEn(request.getDocumentSubmittedSubjectEn());
        settings.setDocumentSubmittedBodyEn(request.getDocumentSubmittedBodyEn());
        settings.setDocumentSubmittedSubjectFr(request.getDocumentSubmittedSubjectFr());
        settings.setDocumentSubmittedBodyFr(request.getDocumentSubmittedBodyFr());

        settings.setDocumentRequestedSubjectEn(request.getDocumentRequestedSubjectEn());
        settings.setDocumentRequestedBodyEn(request.getDocumentRequestedBodyEn());
        settings.setDocumentRequestedSubjectFr(request.getDocumentRequestedSubjectFr());
        settings.setDocumentRequestedBodyFr(request.getDocumentRequestedBodyFr());

        settings.setDocumentSignatureRequestedSubjectEn(request.getDocumentSignatureRequestedSubjectEn());
        settings.setDocumentSignatureRequestedBodyEn(request.getDocumentSignatureRequestedBodyEn());
        settings.setDocumentSignatureRequestedSubjectFr(request.getDocumentSignatureRequestedSubjectFr());
        settings.setDocumentSignatureRequestedBodyFr(request.getDocumentSignatureRequestedBodyFr());

        settings.setDocumentReviewSubjectEn(request.getDocumentReviewSubjectEn());
        settings.setDocumentReviewBodyEn(request.getDocumentReviewBodyEn());
        settings.setDocumentReviewSubjectFr(request.getDocumentReviewSubjectFr());
        settings.setDocumentReviewBodyFr(request.getDocumentReviewBodyFr());

        settings.setStageUpdateSubjectEn(request.getStageUpdateSubjectEn());
        settings.setStageUpdateBodyEn(request.getStageUpdateBodyEn());
        settings.setStageUpdateSubjectFr(request.getStageUpdateSubjectFr());
        settings.setStageUpdateBodyFr(request.getStageUpdateBodyFr());

        settings.setPropertyOfferMadeSubjectEn(request.getPropertyOfferMadeSubjectEn());
        settings.setPropertyOfferMadeBodyEn(request.getPropertyOfferMadeBodyEn());
        settings.setPropertyOfferMadeSubjectFr(request.getPropertyOfferMadeSubjectFr());
        settings.setPropertyOfferMadeBodyFr(request.getPropertyOfferMadeBodyFr());

        settings.setPropertyOfferStatusSubjectEn(request.getPropertyOfferStatusSubjectEn());
        settings.setPropertyOfferStatusBodyEn(request.getPropertyOfferStatusBodyEn());
        settings.setPropertyOfferStatusSubjectFr(request.getPropertyOfferStatusSubjectFr());
        settings.setPropertyOfferStatusBodyFr(request.getPropertyOfferStatusBodyFr());

        settings.setOfferReceivedSubjectEn(request.getOfferReceivedSubjectEn());
        settings.setOfferReceivedBodyEn(request.getOfferReceivedBodyEn());
        settings.setOfferReceivedSubjectFr(request.getOfferReceivedSubjectFr());
        settings.setOfferReceivedBodyFr(request.getOfferReceivedBodyFr());

        settings.setOfferStatusSubjectEn(request.getOfferStatusSubjectEn());
        settings.setOfferStatusBodyEn(request.getOfferStatusBodyEn());
        settings.setOfferStatusSubjectFr(request.getOfferStatusSubjectFr());
        settings.setOfferStatusBodyFr(request.getOfferStatusBodyFr());
    }
}
