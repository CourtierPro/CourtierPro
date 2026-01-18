package com.example.courtierprobackend.email;

import com.example.courtierprobackend.Organization.presentationlayer.model.OrganizationSettingsResponseModel;
import com.example.courtierprobackend.documents.datalayer.DocumentRequest;
import com.example.courtierprobackend.documents.datalayer.valueobjects.TransactionRef;
import com.example.courtierprobackend.transactions.datalayer.enums.TransactionSide;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import java.util.UUID;
import static org.mockito.Mockito.*;

class EmailServiceDocumentNotificationsTest {
    @Test
    void sendDocumentSubmittedNotification_coversAllBranches() throws Exception {
    // Setup mocks
    var orgSettingsService = mock(com.example.courtierprobackend.Organization.businesslayer.OrganizationSettingsService.class);
    var userRepo = mock(com.example.courtierprobackend.user.dataaccesslayer.UserAccountRepository.class);
    EmailService service = spy(new EmailService("a", "b", orgSettingsService, userRepo));
    doReturn(true).when(service).sendEmail(anyString(), anyString(), anyString());
    var userAccount = mock(com.example.courtierprobackend.user.dataaccesslayer.UserAccount.class);
    when(userAccount.isEmailNotificationsEnabled()).thenReturn(true);
    when(userRepo.findByEmail(anyString())).thenReturn(java.util.Optional.of(userAccount));
    var transactionRef = mock(com.example.courtierprobackend.documents.datalayer.valueobjects.TransactionRef.class);
    when(transactionRef.getTransactionId()).thenReturn(java.util.UUID.randomUUID());
    var request = mock(com.example.courtierprobackend.documents.datalayer.DocumentRequest.class);
    when(request.getTransactionRef()).thenReturn(transactionRef);
    // 1. English, subject/body present, docType/documentName different
    OrganizationSettingsResponseModel settings = OrganizationSettingsResponseModel.builder()
        .documentSubmittedSubjectEn("SubjectEn {{documentName}}")
        .documentSubmittedBodyEn("BodyEn {{uploaderName}} {{documentName}} {{documentType}} {{transactionId}}")
        .documentSubmittedSubjectFr("SujetFr {{documentName}}")
        .documentSubmittedBodyFr("CorpsFr {{uploaderName}} {{documentName}} {{documentType}} {{transactionId}}")
        .build();
    when(orgSettingsService.getSettings()).thenReturn(settings);
    service.sendDocumentSubmittedNotification(request, "broker@email.com", "Uploader", "DocName", "DocType", "en");
    verify(service, atLeastOnce()).sendEmail(anyString(), anyString(), anyString());

    // 2. French, subject/body present, docType/documentName same
    service.sendDocumentSubmittedNotification(request, "broker@email.com", "Uploader", "DocType", "DocType", "fr");

    // 3. Subject is blank, triggers fallback
    OrganizationSettingsResponseModel blankSubjectSettings = OrganizationSettingsResponseModel.builder()
        .documentSubmittedSubjectEn("")
        .documentSubmittedBodyEn("BodyEn {{uploaderName}} {{documentName}} {{documentType}} {{transactionId}}")
        .documentSubmittedSubjectFr("")
        .documentSubmittedBodyFr("CorpsFr {{uploaderName}} {{documentName}} {{documentType}} {{transactionId}}")
        .build();
    when(orgSettingsService.getSettings()).thenReturn(blankSubjectSettings);
    service.sendDocumentSubmittedNotification(request, "broker@email.com", "Uploader", "DocName", "DocType", "en");

    // 4. Subject does not contain displayName, triggers append
    OrganizationSettingsResponseModel noDisplayNameSubject = OrganizationSettingsResponseModel.builder()
        .documentSubmittedSubjectEn("SubjectEn")
        .documentSubmittedBodyEn("BodyEn {{uploaderName}} {{documentName}} {{documentType}} {{transactionId}}")
        .documentSubmittedSubjectFr("SujetFr")
        .documentSubmittedBodyFr("CorpsFr {{uploaderName}} {{documentName}} {{documentType}} {{transactionId}}")
        .build();
    when(orgSettingsService.getSettings()).thenReturn(noDisplayNameSubject);
    service.sendDocumentSubmittedNotification(request, "broker@email.com", "Uploader", "DocName", "DocType", "en");

    // 5. Body is blank, triggers fallback
    OrganizationSettingsResponseModel blankBodySettings = OrganizationSettingsResponseModel.builder()
        .documentSubmittedSubjectEn("SubjectEn {{documentName}}")
        .documentSubmittedBodyEn("")
        .documentSubmittedSubjectFr("SujetFr {{documentName}}")
        .documentSubmittedBodyFr("")
        .build();
    when(orgSettingsService.getSettings()).thenReturn(blankBodySettings);
    service.sendDocumentSubmittedNotification(request, "broker@email.com", "Uploader", "DocName", "DocType", "en");

    // 6. docType/documentName null
    when(orgSettingsService.getSettings()).thenReturn(settings);
    service.sendDocumentSubmittedNotification(request, "broker@email.com", "Uploader", null, null, "en");

    // 7. brokerOpt not present
    when(userRepo.findByEmail(anyString())).thenReturn(java.util.Optional.empty());
    service.sendDocumentSubmittedNotification(request, "broker@email.com", "Uploader", "DocName", "DocType", "en");

    // 8. brokerOpt present but notifications disabled
    when(userRepo.findByEmail(anyString())).thenReturn(java.util.Optional.of(userAccount));
    when(userAccount.isEmailNotificationsEnabled()).thenReturn(false);
    service.sendDocumentSubmittedNotification(request, "broker@email.com", "Uploader", "DocName", "DocType", "en");

    // 9. IOException triggers catch
    when(userRepo.findByEmail(anyString())).thenReturn(java.util.Optional.of(userAccount));
    when(userAccount.isEmailNotificationsEnabled()).thenReturn(true);
    EmailService ioService = spy(new EmailService("a", "b", orgSettingsService, userRepo));
    doThrow(new java.io.IOException("fail")).when(ioService).loadTemplateFromClasspath(anyString());
    OrganizationSettingsResponseModel nullBodySettings = OrganizationSettingsResponseModel.builder()
        .documentSubmittedSubjectEn("SubjectEn {{documentName}}")
        .documentSubmittedBodyEn(null)
        .documentSubmittedSubjectFr("SujetFr {{documentName}}")
        .documentSubmittedBodyFr(null)
        .build();
    when(orgSettingsService.getSettings()).thenReturn(nullBodySettings);
    try {
        ioService.sendDocumentSubmittedNotification(request, "broker@email.com", "Uploader", "DocName", "DocType", "en");
    } catch (RuntimeException e) {
        // expected
    }
    }
    @Test
    void sendDocumentRequestedNotification_sendsEmail() throws Exception {
        EmailService service = spy(new EmailService("a", "b", mock(com.example.courtierprobackend.Organization.businesslayer.OrganizationSettingsService.class), mock(com.example.courtierprobackend.user.dataaccesslayer.UserAccountRepository.class)));
        doReturn(true).when(service).sendEmail(anyString(), anyString(), anyString());
        OrganizationSettingsResponseModel settings = OrganizationSettingsResponseModel.builder()
                .documentRequestedSubjectEn("Requested")
                .documentRequestedBodyEn("Body {{clientName}} {{brokerName}} {{documentName}} {{brokerNotes}}")
                .documentRequestedSubjectFr("Demandé")
                .documentRequestedBodyFr("Corps {{clientName}} {{brokerName}} {{documentName}} {{brokerNotes}}")
                .build();
        when(service.organizationSettingsService.getSettings()).thenReturn(settings);
        var userAccount = mock(com.example.courtierprobackend.user.dataaccesslayer.UserAccount.class);
        when(userAccount.isEmailNotificationsEnabled()).thenReturn(true);
        when(service.userAccountRepository.findByEmail(anyString())).thenReturn(java.util.Optional.of(userAccount));
        service.sendDocumentRequestedNotification("to@x.com", "Client", "Broker", "Doc", "MORTGAGE_PRE_APPROVAL", "Notes", "en");
        verify(service).sendEmail(anyString(), anyString(), anyString());
    }

