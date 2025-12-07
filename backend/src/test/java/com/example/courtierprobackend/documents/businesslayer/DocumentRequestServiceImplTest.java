package com.example.courtierprobackend.documents.businesslayer;

import com.example.courtierprobackend.documents.datalayer.DocumentRequest;
import com.example.courtierprobackend.documents.datalayer.DocumentRequestRepository;
import com.example.courtierprobackend.documents.datalayer.SubmittedDocument;
import com.example.courtierprobackend.documents.datalayer.enums.DocumentPartyEnum;
import com.example.courtierprobackend.documents.datalayer.enums.DocumentStatusEnum;
import com.example.courtierprobackend.documents.datalayer.enums.DocumentTypeEnum;
import com.example.courtierprobackend.documents.datalayer.enums.UploadedByRefEnum;
import com.example.courtierprobackend.documents.datalayer.valueobjects.StorageObject;
import com.example.courtierprobackend.documents.datalayer.valueobjects.TransactionRef;
import com.example.courtierprobackend.documents.presentationlayer.models.DocumentRequestRequestDTO;
import com.example.courtierprobackend.documents.presentationlayer.models.DocumentRequestResponseDTO;
import com.example.courtierprobackend.email.EmailService;
import com.example.courtierprobackend.infrastructure.storage.S3StorageService;
import com.example.courtierprobackend.transactions.datalayer.Transaction;
import com.example.courtierprobackend.transactions.datalayer.enums.TransactionSide;
import com.example.courtierprobackend.transactions.datalayer.repositories.TransactionRepository;
import com.example.courtierprobackend.user.dataaccesslayer.UserAccount;
import com.example.courtierprobackend.user.dataaccesslayer.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for DocumentRequestServiceImpl.
 */
@ExtendWith(MockitoExtension.class)
class DocumentRequestServiceImplTest {

    @Mock
    private DocumentRequestRepository repository;

    @Mock
    private S3StorageService storageService;

    @Mock
    private EmailService emailService;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private UserAccountRepository userAccountRepository;

    @InjectMocks
    private DocumentRequestServiceImpl service;

    private DocumentRequest sampleRequest;
    private Transaction sampleTransaction;
    private UserAccount sampleBroker;

    @BeforeEach
    void setUp() {
        TransactionRef transactionRef = TransactionRef.builder()
                .transactionId("TX-123")
                .clientId("CLIENT-1")
                .side(TransactionSide.BUY_SIDE)
                .build();

        sampleRequest = DocumentRequest.builder()
                .id(1L)
                .requestId("REQ-001")
                .transactionRef(transactionRef)
                .docType(DocumentTypeEnum.PROOF_OF_FUNDS)
                .customTitle("Proof of Funds")
                .status(DocumentStatusEnum.REQUESTED)
                .expectedFrom(DocumentPartyEnum.CLIENT)
                .visibleToClient(true)
                .brokerNotes("Please upload bank statement")
                .lastUpdatedAt(LocalDateTime.now())
                .submittedDocuments(new ArrayList<>())
                .build();

        sampleTransaction = new Transaction();
        sampleTransaction.setTransactionId("TX-123");
        sampleTransaction.setClientId("CLIENT-1");
        sampleTransaction.setBrokerId("BROKER-1");
        sampleTransaction.setSide(TransactionSide.BUY_SIDE);

        sampleBroker = new UserAccount();
        sampleBroker.setId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        sampleBroker.setEmail("broker@example.com");
    }

    // ==================== getDocumentsForTransaction ====================

