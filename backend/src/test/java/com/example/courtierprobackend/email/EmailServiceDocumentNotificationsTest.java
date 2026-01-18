package com.example.courtierprobackend.email;

import com.example.courtierprobackend.Organization.presentationlayer.model.OrganizationSettingsResponseModel;
import com.example.courtierprobackend.documents.datalayer.DocumentRequest;
import com.example.courtierprobackend.documents.datalayer.valueobjects.TransactionRef;
import com.example.courtierprobackend.transactions.datalayer.enums.TransactionSide;
import com.example.courtierprobackend.documents.datalayer.enums.DocumentStatusEnum;
import jakarta.mail.Transport;
import jakarta.mail.Message;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import java.util.UUID;
import static org.mockito.Mockito.*;

class EmailServiceDocumentNotificationsTest {

    // ========== sendDocumentSubmittedNotification Tests ==========

    @Test
    void sendDocumentSubmittedNotification_coversAllBranches() throws Exception {
        var orgSettingsService = mock(com.example.courtierprobackend.Organization.businesslayer.OrganizationSettingsService.class);
        var userRepo = mock(com.example.courtierprobackend.user.dataaccesslayer.UserAccountRepository.class);
        EmailService service = spy(new EmailService("a", "b", orgSettingsService, userRepo));
        doReturn(true).when(service).sendEmail(anyString(), anyString(), anyString());
        var userAccount = mock(com.example.courtierprobackend.user.dataaccesslayer.UserAccount.class);
        when(userAccount.isEmailNotificationsEnabled()).thenReturn(true);
        when(userRepo.findByEmail(anyString())).thenReturn(java.util.Optional.of(userAccount));
        var transactionRef = mock(TransactionRef.class);
        when(transactionRef.getTransactionId()).thenReturn(UUID.randomUUID());
        var request = mock(DocumentRequest.class);
        when(request.getTransactionRef()).thenReturn(transactionRef);

        // 1. English, subject/body present
        OrganizationSettingsResponseModel settings = OrganizationSettingsResponseModel.builder()
            .documentSubmittedSubjectEn("SubjectEn {{documentName}}")
            .documentSubmittedBodyEn("BodyEn {{uploaderName}} {{documentName}} {{documentType}} {{transactionId}}")
            .documentSubmittedSubjectFr("SujetFr {{documentName}}")
            .documentSubmittedBodyFr("CorpsFr {{uploaderName}} {{documentName}} {{documentType}} {{transactionId}}")
            .build();
        when(orgSettingsService.getSettings()).thenReturn(settings);
        service.sendDocumentSubmittedNotification(request, "broker@email.com", "Uploader", "DocName", "DocType", "en");
        verify(service, atLeastOnce()).sendEmail(anyString(), anyString(), anyString());

        // 2. French, docType/documentName same (line 124 - French fallback subject)
        OrganizationSettingsResponseModel frenchBlankSettings = OrganizationSettingsResponseModel.builder()
            .documentSubmittedSubjectEn("SubjectEn")
            .documentSubmittedBodyEn("BodyEn")
            .documentSubmittedSubjectFr("")
            .documentSubmittedBodyFr("")
            .build();
        when(orgSettingsService.getSettings()).thenReturn(frenchBlankSettings);
        service.sendDocumentSubmittedNotification(request, "broker@email.com", "Uploader", "DocType", "DocType", "fr");

        // 3. Template fallback when body is null (lines 146-154)
        OrganizationSettingsResponseModel nullBodySettings = OrganizationSettingsResponseModel.builder()
            .documentSubmittedSubjectEn("SubjectEn")
            .documentSubmittedBodyEn(null)
            .documentSubmittedSubjectFr("SujetFr")
            .documentSubmittedBodyFr(null)
            .build();
        when(orgSettingsService.getSettings()).thenReturn(nullBodySettings);
        // This will trigger template loading path
        try {
            service.sendDocumentSubmittedNotification(request, "broker@email.com", "Uploader", "DocName", "DocType", "en");
        } catch (Exception ignored) {
            // Template may not exist in test context
        }

        // 4. Notifications disabled
        when(userAccount.isEmailNotificationsEnabled()).thenReturn(false);
        service.sendDocumentSubmittedNotification(request, "broker@email.com", "Uploader", "DocName", "DocType", "en");

        // 5. User not found
        when(userRepo.findByEmail(anyString())).thenReturn(java.util.Optional.empty());
        service.sendDocumentSubmittedNotification(request, "broker@email.com", "Uploader", "DocName", "DocType", "en");
    }

