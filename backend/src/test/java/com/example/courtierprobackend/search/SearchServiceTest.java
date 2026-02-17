package com.example.courtierprobackend.search;

import com.example.courtierprobackend.documents.datalayer.Document;
import com.example.courtierprobackend.documents.datalayer.DocumentRepository;
import com.example.courtierprobackend.documents.datalayer.enums.DocumentTypeEnum;
import com.example.courtierprobackend.documents.datalayer.valueobjects.TransactionRef;
import com.example.courtierprobackend.search.dto.SearchResultDTO;
import com.example.courtierprobackend.security.UserContextFilter;
import com.example.courtierprobackend.transactions.datalayer.PropertyAddress;
import com.example.courtierprobackend.transactions.datalayer.Transaction;
import com.example.courtierprobackend.transactions.datalayer.repositories.TransactionRepository;
import com.example.courtierprobackend.appointments.datalayer.Appointment;
import com.example.courtierprobackend.appointments.datalayer.AppointmentRepository;
import com.example.courtierprobackend.user.dataaccesslayer.UserAccount;
import com.example.courtierprobackend.user.dataaccesslayer.UserAccountRepository;
import com.example.courtierprobackend.user.dataaccesslayer.UserRole;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class SearchServiceTest {

    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private DocumentRepository documentRequestRepository;
    @Mock
    private UserAccountRepository userAccountRepository;
    @Mock
    private AppointmentRepository appointmentRepository;
    @Mock
    private HttpServletRequest request;

    private SearchService searchService;

    private UUID userId;
    private UUID brokerId;
    private UUID transactionId;

    @BeforeEach
    void setUp() {
        searchService = new SearchService(
            transactionRepository,
            documentRequestRepository,
            userAccountRepository,
            appointmentRepository,
            request
        );
        userId = UUID.randomUUID();
        brokerId = UUID.randomUUID();
        transactionId = UUID.randomUUID();

        // Use lenient() to avoid UnnecessaryStubbingException in early-return tests
        lenient().when(request.getAttribute(UserContextFilter.INTERNAL_USER_ID_ATTR)).thenReturn(userId);
        lenient().when(request.getAttribute(UserContextFilter.USER_ROLE_ATTR)).thenReturn("BROKER");
    }

    // ========== Query Validation Tests ==========

    @Test
    void search_WithNullQuery_ReturnsEmpty() {
        List<SearchResultDTO> results = searchService.search(null);
        assertThat(results).isEmpty();
    }

    @Test
    void search_WithShortQuery_ReturnsEmpty() {
        List<SearchResultDTO> results = searchService.search("a");
        assertThat(results).isEmpty();
    }

    @Test
    void search_WithWhitespaceQuery_ReturnsEmpty() {
        List<SearchResultDTO> results = searchService.search("   ");
        assertThat(results).isEmpty();
    }

    // ========== UUID Search Tests ==========

    @Test
    void search_ByTransactionUUID_ReturnsTransaction() {
        Transaction transaction = createTestTransaction(transactionId, brokerId, userId);
        lenient().when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(transaction));
        lenient().when(userAccountRepository.searchClientsOfBroker(userId, transactionId.toString())).thenReturn(List.of());
        lenient().when(transactionRepository.searchTransactions(eq(userId), eq(transactionId.toString()))).thenReturn(List.of());
        lenient().when(documentRequestRepository.searchDocuments(userId, transactionId.toString())).thenReturn(List.of());
        lenient().when(appointmentRepository.searchAppointments(userId, transactionId.toString())).thenReturn(List.of());

        List<SearchResultDTO> results = searchService.search(transactionId.toString());

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getType()).isEqualTo(SearchResultDTO.SearchResultType.TRANSACTION);
        assertThat(results.get(0).getId()).isEqualTo(transactionId.toString());
    }

    @Test
    void search_ByTransactionUUID_FiltersUnauthorized() {
        UUID otherUser = UUID.randomUUID();
        Transaction transaction = createTestTransaction(transactionId, otherUser, UUID.randomUUID());
        lenient().when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(transaction));
        lenient().when(userAccountRepository.searchClientsOfBroker(userId, transactionId.toString())).thenReturn(List.of());
        lenient().when(transactionRepository.searchTransactions(eq(userId), eq(transactionId.toString()))).thenReturn(List.of());
        lenient().when(documentRequestRepository.searchDocuments(userId, transactionId.toString())).thenReturn(List.of());
        lenient().when(appointmentRepository.searchAppointments(userId, transactionId.toString())).thenReturn(List.of());

        List<SearchResultDTO> results = searchService.search(transactionId.toString());

        // Transaction should not be in results due to access check
        assertThat(results.stream()
                .filter(r -> r.getType() == SearchResultDTO.SearchResultType.TRANSACTION)
                .toList()).isEmpty();
    }

    // ========== Text Search Tests ==========

    @Test
    void search_TextQuery_FindsTransactionsByAddress() {
        String query = "123 Main";
        Transaction transaction = createTestTransaction(transactionId, brokerId, userId);
        
        lenient().when(userAccountRepository.searchClientsOfBroker(userId, query)).thenReturn(List.of());
        lenient().when(transactionRepository.searchTransactions(eq(userId), eq(query))).thenReturn(List.of(transaction));
        lenient().when(documentRequestRepository.searchDocuments(userId, query)).thenReturn(List.of());
        lenient().when(appointmentRepository.searchAppointments(userId, query)).thenReturn(List.of());

        List<SearchResultDTO> results = searchService.search(query);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getType()).isEqualTo(SearchResultDTO.SearchResultType.TRANSACTION);
        assertThat(results.get(0).getTitle()).isEqualTo("123 Main St");
    }

    @Test
    void search_TextQuery_FindsUsersByName() {
        String query = "John";
        UserAccount user = new UserAccount("auth0|123", "john@example.com", "John", "Doe", UserRole.CLIENT, "en");
        
        lenient().when(userAccountRepository.searchClientsOfBroker(userId, query)).thenReturn(List.of(user));
        lenient().when(transactionRepository.searchTransactions(eq(userId), eq(query))).thenReturn(List.of());
        lenient().when(documentRequestRepository.searchDocuments(userId, query)).thenReturn(List.of());
        lenient().when(appointmentRepository.searchAppointments(userId, query)).thenReturn(List.of());

        List<SearchResultDTO> results = searchService.search(query);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getType()).isEqualTo(SearchResultDTO.SearchResultType.USER);
        assertThat(results.get(0).getTitle()).isEqualTo("John Doe");
    }

    @Test
    void search_TextQuery_FindsDocumentsByTitle() {
        String query = "Promise";
        Transaction transaction = createTestTransaction(transactionId, brokerId, userId);
        Document document = createTestDocument(transactionId, "Promise to Purchase");
        
        lenient().when(userAccountRepository.searchClientsOfBroker(userId, query)).thenReturn(List.of());
        lenient().when(transactionRepository.searchTransactions(eq(userId), eq(query))).thenReturn(List.of());
        lenient().when(documentRequestRepository.searchDocuments(userId, query)).thenReturn(List.of(document));
        lenient().when(transactionRepository.findByTransactionIdIn(List.of(transactionId))).thenReturn(List.of(transaction));

        List<SearchResultDTO> results = searchService.search(query);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getType()).isEqualTo(SearchResultDTO.SearchResultType.DOCUMENT);
        assertThat(results.get(0).getTitle()).isEqualTo("Promise to Purchase");
        assertThat(results.get(0).getSubtitle()).isEqualTo("123 Main St");
    }

    // ========== Linked User Search Tests ==========

    @Test
    void search_LinkedUsers_IncludesRelatedTransactions() {
        String query = "Jane";
        UserAccount client = new UserAccount("auth0|456", "jane@example.com", "Jane", "Smith", UserRole.CLIENT, "en");
        Transaction linkedTransaction = createTestTransaction(transactionId, userId, client.getId());
        
        lenient().when(userAccountRepository.searchClientsOfBroker(userId, query)).thenReturn(List.of(client));
        lenient().when(transactionRepository.searchTransactions(eq(userId), eq(query))).thenReturn(List.of());
        lenient().when(transactionRepository.findLinkedToUsers(List.of(client.getId()), userId)).thenReturn(List.of(linkedTransaction));
        lenient().when(documentRequestRepository.searchDocuments(userId, query)).thenReturn(List.of());
        lenient().when(documentRequestRepository.findLinkedToUsers(List.of(client.getId()), userId)).thenReturn(List.of());

        List<SearchResultDTO> results = searchService.search(query);

        assertThat(results).hasSize(2); // User + Transaction
        assertThat(results.stream()
                .filter(r -> r.getType() == SearchResultDTO.SearchResultType.TRANSACTION)
                .toList()).hasSize(1);
    }

    // ========== Helper Methods ==========

    private Transaction createTestTransaction(UUID txId, UUID broker, UUID client) {
        PropertyAddress address = new PropertyAddress("123 Main St", "Montreal", "QC", "H1A 1A1");

        return Transaction.builder()
                .transactionId(txId)
                .brokerId(broker)
                .clientId(client)
                .propertyAddress(address)
                .build();
    }

    private Document createTestDocument(UUID txId, String customTitle) {
        TransactionRef txRef = TransactionRef.builder()
                .transactionId(txId)
                .clientId(userId)
                .build();
        
        return Document.builder()
                .documentId(UUID.randomUUID())
                .transactionRef(txRef)
                .docType(DocumentTypeEnum.PROMISE_TO_PURCHASE)
                .customTitle(customTitle)
                .build();
    }
    // ========== Mapper & Edge Case Tests ==========

    @Test
    void search_TransactionMap_HandlesNullAddressFields() {
        // Arrange
        String query = "123";
        Transaction tx = createTestTransaction(transactionId, brokerId, userId);
        tx.setPropertyAddress(new PropertyAddress(null, null, null, null)); // Null fields
        
        lenient().when(userAccountRepository.searchClientsOfBroker(userId, query)).thenReturn(List.of());
        lenient().when(transactionRepository.searchTransactions(eq(userId), eq(query))).thenReturn(List.of(tx));
        lenient().when(documentRequestRepository.searchDocuments(userId, query)).thenReturn(List.of());

        // Act
        List<SearchResultDTO> results = searchService.search(query);

        // Assert
        SearchResultDTO result = results.get(0);
        assertThat(result.getTitle()).isEqualTo("Unknown Address");
        assertThat(result.getSubtitle()).isEmpty();
    }

    @Test
    void search_TransactionMap_HandlesPartialAddress() {
        String query = "Montreal";
        Transaction tx = createTestTransaction(transactionId, brokerId, userId);
        tx.setPropertyAddress(new PropertyAddress("Street", "Montreal", null, "Zip")); // City only, no province
        
        lenient().when(transactionRepository.searchTransactions(eq(userId), eq(query))).thenReturn(List.of(tx));

        List<SearchResultDTO> results = searchService.search(query);
        assertThat(results.get(0).getSubtitle()).isEqualTo("Montreal");
    }
    
    @Test
    void search_UserMap_HandlesNullNames() {
        String query = "test";
        UserAccount user = new UserAccount("auth0|999", "test@mail.com", null, null, UserRole.CLIENT, "en");
        
        lenient().when(userAccountRepository.searchClientsOfBroker(userId, query)).thenReturn(List.of(user));

        List<SearchResultDTO> results = searchService.search(query);
        assertThat(results.get(0).getTitle()).isEqualTo("Unknown User");
    }
    
    @Test
    void search_CurrentUser_MatchesQuery_AndIsNotDuplicated() {
        // Prepare current user that matches 'john'
        UserAccount currentUser = new UserAccount("auth0|123", "me@mail.com", "John", "Me", UserRole.BROKER, "en");
        currentUser.setId(userId);
        
        // Prepare a client lookup that ALSO returns the current user (if that were possible) or just emptylist
        // We want to verify dedup logic if matchedUsers already contains it?
        // Service logic: matchedUsers = searchClientsOfBroker(...).
        // if (matchedUsers.stream().noneMatch(u -> u.getId().equals(userId))) { add currentUser }
        
        lenient().when(userAccountRepository.findById(userId)).thenReturn(Optional.of(currentUser));
        lenient().when(userAccountRepository.searchClientsOfBroker(userId, "John")).thenReturn(List.of()); // Empty clients

        List<SearchResultDTO> results = searchService.search("John");
        
        // Should find current user
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getId()).isEqualTo(userId.toString());
    }

    @Test
    void searchDocumentById_WhenNotRelated_ReturnsEmpty() {
        UUID docId = UUID.randomUUID();
        Document doc = createTestDocument(transactionId, "Doc");
        
        // Fix: Ensure the document belongs to a different client so isClient check fails
        TransactionRef originalRef = doc.getTransactionRef();
        TransactionRef otherClientRef = TransactionRef.builder()
                .transactionId(originalRef.getTransactionId())
                .clientId(UUID.randomUUID()) // Different user
                .side(originalRef.getSide())
                .build();
        doc.setTransactionRef(otherClientRef);
        
        // Transaction exists but user is neither client nor broker
        Transaction tx = createTestTransaction(transactionId, UUID.randomUUID(), UUID.randomUUID());
        
        lenient().when(documentRequestRepository.findByDocumentId(docId)).thenReturn(Optional.of(doc));
        // Strict stubbing complains if we don't stub the call made with docId (searchById -> searchTransactionById -> findByTransactionId(docId))
        lenient().when(transactionRepository.findByTransactionId(docId)).thenReturn(Optional.empty());
        lenient().when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(tx));

        List<SearchResultDTO> results = searchService.search(docId.toString());
        assertThat(results).isEmpty();
    }
    
    @Test
    void search_WithValidNonUuidString_SkipsIdSearch() {
        // "Valid" length string but not a UUID
        String query = "NotAUUIDString"; 
        
        lenient().when(transactionRepository.searchTransactions(eq(userId), eq(query))).thenReturn(List.of());
        
        // Should not throw exception
        List<SearchResultDTO> results = searchService.search(query);
        assertThat(results).isEmpty();
    }

    @Test
    void search_Users_WhenNoneFound_ReturnsEmpty() {
        String query = "Ghost";
        lenient().when(userAccountRepository.searchClientsOfBroker(userId, query)).thenReturn(List.of());
        lenient().when(transactionRepository.searchTransactions(eq(userId), eq(query))).thenReturn(List.of());
        lenient().when(documentRequestRepository.searchDocuments(userId, query)).thenReturn(List.of());
        lenient().when(appointmentRepository.searchAppointments(userId, query)).thenReturn(List.of());
        List<SearchResultDTO> results = searchService.search(query);
        assertThat(results).isEmpty();
    }

    // ========== Appointment Search Tests ==========

    @Test
    void search_TextQuery_FindsAppointmentsByTitle() {
        String query = "Meeting";
        Appointment appointment = Appointment.builder()
                .appointmentId(UUID.randomUUID())
                .title("Client Meeting")
                .fromDateTime(java.time.LocalDateTime.now())
                .location("Office")
                .build();

        lenient().when(userAccountRepository.searchClientsOfBroker(userId, query)).thenReturn(List.of());
        lenient().when(transactionRepository.searchTransactions(eq(userId), eq(query))).thenReturn(List.of());
        lenient().when(documentRequestRepository.searchDocuments(userId, query)).thenReturn(List.of());
        lenient().when(appointmentRepository.searchAppointments(userId, query)).thenReturn(List.of(appointment));

        List<SearchResultDTO> results = searchService.search(query);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getType()).isEqualTo(SearchResultDTO.SearchResultType.APPOINTMENT);
        assertThat(results.get(0).getTitle()).isEqualTo("Client Meeting");
        assertThat(results.get(0).getSubtitle()).contains("Office");
    }

    @Test
    void search_AppointmentMap_HandlesNullLocation() {
        String query = "Call";
        Appointment appointment = Appointment.builder()
                .appointmentId(UUID.randomUUID())
                .title("Phone Call")
                .fromDateTime(java.time.LocalDateTime.now())
                .location(null) // Null location
                .build();

        lenient().when(appointmentRepository.searchAppointments(userId, query)).thenReturn(List.of(appointment));

        List<SearchResultDTO> results = searchService.search(query);
        
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getSubtitle()).doesNotContain("•"); // Should be just date/time
    }
}
