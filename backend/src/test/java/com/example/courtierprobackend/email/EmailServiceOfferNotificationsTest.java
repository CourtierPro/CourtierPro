package com.example.courtierprobackend.email;

import com.example.courtierprobackend.Organization.presentationlayer.model.OrganizationSettingsResponseModel;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;

class EmailServiceOfferNotificationsTest {

    // ========== sendOfferReceivedNotification Tests ==========

    @Test
    void sendOfferReceivedNotification_englishPath() throws Exception {
        var orgSettingsService = mock(com.example.courtierprobackend.Organization.businesslayer.OrganizationSettingsService.class);
        EmailService service = spy(new EmailService("a", "b", orgSettingsService, null));
        doReturn(true).when(service).sendEmail(anyString(), anyString(), anyString());
        
        OrganizationSettingsResponseModel settings = OrganizationSettingsResponseModel.builder()
                .offerReceivedSubjectEn("Offer Received")
                .offerReceivedBodyEn("Body {{clientName}} {{buyerName}} {{offerAmount}}")
                .offerReceivedSubjectFr("Offre reçue")
                .offerReceivedBodyFr("Corps {{clientName}} {{buyerName}} {{offerAmount}}")
                .build();
        when(orgSettingsService.getSettings()).thenReturn(settings);
        
        service.sendOfferReceivedNotification("to@x.com", "Client", "Broker", "Buyer", "100000", "en");
        verify(service).sendEmail(anyString(), anyString(), anyString());
    }

    @Test
    void sendOfferReceivedNotification_frenchPath() throws Exception {
        var orgSettingsService = mock(com.example.courtierprobackend.Organization.businesslayer.OrganizationSettingsService.class);
        EmailService service = spy(new EmailService("a", "b", orgSettingsService, null));
        doReturn(true).when(service).sendEmail(anyString(), anyString(), anyString());
        
        // French path (line 452, 456-457)
        OrganizationSettingsResponseModel settings = OrganizationSettingsResponseModel.builder()
                .offerReceivedSubjectEn("Received")
                .offerReceivedBodyEn("Body")
                .offerReceivedSubjectFr("Offre reçue")
                .offerReceivedBodyFr("Corps {{clientName}} {{brokerName}} {{buyerName}} {{offerAmount}}")
                .build();
        when(orgSettingsService.getSettings()).thenReturn(settings);
        
        service.sendOfferReceivedNotification("to@x.com", "Client", "Broker", "Buyer", "100000", "fr");
        verify(service, atLeastOnce()).sendEmail(anyString(), anyString(), anyString());
    }

    @Test
    void sendOfferReceivedNotification_clientNameFallbacks() throws Exception {
        var orgSettingsService = mock(com.example.courtierprobackend.Organization.businesslayer.OrganizationSettingsService.class);
        EmailService service = spy(new EmailService("a", "b", orgSettingsService, null));
        doReturn(true).when(service).sendEmail(anyString(), anyString(), anyString());
        
        OrganizationSettingsResponseModel settings = OrganizationSettingsResponseModel.builder()
                .offerReceivedSubjectEn("Received")
                .offerReceivedBodyEn("Body {{clientName}}")
                .offerReceivedSubjectFr("Reçue")
                .offerReceivedBodyFr("Corps {{clientName}}")
                .build();
        when(orgSettingsService.getSettings()).thenReturn(settings);
        
        // clientName null -> use email (lines 460-462)
        service.sendOfferReceivedNotification("to@x.com", null, "Broker", "Buyer", "100000", "en");
        
        // clientName blank -> use email
        service.sendOfferReceivedNotification("to@x.com", "  ", "Broker", "Buyer", "100000", "en");
        
        // clientName null AND email blank -> use "there" in English
        service.sendOfferReceivedNotification("", null, "Broker", "Buyer", "100000", "en");
        
        // French fallback -> "client"
        service.sendOfferReceivedNotification("", null, "Broker", "Buyer", "100000", "fr");
    }

    @Test
    void sendOfferReceivedNotification_exceptionHandling() throws Exception {
        var orgSettingsService = mock(com.example.courtierprobackend.Organization.businesslayer.OrganizationSettingsService.class);
        EmailService service = spy(new EmailService("a", "b", orgSettingsService, null));
        doThrow(new jakarta.mail.MessagingException("fail")).when(service).sendEmail(anyString(), anyString(), anyString());
        
        OrganizationSettingsResponseModel settings = OrganizationSettingsResponseModel.builder()
                .offerReceivedSubjectEn("Received")
                .offerReceivedBodyEn("Body")
                .build();
        when(orgSettingsService.getSettings()).thenReturn(settings);
        
        // Should not throw - just logs (lines 471-472)
        service.sendOfferReceivedNotification("to@x.com", "Client", "Broker", "Buyer", "100000", "en");
    }

