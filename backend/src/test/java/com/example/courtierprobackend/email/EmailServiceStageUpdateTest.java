package com.example.courtierprobackend.email;

import com.example.courtierprobackend.Organization.presentationlayer.model.OrganizationSettingsResponseModel;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;

class EmailServiceStageUpdateTest {

    @Test
    void sendStageUpdateEmail_englishPath() throws Exception {
        var orgSettingsService = mock(com.example.courtierprobackend.Organization.businesslayer.OrganizationSettingsService.class);
        EmailService service = spy(new EmailService("a", "b", null, null, orgSettingsService, null, null));
        doReturn(true).when(service).sendEmail(anyString(), anyString(), anyString());
        
        OrganizationSettingsResponseModel settings = OrganizationSettingsResponseModel.builder()
                .stageUpdateSubjectEn("Stage Update")
                .stageUpdateBodyEn("Body {{clientName}} {{brokerName}} {{transactionAddress}} {{newStage}}")
                .stageUpdateSubjectFr("Mise à jour")
                .stageUpdateBodyFr("Corps {{clientName}} {{brokerName}} {{transactionAddress}} {{newStage}}")
                .build();
        when(orgSettingsService.getSettings()).thenReturn(settings);
        
        service.sendStageUpdateEmail("to@x.com", "Client", "Broker", "Addr", "BUYER_FINANCIAL_PREPARATION", "en");
        verify(service).sendEmail(anyString(), anyString(), anyString());
    }

    @Test
    void sendStageUpdateEmail_frenchPath() throws Exception {
        var orgSettingsService = mock(com.example.courtierprobackend.Organization.businesslayer.OrganizationSettingsService.class);
        EmailService service = spy(new EmailService("a", "b", null, null, orgSettingsService, null, null));
        doReturn(true).when(service).sendEmail(anyString(), anyString(), anyString());
        
        // French path (line 597, 602-603)
        OrganizationSettingsResponseModel settings = OrganizationSettingsResponseModel.builder()
                .stageUpdateSubjectEn("Update")
                .stageUpdateBodyEn("Body")
                .stageUpdateSubjectFr("Mise à jour de la transaction")
                .stageUpdateBodyFr("Corps {{clientName}} {{brokerName}} {{transactionAddress}} {{newStage}}")
                .build();
        when(orgSettingsService.getSettings()).thenReturn(settings);
        
        service.sendStageUpdateEmail("to@x.com", "Client", "Broker", "Addr", "SELLER_OFFER_AND_NEGOTIATION", "fr");
        verify(service, atLeastOnce()).sendEmail(anyString(), anyString(), anyString());
    }

    @Test
    void sendStageUpdateEmail_nullSettingsFallbacks() throws Exception {
        var orgSettingsService = mock(com.example.courtierprobackend.Organization.businesslayer.OrganizationSettingsService.class);
        EmailService service = spy(new EmailService("a", "b", null, null, orgSettingsService, null, null));
        doReturn(true).when(service).sendEmail(anyString(), anyString(), anyString());
        
        // Null subject/body from settings (lines 607, 610-612)
        OrganizationSettingsResponseModel nullSettings = OrganizationSettingsResponseModel.builder()
                .stageUpdateSubjectEn(null)
                .stageUpdateBodyEn(null)
                .stageUpdateSubjectFr(null)
                .stageUpdateBodyFr(null)
                .build();
        when(orgSettingsService.getSettings()).thenReturn(nullSettings);
        
        service.sendStageUpdateEmail("to@x.com", "Client", "Broker", "Addr", "BUYER_FINANCIAL_PREPARATION", "en");
        service.sendStageUpdateEmail("to@x.com", "Client", "Broker", "Addr", "BUYER_FINANCIAL_PREPARATION", "fr");
    }

    @Test
    void sendStageUpdateEmail_blankSettingsFallbacks() throws Exception {
        var orgSettingsService = mock(com.example.courtierprobackend.Organization.businesslayer.OrganizationSettingsService.class);
        EmailService service = spy(new EmailService("a", "b", null, null, orgSettingsService, null, null));
        doReturn(true).when(service).sendEmail(anyString(), anyString(), anyString());
        
        // Blank subject/body from settings
        OrganizationSettingsResponseModel blankSettings = OrganizationSettingsResponseModel.builder()
                .stageUpdateSubjectEn("")
                .stageUpdateBodyEn("")
                .stageUpdateSubjectFr("")
                .stageUpdateBodyFr("")
                .build();
        when(orgSettingsService.getSettings()).thenReturn(blankSettings);
        
        service.sendStageUpdateEmail("to@x.com", "Client", "Broker", "Addr", "CLOSED", "en");
        service.sendStageUpdateEmail("to@x.com", "Client", "Broker", "Addr", "CLOSED", "fr");
    }

    @Test
    void sendStageUpdateEmail_exceptionHandling() throws Exception {
        var orgSettingsService = mock(com.example.courtierprobackend.Organization.businesslayer.OrganizationSettingsService.class);
        EmailService service = spy(new EmailService("a", "b", null, null, orgSettingsService, null, null));
        doThrow(new jakarta.mail.MessagingException("fail")).when(service).sendEmail(anyString(), anyString(), anyString());
        
        OrganizationSettingsResponseModel settings = OrganizationSettingsResponseModel.builder()
                .stageUpdateSubjectEn("Update")
                .stageUpdateBodyEn("Body")
                .build();
        when(orgSettingsService.getSettings()).thenReturn(settings);
        
        // Should not throw - just logs (lines 624-625)
        service.sendStageUpdateEmail("to@x.com", "Client", "Broker", "Addr", "STAGE", "en");
    }
}
