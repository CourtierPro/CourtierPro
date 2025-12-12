package com.example.courtierprobackend.search;

import com.example.courtierprobackend.documents.datalayer.DocumentRequest;
import com.example.courtierprobackend.documents.datalayer.DocumentRequestRepository;
import com.example.courtierprobackend.documents.datalayer.enums.DocumentTypeEnum;
import com.example.courtierprobackend.documents.datalayer.valueobjects.TransactionRef;
import com.example.courtierprobackend.search.dto.SearchResultDTO;
import com.example.courtierprobackend.security.UserContextFilter;
import com.example.courtierprobackend.transactions.datalayer.PropertyAddress;
import com.example.courtierprobackend.transactions.datalayer.Transaction;
import com.example.courtierprobackend.transactions.datalayer.repositories.TransactionRepository;
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
    private DocumentRequestRepository documentRequestRepository;
    @Mock
    private UserAccountRepository userAccountRepository;
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
                request
        );
        userId = UUID.randomUUID();
        brokerId = UUID.randomUUID();
        transactionId = UUID.randomUUID();
        
        // Use lenient() to avoid UnnecessaryStubbingException in early-return tests
        lenient().when(request.getAttribute(UserContextFilter.INTERNAL_USER_ID_ATTR)).thenReturn(userId);
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
        when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(transaction));
        when(userAccountRepository.searchClientsOfBroker(userId, transactionId.toString())).thenReturn(List.of());
        when(transactionRepository.searchTransactions(userId, transactionId.toString())).thenReturn(List.of());
        when(documentRequestRepository.searchDocuments(userId, transactionId.toString())).thenReturn(List.of());

        List<SearchResultDTO> results = searchService.search(transactionId.toString());

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getType()).isEqualTo(SearchResultDTO.SearchResultType.TRANSACTION);
        assertThat(results.get(0).getId()).isEqualTo(transactionId.toString());
    }

    @Test
    void search_ByTransactionUUID_FiltersUnauthorized() {
        UUID otherUser = UUID.randomUUID();
        Transaction transaction = createTestTransaction(transactionId, otherUser, UUID.randomUUID());
        when(transactionRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(transaction));
        when(userAccountRepository.searchClientsOfBroker(userId, transactionId.toString())).thenReturn(List.of());
        when(transactionRepository.searchTransactions(userId, transactionId.toString())).thenReturn(List.of());
        when(documentRequestRepository.searchDocuments(userId, transactionId.toString())).thenReturn(List.of());

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
        
        when(userAccountRepository.searchClientsOfBroker(userId, query)).thenReturn(List.of());
        when(transactionRepository.searchTransactions(userId, query)).thenReturn(List.of(transaction));
        when(documentRequestRepository.searchDocuments(userId, query)).thenReturn(List.of());

        List<SearchResultDTO> results = searchService.search(query);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getType()).isEqualTo(SearchResultDTO.SearchResultType.TRANSACTION);
        assertThat(results.get(0).getTitle()).isEqualTo("123 Main St");
    }

    @Test
    void search_TextQuery_FindsUsersByName() {
        String query = "John";
        UserAccount user = new UserAccount("auth0|123", "john@example.com", "John", "Doe", UserRole.CLIENT, "en");
        
        when(userAccountRepository.searchClientsOfBroker(userId, query)).thenReturn(List.of(user));
        when(transactionRepository.searchTransactions(userId, query)).thenReturn(List.of());
        when(documentRequestRepository.searchDocuments(userId, query)).thenReturn(List.of());

        List<SearchResultDTO> results = searchService.search(query);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getType()).isEqualTo(SearchResultDTO.SearchResultType.USER);
        assertThat(results.get(0).getTitle()).isEqualTo("John Doe");
    }

    @Test
    void search_TextQuery_FindsDocumentsByTitle() {
        String query = "Promise";
        Transaction transaction = createTestTransaction(transactionId, brokerId, userId);
        DocumentRequest document = createTestDocument(transactionId, "Promise to Purchase");
        
        when(userAccountRepository.searchClientsOfBroker(userId, query)).thenReturn(List.of());
        when(transactionRepository.searchTransactions(userId, query)).thenReturn(List.of());
        when(documentRequestRepository.searchDocuments(userId, query)).thenReturn(List.of(document));
        when(transactionRepository.findByTransactionIdIn(List.of(transactionId))).thenReturn(List.of(transaction));

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
        
        when(userAccountRepository.searchClientsOfBroker(userId, query)).thenReturn(List.of(client));
        when(transactionRepository.searchTransactions(userId, query)).thenReturn(List.of());
        when(transactionRepository.findLinkedToUsers(List.of(client.getId()), userId)).thenReturn(List.of(linkedTransaction));
        when(documentRequestRepository.searchDocuments(userId, query)).thenReturn(List.of());
        when(documentRequestRepository.findLinkedToUsers(List.of(client.getId()), userId)).thenReturn(List.of());

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

    private DocumentRequest createTestDocument(UUID txId, String customTitle) {
        TransactionRef txRef = TransactionRef.builder()
                .transactionId(txId)
                .clientId(userId)
                .build();
        
        return DocumentRequest.builder()
                .requestId(UUID.randomUUID())
                .transactionRef(txRef)
                .docType(DocumentTypeEnum.PROMISE_TO_PURCHASE)
                .customTitle(customTitle)
                .build();
    }
}