    // ========== sendOfferStatusChangedNotification Tests ==========

    @Test
    void sendOfferStatusChangedNotification_englishPath() throws Exception {
        var orgSettingsService = mock(com.example.courtierprobackend.Organization.businesslayer.OrganizationSettingsService.class);
        EmailService service = spy(new EmailService("a", "b", orgSettingsService, null));
        doReturn(true).when(service).sendEmail(anyString(), anyString(), anyString());
        
        OrganizationSettingsResponseModel settings = OrganizationSettingsResponseModel.builder()
                .offerStatusSubjectEn("Offer Status")
                .offerStatusBodyEn("Body {{clientName}} {{buyerName}} {{previousStatus}} {{newStatus}}")
                .offerStatusSubjectFr("Statut d'offre")
                .offerStatusBodyFr("Corps {{clientName}} {{buyerName}} {{previousStatus}} {{newStatus}}")
                .build();
        when(orgSettingsService.getSettings()).thenReturn(settings);
        
        service.sendOfferStatusChangedNotification("to@x.com", "Client", "Broker", "Buyer", "PENDING", "ACCEPTED", "en");
        verify(service).sendEmail(anyString(), anyString(), anyString());
    }

    @Test
    void sendOfferStatusChangedNotification_frenchPath() throws Exception {
        var orgSettingsService = mock(com.example.courtierprobackend.Organization.businesslayer.OrganizationSettingsService.class);
        EmailService service = spy(new EmailService("a", "b", orgSettingsService, null));
        doReturn(true).when(service).sendEmail(anyString(), anyString(), anyString());
        
        // French path (line 488, 495-496)
        OrganizationSettingsResponseModel settings = OrganizationSettingsResponseModel.builder()
                .offerStatusSubjectEn("Status")
                .offerStatusBodyEn("Body")
                .offerStatusSubjectFr("Statut")
                .offerStatusBodyFr("Corps {{clientName}} {{buyerName}} {{previousStatus}} {{newStatus}}")
                .build();
        when(orgSettingsService.getSettings()).thenReturn(settings);
        
        service.sendOfferStatusChangedNotification("to@x.com", "Client", "Broker", "Buyer", "DECLINED", "WITHDRAWN", "fr");
        verify(service, atLeastOnce()).sendEmail(anyString(), anyString(), anyString());
    }

    @Test
    void sendOfferStatusChangedNotification_clientNameFallbacks() throws Exception {
        var orgSettingsService = mock(com.example.courtierprobackend.Organization.businesslayer.OrganizationSettingsService.class);
        EmailService service = spy(new EmailService("a", "b", orgSettingsService, null));
        doReturn(true).when(service).sendEmail(anyString(), anyString(), anyString());
        
        OrganizationSettingsResponseModel settings = OrganizationSettingsResponseModel.builder()
                .offerStatusSubjectEn("Status")
                .offerStatusBodyEn("Body {{clientName}}")
                .offerStatusSubjectFr("Statut")
                .offerStatusBodyFr("Corps {{clientName}}")
                .build();
        when(orgSettingsService.getSettings()).thenReturn(settings);
        
        // clientName null -> use email (lines 499-501)
        service.sendOfferStatusChangedNotification("to@x.com", null, "Broker", "Buyer", "A", "B", "en");
        
        // clientName blank -> use email
        service.sendOfferStatusChangedNotification("to@x.com", "  ", "Broker", "Buyer", "A", "B", "en");
        
        // clientName null AND email blank -> use "there"
        service.sendOfferStatusChangedNotification("", null, "Broker", "Buyer", "A", "B", "en");
        
        // French fallback -> "client"
        service.sendOfferStatusChangedNotification("", null, "Broker", "Buyer", "A", "B", "fr");
    }

    @Test
    void sendOfferStatusChangedNotification_exceptionHandling() throws Exception {
        var orgSettingsService = mock(com.example.courtierprobackend.Organization.businesslayer.OrganizationSettingsService.class);
        EmailService service = spy(new EmailService("a", "b", orgSettingsService, null));
        doThrow(new jakarta.mail.MessagingException("fail")).when(service).sendEmail(anyString(), anyString(), anyString());
        
        OrganizationSettingsResponseModel settings = OrganizationSettingsResponseModel.builder()
                .offerStatusSubjectEn("Status")
                .offerStatusBodyEn("Body")
                .build();
        when(orgSettingsService.getSettings()).thenReturn(settings);
        
        // Should not throw - just logs (lines 511-512)
        service.sendOfferStatusChangedNotification("to@x.com", "Client", "Broker", "Buyer", "A", "B", "en");
    }
}
