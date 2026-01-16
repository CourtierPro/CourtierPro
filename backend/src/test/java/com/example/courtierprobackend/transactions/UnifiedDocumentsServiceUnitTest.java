package com.example.courtierprobackend.transactions;

import com.example.courtierprobackend.audit.timeline_audit.businesslayer.TimelineService;
import com.example.courtierprobackend.common.exceptions.ForbiddenException;
import com.example.courtierprobackend.common.exceptions.NotFoundException;
import com.example.courtierprobackend.documents.datalayer.DocumentRequest;
import com.example.courtierprobackend.documents.datalayer.DocumentRequestRepository;
import com.example.courtierprobackend.documents.datalayer.SubmittedDocument;
import com.example.courtierprobackend.documents.datalayer.enums.DocumentStatusEnum;
import com.example.courtierprobackend.documents.datalayer.enums.DocumentTypeEnum;
import com.example.courtierprobackend.documents.datalayer.valueobjects.StorageObject;
import com.example.courtierprobackend.email.EmailService;
import com.example.courtierprobackend.infrastructure.storage.S3StorageService;
import com.example.courtierprobackend.notifications.businesslayer.NotificationService;
import com.example.courtierprobackend.transactions.businesslayer.TransactionServiceImpl;
import com.example.courtierprobackend.transactions.datalayer.*;
import com.example.courtierprobackend.transactions.datalayer.dto.UnifiedDocumentDTO;
import com.example.courtierprobackend.transactions.datalayer.enums.BuyerOfferStatus;
import com.example.courtierprobackend.transactions.datalayer.enums.ReceivedOfferStatus;
import com.example.courtierprobackend.transactions.datalayer.enums.TransactionSide;
import com.example.courtierprobackend.transactions.datalayer.enums.TransactionStatus;
import com.example.courtierprobackend.transactions.datalayer.repositories.*;
import com.example.courtierprobackend.user.dataaccesslayer.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * Unit tests for getAllTransactionDocuments method in TransactionServiceImpl.
 * Tests the unified document aggregation from all sources.
 */