    @Test
    void sendDocumentEditedNotification_sendsEmail() throws Exception {
        EmailService service = spy(new EmailService("a", "b", null, null));
        doReturn(true).when(service).sendEmail(anyString(), anyString(), anyString());
        service.sendDocumentEditedNotification("to@x.com", "Client", "Broker", "Doc", "MORTGAGE_PRE_APPROVAL", "en");
        verify(service).sendEmail(anyString(), anyString(), anyString());
    }

    @Test
    void sendDocumentStatusUpdatedNotification_sendsEmail() throws Exception {
        EmailService service = spy(new EmailService("a", "b", mock(com.example.courtierprobackend.Organization.businesslayer.OrganizationSettingsService.class), null));
        doReturn(true).when(service).sendEmail(anyString(), anyString(), anyString());
        OrganizationSettingsResponseModel settings = OrganizationSettingsResponseModel.builder()
                .documentReviewSubjectEn("Review")
                .documentReviewBodyEn("Body {{clientName}} {{brokerName}} {{documentName}} {{transactionId}} {{status}} {{brokerNotes}}")
                .documentReviewSubjectFr("Revue")
                .documentReviewBodyFr("Corps {{clientName}} {{brokerName}} {{documentName}} {{transactionId}} {{status}} {{brokerNotes}}")
                .build();
        when(service.organizationSettingsService.getSettings()).thenReturn(settings);
        var request = mock(DocumentRequest.class);
        var transactionRef = new TransactionRef(UUID.randomUUID(), null, TransactionSide.BUY_SIDE);
        when(request.getTransactionRef()).thenReturn(transactionRef);
        when(request.getStatus()).thenReturn(com.example.courtierprobackend.documents.datalayer.enums.DocumentStatusEnum.APPROVED);
        when(request.getBrokerNotes()).thenReturn("Notes");
        service.sendDocumentStatusUpdatedNotification(request, "to@x.com", "Client", "Broker", "Doc", "MORTGAGE_PRE_APPROVAL", "en");
        verify(service).sendEmail(anyString(), anyString(), anyString());
    }
}
