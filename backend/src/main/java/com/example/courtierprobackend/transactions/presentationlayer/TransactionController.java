package com.example.courtierprobackend.transactions.presentationlayer;

import com.example.courtierprobackend.audit.timeline_audit.businesslayer.TimelineService;
import com.example.courtierprobackend.audit.timeline_audit.presentationlayer.TimelineEntryDTO;
import com.example.courtierprobackend.transactions.businesslayer.TransactionService;
import com.example.courtierprobackend.transactions.datalayer.dto.TransactionRequestDTO;
import com.example.courtierprobackend.transactions.datalayer.dto.TransactionResponseDTO;
import com.example.courtierprobackend.transactions.datalayer.dto.NoteRequestDTO;
import com.example.courtierprobackend.transactions.datalayer.dto.StageUpdateRequestDTO;
import com.example.courtierprobackend.transactions.datalayer.dto.AddParticipantRequestDTO;
import com.example.courtierprobackend.transactions.datalayer.dto.ParticipantResponseDTO;

import com.example.courtierprobackend.security.UserContextUtils;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/transactions")
@RequiredArgsConstructor
public class TransactionController {

    /**
     * Get timeline entries visible to the client for a transaction.
     */
    @GetMapping("/{transactionId}/timeline/client")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<List<TimelineEntryDTO>> getClientTransactionTimeline(
            @PathVariable UUID transactionId,
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest request) {
        List<TimelineEntryDTO> dtos = timelineService.getTimelineForClient(transactionId);
        return ResponseEntity.ok(dtos);
    }

    /**
     * Save internal notes and create a NOTE event in the timeline.
     */
    @PostMapping("/{transactionId}/internal-notes")
    @PreAuthorize("hasRole('BROKER')")
    public ResponseEntity<Void> saveInternalNotes(
            @PathVariable UUID transactionId,
            @RequestBody String notes,
            @RequestHeader(value = "x-broker-id", required = false) String brokerHeader,
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest request) {
        UUID brokerId = UserContextUtils.resolveUserId(request, brokerHeader);
        service.saveInternalNotes(transactionId, notes, brokerId);
        return ResponseEntity.ok().build();
    }

    private final TransactionService service;
    private final TimelineService timelineService;

    // Helper to extract user ID (returns UUID)

    @PostMapping
    @PreAuthorize("hasRole('BROKER')")
    public ResponseEntity<TransactionResponseDTO> createTransaction(
            @Valid @RequestBody TransactionRequestDTO transactionDTO,
            @RequestHeader(value = "x-broker-id", required = false) String brokerHeader,
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest request) {
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
            HttpServletRequest request) {
        UUID brokerId = UserContextUtils.resolveUserId(request, brokerHeader);
        note.setTransactionId(transactionId);
        note.setActorId(brokerId); // Injecte l'actorId côté backend

        TimelineEntryDTO created = service.createNote(transactionId, note, brokerId);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/{transactionId}/notes")
    @PreAuthorize("hasRole('BROKER')")
    public ResponseEntity<List<TimelineEntryDTO>> getNotes(
            @PathVariable UUID transactionId,
            @RequestHeader(value = "x-broker-id", required = false) String brokerHeader,
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest request) {
        UUID brokerId = UserContextUtils.resolveUserId(request, brokerHeader);
        return ResponseEntity.ok(service.getNotes(transactionId, brokerId));
    }

    @GetMapping("/{transactionId}/timeline")
    @PreAuthorize("hasAnyRole('BROKER', 'CLIENT')")
    public ResponseEntity<List<TimelineEntryDTO>> getTransactionTimeline(
            @PathVariable UUID transactionId,
            @RequestHeader(value = "x-broker-id", required = false) String brokerHeader,
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest request) {
        // Optionally: check access rights here
        List<TimelineEntryDTO> dtos = timelineService.getTimelineForTransaction(transactionId);
        return ResponseEntity.ok(dtos);
    }

    @GetMapping
    @PreAuthorize("hasRole('BROKER')")
    public ResponseEntity<List<TransactionResponseDTO>> getBrokerTransactions(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String stage,
            @RequestParam(required = false) String side,
            @RequestHeader(value = "x-broker-id", required = false) String brokerHeader,
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest request) {
        UUID brokerId = UserContextUtils.resolveUserId(request, brokerHeader);
        return ResponseEntity.ok(service.getBrokerTransactions(brokerId, status, stage, side));
    }

    @GetMapping("/{transactionId}")
    @PreAuthorize("hasAnyRole('BROKER', 'CLIENT')")
    public ResponseEntity<TransactionResponseDTO> getTransactionById(
            @PathVariable UUID transactionId,
            @RequestHeader(value = "x-broker-id", required = false) String brokerHeader,
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest request) {
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
            HttpServletRequest request) {
        UUID brokerId = UserContextUtils.resolveUserId(request, brokerHeader);

        TransactionResponseDTO updated = service.updateTransactionStage(transactionId, dto, brokerId);
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/{transactionId}/pin")
    @PreAuthorize("hasRole('BROKER')")
    public ResponseEntity<Void> pinTransaction(
            @PathVariable UUID transactionId,
            @RequestHeader(value = "x-broker-id", required = false) String brokerHeader,
            HttpServletRequest request) {
        UUID brokerId = UserContextUtils.resolveUserId(request, brokerHeader);
        service.pinTransaction(transactionId, brokerId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{transactionId}/pin")
    @PreAuthorize("hasRole('BROKER')")
    public ResponseEntity<Void> unpinTransaction(
            @PathVariable UUID transactionId,
            @RequestHeader(value = "x-broker-id", required = false) String brokerHeader,
            HttpServletRequest request) {
        UUID brokerId = UserContextUtils.resolveUserId(request, brokerHeader);
        service.unpinTransaction(transactionId, brokerId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/pinned")
    @PreAuthorize("hasRole('BROKER')")
    public ResponseEntity<Set<UUID>> getPinnedTransactionIds(
            @RequestHeader(value = "x-broker-id", required = false) String brokerHeader,
            HttpServletRequest request) {
        UUID brokerId = UserContextUtils.resolveUserId(request, brokerHeader);
        return ResponseEntity.ok(service.getPinnedTransactionIds(brokerId));
    }

    @PostMapping("/{transactionId}/participants")
    @PreAuthorize("hasRole('BROKER')")
    public ResponseEntity<ParticipantResponseDTO> addParticipant(
            @PathVariable UUID transactionId,
            @Valid @RequestBody AddParticipantRequestDTO dto,
            @RequestHeader(value = "x-broker-id", required = false) String brokerHeader,
            HttpServletRequest request) {
        UUID brokerId = UserContextUtils.resolveUserId(request, brokerHeader);
        return ResponseEntity.status(HttpStatus.CREATED).body(service.addParticipant(transactionId, dto, brokerId));
    }

    @DeleteMapping("/{transactionId}/participants/{participantId}")
    @PreAuthorize("hasRole('BROKER')")
    public ResponseEntity<Void> removeParticipant(
            @PathVariable UUID transactionId,
            @PathVariable UUID participantId,
            @RequestHeader(value = "x-broker-id", required = false) String brokerHeader,
            HttpServletRequest request) {
        UUID brokerId = UserContextUtils.resolveUserId(request, brokerHeader);
        service.removeParticipant(transactionId, participantId, brokerId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{transactionId}/participants")
    @PreAuthorize("hasAnyRole('BROKER', 'CLIENT')")
    public ResponseEntity<List<ParticipantResponseDTO>> getParticipants(
            @PathVariable UUID transactionId) {
        return ResponseEntity.ok(service.getParticipants(transactionId));
    }
}