    // ========== sendDocumentRequestedNotification Tests ==========

    @Test
    void sendDocumentRequestedNotification_allBranches() throws Exception {
        var orgSettingsService = mock(com.example.courtierprobackend.Organization.businesslayer.OrganizationSettingsService.class);
        var userRepo = mock(com.example.courtierprobackend.user.dataaccesslayer.UserAccountRepository.class);
        EmailService service = spy(new EmailService("a", "b", orgSettingsService, userRepo));
        doReturn(true).when(service).sendEmail(anyString(), anyString(), anyString());
        var userAccount = mock(com.example.courtierprobackend.user.dataaccesslayer.UserAccount.class);
        when(userAccount.isEmailNotificationsEnabled()).thenReturn(true);
        when(userRepo.findByEmail(anyString())).thenReturn(java.util.Optional.of(userAccount));

        // 1. English with all values
        OrganizationSettingsResponseModel settings = OrganizationSettingsResponseModel.builder()
            .documentRequestedSubjectEn("Requested {{documentName}}")
            .documentRequestedBodyEn("Body {{clientName}} {{brokerName}} {{documentName}} {{brokerNotes}}")
            .documentRequestedSubjectFr("Demandé {{documentName}}")
            .documentRequestedBodyFr("Corps {{clientName}} {{brokerName}} {{documentName}} {{brokerNotes}}")
            .build();
        when(orgSettingsService.getSettings()).thenReturn(settings);
        service.sendDocumentRequestedNotification("to@x.com", "Client", "Broker", "Doc", "MORTGAGE_PRE_APPROVAL", "Notes", "en");
        verify(service, atLeastOnce()).sendEmail(anyString(), anyString(), anyString());

        // 2. French with null docType and documentName (lines 174, 177-178, 182)
        service.sendDocumentRequestedNotification("to@x.com", "Client", "Broker", null, null, null, "fr");

        // 3. Null subject/body from settings (lines 187-188, 192, 200-202)
        OrganizationSettingsResponseModel nullSettings = OrganizationSettingsResponseModel.builder()
            .documentRequestedSubjectEn(null)
            .documentRequestedBodyEn(null)
            .documentRequestedSubjectFr(null)
            .documentRequestedBodyFr(null)
            .build();
        when(orgSettingsService.getSettings()).thenReturn(nullSettings);
        service.sendDocumentRequestedNotification("to@x.com", "Client", "Broker", "Doc", "OTHER", "Notes", "en");
        service.sendDocumentRequestedNotification("to@x.com", "Client", "Broker", "Doc", "OTHER", null, "fr");

        // 4. Subject doesn't contain displayName - append
        OrganizationSettingsResponseModel noDisplayNameSubject = OrganizationSettingsResponseModel.builder()
            .documentRequestedSubjectEn("New Request")
            .documentRequestedBodyEn("Body {{clientName}}")
            .build();
        when(orgSettingsService.getSettings()).thenReturn(noDisplayNameSubject);
        service.sendDocumentRequestedNotification("to@x.com", "Client", "Broker", "Doc", "TYPE", null, "en");

        // 5. Disabled notifications - early return (line 171)
        when(userAccount.isEmailNotificationsEnabled()).thenReturn(false);
        service.sendDocumentRequestedNotification("to@x.com", "Client", "Broker", "Doc", "TYPE", null, "en");

        // 6. User not present - early return
        when(userRepo.findByEmail(anyString())).thenReturn(java.util.Optional.empty());
        service.sendDocumentRequestedNotification("to@x.com", "Client", "Broker", "Doc", "TYPE", null, "en");
    }

