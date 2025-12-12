package com.example.courtierprobackend.search;

import com.example.courtierprobackend.documents.datalayer.DocumentRequest;
import com.example.courtierprobackend.documents.datalayer.DocumentRequestRepository;
import com.example.courtierprobackend.search.dto.SearchResultDTO;
import com.example.courtierprobackend.security.UserContextUtils;
import com.example.courtierprobackend.transactions.datalayer.Transaction;
import com.example.courtierprobackend.transactions.datalayer.repositories.TransactionRepository;
import com.example.courtierprobackend.user.dataaccesslayer.UserAccount;
import com.example.courtierprobackend.user.dataaccesslayer.UserAccountRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SearchService {

    private final TransactionRepository transactionRepository;
    private final DocumentRequestRepository documentRequestRepository;
    private final UserAccountRepository userAccountRepository;
    private final HttpServletRequest request;

    /**
     * Main search entry point. Orchestrates searches across transactions, documents, and users.
     */
    public List<SearchResultDTO> search(String query) {
        if (query == null || query.trim().length() < 2) {
            return List.of();
        }

        UUID userId = UserContextUtils.resolveUserId(request);
        Set<SearchResultDTO> results = new LinkedHashSet<>();

        // 1. Direct ID search (if query is UUID)
        searchById(query, userId, results);

        // 2. Search users and get matched IDs for related searches
        List<UUID> matchedUserIds = searchUsers(userId, query, results);

        // 3. Search transactions (text + linked users)
        searchTransactions(userId, query, matchedUserIds, results);

        // 4. Search documents (text + linked users)
        searchDocuments(userId, query, matchedUserIds, results);

        return new ArrayList<>(results);
    }

    /**
     * Searches by UUID if query is a valid UUID. Adds matching transaction/document to results.
     */
    private void searchById(String query, UUID userId, Set<SearchResultDTO> results) {
        try {
            UUID potentialId = UUID.fromString(query);
            searchTransactionById(potentialId, userId, results);
            searchDocumentById(potentialId, userId, results);
        } catch (IllegalArgumentException e) {
            // Not a UUID, skip
        }
    }

    private void searchTransactionById(UUID transactionId, UUID userId, Set<SearchResultDTO> results) {
        transactionRepository.findByTransactionId(transactionId).ifPresent(t -> {
            if (t.getBrokerId().equals(userId) || t.getClientId().equals(userId)) {
                results.add(mapTransaction(t));
            }
        });
    }

    private void searchDocumentById(UUID documentId, UUID userId, Set<SearchResultDTO> results) {
        documentRequestRepository.findByRequestId(documentId).ifPresent(d -> {
            boolean isClient = d.getTransactionRef().getClientId().equals(userId);
            boolean isBroker = transactionRepository
                    .findByTransactionId(d.getTransactionRef().getTransactionId())
                    .map(t -> t.getBrokerId().equals(userId))
                    .orElse(false);

            if (isClient || isBroker) {
                String address = transactionRepository
                        .findByTransactionId(d.getTransactionRef().getTransactionId())
                        .map(t -> t.getPropertyAddress() != null ? t.getPropertyAddress().getStreet() : "Unknown Address")
                        .orElse("Unknown Address");
                results.add(mapDocument(d, address));
            }
        });
    }

    /**
     * Searches users (clients of broker + self) and adds to results.
     * Returns list of matched user IDs for related searches.
     */
    private List<UUID> searchUsers(UUID userId, String query, Set<SearchResultDTO> results) {
        List<UserAccount> matchedUsers = new ArrayList<>(userAccountRepository.searchClientsOfBroker(userId, query));

        // Check if current user matches
        userAccountRepository.findById(userId).ifPresent(currentUser -> {
            String fullName = (currentUser.getFirstName() + " " + currentUser.getLastName()).toLowerCase();
            String email = currentUser.getEmail().toLowerCase();
            String lowerQuery = query.toLowerCase();

            if (fullName.contains(lowerQuery) || email.contains(lowerQuery)) {
                if (matchedUsers.stream().noneMatch(u -> u.getId().equals(userId))) {
                    matchedUsers.add(currentUser);
                }
            }
        });

        // Add users to results
        results.addAll(matchedUsers.stream()
                .map(this::mapUser)
                .collect(Collectors.toList()));

        return matchedUsers.stream()
                .map(UserAccount::getId)
                .collect(Collectors.toList());
    }

    /**
     * Searches transactions by text and linked users.
     */
    private void searchTransactions(UUID userId, String query, List<UUID> matchedUserIds, Set<SearchResultDTO> results) {
        List<Transaction> transactions = new ArrayList<>(transactionRepository.searchTransactions(userId, query));

        if (!matchedUserIds.isEmpty()) {
            transactions.addAll(transactionRepository.findLinkedToUsers(matchedUserIds, userId));
        }

        results.addAll(transactions.stream()
                .map(this::mapTransaction)
                .collect(Collectors.toList()));
    }

    /**
     * Searches documents by text and linked users.
     */
    private void searchDocuments(UUID userId, String query, List<UUID> matchedUserIds, Set<SearchResultDTO> results) {
        List<DocumentRequest> documents = new ArrayList<>(documentRequestRepository.searchDocuments(userId, query));

        if (!matchedUserIds.isEmpty()) {
            documents.addAll(documentRequestRepository.findLinkedToUsers(matchedUserIds, userId));
        }

        // Batch-fetch transactions for subtitles
        Map<UUID, Transaction> transactionMap = fetchTransactionMap(documents);

        results.addAll(documents.stream()
                .map(d -> {
                    Transaction t = transactionMap.get(d.getTransactionRef().getTransactionId());
                    String address = (t != null && t.getPropertyAddress() != null)
                            ? t.getPropertyAddress().getStreet()
                            : "Unknown Address";
                    return mapDocument(d, address);
                })
                .collect(Collectors.toList()));
    }

    private Map<UUID, Transaction> fetchTransactionMap(List<DocumentRequest> documents) {
        List<UUID> transactionIds = documents.stream()
                .map(d -> d.getTransactionRef().getTransactionId())
                .distinct()
                .collect(Collectors.toList());

        if (transactionIds.isEmpty()) {
            return Map.of();
        }

        return transactionRepository.findByTransactionIdIn(transactionIds).stream()
                .collect(Collectors.toMap(Transaction::getTransactionId, t -> t));
    }

    // ========== Mappers ==========

    private SearchResultDTO mapTransaction(Transaction t) {
        return SearchResultDTO.builder()
                .id(t.getTransactionId().toString())
                .type(SearchResultDTO.SearchResultType.TRANSACTION)
                .title(t.getPropertyAddress().getStreet())
                .subtitle(t.getPropertyAddress().getCity() + ", " + t.getPropertyAddress().getProvince())
                .url("/transactions/" + t.getTransactionId())
                .build();
    }

    private SearchResultDTO mapDocument(DocumentRequest d, String address) {
        return SearchResultDTO.builder()
                .id(d.getRequestId().toString())
                .type(SearchResultDTO.SearchResultType.DOCUMENT)
                .title(d.getCustomTitle() != null ? d.getCustomTitle() : d.getDocType().name())
                .subtitle("Transaction: " + address)
                .url("/transactions/" + d.getTransactionRef().getTransactionId() + "?tab=documents&focus=" + d.getRequestId())
                .build();
    }

    private SearchResultDTO mapUser(UserAccount u) {
        return SearchResultDTO.builder()
                .id(u.getId().toString())
                .type(SearchResultDTO.SearchResultType.USER)
                .title(u.getFirstName() + " " + u.getLastName())
                .subtitle(u.getEmail())
                .url("/contacts/" + u.getId())
                .build();
    }
}
