package com.example.courtierprobackend.email;

import com.example.courtierprobackend.Organization.presentationlayer.model.OrganizationSettingsResponseModel;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import static org.mockito.Mockito.*;

class EmailServicePropertyOfferNotificationsTest {
    @Test
    void sendPropertyOfferMadeNotification_sendsEmail() throws Exception {
        EmailService service = spy(new EmailService("a", "b", mock(com.example.courtierprobackend.Organization.businesslayer.OrganizationSettingsService.class), null));
        doReturn(true).when(service).sendEmail(anyString(), anyString(), anyString());
        OrganizationSettingsResponseModel settings = OrganizationSettingsResponseModel.builder()
                .propertyOfferMadeSubjectEn("Offer Made")
                .propertyOfferMadeBodyEn("Body {{clientName}} {{brokerName}} {{propertyAddress}} {{offerAmount}} {{offerRound}}")
                .propertyOfferMadeSubjectFr("Offre faite")
                .propertyOfferMadeBodyFr("Corps {{clientName}} {{brokerName}} {{propertyAddress}} {{offerAmount}} {{offerRound}}")
                .build();
        when(service.organizationSettingsService.getSettings()).thenReturn(settings);
        service.sendPropertyOfferMadeNotification("to@x.com", "Client", "Broker", "Addr", "100000", 1, "en");
        verify(service).sendEmail(anyString(), anyString(), anyString());
    }

    @Test
    void sendPropertyOfferStatusChangedNotification_sendsEmail() throws Exception {
        EmailService service = spy(new EmailService("a", "b", mock(com.example.courtierprobackend.Organization.businesslayer.OrganizationSettingsService.class), null));
        doReturn(true).when(service).sendEmail(anyString(), anyString(), anyString());
        OrganizationSettingsResponseModel settings = OrganizationSettingsResponseModel.builder()
                .propertyOfferStatusSubjectEn("Offer Status")
                .propertyOfferStatusBodyEn("Body {{clientName}} {{brokerName}} {{propertyAddress}} {{previousStatus}} {{newStatus}} {{counterpartyResponse}}")
                .propertyOfferStatusSubjectFr("Statut d'offre")
                .propertyOfferStatusBodyFr("Corps {{clientName}} {{brokerName}} {{propertyAddress}} {{previousStatus}} {{newStatus}} {{counterpartyResponse}}")
                .build();
        when(service.organizationSettingsService.getSettings()).thenReturn(settings);
        service.sendPropertyOfferStatusChangedNotification("to@x.com", "Client", "Broker", "Addr", "PENDING", "ACCEPTED", "Counter", "en");
        verify(service).sendEmail(anyString(), anyString(), anyString());
    }
}