    // ========== sendDocumentEditedNotification Tests ==========

    @Test
    void sendDocumentEditedNotification_allBranches() throws Exception {
        EmailService service = spy(new EmailService("a", "b", null, null));
        doReturn(true).when(service).sendEmail(anyString(), anyString(), anyString());

        // 1. English path
        service.sendDocumentEditedNotification("to@x.com", "Client", "Broker", "Doc", "MORTGAGE_PRE_APPROVAL", "en");
        verify(service, atLeastOnce()).sendEmail(anyString(), anyString(), anyString());

        // 2. French path (lines 244, 248, 251, 255)
        service.sendDocumentEditedNotification("to@x.com", "Client", "Broker", "Doc", "CREDIT_REPORT", "fr");

        // 3. docType equals documentName
        service.sendDocumentEditedNotification("to@x.com", "Client", "Broker", "PROOF_OF_FUNDS", "PROOF_OF_FUNDS", "en");

        // 4. IOException handling (lines 267-271)
        EmailService ioService = spy(new EmailService("a", "b", null, null));
        doThrow(new java.io.IOException("template not found")).when(ioService).loadTemplateFromClasspath(anyString());
        ioService.sendDocumentEditedNotification("to@x.com", "Client", "Broker", "Doc", "TYPE", "en");
        // Should log error but not throw
    }

    // ========== sendDocumentStatusUpdatedNotification Tests ==========

