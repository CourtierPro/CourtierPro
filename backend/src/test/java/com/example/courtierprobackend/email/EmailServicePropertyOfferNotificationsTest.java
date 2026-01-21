package com.example.courtierprobackend.email;

import com.example.courtierprobackend.Organization.presentationlayer.model.OrganizationSettingsResponseModel;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;

class EmailServicePropertyOfferNotificationsTest {

    // ========== sendPropertyOfferMadeNotification Tests ==========

    @Test
    void sendPropertyOfferMadeNotification_englishPath() throws Exception {
        var orgSettingsService = mock(com.example.courtierprobackend.Organization.businesslayer.OrganizationSettingsService.class);
        EmailService service = spy(new EmailService("a", "b", null, null, orgSettingsService, null, null));
        doReturn(true).when(service).sendEmail(anyString(), anyString(), anyString());
        
        OrganizationSettingsResponseModel settings = OrganizationSettingsResponseModel.builder()
                .propertyOfferMadeSubjectEn("Offer Made")
                .propertyOfferMadeBodyEn("Body {{clientName}} {{brokerName}} {{propertyAddress}} {{offerAmount}} {{offerRound}}")
                .propertyOfferMadeSubjectFr("Offre faite")
                .propertyOfferMadeBodyFr("Corps {{clientName}} {{brokerName}} {{propertyAddress}} {{offerAmount}} {{offerRound}}")
                .build();
        when(orgSettingsService.getSettings()).thenReturn(settings);
        
        service.sendPropertyOfferMadeNotification("to@x.com", "Client", "Broker", "Addr", "100000", 1, "en");
        verify(service).sendEmail(anyString(), anyString(), anyString());
    }

    @Test
    void sendPropertyOfferMadeNotification_frenchPath() throws Exception {
        var orgSettingsService = mock(com.example.courtierprobackend.Organization.businesslayer.OrganizationSettingsService.class);
        EmailService service = spy(new EmailService("a", "b", null, null, orgSettingsService, null, null));
        doReturn(true).when(service).sendEmail(anyString(), anyString(), anyString());
        
        // French with settings (lines 359, 363-364)
        OrganizationSettingsResponseModel settings = OrganizationSettingsResponseModel.builder()
                .propertyOfferMadeSubjectEn("Offer Made")
                .propertyOfferMadeBodyEn("Body")
                .propertyOfferMadeSubjectFr("Offre faite")
                .propertyOfferMadeBodyFr("Corps {{clientName}} {{brokerName}} {{propertyAddress}}")
                .build();
        when(orgSettingsService.getSettings()).thenReturn(settings);
        
        service.sendPropertyOfferMadeNotification("to@x.com", "Client", "Broker", "Addr", "100000", 1, "fr");
        verify(service, atLeastOnce()).sendEmail(anyString(), anyString(), anyString());
    }

    @Test
    void sendPropertyOfferMadeNotification_nullSettings() throws Exception {
        var orgSettingsService = mock(com.example.courtierprobackend.Organization.businesslayer.OrganizationSettingsService.class);
        EmailService service = spy(new EmailService("a", "b", null, null, orgSettingsService, null, null));
        doReturn(true).when(service).sendEmail(anyString(), anyString(), anyString());
        
        // Null subject and body from settings (lines 366, 369)
        OrganizationSettingsResponseModel nullSettings = OrganizationSettingsResponseModel.builder()
                .propertyOfferMadeSubjectEn(null)
                .propertyOfferMadeBodyEn(null)
                .propertyOfferMadeSubjectFr(null)
                .propertyOfferMadeBodyFr(null)
                .build();
        when(orgSettingsService.getSettings()).thenReturn(nullSettings);
        
        service.sendPropertyOfferMadeNotification("to@x.com", "Client", "Broker", "Addr", "100000", 1, "en");
        service.sendPropertyOfferMadeNotification("to@x.com", "Client", "Broker", "Addr", "100000", 1, "fr");
    }

