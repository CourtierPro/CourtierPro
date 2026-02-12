package com.example.courtierprobackend.documents.businesslayer;

import com.example.courtierprobackend.audit.timeline_audit.businesslayer.TimelineService;
import com.example.courtierprobackend.common.exceptions.BadRequestException;
import com.example.courtierprobackend.common.exceptions.ForbiddenException;
import com.example.courtierprobackend.common.exceptions.NotFoundException;
import com.example.courtierprobackend.documents.datalayer.Document;
import com.example.courtierprobackend.documents.datalayer.DocumentRepository;
import com.example.courtierprobackend.documents.datalayer.TransactionStageChecklistStateRepository;
import com.example.courtierprobackend.documents.datalayer.enums.DocumentFlowEnum;
import com.example.courtierprobackend.documents.datalayer.enums.DocumentStatusEnum;
import com.example.courtierprobackend.documents.datalayer.valueobjects.TransactionRef;
import com.example.courtierprobackend.documents.presentationlayer.models.DocumentReviewRequestDTO;
import com.example.courtierprobackend.documents.presentationlayer.models.StageChecklistResponseDTO;
import com.example.courtierprobackend.email.EmailService;
import com.example.courtierprobackend.infrastructure.storage.ObjectStorageService;
import com.example.courtierprobackend.notifications.businesslayer.NotificationService;
import com.example.courtierprobackend.transactions.datalayer.Transaction;
import com.example.courtierprobackend.transactions.datalayer.enums.TransactionSide;
import com.example.courtierprobackend.transactions.datalayer.repositories.DocumentConditionLinkRepository;
import com.example.courtierprobackend.transactions.datalayer.repositories.TransactionParticipantRepository;
import com.example.courtierprobackend.transactions.datalayer.repositories.TransactionRepository;
import com.example.courtierprobackend.user.dataaccesslayer.UserAccount;
import com.example.courtierprobackend.user.dataaccesslayer.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.MessageSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DocumentServiceCoverageTest {

    @Mock private DocumentRepository repository;
    @Mock private TransactionRepository transactionRepository;
    @Mock private UserAccountRepository userAccountRepository;
    @Mock private ObjectStorageService storageService;
    @Mock private EmailService emailService;
    @Mock private NotificationService notificationService;
    @Mock private TimelineService timelineService;
    @Mock private MessageSource messageSource;
    @Mock private TransactionParticipantRepository participantRepository;
    @Mock private DocumentConditionLinkRepository documentConditionLinkRepository;
    @Mock private TransactionStageChecklistStateRepository checklistStateRepository;

    @InjectMocks
    private DocumentServiceImpl documentService;

    private UUID transactionId = UUID.randomUUID();
    private UUID documentId = UUID.randomUUID();
    private UUID brokerId = UUID.randomUUID();
    private UUID clientId = UUID.randomUUID();
    private UUID otherId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
    }

    // --- sendDocumentRequest ---

    @Test
    void sendDocumentRequest_notDraft_throws() {
        Document doc = new Document();
        doc.setDocumentId(documentId);
        doc.setStatus(DocumentStatusEnum.REQUESTED);
        doc.setTransactionRef(new TransactionRef(transactionId, null, TransactionSide.BUY_SIDE));

        when(repository.findByDocumentId(documentId)).thenReturn(Optional.of(doc));
        Transaction tx = new Transaction();
        tx.setTransactionId(transactionId);
        tx.setBrokerId(brokerId);
        when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));
        when(participantRepository.findByTransactionId(transactionId)).thenReturn(new ArrayList<>());
        when(userAccountRepository.findById(brokerId)).thenReturn(Optional.empty()); // Or mock user

        assertThatThrownBy(() -> documentService.sendDocumentRequest(documentId, brokerId))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Only draft documents can be sent");
    }

    @Test
    void sendDocumentRequest_requiresSignatureButNoVersions_throws() {
        Document doc = new Document();
        doc.setDocumentId(documentId);
        doc.setStatus(DocumentStatusEnum.DRAFT);
        doc.setRequiresSignature(true);
        doc.setVersions(new ArrayList<>()); // Empty versions
        doc.setTransactionRef(new TransactionRef(transactionId, null, TransactionSide.BUY_SIDE));

        when(repository.findByDocumentId(documentId)).thenReturn(Optional.of(doc));
        Transaction tx = new Transaction();
        tx.setTransactionId(transactionId);
        tx.setBrokerId(brokerId);
        when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));
        when(participantRepository.findByTransactionId(transactionId)).thenReturn(new ArrayList<>());

        assertThatThrownBy(() -> documentService.sendDocumentRequest(documentId, brokerId))
                 .isInstanceOf(BadRequestException.class)
                 .hasMessageContaining("Source document must be attached");
    }

    // --- shareDocumentWithClient ---

    @Test
    void shareDocumentWithClient_notDraft_throws() {
        Document doc = new Document();
        doc.setDocumentId(documentId);
        doc.setStatus(DocumentStatusEnum.SUBMITTED);
        doc.setTransactionRef(new TransactionRef(transactionId, null, TransactionSide.BUY_SIDE));

        when(repository.findByDocumentId(documentId)).thenReturn(Optional.of(doc));
        Transaction tx = new Transaction();
        tx.setTransactionId(transactionId);
        tx.setBrokerId(brokerId);
        when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));
        when(participantRepository.findByTransactionId(transactionId)).thenReturn(new ArrayList<>());

        assertThatThrownBy(() -> documentService.shareDocumentWithClient(documentId, brokerId))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Only draft documents can be shared");
    }

    @Test
    void shareDocumentWithClient_wrongFlow_throws() {
        Document doc = new Document();
        doc.setDocumentId(documentId);
        doc.setStatus(DocumentStatusEnum.DRAFT);
        doc.setFlow(DocumentFlowEnum.REQUEST); // Not UPLOAD
        doc.setTransactionRef(new TransactionRef(transactionId, null, TransactionSide.BUY_SIDE));

        when(repository.findByDocumentId(documentId)).thenReturn(Optional.of(doc));
        Transaction tx = new Transaction();
        tx.setTransactionId(transactionId);
        tx.setBrokerId(brokerId);
        when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));
        when(participantRepository.findByTransactionId(transactionId)).thenReturn(new ArrayList<>());

        assertThatThrownBy(() -> documentService.shareDocumentWithClient(documentId, brokerId))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Only UPLOAD flow documents");
    }

    // --- reviewDocument ---

    @Test
    void reviewDocument_notSubmitted_throws() {
        Document doc = new Document();
        doc.setDocumentId(documentId);
        doc.setStatus(DocumentStatusEnum.DRAFT);
        doc.setTransactionRef(new TransactionRef(transactionId, null, TransactionSide.BUY_SIDE));

        when(repository.findByDocumentId(documentId)).thenReturn(Optional.of(doc));
        Transaction tx = new Transaction();
        tx.setTransactionId(transactionId);
        tx.setBrokerId(brokerId);
        when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));
        when(participantRepository.findByTransactionId(transactionId)).thenReturn(new ArrayList<>());

        DocumentReviewRequestDTO review = new DocumentReviewRequestDTO();
        review.setDecision(DocumentStatusEnum.APPROVED);

        assertThatThrownBy(() -> documentService.reviewDocument(transactionId, documentId, review, brokerId))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Only submitted documents can be reviewed");
    }

    @Test
    void reviewDocument_transactionMismatch_throws() {
        Document doc = new Document();
        doc.setDocumentId(documentId);
        doc.setTransactionRef(new TransactionRef(UUID.randomUUID(), null, TransactionSide.BUY_SIDE)); // Wrong Tx ID

        when(repository.findByDocumentId(documentId)).thenReturn(Optional.of(doc));
        
        DocumentReviewRequestDTO review = new DocumentReviewRequestDTO();

        assertThatThrownBy(() -> documentService.reviewDocument(transactionId, documentId, review, brokerId))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Document does not belong to this transaction");
    }


    // --- Access Control (verifyBrokerOrCoManager) ---

    @Test
    void sendDocumentRequest_nonBroker_throwsForbidden() {
        Document doc = new Document();
        doc.setDocumentId(documentId);
        doc.setTransactionRef(new TransactionRef(transactionId, null, TransactionSide.BUY_SIDE));

        when(repository.findByDocumentId(documentId)).thenReturn(Optional.of(doc));
        Transaction tx = new Transaction();
        tx.setTransactionId(transactionId);
        tx.setBrokerId(brokerId);
        when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));
        
        // Mock empty participants, so only brokerId matches
        when(participantRepository.findByTransactionId(transactionId)).thenReturn(new ArrayList<>());
        
        // User is otherId, not brokerId
        when(userAccountRepository.findById(otherId)).thenReturn(Optional.of(new UserAccount()));

        // Use random ID not matching brokerId
        assertThatThrownBy(() -> documentService.sendDocumentRequest(documentId, otherId))
                .isInstanceOf(ForbiddenException.class);
    }
    
    // --- getStageChecklist ---

    @Test
    void getStageChecklist_viewAccessDenied_throws() {
        Transaction tx = new Transaction();
        tx.setTransactionId(transactionId);
        tx.setBrokerId(brokerId);
        tx.setClientId(clientId);
        
        when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));
        when(participantRepository.findByTransactionId(transactionId)).thenReturn(new ArrayList<>());

        // User is otherId, not client or broker
        when(userAccountRepository.findById(otherId)).thenReturn(Optional.of(new UserAccount()));

        assertThatThrownBy(() -> documentService.getStageChecklist(transactionId, "CONDITIONAL", otherId))
                 .isInstanceOf(ForbiddenException.class);
    }

    // --- deleteDocument ---

    @Test
    void deleteDocument_notDraft_throws() {
        Document doc = new Document();
        doc.setDocumentId(documentId);
        doc.setStatus(DocumentStatusEnum.SUBMITTED);
        doc.setTransactionRef(new TransactionRef(transactionId, null, TransactionSide.BUY_SIDE));

        when(repository.findByDocumentId(documentId)).thenReturn(Optional.of(doc));
        Transaction tx = new Transaction();
        tx.setTransactionId(transactionId);
        tx.setBrokerId(brokerId);
        when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));
        when(participantRepository.findByTransactionId(transactionId)).thenReturn(new ArrayList<>());

        assertThatThrownBy(() -> documentService.deleteDocument(documentId, brokerId))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Only draft documents can be deleted");
    }

    @Test
    void deleteDocument_notFound_throws() {
        when(repository.findByDocumentId(documentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> documentService.deleteDocument(documentId, brokerId))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Document not found");
    }

    // --- setChecklistManualState ---

    @Test
    void setChecklistManualState_unknownItem_throws() {
        Transaction tx = new Transaction();
        tx.setTransactionId(transactionId);
        tx.setSide(TransactionSide.BUY_SIDE);
        tx.setBrokerId(brokerId);
        
        when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));
        when(participantRepository.findByTransactionId(transactionId)).thenReturn(new ArrayList<>());
        when(userAccountRepository.findById(brokerId)).thenReturn(Optional.of(new UserAccount()));

        assertThatThrownBy(() -> documentService.setChecklistManualState(transactionId, "BUYER_OFFER_AND_NEGOTIATION", "unknown_key", true, brokerId))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Unknown checklist item");
    }

    // --- uploadFileToDocument ---

    @Test
    void uploadFileToDocument_wrongTransaction_throws() {
        Document doc = new Document();
        doc.setDocumentId(documentId);
        doc.setTransactionRef(new TransactionRef(UUID.randomUUID(), null, TransactionSide.BUY_SIDE));

        when(repository.findByDocumentId(documentId)).thenReturn(Optional.of(doc));

        assertThatThrownBy(() -> documentService.uploadFileToDocument(transactionId, documentId, null, brokerId, null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Document does not belong to transaction");
    }
}
