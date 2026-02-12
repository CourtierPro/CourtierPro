package com.example.courtierprobackend.search;

import com.example.courtierprobackend.appointments.datalayer.Appointment;
import com.example.courtierprobackend.appointments.datalayer.AppointmentRepository;
import com.example.courtierprobackend.documents.datalayer.Document;
import com.example.courtierprobackend.documents.datalayer.DocumentRepository;
import com.example.courtierprobackend.search.dto.SearchResultDTO;
import com.example.courtierprobackend.security.UserContextUtils;
import com.example.courtierprobackend.transactions.datalayer.Transaction;
import com.example.courtierprobackend.transactions.datalayer.repositories.TransactionRepository;
import com.example.courtierprobackend.user.dataaccesslayer.UserAccount;
import com.example.courtierprobackend.user.dataaccesslayer.UserAccountRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SearchService {

    private final TransactionRepository transactionRepository;
    private final DocumentRepository documentRepository;
    private final UserAccountRepository userAccountRepository;
    private final AppointmentRepository appointmentRepository;
    private final HttpServletRequest request;

    /**
     * Main search entry point. Orchestrates searches across transactions, documents, and users.
     */
    public List<SearchResultDTO> search(String query) {
        if (query == null || query.trim().length() < 2) {
            return List.of();
        }

        // Expand query for potential bilingual synonyms (e.g. "notaire" <-> "notary")
        Set<String> searchTerms = resolveSynonyms(query);

        UUID userId = UserContextUtils.resolveUserId(request);
        Set<SearchResultDTO> results = new LinkedHashSet<>();

        // 1. Direct ID search (if query is UUID)
        searchById(query, userId, results);

        for (String term : searchTerms) {
            // 2. Search users
            List<UUID> matchedUserIds = searchUsers(userId, term, results);

            // 3. Search transactions
            searchTransactions(userId, term, matchedUserIds, results);

            // 4. Search documents
            searchDocuments(userId, term, matchedUserIds, results);

            // 5. Search appointments
            searchAppointments(userId, term, results);
        }

        return new ArrayList<>(results);
    }
    
    private Set<String> resolveSynonyms(String query) {
        Set<String> terms = new HashSet<>();
        terms.add(query);
        
        String lowerQuery = query.toLowerCase().trim();
        
        // Manual mapping of common bilingual search terms
        if (lowerQuery.contains("notaire") || lowerQuery.contains("notariale")) {
            terms.add("notary");
        }
        if (lowerQuery.contains("assurance")) {
            terms.add("insurance");
        }
        if (lowerQuery.contains("hypothèque") || lowerQuery.contains("hypotheque")) {
            terms.add("mortgage");
        }
        if (lowerQuery.contains("inspection")) {
            terms.add("inspection"); // Same
        }
        if (lowerQuery.contains("signature")) {
            terms.add("signing");
        }
        if (lowerQuery.contains("banque") || lowerQuery.contains("relevé") || lowerQuery.contains("releve")) {
            terms.add("bank");
            terms.add("statement");
        }
        
        // Reverse mappings (English -> French equivalent/synonym context)
        if (lowerQuery.contains("notary")) {
            terms.add("notaire");
        }
        if (lowerQuery.contains("mortgage")) {
            terms.add("hypotheque");
        }
        if (lowerQuery.contains("signing")) {
            terms.add("signature");
        }
        
        return terms;
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
        documentRepository.findByDocumentId(documentId).ifPresent(d -> {
            boolean isClient = d.getTransactionRef().getClientId().equals(userId);
            
            Transaction t = transactionRepository
                    .findByTransactionId(d.getTransactionRef().getTransactionId())
                    .orElse(null);

            boolean isBroker = (t != null && t.getBrokerId().equals(userId));

            if (isClient || isBroker) {
                String address = (t != null && t.getPropertyAddress() != null) 
                        ? t.getPropertyAddress().getStreet() 
                        : "Unknown Address";
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
     * Searches transactions using efficient set-based deduplication before mapping.
     */
    private void searchTransactions(UUID userId, String query, List<UUID> matchedUserIds, Set<SearchResultDTO> results) {
        // Use a map to deduplicate by ID efficiently before mapping
        Map<UUID, Transaction> uniqueTransactions = new HashMap<>();

        // 1. Text search
        transactionRepository.searchTransactions(userId, query)
                .forEach(t -> uniqueTransactions.put(t.getTransactionId(), t));

        // 2. Linked user search
        if (!matchedUserIds.isEmpty()) {
            transactionRepository.findLinkedToUsers(matchedUserIds, userId)
                    .forEach(t -> uniqueTransactions.putIfAbsent(t.getTransactionId(), t));
        }

        results.addAll(uniqueTransactions.values().stream()
                .map(this::mapTransaction)
                .collect(Collectors.toList()));
    }

    /**
     * Searches documents using efficient set-based deduplication before mapping.
     */
    private void searchDocuments(UUID userId, String query, List<UUID> matchedUserIds, Set<SearchResultDTO> results) {
        Map<UUID, Document> uniqueDocuments = new HashMap<>();

        // 1. Text search
        documentRepository.searchDocuments(userId, query)
                .forEach(d -> uniqueDocuments.put(d.getDocumentId(), d));

        // 2. Linked user search
        if (!matchedUserIds.isEmpty()) {
            documentRepository.findLinkedToUsers(matchedUserIds, userId)
                    .forEach(d -> uniqueDocuments.putIfAbsent(d.getDocumentId(), d));
        }
        
        List<Document> documents = new ArrayList<>(uniqueDocuments.values());

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

    private void searchAppointments(UUID userId, String query, Set<SearchResultDTO> results) {
        appointmentRepository.searchAppointments(userId, query)
                .stream()
                .map(this::mapAppointment)
                .forEach(results::add);
    }

    private Map<UUID, Transaction> fetchTransactionMap(List<Document> documents) {
        if (documents.isEmpty()) {
            return Map.of();
        }

        List<UUID> transactionIds = documents.stream()
                .map(d -> d.getTransactionRef().getTransactionId())
                .distinct()
                .collect(Collectors.toList());

        return transactionRepository.findByTransactionIdIn(transactionIds).stream()
                .collect(Collectors.toMap(Transaction::getTransactionId, t -> t));
    }

    // ========== Mappers ==========

    private SearchResultDTO mapTransaction(Transaction t) {
        String street = "Unknown Address";
        String city = "";
        String province = "";
        
        if (t.getPropertyAddress() != null) {
            street = t.getPropertyAddress().getStreet() != null ? t.getPropertyAddress().getStreet() : "Unknown Address";
            city = t.getPropertyAddress().getCity() != null ? t.getPropertyAddress().getCity() : "";
            province = t.getPropertyAddress().getProvince() != null ? t.getPropertyAddress().getProvince() : "";
        }

        String subtitle = city;
        if (!city.isEmpty() && !province.isEmpty()) {
            subtitle += ", " + province;
        } else if (city.isEmpty()) {
            subtitle = province;
        }

        return SearchResultDTO.builder()
                .id(t.getTransactionId().toString())
                .type(SearchResultDTO.SearchResultType.TRANSACTION)
                .title(street)
                .subtitle(subtitle)
                .url("/transactions/" + t.getTransactionId())
                .build();
    }

    private SearchResultDTO mapDocument(Document d, String address) {
        return SearchResultDTO.builder()
                .id(d.getDocumentId().toString())
                .type(SearchResultDTO.SearchResultType.DOCUMENT)
                .title(d.getCustomTitle() != null ? d.getCustomTitle() : d.getDocType().name())
                .subtitle(address)
                .url("/transactions/" + d.getTransactionRef().getTransactionId() + "?tab=documents&focus=" + d.getDocumentId())
                .build();
    }

    private SearchResultDTO mapUser(UserAccount u) {
        String firstName = u.getFirstName() != null ? u.getFirstName() : "";
        String lastName = u.getLastName() != null ? u.getLastName() : "";
        String title = (firstName + " " + lastName).trim();
        
        return SearchResultDTO.builder()
                .id(u.getId().toString())
                .type(SearchResultDTO.SearchResultType.USER)
                .title(title.isEmpty() ? "Unknown User" : title)
                .subtitle(u.getEmail())
                .url("/contacts/" + u.getId())
                .build();
    }

    private SearchResultDTO mapAppointment(Appointment a) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM d, h:mm a");
        String subtitle = a.getFromDateTime().format(formatter);
        if (a.getLocation() != null && !a.getLocation().isEmpty()) {
            subtitle += " • " + a.getLocation();
        }

        return SearchResultDTO.builder()
                .id(a.getAppointmentId().toString())
                .type(SearchResultDTO.SearchResultType.APPOINTMENT)
                .title(a.getTitle())
                .subtitle(subtitle)
                .url("/appointments?focus=" + a.getAppointmentId())
                .build();
    }
}