    @Test
    void sendPropertyOfferMadeNotification_clientNameFallbacks() throws Exception {
        var orgSettingsService = mock(com.example.courtierprobackend.Organization.businesslayer.OrganizationSettingsService.class);
        EmailService service = spy(new EmailService("a", "b", null, null, orgSettingsService, null, null));
        doReturn(true).when(service).sendEmail(anyString(), anyString(), anyString());
        
        OrganizationSettingsResponseModel settings = OrganizationSettingsResponseModel.builder()
                .propertyOfferMadeSubjectEn("Offer")
                .propertyOfferMadeBodyEn("Body {{clientName}}")
                .propertyOfferMadeSubjectFr("Offre")
                .propertyOfferMadeBodyFr("Corps {{clientName}}")
                .build();
        when(orgSettingsService.getSettings()).thenReturn(settings);
        
        // clientName null -> use email (lines 373-375)
        service.sendPropertyOfferMadeNotification("to@x.com", null, "Broker", "Addr", "100000", 1, "en");
        
        // clientName blank -> use email
        service.sendPropertyOfferMadeNotification("to@x.com", "  ", "Broker", "Addr", "100000", 1, "en");
        
        // clientName null AND email blank -> use "there" in English
        service.sendPropertyOfferMadeNotification("", null, "Broker", "Addr", "100000", 1, "en");
        
        // French fallback -> "client"
        service.sendPropertyOfferMadeNotification("", null, "Broker", "Addr", "100000", 1, "fr");
    }

    @Test
    void sendPropertyOfferMadeNotification_exceptionHandling() throws Exception {
        var orgSettingsService = mock(com.example.courtierprobackend.Organization.businesslayer.OrganizationSettingsService.class);
        EmailService service = spy(new EmailService("a", "b", null, null, orgSettingsService, null, null));
        doThrow(new jakarta.mail.MessagingException("fail")).when(service).sendEmail(anyString(), anyString(), anyString());
        
        OrganizationSettingsResponseModel settings = OrganizationSettingsResponseModel.builder()
                .propertyOfferMadeSubjectEn("Offer")
                .propertyOfferMadeBodyEn("Body")
                .build();
        when(orgSettingsService.getSettings()).thenReturn(settings);
        
        // Should not throw - just logs (lines 385-386)
        service.sendPropertyOfferMadeNotification("to@x.com", "Client", "Broker", "Addr", "100000", 1, "en");
    }

    // ========== sendPropertyOfferStatusChangedNotification Tests ==========

    @Test
    void sendPropertyOfferStatusChangedNotification_englishPath() throws Exception {
        var orgSettingsService = mock(com.example.courtierprobackend.Organization.businesslayer.OrganizationSettingsService.class);
        EmailService service = spy(new EmailService("a", "b", null, null, orgSettingsService, null, null));
        doReturn(true).when(service).sendEmail(anyString(), anyString(), anyString());
        
        OrganizationSettingsResponseModel settings = OrganizationSettingsResponseModel.builder()
                .propertyOfferStatusSubjectEn("Offer Status")
                .propertyOfferStatusBodyEn("Body {{clientName}} {{brokerName}} {{propertyAddress}} {{previousStatus}} {{newStatus}} {{counterpartyResponse}}")
                .propertyOfferStatusSubjectFr("Statut d'offre")
                .propertyOfferStatusBodyFr("Corps {{clientName}} {{brokerName}} {{propertyAddress}} {{previousStatus}} {{newStatus}} {{counterpartyResponse}}")
                .build();
        when(orgSettingsService.getSettings()).thenReturn(settings);
        
        service.sendPropertyOfferStatusChangedNotification("to@x.com", "Client", "Broker", "Addr", "PENDING", "ACCEPTED", "Counter", "en");
        verify(service).sendEmail(anyString(), anyString(), anyString());
    }

    @Test
    void sendPropertyOfferStatusChangedNotification_frenchPath() throws Exception {
        var orgSettingsService = mock(com.example.courtierprobackend.Organization.businesslayer.OrganizationSettingsService.class);
        EmailService service = spy(new EmailService("a", "b", null, null, orgSettingsService, null, null));
        doReturn(true).when(service).sendEmail(anyString(), anyString(), anyString());
        
        // French path (line 403, 410-411)
        OrganizationSettingsResponseModel settings = OrganizationSettingsResponseModel.builder()
                .propertyOfferStatusSubjectEn("Status")
                .propertyOfferStatusBodyEn("Body")
                .propertyOfferStatusSubjectFr("Statut")
                .propertyOfferStatusBodyFr("Corps {{clientName}} {{previousStatus}} {{newStatus}}")
                .build();
        when(orgSettingsService.getSettings()).thenReturn(settings);
        
        service.sendPropertyOfferStatusChangedNotification("to@x.com", "Client", "Broker", "Addr", "OFFER_MADE", "COUNTERED", null, "fr");
        verify(service, atLeastOnce()).sendEmail(anyString(), anyString(), anyString());
    }

