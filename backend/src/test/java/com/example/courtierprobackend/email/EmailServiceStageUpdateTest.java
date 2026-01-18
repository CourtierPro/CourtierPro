package com.example.courtierprobackend.email;

import com.example.courtierprobackend.Organization.presentationlayer.model.OrganizationSettingsResponseModel;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.assertThat;

class EmailServiceStageUpdateTest {
    @Test
    void sendStageUpdateEmail_sendsEmail() throws Exception {
        EmailService service = spy(new EmailService("a", "b", mock(com.example.courtierprobackend.Organization.businesslayer.OrganizationSettingsService.class), null));
        doReturn(true).when(service).sendEmail(anyString(), anyString(), anyString());
        OrganizationSettingsResponseModel settings = OrganizationSettingsResponseModel.builder()
                .stageUpdateSubjectEn("Stage Update")
                .stageUpdateBodyEn("Body {{clientName}} {{transactionAddress}} {{newStage}}")
                .stageUpdateSubjectFr("Mise à jour")
                .stageUpdateBodyFr("Corps {{clientName}} {{transactionAddress}} {{newStage}}")
                .build();
        when(service.organizationSettingsService.getSettings()).thenReturn(settings);
        service.sendStageUpdateEmail("to@x.com", "Client", "Broker", "Addr", "BUYER_PREQUALIFY_FINANCIALLY", "en");
        verify(service).sendEmail(anyString(), anyString(), anyString());
    }
}