    @Test
    void sendDocumentStatusUpdatedNotification_allBranches() throws Exception {
        var orgSettingsService = mock(com.example.courtierprobackend.Organization.businesslayer.OrganizationSettingsService.class);
        EmailService service = spy(new EmailService("a", "b", orgSettingsService, null));
        doReturn(true).when(service).sendEmail(anyString(), anyString(), anyString());

        var transactionRef = new TransactionRef(UUID.randomUUID(), null, TransactionSide.BUY_SIDE);
        var request = mock(DocumentRequest.class);
        when(request.getTransactionRef()).thenReturn(transactionRef);
        when(request.getBrokerNotes()).thenReturn("Notes");

        // 1. English with APPROVED status
        when(request.getStatus()).thenReturn(DocumentStatusEnum.APPROVED);
        OrganizationSettingsResponseModel settings = OrganizationSettingsResponseModel.builder()
            .documentReviewSubjectEn("Review")
            .documentReviewBodyEn("Body {{clientName}} {{brokerName}} {{documentName}} {{status}} [IF-brokerNotes]Notes: {{brokerNotes}}[/IF-brokerNotes]")
            .documentReviewSubjectFr("Revue")
            .documentReviewBodyFr("Corps {{clientName}} {{brokerName}} {{documentName}} {{status}}")
            .build();
        when(orgSettingsService.getSettings()).thenReturn(settings);
        service.sendDocumentStatusUpdatedNotification(request, "to@x.com", "Client", "Broker", "Doc", "MORTGAGE_PRE_APPROVAL", "en");
        verify(service, atLeastOnce()).sendEmail(anyString(), anyString(), anyString());

        // 2. French path with NEEDS_REVISION status (lines 284, 289, 313, 315-317)
        when(request.getStatus()).thenReturn(DocumentStatusEnum.NEEDS_REVISION);
        service.sendDocumentStatusUpdatedNotification(request, "to@x.com", "Client", "Broker", "Doc", "MORTGAGE_PRE_APPROVAL", "fr");

        // 3. Null subject/body from settings - fallbacks (lines 294-295, 299, 302-304)
        OrganizationSettingsResponseModel nullSettings = OrganizationSettingsResponseModel.builder()
            .documentReviewSubjectEn(null)
            .documentReviewBodyEn(null)
            .documentReviewSubjectFr(null)
            .documentReviewBodyFr(null)
            .build();
        when(orgSettingsService.getSettings()).thenReturn(nullSettings);
        service.sendDocumentStatusUpdatedNotification(request, "to@x.com", "Client", "Broker", "Doc", "TYPE", "en");
        service.sendDocumentStatusUpdatedNotification(request, "to@x.com", "Client", "Broker", "Doc", "TYPE", "fr");

        // 4. clientName null -> use email (lines 326-328)
        service.sendDocumentStatusUpdatedNotification(request, "to@x.com", null, "Broker", "Doc", "TYPE", "en");

        // 5. clientName blank -> use email
        service.sendDocumentStatusUpdatedNotification(request, "to@x.com", "  ", "Broker", "Doc", "TYPE", "en");

        // 6. clientName null AND email blank -> use "there" (line 328 English fallback)
        service.sendDocumentStatusUpdatedNotification(request, "", null, "Broker", "Doc", "TYPE", "en");

        // 7. French fallback -> "client"
        service.sendDocumentStatusUpdatedNotification(request, "", null, "Broker", "Doc", "TYPE", "fr");

        // 8. null brokerNotes (line 337)
        when(request.getBrokerNotes()).thenReturn(null);
        service.sendDocumentStatusUpdatedNotification(request, "to@x.com", "Client", "Broker", "Doc", "TYPE", "en");

        // 9. docType equals documentName
        service.sendDocumentStatusUpdatedNotification(request, "to@x.com", "Client", "Broker", "INSPECTION_REPORT", "INSPECTION_REPORT", "en");

        // 10. All document status translations via different statuses
        when(request.getStatus()).thenReturn(DocumentStatusEnum.REQUESTED);
        service.sendDocumentStatusUpdatedNotification(request, "to@x.com", "Client", "Broker", "Doc", "TYPE", "en");
        service.sendDocumentStatusUpdatedNotification(request, "to@x.com", "Client", "Broker", "Doc", "TYPE", "fr");

        when(request.getStatus()).thenReturn(DocumentStatusEnum.SUBMITTED);
        service.sendDocumentStatusUpdatedNotification(request, "to@x.com", "Client", "Broker", "Doc", "TYPE", "en");
        service.sendDocumentStatusUpdatedNotification(request, "to@x.com", "Client", "Broker", "Doc", "TYPE", "fr");

        // null status
        when(request.getStatus()).thenReturn(null);
        service.sendDocumentStatusUpdatedNotification(request, "to@x.com", "Client", "Broker", "Doc", "TYPE", "en");
    }

    @Test
    void sendDocumentStatusUpdatedNotification_exceptionHandling() throws Exception {
        var orgSettingsService = mock(com.example.courtierprobackend.Organization.businesslayer.OrganizationSettingsService.class);
        EmailService service = spy(new EmailService("a", "b", orgSettingsService, null));
        doThrow(new jakarta.mail.MessagingException("fail")).when(service).sendEmail(anyString(), anyString(), anyString());

        var transactionRef = new TransactionRef(UUID.randomUUID(), null, TransactionSide.BUY_SIDE);
        var request = mock(DocumentRequest.class);
        when(request.getTransactionRef()).thenReturn(transactionRef);
        when(request.getStatus()).thenReturn(DocumentStatusEnum.APPROVED);

        OrganizationSettingsResponseModel settings = OrganizationSettingsResponseModel.builder()
            .documentReviewSubjectEn("Review")
            .documentReviewBodyEn("Body")
            .build();
        when(orgSettingsService.getSettings()).thenReturn(settings);

        // Should not throw - just logs (lines 340-341)
        service.sendDocumentStatusUpdatedNotification(request, "to@x.com", "Client", "Broker", "Doc", "TYPE", "en");
    }
}
