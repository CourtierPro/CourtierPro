package com.example.courtierprobackend.transactions.presentationlayer;

import com.example.courtierprobackend.common.exceptions.ForbiddenException;
import com.example.courtierprobackend.security.UserContextUtils;
import com.example.courtierprobackend.transactions.businesslayer.TransactionService;
import com.example.courtierprobackend.user.dataaccesslayer.UserAccountRepository;
import com.example.courtierprobackend.user.dataaccesslayer.UserAccount;
import com.example.courtierprobackend.transactions.datalayer.dto.TransactionResponseDTO;
import com.example.courtierprobackend.transactions.datalayer.dto.PropertyResponseDTO;
import com.example.courtierprobackend.transactions.datalayer.dto.OfferResponseDTO;
import com.example.courtierprobackend.transactions.datalayer.dto.PropertyOfferResponseDTO;
import com.example.courtierprobackend.transactions.datalayer.dto.OfferDocumentResponseDTO;
import com.example.courtierprobackend.transactions.datalayer.dto.ClientOfferDecisionDTO;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controller for client-facing transaction endpoints.
 * Allows authenticated clients to view their own transactions.
 */
@RestController
@RequestMapping("/clients")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CLIENT')")
public class ClientTransactionController {

    private final TransactionService service;
    private final UserAccountRepository userAccountRepository;

    /**
     * Resolves the internal client UUID from request attributes (set by UserContextFilter).
     * Validates that the path clientId matches the authenticated user's internal ID.
     */
    private UUID resolveAndValidateClientId(HttpServletRequest request, String pathClientId) {
        UUID internalId = UserContextUtils.resolveUserId(request);
        // Try to parse as UUID first
        try {
            UUID pathUuid = UUID.fromString(pathClientId);
            if (!internalId.equals(pathUuid)) {
                throw new ForbiddenException("You can only access your own transactions");
            }
            return internalId;
        } catch (IllegalArgumentException e) {
            // Not a UUID, try to resolve Auth0 ID to internal UUID
            UserAccount account = userAccountRepository.findByAuth0UserId(pathClientId)
                .orElseThrow(() -> new ForbiddenException("Invalid client ID: not found"));
            if (!internalId.equals(account.getId())) {
                throw new ForbiddenException("You can only access your own transactions");
            }
            return internalId;
        }
    }

    /**
     * Get all transactions for the authenticated client.
     * The clientId in the path must match the authenticated user's internal UUID.
     */
    @GetMapping("/{clientId}/transactions")
    public ResponseEntity<List<TransactionResponseDTO>> getClientTransactions(
            @PathVariable String clientId,
            HttpServletRequest request
    ) {
        UUID validatedClientId = resolveAndValidateClientId(request, clientId);
        return ResponseEntity.ok(service.getClientTransactions(validatedClientId));
    }

    // ==================== PROPERTY ENDPOINTS (CLIENT - READ ONLY) ====================

    /**
     * Get all properties for a transaction (read-only, no broker notes).
     */
    @GetMapping("/{clientId}/transactions/{transactionId}/properties")
    public ResponseEntity<List<PropertyResponseDTO>> getTransactionProperties(
            @PathVariable String clientId,
            @PathVariable UUID transactionId,
            HttpServletRequest request
    ) {
        UUID validatedClientId = resolveAndValidateClientId(request, clientId);
        // isBroker = false means broker notes will be stripped
        return ResponseEntity.ok(service.getProperties(transactionId, validatedClientId, false));
    }

    /**
     * Get a single property by ID (read-only, no broker notes).
     */
    @GetMapping("/{clientId}/transactions/{transactionId}/properties/{propertyId}")
    public ResponseEntity<PropertyResponseDTO> getPropertyById(
            @PathVariable String clientId,
            @PathVariable UUID transactionId,
            @PathVariable UUID propertyId,
            HttpServletRequest request
    ) {
        UUID validatedClientId = resolveAndValidateClientId(request, clientId);
        // isBroker = false means broker notes will be stripped
        return ResponseEntity.ok(service.getPropertyById(propertyId, validatedClientId, false));
    }

