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

    public List<SearchResultDTO> search(String query) {
        if (query == null || query.trim().length() < 2) {
            return List.of();
        }

        UUID userId = UserContextUtils.resolveUserId(request);
        
        // Use Sets to avoid duplicates
        Set<SearchResultDTO> results = new LinkedHashSet<>();

        // 0. Direct ID Search (if query is UUID)
        try {
            UUID potentialId = UUID.fromString(query);
            
            // Search Transaction by ID (Strict Access Check)
            transactionRepository.findByTransactionId(potentialId).ifPresent(t -> {
                if (t.getBrokerId().equals(userId) || t.getClientId().equals(userId)) {
                    results.add(SearchResultDTO.builder()
                        .id(t.getTransactionId().toString())
                        .type(SearchResultDTO.SearchResultType.TRANSACTION)
                        .title(t.getPropertyAddress().getStreet())
                        .subtitle(t.getPropertyAddress().getCity() + ", " + t.getPropertyAddress().getProvince())
                        .url("/transactions/" + t.getTransactionId())
                        .build());
                }
            });

            // Search Document by ID (Strict Access Check via Repository or Manual)
            documentRequestRepository.findByRequestId(potentialId).ifPresent(d -> {
                // To check access, we need to know if current user is client or broker of transaction
                // Note: findByRequestId doesn't filter by user, so we must check manually
                boolean isClient = d.getTransactionRef().getClientId().equals(userId);
                boolean isBroker = false; 
                
                // We need to fetch transaction to check broker
                Optional<Transaction> tOpt = transactionRepository.findByTransactionId(d.getTransactionRef().getTransactionId());
                if (tOpt.isPresent() && tOpt.get().getBrokerId().equals(userId)) {
                    isBroker = true;
                }

                if (isClient || isBroker) {
                     String address = (tOpt.isPresent() && tOpt.get().getPropertyAddress() != null) ? tOpt.get().getPropertyAddress().getStreet() : "Unknown Address";
                     results.add(SearchResultDTO.builder()
                        .id(d.getRequestId().toString())
                        .type(SearchResultDTO.SearchResultType.DOCUMENT)
                        .title(d.getCustomTitle() != null ? d.getCustomTitle() : d.getDocType().name())
                        .subtitle("Transaction: " + address)
                        .url("/transactions/" + d.getTransactionRef().getTransactionId() + "?tab=documents&focus=" + d.getRequestId()) 
                        .build());
                }
            });

        } catch (IllegalArgumentException e) {
            // Not a UUID, continue to text search
        }

        // 1. Search Users (Clients) first to get IDs for related search
        // Also check if the current user matches the query (e.g. searching for themselves)
        List<UserAccount> matchedUsers = new ArrayList<>(userAccountRepository.searchClientsOfBroker(userId, query));
        
        userAccountRepository.findById(userId).ifPresent(currentUser -> {
             String fullName = (currentUser.getFirstName() + " " + currentUser.getLastName()).toLowerCase();
             String email = currentUser.getEmail().toLowerCase();
             String lowerQuery = query.toLowerCase();
             
             if (fullName.contains(lowerQuery) || email.contains(lowerQuery)) {
                 // Avoid duplicates if for some reason searchClientsOfBroker returned self (unlikely but possible)
                 if (matchedUsers.stream().noneMatch(u -> u.getId().equals(userId))) {
                     matchedUsers.add(currentUser);
                 }
             }
        });

        List<UUID> matchedUserIds = matchedUsers.stream().map(UserAccount::getId).collect(Collectors.toList());

        // Add matched users to results
        results.addAll(matchedUsers.stream()
                .map(u -> SearchResultDTO.builder()
                        .id(u.getId().toString())
                        .type(SearchResultDTO.SearchResultType.USER)
                        .title(u.getFirstName() + " " + u.getLastName())
                        .subtitle(u.getEmail())
                        .url("/contacts/" + u.getId())
                        .build())
                .collect(Collectors.toList()));

        // 2. Search Transactions (Text match OR Linked User match)
        // Text match
        List<Transaction> transactions = new ArrayList<>(transactionRepository.searchTransactions(userId, query));
        // Linked User match
        if (!matchedUserIds.isEmpty()) {
            transactions.addAll(transactionRepository.findLinkedToUsers(matchedUserIds, userId));
        }

        results.addAll(transactions.stream()
                .map(t -> SearchResultDTO.builder()
                        .id(t.getTransactionId().toString())
                        .type(SearchResultDTO.SearchResultType.TRANSACTION)
                        .title(t.getPropertyAddress().getStreet())
                        .subtitle(t.getPropertyAddress().getCity() + ", " + t.getPropertyAddress().getProvince())
                        .url("/transactions/" + t.getTransactionId())
                        .build())
                .collect(Collectors.toList()));

        // 3. Search Documents (Text match OR Linked User match)
        List<DocumentRequest> documents = new ArrayList<>(documentRequestRepository.searchDocuments(userId, query));
        if (!matchedUserIds.isEmpty()) {
            documents.addAll(documentRequestRepository.findLinkedToUsers(matchedUserIds, userId));
        }

        // Fetch related transactions to display address in subtitle
        List<UUID> docTransactionIds = documents.stream()
                .map(d -> d.getTransactionRef().getTransactionId())
                .distinct()
                .collect(Collectors.toList());
        
        Map<UUID, Transaction> transactionMap = new HashMap<>();
        if (!docTransactionIds.isEmpty()) {
            List<Transaction> docTransactions = transactionRepository.findByTransactionIdIn(docTransactionIds);
            for (Transaction t : docTransactions) {
                transactionMap.put(t.getTransactionId(), t);
            }
        }

        List<SearchResultDTO> documentResults = documents.stream()
                .map(d -> {
                    Transaction t = transactionMap.get(d.getTransactionRef().getTransactionId());
                    String address = (t != null && t.getPropertyAddress() != null) ? t.getPropertyAddress().getStreet() : "Unknown Address";
                    
                    return SearchResultDTO.builder()
                        .id(d.getRequestId().toString())
                        .type(SearchResultDTO.SearchResultType.DOCUMENT)
                        .title(d.getCustomTitle() != null ? d.getCustomTitle() : d.getDocType().name())
                        .subtitle("Transaction: " + address)
                        .url("/transactions/" + d.getTransactionRef().getTransactionId() + "?tab=documents&focus=" + d.getRequestId()) 
                        .build();
                })
                .collect(Collectors.toList());
        results.addAll(documentResults);

        return new ArrayList<>(results);
    }
}