    @Test
    void getDocumentsForTransaction_success_returnsList() {
        when(repository.findByTransactionRef_TransactionId("TX-123"))
                .thenReturn(List.of(sampleRequest));

        List<DocumentRequestResponseDTO> result = service.getDocumentsForTransaction("TX-123");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getRequestId()).isEqualTo("REQ-001");
        verify(repository).findByTransactionRef_TransactionId("TX-123");
    }

    @Test
    void getDocumentsForTransaction_emptyList_returnsEmpty() {
        when(repository.findByTransactionRef_TransactionId("TX-999"))
                .thenReturn(List.of());

        List<DocumentRequestResponseDTO> result = service.getDocumentsForTransaction("TX-999");

        assertThat(result).isEmpty();
    }

    // ==================== getDocumentRequest ====================

    @Test
    void getDocumentRequest_success_returnsDTO() {
        when(repository.findByRequestId("REQ-001"))
                .thenReturn(Optional.of(sampleRequest));

        DocumentRequestResponseDTO result = service.getDocumentRequest("REQ-001");

        assertThat(result.getRequestId()).isEqualTo("REQ-001");
        assertThat(result.getDocType()).isEqualTo(DocumentTypeEnum.PROOF_OF_FUNDS);
    }

    @Test
    void getDocumentRequest_notFound_throwsException() {
        when(repository.findByRequestId("REQ-999"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getDocumentRequest("REQ-999"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Document request not found");
    }

    // ==================== createDocumentRequest ====================

    @Test
    void createDocumentRequest_success_createsAndReturnsDTO() {
        when(transactionRepository.findByTransactionId("TX-123"))
                .thenReturn(Optional.of(sampleTransaction));
        when(repository.save(any(DocumentRequest.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        DocumentRequestRequestDTO dto = DocumentRequestRequestDTO.builder()
                .docType(DocumentTypeEnum.PROOF_OF_FUNDS)
                .customTitle("Proof of Funds")
                .expectedFrom(DocumentPartyEnum.CLIENT)
                .visibleToClient(true)
                .brokerNotes("Please upload")
                .build();

        DocumentRequestResponseDTO result = service.createDocumentRequest("TX-123", dto);

        assertThat(result.getDocType()).isEqualTo(DocumentTypeEnum.PROOF_OF_FUNDS);
        assertThat(result.getStatus()).isEqualTo(DocumentStatusEnum.REQUESTED);
        assertThat(result.getRequestId()).isNotNull();
        verify(repository).save(any(DocumentRequest.class));
    }

    @Test
    void createDocumentRequest_transactionNotFound_throwsException() {
        when(transactionRepository.findByTransactionId("TX-999"))
                .thenReturn(Optional.empty());

        DocumentRequestRequestDTO dto = DocumentRequestRequestDTO.builder()
                .docType(DocumentTypeEnum.PROOF_OF_FUNDS)
                .build();

        assertThatThrownBy(() -> service.createDocumentRequest("TX-999", dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Transaction not found");
    }

    @Test
    void createDocumentRequest_visibleToClientDefaultsToTrue() {
        when(transactionRepository.findByTransactionId("TX-123"))
                .thenReturn(Optional.of(sampleTransaction));
        when(repository.save(any(DocumentRequest.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        DocumentRequestRequestDTO dto = DocumentRequestRequestDTO.builder()
                .docType(DocumentTypeEnum.PROOF_OF_FUNDS)
                .visibleToClient(null)
                .build();

        DocumentRequestResponseDTO result = service.createDocumentRequest("TX-123", dto);

        assertThat(result.isVisibleToClient()).isTrue();
    }

    // ==================== updateDocumentRequest ====================

    @Test
    void updateDocumentRequest_success_updatesAllFields() {
        when(repository.findByRequestId("REQ-001"))
                .thenReturn(Optional.of(sampleRequest));
        when(repository.save(any(DocumentRequest.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        DocumentRequestRequestDTO dto = DocumentRequestRequestDTO.builder()
                .docType(DocumentTypeEnum.MORTGAGE_APPROVAL)
                .customTitle("Updated Title")
                .expectedFrom(DocumentPartyEnum.LENDER)
                .visibleToClient(false)
                .brokerNotes("Updated notes")
                .build();

        DocumentRequestResponseDTO result = service.updateDocumentRequest("REQ-001", dto);

        assertThat(result.getDocType()).isEqualTo(DocumentTypeEnum.MORTGAGE_APPROVAL);
        assertThat(result.getCustomTitle()).isEqualTo("Updated Title");
        assertThat(result.getExpectedFrom()).isEqualTo(DocumentPartyEnum.LENDER);
        assertThat(result.isVisibleToClient()).isFalse();
        assertThat(result.getBrokerNotes()).isEqualTo("Updated notes");
    }

    @Test
    void updateDocumentRequest_partialUpdate_onlyUpdatesProvidedFields() {
        when(repository.findByRequestId("REQ-001"))
                .thenReturn(Optional.of(sampleRequest));
        when(repository.save(any(DocumentRequest.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        DocumentRequestRequestDTO dto = DocumentRequestRequestDTO.builder()
                .customTitle("Only Title Updated")
                .build();

        DocumentRequestResponseDTO result = service.updateDocumentRequest("REQ-001", dto);

        assertThat(result.getCustomTitle()).isEqualTo("Only Title Updated");
        assertThat(result.getDocType()).isEqualTo(DocumentTypeEnum.PROOF_OF_FUNDS); // unchanged
    }

    @Test
    void updateDocumentRequest_notFound_throwsException() {
        when(repository.findByRequestId("REQ-999"))
                .thenReturn(Optional.empty());

        DocumentRequestRequestDTO dto = DocumentRequestRequestDTO.builder().build();

        assertThatThrownBy(() -> service.updateDocumentRequest("REQ-999", dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Document request not found");
    }

    // ==================== deleteDocumentRequest ====================

    @Test
    void deleteDocumentRequest_success_deletesRequest() {
        when(repository.findByRequestId("REQ-001"))
                .thenReturn(Optional.of(sampleRequest));

        service.deleteDocumentRequest("REQ-001");

        verify(repository).delete(sampleRequest);
    }

    @Test
    void deleteDocumentRequest_notFound_throwsException() {
        when(repository.findByRequestId("REQ-999"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteDocumentRequest("REQ-999"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Document request not found");
    }

    // ==================== submitDocument ====================

    @Test
    void submitDocument_success_uploadsFileAndSendsEmail() throws IOException {
        sampleTransaction.setBrokerId("00000000-0000-0000-0000-000000000001");
        
        when(repository.findByRequestId("REQ-001"))
                .thenReturn(Optional.of(sampleRequest));
        when(transactionRepository.findByTransactionId("TX-123"))
                .thenReturn(Optional.of(sampleTransaction));
        when(userAccountRepository.findById(UUID.fromString("00000000-0000-0000-0000-000000000001")))
                .thenReturn(Optional.of(sampleBroker));

        StorageObject storageObject = StorageObject.builder()
                .s3Key("transactions/TX-123/documents/REQ-001/file.pdf")
                .fileName("file.pdf")
                .mimeType("application/pdf")
                .sizeBytes(12345L)
                .build();

        MultipartFile mockFile = mock(MultipartFile.class);
        when(mockFile.getOriginalFilename()).thenReturn("file.pdf");
        when(mockFile.getContentType()).thenReturn("application/pdf");
        when(mockFile.getSize()).thenReturn(12345L);

        when(storageService.uploadFile(mockFile, "TX-123", "REQ-001"))
                .thenReturn(storageObject);
        when(repository.save(any(DocumentRequest.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        DocumentRequestResponseDTO result = service.submitDocument(
                "TX-123", "REQ-001", mockFile, "USER-1", UploadedByRefEnum.CLIENT
        );

        assertThat(result.getStatus()).isEqualTo(DocumentStatusEnum.SUBMITTED);
        assertThat(result.getSubmittedDocuments()).hasSize(1);
        verify(emailService).sendDocumentSubmittedNotification(any(), eq("broker@example.com"));
    }

    @Test
    void submitDocument_transactionMismatch_throwsException() throws IOException {
        TransactionRef differentRef = TransactionRef.builder()
                .transactionId("TX-DIFFERENT")
                .build();
        sampleRequest.setTransactionRef(differentRef);

        when(repository.findByRequestId("REQ-001"))
                .thenReturn(Optional.of(sampleRequest));

        MultipartFile mockFile = mock(MultipartFile.class);

        assertThatThrownBy(() -> service.submitDocument(
                "TX-123", "REQ-001", mockFile, "USER-1", UploadedByRefEnum.CLIENT
        ))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("does not belong to transaction");
    }

    @Test
    void submitDocument_requestNotFound_throwsException() throws IOException {
        when(repository.findByRequestId("REQ-999"))
                .thenReturn(Optional.empty());

        MultipartFile mockFile = mock(MultipartFile.class);

        assertThatThrownBy(() -> service.submitDocument(
                "TX-123", "REQ-999", mockFile, "USER-1", UploadedByRefEnum.CLIENT
        ))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Document request not found");
    }

    // ==================== Mapping Tests ====================

    @Test
    void mapToResponseDTO_includesSubmittedDocuments() {
        SubmittedDocument submittedDoc = SubmittedDocument.builder()
                .documentId("DOC-001")
                .uploadedAt(LocalDateTime.now())
                .storageObject(StorageObject.builder()
                        .s3Key("key")
                        .fileName("test.pdf")
                        .mimeType("application/pdf")
                        .sizeBytes(100L)
                        .build())
                .build();
        sampleRequest.getSubmittedDocuments().add(submittedDoc);

        when(repository.findByRequestId("REQ-001"))
                .thenReturn(Optional.of(sampleRequest));

        DocumentRequestResponseDTO result = service.getDocumentRequest("REQ-001");

        assertThat(result.getSubmittedDocuments()).hasSize(1);
        assertThat(result.getSubmittedDocuments().get(0).getDocumentId()).isEqualTo("DOC-001");
    }
}
