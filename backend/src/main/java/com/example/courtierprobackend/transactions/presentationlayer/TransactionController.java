package com.example.courtierprobackend.transactions.presentationlayer;

import com.example.courtierprobackend.transactions.businesslayer.TransactionService;
import com.example.courtierprobackend.transactions.datalayer.dto.TransactionRequestDTO;
import com.example.courtierprobackend.transactions.datalayer.dto.TransactionResponseDTO;

import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService service;

    /**
     * Placeholder until Auth0 is integrated.
     * Later this brokerId will come from JWT claims.
     */
    private String extractBrokerId(String headerBrokerId) {
        return headerBrokerId; // future: SecurityContext → Auth0 JWT
    }

    @PostMapping
    public ResponseEntity<TransactionResponseDTO> createTransaction(
            @RequestBody TransactionRequestDTO dto,
            @RequestHeader("x-broker-id") String brokerIdHeader
    ) {
        dto.setBrokerId(extractBrokerId(brokerIdHeader));

        TransactionResponseDTO response = service.createTransaction(dto);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<TransactionResponseDTO>> getBrokerTransactions(
            @RequestHeader("x-broker-id") String brokerIdHeader
    ) {
        String brokerId = extractBrokerId(brokerIdHeader);

        return ResponseEntity.ok(
                service.getBrokerTransactions(brokerId)
        );
    }

    @GetMapping("/{transactionId}")
    public ResponseEntity<TransactionResponseDTO> getTransactionById(
            @PathVariable String transactionId,
            @RequestHeader("x-broker-id") String brokerIdHeader
    ) {
        String brokerId = extractBrokerId(brokerIdHeader);

        return ResponseEntity.ok(
                service.getByTransactionId(transactionId, brokerId)
        );
    }
}
