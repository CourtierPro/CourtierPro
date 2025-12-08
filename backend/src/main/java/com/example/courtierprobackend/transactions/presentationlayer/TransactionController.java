package com.example.courtierprobackend.transactions.presentationlayer;

import com.example.courtierprobackend.transactions.businesslayer.TransactionService;
import com.example.courtierprobackend.transactions.datalayer.dto.TransactionRequestDTO;
import com.example.courtierprobackend.transactions.datalayer.dto.TransactionResponseDTO;
import com.example.courtierprobackend.transactions.datalayer.dto.NoteRequestDTO;
import com.example.courtierprobackend.transactions.datalayer.dto.TimelineEntryDTO;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService service;

    // -------- UserId extraction (PROD = Auth0, DEV = x-broker-id / x-user-id) --------
    private String resolveUserId(Jwt jwt, String headerId) {

        // DEV mode: header
        if (StringUtils.hasText(headerId)) {
            return headerId;
        }

        // PROD mode: Auth0 token
        if (jwt != null) {
            String fromToken = jwt.getClaimAsString("sub");
            if (StringUtils.hasText(fromToken)) {
                return fromToken;
            }
        }

        throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Unable to resolve broker id from token or header"
        );
    }

    @PostMapping
    @PreAuthorize("hasRole('BROKER')")
    public ResponseEntity<TransactionResponseDTO> createTransaction(
            @Valid @RequestBody TransactionRequestDTO dto,
            @RequestHeader(value = "x-broker-id", required = false) String brokerHeader,
            @AuthenticationPrincipal Jwt jwt
    ) {
        String brokerId = resolveUserId(jwt, brokerHeader);
        dto.setBrokerId(brokerId);

        TransactionResponseDTO response = service.createTransaction(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{transactionId}/notes")
    @PreAuthorize("hasRole('BROKER')")
    public ResponseEntity<TimelineEntryDTO> createNote(
            @PathVariable String transactionId,
            @Valid @RequestBody NoteRequestDTO note,
            @RequestHeader(value = "x-broker-id", required = false) String brokerHeader,
            @AuthenticationPrincipal Jwt jwt
    ) {
        String brokerId = resolveUserId(jwt, brokerHeader);
        note.setTransactionId(transactionId);

        TimelineEntryDTO created = service.createNote(transactionId, note, brokerId);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/{transactionId}/notes")
    @PreAuthorize("hasRole('BROKER')")
    public ResponseEntity<List<TimelineEntryDTO>> getNotes(
            @PathVariable String transactionId,
            @RequestHeader(value = "x-broker-id", required = false) String brokerHeader,
            @AuthenticationPrincipal Jwt jwt
    ) {
        String brokerId = resolveUserId(jwt, brokerHeader);
        return ResponseEntity.ok(service.getNotes(transactionId, brokerId));
    }

    @GetMapping
    @PreAuthorize("hasRole('BROKER')")
    public ResponseEntity<List<TransactionResponseDTO>> getBrokerTransactions(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String stage,
            @RequestParam(required = false) String side,
            @RequestHeader(value = "x-broker-id", required = false) String brokerHeader,
            @AuthenticationPrincipal Jwt jwt
    ) {
        String brokerId = resolveUserId(jwt, brokerHeader);
        return ResponseEntity.ok(service.getBrokerTransactions(brokerId, status, stage, side));
    }

    @GetMapping("/{transactionId}")
    @PreAuthorize("hasAnyRole('BROKER', 'CLIENT')")
    public ResponseEntity<TransactionResponseDTO> getTransactionById(
            @PathVariable String transactionId,
            @RequestHeader(value = "x-broker-id", required = false) String brokerHeader,
            @AuthenticationPrincipal Jwt jwt
    ) {
        String userId = resolveUserId(jwt, brokerHeader);
        return ResponseEntity.ok(service.getByTransactionId(transactionId, userId));
    }
}
