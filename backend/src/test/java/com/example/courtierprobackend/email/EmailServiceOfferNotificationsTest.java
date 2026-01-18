package com.example.courtierprobackend.email;

import com.example.courtierprobackend.Organization.presentationlayer.model.OrganizationSettingsResponseModel;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import static org.mockito.Mockito.*;

class EmailServiceOfferNotificationsTest {
    @Test
    void sendOfferReceivedNotification_sendsEmail() throws Exception {
        EmailService service = spy(new EmailService("a", "b", mock(com.example.courtierprobackend.Organization.businesslayer.OrganizationSettingsService.class), null));
        doReturn(true).when(service).sendEmail(anyString(), anyString(), anyString());
        OrganizationSettingsResponseModel settings = OrganizationSettingsResponseModel.builder()
                .offerReceivedSubjectEn("Offer Received")
                .offerReceivedBodyEn("Body {{clientName}} {{buyerName}} {{offerAmount}}")
                .offerReceivedSubjectFr("Offre reçue")
                .offerReceivedBodyFr("Corps {{clientName}} {{buyerName}} {{offerAmount}}")
                .build();
        when(service.organizationSettingsService.getSettings()).thenReturn(settings);
        service.sendOfferReceivedNotification("to@x.com", "Client", "Broker", "Buyer", "100000", "en");
        verify(service).sendEmail(anyString(), anyString(), anyString());
    }

    @Test
    void sendOfferStatusChangedNotification_sendsEmail() throws Exception {
        EmailService service = spy(new EmailService("a", "b", mock(com.example.courtierprobackend.Organization.businesslayer.OrganizationSettingsService.class), null));
        doReturn(true).when(service).sendEmail(anyString(), anyString(), anyString());
        OrganizationSettingsResponseModel settings = OrganizationSettingsResponseModel.builder()
                .offerStatusSubjectEn("Offer Status")
                .offerStatusBodyEn("Body {{clientName}} {{buyerName}} {{previousStatus}} {{newStatus}}")
                .offerStatusSubjectFr("Statut d'offre")
                .offerStatusBodyFr("Corps {{clientName}} {{buyerName}} {{previousStatus}} {{newStatus}}")
                .build();
        when(service.organizationSettingsService.getSettings()).thenReturn(settings);
        service.sendOfferStatusChangedNotification("to@x.com", "Client", "Broker", "Buyer", "PENDING", "ACCEPTED", "en");
        verify(service).sendEmail(anyString(), anyString(), anyString());
    }
}
