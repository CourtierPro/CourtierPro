package com.example.courtierprobackend.transactions.presentationlayer;

import com.example.courtierprobackend.common.exceptions.ForbiddenException;
import com.example.courtierprobackend.security.UserContextUtils;
import com.example.courtierprobackend.security.UserContextFilter;
import com.example.courtierprobackend.transactions.businesslayer.TransactionService;
import com.example.courtierprobackend.transactions.datalayer.dto.TransactionResponseDTO;
import com.example.courtierprobackend.transactions.datalayer.dto.PropertyResponseDTO;

import jakarta.servlet.http.HttpServletRequest;
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

    /**
     * Resolves the internal client UUID from request attributes (set by UserContextFilter).
     * Validates that the path clientId matches the authenticated user's internal ID.
     */
    private UUID resolveAndValidateClientId(HttpServletRequest request, UUID pathClientId) {
        UUID internalId = UserContextUtils.resolveUserId(request);

        // Security check: ensure the client can only access their own transactions
        // The pathClientId from frontend should now match the internal UUID
        if (!internalId.equals(pathClientId)) {
            throw new ForbiddenException("You can only access your own transactions");
        }

        return internalId;
    }

    /**
     * Get all transactions for the authenticated client.
     * The clientId in the path must match the authenticated user's internal UUID.
     */
    @GetMapping("/{clientId}/transactions")
    public ResponseEntity<List<TransactionResponseDTO>> getClientTransactions(
            @PathVariable UUID clientId,
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
            @PathVariable UUID clientId,
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
            @PathVariable UUID clientId,
            @PathVariable UUID transactionId,
            @PathVariable UUID propertyId,
            HttpServletRequest request
    ) {
        UUID validatedClientId = resolveAndValidateClientId(request, clientId);
        // isBroker = false means broker notes will be stripped
        return ResponseEntity.ok(service.getPropertyById(propertyId, validatedClientId, false));
    }
}

