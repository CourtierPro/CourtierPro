package com.example.courtierprobackend.transactions.presentationlayer;

import com.example.courtierprobackend.transactions.businesslayer.TransactionService;
import com.example.courtierprobackend.transactions.datalayer.dto.TransactionRequestDTO;
import com.example.courtierprobackend.transactions.datalayer.dto.TransactionResponseDTO;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService service;

    // BrokerId extraction: supports DEV + PROD
    private String resolveBrokerId(Jwt jwt, String headerBrokerId) {

        // PROD: Auth0 token provided
        if (jwt != null) {
            return jwt.getSubject(); // Auth0 user ID
        }

        // DEV: using x-broker-id header
        return headerBrokerId;
    }


    @PostMapping
    public ResponseEntity<TransactionResponseDTO> createTransaction(
            @Valid @RequestBody TransactionRequestDTO dto,
            @RequestHeader(value = "x-broker-id", required = false) String brokerHeader,
            @AuthenticationPrincipal Jwt jwt
    ) {
        String brokerId = resolveBrokerId(jwt, brokerHeader);
        dto.setBrokerId(brokerId);

        TransactionResponseDTO response = service.createTransaction(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<TransactionResponseDTO>> getBrokerTransactions(
            @RequestHeader(value = "x-broker-id", required = false) String brokerHeader,
            @AuthenticationPrincipal Jwt jwt
    ) {
        String brokerId = resolveBrokerId(jwt, brokerHeader);
        return ResponseEntity.ok(service.getBrokerTransactions(brokerId));
    }

    @GetMapping("/{transactionId}")
    public ResponseEntity<TransactionResponseDTO> getTransactionById(
            @PathVariable String transactionId,
            @RequestHeader(value = "x-broker-id", required = false) String brokerHeader,
            @AuthenticationPrincipal Jwt jwt
    ) {
        String brokerId = resolveBrokerId(jwt, brokerHeader);
        return ResponseEntity.ok(service.getByTransactionId(transactionId, brokerId));
    }
}