    @Test
    void sendPropertyOfferStatusChangedNotification_clientNameFallbacks() throws Exception {
        var orgSettingsService = mock(com.example.courtierprobackend.Organization.businesslayer.OrganizationSettingsService.class);
        EmailService service = spy(new EmailService("a", "b", null, null, orgSettingsService, null, null));
        doReturn(true).when(service).sendEmail(anyString(), anyString(), anyString());
        
        OrganizationSettingsResponseModel settings = OrganizationSettingsResponseModel.builder()
                .propertyOfferStatusSubjectEn("Status")
                .propertyOfferStatusBodyEn("Body {{clientName}}")
                .propertyOfferStatusSubjectFr("Statut")
                .propertyOfferStatusBodyFr("Corps {{clientName}}")
                .build();
        when(orgSettingsService.getSettings()).thenReturn(settings);
        
        // clientName null -> use email (lines 414-416)
        service.sendPropertyOfferStatusChangedNotification("to@x.com", null, "Broker", "Addr", "A", "B", null, "en");
        
        // clientName blank -> use email
        service.sendPropertyOfferStatusChangedNotification("to@x.com", "  ", "Broker", "Addr", "A", "B", null, "en");
        
        // clientName null AND email blank -> use "there"
        service.sendPropertyOfferStatusChangedNotification("", null, "Broker", "Addr", "A", "B", null, "en");
        
        // French fallback -> "client"
        service.sendPropertyOfferStatusChangedNotification("", null, "Broker", "Addr", "A", "B", null, "fr");
    }

    @Test
    void sendPropertyOfferStatusChangedNotification_counterpartyResponseHandling() throws Exception {
        var orgSettingsService = mock(com.example.courtierprobackend.Organization.businesslayer.OrganizationSettingsService.class);
        EmailService service = spy(new EmailService("a", "b", null, null, orgSettingsService, null, null));
        doReturn(true).when(service).sendEmail(anyString(), anyString(), anyString());
        
        OrganizationSettingsResponseModel settings = OrganizationSettingsResponseModel.builder()
                .propertyOfferStatusSubjectEn("Status")
                .propertyOfferStatusBodyEn("Body [IF-counterpartyResponse]Response: {{counterpartyResponse}}[/IF-counterpartyResponse]")
                .build();
        when(orgSettingsService.getSettings()).thenReturn(settings);
        
        // With counterpartyResponse (line 431)
        service.sendPropertyOfferStatusChangedNotification("to@x.com", "Client", "Broker", "Addr", "A", "B", "Counter response", "en");
        
        // Without counterpartyResponse (null)
        service.sendPropertyOfferStatusChangedNotification("to@x.com", "Client", "Broker", "Addr", "A", "B", null, "en");
    }

    @Test
    void sendPropertyOfferStatusChangedNotification_exceptionHandling() throws Exception {
        var orgSettingsService = mock(com.example.courtierprobackend.Organization.businesslayer.OrganizationSettingsService.class);
        EmailService service = spy(new EmailService("a", "b", null, null, orgSettingsService, null, null));
        doThrow(new jakarta.mail.MessagingException("fail")).when(service).sendEmail(anyString(), anyString(), anyString());
        
        OrganizationSettingsResponseModel settings = OrganizationSettingsResponseModel.builder()
                .propertyOfferStatusSubjectEn("Status")
                .propertyOfferStatusBodyEn("Body")
                .build();
        when(orgSettingsService.getSettings()).thenReturn(settings);
        
        // Should not throw - just logs (lines 434-435)
        service.sendPropertyOfferStatusChangedNotification("to@x.com", "Client", "Broker", "Addr", "A", "B", null, "en");
    }
}
