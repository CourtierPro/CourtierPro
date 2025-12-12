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

        // 1. Search Users (Clients) first to get IDs for related search
        List<UserAccount> matchedUsers = userAccountRepository.searchClientsOfBroker(userId, query);
        List<UUID> matchedClientIds = matchedUsers.stream().map(UserAccount::getId).collect(Collectors.toList());

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

        // 2. Search Transactions (Text match OR Client match)
        List<Transaction> transactions = new ArrayList<>(transactionRepository.searchTransactions(userId, query));
        if (!matchedClientIds.isEmpty()) {
            transactions.addAll(transactionRepository.findByClientIdIn(matchedClientIds));
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

        // 3. Search Documents (Text match OR Client match)
        List<DocumentRequest> documents = new ArrayList<>(documentRequestRepository.searchDocuments(userId, query));
        if (!matchedClientIds.isEmpty()) {
            documents.addAll(documentRequestRepository.findByTransactionRef_ClientIdIn(matchedClientIds));
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
                        .url("/transactions/" + d.getTransactionRef().getTransactionId()) 
                        .build();
                })
                .collect(Collectors.toList());
        results.addAll(documentResults);

        return new ArrayList<>(results);
    }
}
