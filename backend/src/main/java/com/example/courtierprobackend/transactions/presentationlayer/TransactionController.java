package com.example.courtierprobackend.transactions.presentationlayer;

import com.example.courtierprobackend.transactions.businesslayer.TransactionService;
import com.example.courtierprobackend.transactions.datalayer.dto.TransactionRequestDTO;
import com.example.courtierprobackend.transactions.datalayer.dto.TransactionResponseDTO;
import com.example.courtierprobackend.transactions.datalayer.dto.NoteRequestDTO;
import com.example.courtierprobackend.transactions.datalayer.dto.TimelineEntryDTO;
import com.example.courtierprobackend.transactions.datalayer.dto.StageUpdateRequestDTO;

import com.example.courtierprobackend.security.UserContextUtils;
import com.example.courtierprobackend.security.UserContextFilter;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService service;

    // Helper to extract user ID (returns UUID)


    @PostMapping
    @PreAuthorize("hasRole('BROKER')")
    public ResponseEntity<TransactionResponseDTO> createTransaction(
            @Valid @RequestBody TransactionRequestDTO transactionDTO,
            @RequestHeader(value = "x-broker-id", required = false) String brokerHeader,
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest request
    ) {
        UUID brokerId = UserContextUtils.resolveUserId(request, brokerHeader);
        // Force brokerId from token/header onto DTO to prevent spoofing
        transactionDTO.setBrokerId(brokerId);

        TransactionResponseDTO response = service.createTransaction(transactionDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{transactionId}/notes")
    @PreAuthorize("hasRole('BROKER')")
    public ResponseEntity<TimelineEntryDTO> createNote(
            @PathVariable UUID transactionId,
            @Valid @RequestBody NoteRequestDTO note,
            @RequestHeader(value = "x-broker-id", required = false) String brokerHeader,
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest request
    ) {
        UUID brokerId = UserContextUtils.resolveUserId(request, brokerHeader);
        note.setTransactionId(transactionId);

        TimelineEntryDTO created = service.createNote(transactionId, note, brokerId);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/{transactionId}/notes")
    @PreAuthorize("hasRole('BROKER')")
    public ResponseEntity<List<TimelineEntryDTO>> getNotes(
            @PathVariable UUID transactionId,
            @RequestHeader(value = "x-broker-id", required = false) String brokerHeader,
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest request
    ) {
        UUID brokerId = UserContextUtils.resolveUserId(request, brokerHeader);
        return ResponseEntity.ok(service.getNotes(transactionId, brokerId));
    }

    @GetMapping
    @PreAuthorize("hasRole('BROKER')")
    public ResponseEntity<List<TransactionResponseDTO>> getBrokerTransactions(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String stage,
            @RequestParam(required = false) String side,
            @RequestHeader(value = "x-broker-id", required = false) String brokerHeader,
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest request
    ) {
        UUID brokerId = UserContextUtils.resolveUserId(request, brokerHeader);
        return ResponseEntity.ok(service.getBrokerTransactions(brokerId, status, stage, side));
    }

    @GetMapping("/{transactionId}")
    @PreAuthorize("hasAnyRole('BROKER', 'CLIENT')")
    public ResponseEntity<TransactionResponseDTO> getTransactionById(
            @PathVariable UUID transactionId,
            @RequestHeader(value = "x-broker-id", required = false) String brokerHeader,
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest request
    ) {
        UUID userId = UserContextUtils.resolveUserId(request, brokerHeader);
        return ResponseEntity.ok(service.getByTransactionId(transactionId, userId));
    }

    @PatchMapping("/{transactionId}/stage")
    @PreAuthorize("hasRole('BROKER')")
    public ResponseEntity<TransactionResponseDTO> updateTransactionStage(
            @PathVariable UUID transactionId,
            @Valid @RequestBody StageUpdateRequestDTO dto,
            @RequestHeader(value = "x-broker-id", required = false) String brokerHeader,
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest request
    ) {
        UUID brokerId = UserContextUtils.resolveUserId(request, brokerHeader);

        TransactionResponseDTO updated = service.updateTransactionStage(transactionId, dto, brokerId);
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/{transactionId}/pin")
    @PreAuthorize("hasRole('BROKER')")
    public ResponseEntity<Void> pinTransaction(
            @PathVariable UUID transactionId,
            @RequestHeader(value = "x-broker-id", required = false) String brokerHeader,
            HttpServletRequest request
    ) {
        UUID brokerId = UserContextUtils.resolveUserId(request, brokerHeader);
        service.pinTransaction(transactionId, brokerId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{transactionId}/pin")
    @PreAuthorize("hasRole('BROKER')")
    public ResponseEntity<Void> unpinTransaction(
            @PathVariable UUID transactionId,
            @RequestHeader(value = "x-broker-id", required = false) String brokerHeader,
            HttpServletRequest request
    ) {
        UUID brokerId = UserContextUtils.resolveUserId(request, brokerHeader);
        service.unpinTransaction(transactionId, brokerId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/pinned")
    @PreAuthorize("hasRole('BROKER')")
    public ResponseEntity<Set<UUID>> getPinnedTransactionIds(
            @RequestHeader(value = "x-broker-id", required = false) String brokerHeader,
            HttpServletRequest request
    ) {
        UUID brokerId = UserContextUtils.resolveUserId(request, brokerHeader);
        return ResponseEntity.ok(service.getPinnedTransactionIds(brokerId));
    }
}
