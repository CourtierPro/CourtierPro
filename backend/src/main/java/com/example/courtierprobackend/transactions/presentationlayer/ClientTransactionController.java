package com.example.courtierprobackend.transactions.presentationlayer;

import com.example.courtierprobackend.transactions.businesslayer.TransactionService;
import com.example.courtierprobackend.transactions.datalayer.dto.TransactionResponseDTO;
import com.example.courtierprobackend.transactions.datalayer.dto.TimelineEntryDTO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.LocalDateTime;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.List;

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
    private static final Logger log = LoggerFactory.getLogger(ClientTransactionController.class);

    /**
     * Resolves the client ID from the JWT token.
     * Ensures that the requested clientId matches the authenticated user's ID.
     */
    private String resolveAndValidateClientId(Jwt jwt, String pathClientId) {
        if (jwt == null) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Authentication required"
            );
        }

        String tokenClientId = jwt.getClaimAsString("sub");
        if (!StringUtils.hasText(tokenClientId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Unable to resolve client id from token"
            );
        }

        // Security check: ensure the client can only access their own transactions
        if (!tokenClientId.equals(pathClientId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You can only access your own transactions"
            );
        }

        return tokenClientId;
    }

    /**
     * Get all transactions for the authenticated client.
     * The clientId in the path must match the authenticated user's ID.
     */
    @GetMapping("/{clientId}/transactions")
    public ResponseEntity<List<TransactionResponseDTO>> getClientTransactions(
            @PathVariable String clientId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        String validatedClientId = resolveAndValidateClientId(jwt, clientId);
        return ResponseEntity.ok(service.getClientTransactions(validatedClientId));
    }

    @GetMapping("/{clientId}/transactions/{transactionId}/timeline")
    public ResponseEntity<java.util.List<TimelineEntryDTO>> getClientTransactionTimeline(
            @PathVariable String clientId,
            @PathVariable String transactionId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        String validatedClientId = resolveAndValidateClientId(jwt, clientId);

        java.util.List<TimelineEntryDTO> timeline = service.getClientTransactionTimeline(transactionId, validatedClientId);

        log.info("Transaction Status Viewed | User: {} | Transaction: {} | Timestamp: {}",
                validatedClientId, transactionId, LocalDateTime.now());

        return ResponseEntity.ok(timeline);
    }
}