    /**
     * Get all offers for a property (buy-side).
     */
    @GetMapping("/{clientId}/transactions/{transactionId}/properties/{propertyId}/offers")
    public ResponseEntity<List<PropertyOfferResponseDTO>> getPropertyOffers(
            @PathVariable UUID clientId,
            @PathVariable UUID transactionId,
            @PathVariable UUID propertyId,
            HttpServletRequest request
    ) {
        UUID validatedClientId = resolveAndValidateClientId(request, clientId);
        // isBroker = false
        return ResponseEntity.ok(service.getPropertyOffers(propertyId, validatedClientId, false));
    }

    /**
     * Get documents for a buy-side property offer.
     */
    @GetMapping("/{clientId}/transactions/{transactionId}/properties/{propertyId}/offers/{propertyOfferId}/documents")
    public ResponseEntity<List<OfferDocumentResponseDTO>> getPropertyOfferDocuments(
            @PathVariable UUID clientId,
            @PathVariable UUID transactionId,
            @PathVariable UUID propertyId,
            @PathVariable UUID propertyOfferId,
            HttpServletRequest request
    ) {
        UUID validatedClientId = resolveAndValidateClientId(request, clientId);
        return ResponseEntity.ok(service.getPropertyOfferDocuments(propertyOfferId, validatedClientId, false));
    }

    // ==================== OFFER ENDPOINTS (CLIENT - READ ONLY) ====================

    /**
     * Get all offers for a transaction (read-only, no broker notes).
     * Only applicable for sell-side transactions.
     */
    @GetMapping("/{clientId}/transactions/{transactionId}/offers")
    public ResponseEntity<List<OfferResponseDTO>> getTransactionOffers(
            @PathVariable String clientId,
            @PathVariable UUID transactionId,
            HttpServletRequest request
    ) {
        UUID validatedClientId = resolveAndValidateClientId(request, clientId);
        // isBroker = false means broker notes will be stripped
        return ResponseEntity.ok(service.getOffers(transactionId, validatedClientId, false));
    }

    /**
     * Get a single offer by ID (read-only, no broker notes).
     */
    @GetMapping("/{clientId}/transactions/{transactionId}/offers/{offerId}")
    public ResponseEntity<OfferResponseDTO> getOfferById(
            @PathVariable String clientId,
            @PathVariable UUID transactionId,
            @PathVariable UUID offerId,
            HttpServletRequest request
    ) {
        UUID validatedClientId = resolveAndValidateClientId(request, clientId);
        // isBroker = false means broker notes will be stripped
        return ResponseEntity.ok(service.getOfferById(offerId, validatedClientId, false));
    }

    /**
     * Get documents for a sell-side offer.
     */
    @GetMapping("/{clientId}/transactions/{transactionId}/offers/{offerId}/documents")
    public ResponseEntity<List<OfferDocumentResponseDTO>> getOfferDocuments(
            @PathVariable UUID clientId,
            @PathVariable UUID transactionId,
            @PathVariable UUID offerId,
            HttpServletRequest request
    ) {
        UUID validatedClientId = resolveAndValidateClientId(request, clientId);
        return ResponseEntity.ok(service.getOfferDocuments(offerId, validatedClientId, false));
    }

    /**
     * Submit client's decision on a received offer (sell-side).
     * The client indicates whether they want to accept, decline, or counter the offer.
     * The broker will then finalize the decision.
     */
    @PutMapping("/{clientId}/transactions/{transactionId}/offers/{offerId}/decision")
    public ResponseEntity<OfferResponseDTO> submitOfferDecision(
            @PathVariable UUID clientId,
            @PathVariable UUID transactionId,
            @PathVariable UUID offerId,
            @Valid @RequestBody ClientOfferDecisionDTO dto,
            HttpServletRequest request
    ) {
        UUID validatedClientId = resolveAndValidateClientId(request, clientId);
        return ResponseEntity.ok(service.submitClientOfferDecision(offerId, dto, validatedClientId));
    }
}