@ExtendWith(MockitoExtension.class)
class UnifiedDocumentsServiceUnitTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private OfferRepository offerRepository;

    @Mock
    private PropertyRepository propertyRepository;

    @Mock
    private PropertyOfferRepository propertyOfferRepository;

    @Mock
    private OfferDocumentRepository offerDocumentRepository;

    @Mock
    private DocumentRequestRepository documentRequestRepository;

    @Mock
    private PinnedTransactionRepository pinnedTransactionRepository;

    @Mock
    private TransactionParticipantRepository participantRepository;

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private TimelineService timelineService;

    @Mock
    private ConditionRepository conditionRepository;

    @Mock
    private OfferRevisionRepository offerRevisionRepository;

    @Mock
    private S3StorageService s3StorageService;

    @InjectMocks
    private TransactionServiceImpl service;

    private UUID transactionId;
    private UUID brokerId;
    private UUID clientId;
    private Transaction transaction;

    @BeforeEach
    void setUp() {
        transactionId = UUID.randomUUID();
        brokerId = UUID.randomUUID();
        clientId = UUID.randomUUID();

        transaction = new Transaction();
        transaction.setTransactionId(transactionId);
        transaction.setBrokerId(brokerId);
        transaction.setClientId(clientId);
        transaction.setSide(TransactionSide.SELL_SIDE);
        transaction.setStatus(TransactionStatus.ACTIVE);
    }

    @Nested
    @DisplayName("getAllTransactionDocuments")
    class GetAllTransactionDocumentsTests {

        @Test
        @DisplayName("should aggregate documents from all sources")
        void getAllDocuments_allSources_returnsAggregated() {
            // Setup document request with submitted document
            DocumentRequest docRequest = createDocumentRequest();
            when(documentRequestRepository.findByTransactionRef_TransactionId(transactionId))
                    .thenReturn(List.of(docRequest));

            // Setup offer with document
            Offer offer = createOffer();
            OfferDocument offerDoc = createOfferDocument(offer.getOfferId(), null);
            when(offerRepository.findByTransactionIdOrderByCreatedAtDesc(transactionId))
                    .thenReturn(List.of(offer));
            when(offerDocumentRepository.findByOfferIdOrderByCreatedAtDesc(offer.getOfferId()))
                    .thenReturn(List.of(offerDoc));

            // Setup property offer with document
            Property property = createProperty();
            PropertyOffer propertyOffer = createPropertyOffer(property.getPropertyId());
            OfferDocument propOfferDoc = createOfferDocument(null, propertyOffer.getPropertyOfferId());
            when(propertyRepository.findByTransactionIdOrderByCreatedAtDesc(transactionId))
                    .thenReturn(List.of(property));
            when(propertyOfferRepository.findByPropertyIdOrderByOfferRoundDesc(property.getPropertyId()))
                    .thenReturn(List.of(propertyOffer));
            when(offerDocumentRepository.findByPropertyOfferIdOrderByCreatedAtDesc(propertyOffer.getPropertyOfferId()))
                    .thenReturn(List.of(propOfferDoc));

            when(transactionRepository.findByTransactionId(transactionId))
                    .thenReturn(Optional.of(transaction));

            List<UnifiedDocumentDTO> result = service.getAllTransactionDocuments(transactionId, brokerId, true);

            assertThat(result).hasSize(3);
            assertThat(result).extracting("source")
                    .containsExactlyInAnyOrder("CLIENT_UPLOAD", "OFFER_ATTACHMENT", "PROPERTY_OFFER_ATTACHMENT");
        }

        @Test
        @DisplayName("should return empty list when no documents")
        void getAllDocuments_noDocuments_returnsEmptyList() {
            when(transactionRepository.findByTransactionId(transactionId))
                    .thenReturn(Optional.of(transaction));
            when(documentRequestRepository.findByTransactionRef_TransactionId(transactionId))
                    .thenReturn(List.of());
            when(offerRepository.findByTransactionIdOrderByCreatedAtDesc(transactionId))
                    .thenReturn(List.of());
            when(propertyRepository.findByTransactionIdOrderByCreatedAtDesc(transactionId))
                    .thenReturn(List.of());

            List<UnifiedDocumentDTO> result = service.getAllTransactionDocuments(transactionId, brokerId, true);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should throw NotFoundException when transaction not found")
        void getAllDocuments_transactionNotFound_throws() {
            when(transactionRepository.findByTransactionId(transactionId))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getAllTransactionDocuments(transactionId, brokerId, true))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessage("Transaction not found");
        }

        @Test
        @DisplayName("should throw ForbiddenException when user has no access")
        void getAllDocuments_noAccess_throws() {
            UUID unauthorizedUserId = UUID.randomUUID();
            when(transactionRepository.findByTransactionId(transactionId))
                    .thenReturn(Optional.of(transaction));

            assertThatThrownBy(() -> service.getAllTransactionDocuments(transactionId, unauthorizedUserId, true))
                    .isInstanceOf(ForbiddenException.class);
        }

        @Test
        @DisplayName("should allow client access")
        void getAllDocuments_asClient_allowed() {
            when(transactionRepository.findByTransactionId(transactionId))
                    .thenReturn(Optional.of(transaction));
            when(documentRequestRepository.findByTransactionRef_TransactionId(transactionId))
                    .thenReturn(List.of());
            when(offerRepository.findByTransactionIdOrderByCreatedAtDesc(transactionId))
                    .thenReturn(List.of());
            when(propertyRepository.findByTransactionIdOrderByCreatedAtDesc(transactionId))
                    .thenReturn(List.of());

            List<UnifiedDocumentDTO> result = service.getAllTransactionDocuments(transactionId, clientId, false);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should sort documents by uploadedAt descending")
        void getAllDocuments_sortsByUploadedAt() {
            // Setup two offers with documents with different dates
            Offer offer1 = createOffer();
            OfferDocument doc1 = createOfferDocument(offer1.getOfferId(), null);
            doc1.setCreatedAt(LocalDateTime.now().minusDays(2));

            Offer offer2 = createOffer();
            OfferDocument doc2 = createOfferDocument(offer2.getOfferId(), null);
            doc2.setCreatedAt(LocalDateTime.now());

            when(transactionRepository.findByTransactionId(transactionId))
                    .thenReturn(Optional.of(transaction));
            when(documentRequestRepository.findByTransactionRef_TransactionId(transactionId))
                    .thenReturn(List.of());
            when(offerRepository.findByTransactionIdOrderByCreatedAtDesc(transactionId))
                    .thenReturn(List.of(offer1, offer2));
            when(offerDocumentRepository.findByOfferIdOrderByCreatedAtDesc(offer1.getOfferId()))
                    .thenReturn(List.of(doc1));
            when(offerDocumentRepository.findByOfferIdOrderByCreatedAtDesc(offer2.getOfferId()))
                    .thenReturn(List.of(doc2));
            when(propertyRepository.findByTransactionIdOrderByCreatedAtDesc(transactionId))
                    .thenReturn(List.of());

            List<UnifiedDocumentDTO> result = service.getAllTransactionDocuments(transactionId, brokerId, true);

            assertThat(result).hasSize(2);
            // First should be the newer one (doc2)
            assertThat(result.get(0).getUploadedAt()).isAfter(result.get(1).getUploadedAt());
        }
    }

    // ==================== Helper Methods ====================

    private DocumentRequest createDocumentRequest() {
        UUID requestId = UUID.randomUUID();
        DocumentRequest request = new DocumentRequest();
        request.setRequestId(requestId);
        request.setDocType(DocumentTypeEnum.MORTGAGE_PRE_APPROVAL);
        request.setStatus(DocumentStatusEnum.APPROVED);
        
        StorageObject storage = StorageObject.builder()
                .s3Key("documents/test.pdf")
                .fileName("test.pdf")
                .mimeType("application/pdf")
                .sizeBytes(1024L)
                .build();
        
        SubmittedDocument submitted = new SubmittedDocument();
        submitted.setDocumentId(UUID.randomUUID());
        submitted.setUploadedAt(LocalDateTime.now());
        submitted.setStorageObject(storage);
        submitted.setDocumentRequest(request);
        
        request.setSubmittedDocuments(new ArrayList<>(List.of(submitted)));
        return request;
    }

    private Offer createOffer() {
        return Offer.builder()
                .id(1L)
                .offerId(UUID.randomUUID())
                .transactionId(transactionId)
                .buyerName("John Doe")
                .offerAmount(BigDecimal.valueOf(500000))
                .status(ReceivedOfferStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private Property createProperty() {
        return Property.builder()
                .id(1L)
                .propertyId(UUID.randomUUID())
                .transactionId(transactionId)
                .createdAt(LocalDateTime.now())
                .build();
    }

    private PropertyOffer createPropertyOffer(UUID propertyId) {
        return PropertyOffer.builder()
                .id(1L)
                .propertyOfferId(UUID.randomUUID())
                .propertyId(propertyId)
                .offerAmount(BigDecimal.valueOf(500000))
                .status(BuyerOfferStatus.OFFER_MADE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private OfferDocument createOfferDocument(UUID offerId, UUID propertyOfferId) {
        return OfferDocument.builder()
                .id(1L)
                .documentId(UUID.randomUUID())
                .offerId(offerId)
                .propertyOfferId(propertyOfferId)
                .s3Key("documents/test.pdf")
                .fileName("test.pdf")
                .mimeType("application/pdf")
                .sizeBytes(1024L)
                .uploadedBy(brokerId)
                .createdAt(LocalDateTime.now())
                .build();
    }
}
