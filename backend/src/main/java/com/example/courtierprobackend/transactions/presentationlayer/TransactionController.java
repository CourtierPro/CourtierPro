package com.example.courtierprobackend.transactions.presentationlayer;

import com.example.courtierprobackend.audit.timeline_audit.businesslayer.TimelineService;
import com.example.courtierprobackend.audit.timeline_audit.presentationlayer.TimelineEntryDTO;
import com.example.courtierprobackend.transactions.businesslayer.TransactionService;
import com.example.courtierprobackend.transactions.datalayer.dto.TransactionRequestDTO;
import com.example.courtierprobackend.transactions.datalayer.dto.TransactionResponseDTO;
import com.example.courtierprobackend.transactions.datalayer.dto.NoteRequestDTO;
import com.example.courtierprobackend.transactions.datalayer.dto.StageUpdateRequestDTO;
import com.example.courtierprobackend.transactions.datalayer.dto.AddParticipantRequestDTO;
import com.example.courtierprobackend.transactions.datalayer.dto.UpdateParticipantRequestDTO;
import com.example.courtierprobackend.transactions.datalayer.dto.ParticipantResponseDTO;
import com.example.courtierprobackend.transactions.datalayer.dto.PropertyRequestDTO;
import com.example.courtierprobackend.transactions.datalayer.dto.PropertyResponseDTO;
import com.example.courtierprobackend.transactions.datalayer.dto.OfferRequestDTO;
import com.example.courtierprobackend.transactions.datalayer.dto.OfferResponseDTO;
import com.example.courtierprobackend.transactions.datalayer.dto.ConditionRequestDTO;
import com.example.courtierprobackend.transactions.datalayer.dto.ConditionResponseDTO;
import com.example.courtierprobackend.transactions.datalayer.dto.PropertyOfferRequestDTO;
import com.example.courtierprobackend.transactions.datalayer.dto.PropertyOfferResponseDTO;
import com.example.courtierprobackend.transactions.datalayer.dto.OfferRevisionResponseDTO;
import com.example.courtierprobackend.transactions.datalayer.dto.OfferDocumentResponseDTO;
import com.example.courtierprobackend.transactions.datalayer.enums.ConditionStatus;

import com.example.courtierprobackend.security.UserContextUtils;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
    @PreAuthorize("hasAnyRole('BROKER', 'CLIENT')")
    public ResponseEntity<List<TransactionResponseDTO>> getTransactions(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String stage,
            @RequestParam(required = false) String side,
            @RequestHeader(value = "x-broker-id", required = false) String brokerHeader,
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest request) {
        UUID userId = UserContextUtils.resolveUserId(request, brokerHeader);
        boolean isBroker = UserContextUtils.isBroker(request);

        if (isBroker) {
            return ResponseEntity.ok(service.getBrokerTransactions(userId, status, stage, side));
        } else {
            // New service method for clients
            return ResponseEntity.ok(service.getClientTransactions(userId));
        }
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

    // ==================== ARCHIVE ENDPOINTS ====================

    @PostMapping("/{transactionId}/archive")
    @PreAuthorize("hasRole('BROKER')")
    public ResponseEntity<Void> archiveTransaction(
            @PathVariable UUID transactionId,
            @RequestHeader(value = "x-broker-id", required = false) String brokerHeader,
            HttpServletRequest request) {
        UUID brokerId = UserContextUtils.resolveUserId(request, brokerHeader);
        service.archiveTransaction(transactionId, brokerId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{transactionId}/archive")
    @PreAuthorize("hasRole('BROKER')")
    public ResponseEntity<Void> unarchiveTransaction(
            @PathVariable UUID transactionId,
            @RequestHeader(value = "x-broker-id", required = false) String brokerHeader,
            HttpServletRequest request) {
        UUID brokerId = UserContextUtils.resolveUserId(request, brokerHeader);
        service.unarchiveTransaction(transactionId, brokerId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/archived")
    @PreAuthorize("hasRole('BROKER')")
    public ResponseEntity<List<TransactionResponseDTO>> getArchivedTransactions(
            @RequestHeader(value = "x-broker-id", required = false) String brokerHeader,
            HttpServletRequest request) {
        UUID brokerId = UserContextUtils.resolveUserId(request, brokerHeader);
        return ResponseEntity.ok(service.getArchivedTransactions(brokerId));
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

    @PutMapping("/{transactionId}/participants/{participantId}")
    @PreAuthorize("hasRole('BROKER')")
    public ResponseEntity<ParticipantResponseDTO> updateParticipant(
            @PathVariable UUID transactionId,
            @PathVariable UUID participantId,
            @Valid @RequestBody UpdateParticipantRequestDTO dto,
            @RequestHeader(value = "x-broker-id", required = false) String brokerHeader,
            HttpServletRequest request) {
        UUID brokerId = UserContextUtils.resolveUserId(request, brokerHeader);
        return ResponseEntity.ok(service.updateParticipant(transactionId, participantId, dto, brokerId));
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
            @PathVariable UUID transactionId,
            @RequestHeader(value = "x-broker-id", required = false) String brokerHeader,
            HttpServletRequest request) {
        UUID userId = UserContextUtils.resolveUserId(request, brokerHeader);
        return ResponseEntity.ok(service.getParticipants(transactionId, userId));
    }

    // ==================== PROPERTY ENDPOINTS ====================

    @GetMapping("/{transactionId}/properties")
    @PreAuthorize("hasAnyRole('BROKER', 'CLIENT')")
    public ResponseEntity<List<PropertyResponseDTO>> getProperties(
            @PathVariable UUID transactionId,
            @RequestHeader(value = "x-broker-id", required = false) String brokerHeader,
            HttpServletRequest request) {
        UUID userId = UserContextUtils.resolveUserId(request, brokerHeader);
        boolean isBroker = UserContextUtils.isBroker(request);
        return ResponseEntity.ok(service.getProperties(transactionId, userId, isBroker));
    }

    @PostMapping("/{transactionId}/properties")
    @PreAuthorize("hasRole('BROKER')")
    public ResponseEntity<PropertyResponseDTO> addProperty(
            @PathVariable UUID transactionId,
            @Valid @RequestBody PropertyRequestDTO dto,
            @RequestHeader(value = "x-broker-id", required = false) String brokerHeader,
            HttpServletRequest request) {
        UUID brokerId = UserContextUtils.resolveUserId(request, brokerHeader);
        return ResponseEntity.status(HttpStatus.CREATED).body(service.addProperty(transactionId, dto, brokerId));
    }

    @PutMapping("/{transactionId}/properties/{propertyId}")
    @PreAuthorize("hasRole('BROKER')")
    public ResponseEntity<PropertyResponseDTO> updateProperty(
            @PathVariable UUID transactionId,
            @PathVariable UUID propertyId,
            @Valid @RequestBody PropertyRequestDTO dto,
            @RequestHeader(value = "x-broker-id", required = false) String brokerHeader,
            HttpServletRequest request) {
        UUID brokerId = UserContextUtils.resolveUserId(request, brokerHeader);
        return ResponseEntity.ok(service.updateProperty(transactionId, propertyId, dto, brokerId));
    }

    @DeleteMapping("/{transactionId}/properties/{propertyId}")
    @PreAuthorize("hasRole('BROKER')")
    public ResponseEntity<Void> removeProperty(
            @PathVariable UUID transactionId,
            @PathVariable UUID propertyId,
            @RequestHeader(value = "x-broker-id", required = false) String brokerHeader,
            HttpServletRequest request) {
        UUID brokerId = UserContextUtils.resolveUserId(request, brokerHeader);
        service.removeProperty(transactionId, propertyId, brokerId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{transactionId}/properties/{propertyId}")
    @PreAuthorize("hasAnyRole('BROKER', 'CLIENT')")
    public ResponseEntity<PropertyResponseDTO> getPropertyById(
            @PathVariable UUID transactionId,
            @PathVariable UUID propertyId,
            @RequestHeader(value = "x-broker-id", required = false) String brokerHeader,
            HttpServletRequest request) {
        UUID userId = UserContextUtils.resolveUserId(request, brokerHeader);
        boolean isBroker = UserContextUtils.isBroker(request);
        return ResponseEntity.ok(service.getPropertyById(propertyId, userId, isBroker));
    }

    @PutMapping("/{transactionId}/active-property/{propertyId}")
    @PreAuthorize("hasRole('BROKER')")
    public ResponseEntity<Void> setActiveProperty(
            @PathVariable UUID transactionId,
            @PathVariable UUID propertyId,
            @RequestHeader(value = "x-broker-id", required = false) String brokerHeader,
            HttpServletRequest request) {
        UUID brokerId = UserContextUtils.resolveUserId(request, brokerHeader);
        service.setActiveProperty(transactionId, propertyId, brokerId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{transactionId}/active-property")
    @PreAuthorize("hasRole('BROKER')")
    public ResponseEntity<Void> clearActiveProperty(
            @PathVariable UUID transactionId,
            @RequestHeader(value = "x-broker-id", required = false) String brokerHeader,
            HttpServletRequest request) {
        UUID brokerId = UserContextUtils.resolveUserId(request, brokerHeader);
        service.clearActiveProperty(transactionId, brokerId);
        return ResponseEntity.noContent().build();
    }

    // ==================== PROPERTY OFFER ENDPOINTS ====================

    @GetMapping("/properties/{propertyId}/offers")
    @PreAuthorize("hasAnyRole('BROKER', 'CLIENT')")
    public ResponseEntity<List<PropertyOfferResponseDTO>> getPropertyOffers(
            @PathVariable UUID propertyId,
            @RequestHeader(value = "x-broker-id", required = false) String brokerHeader,
            HttpServletRequest request) {
        UUID userId = UserContextUtils.resolveUserId(request, brokerHeader);
        boolean isBroker = UserContextUtils.isBroker(request);
        return ResponseEntity.ok(service.getPropertyOffers(propertyId, userId, isBroker));
    }

    @PostMapping("/properties/{propertyId}/offers")
    @PreAuthorize("hasRole('BROKER')")
    public ResponseEntity<PropertyOfferResponseDTO> addPropertyOffer(
            @PathVariable UUID propertyId,
            @Valid @RequestBody PropertyOfferRequestDTO dto,
            @RequestHeader(value = "x-broker-id", required = false) String brokerHeader,
            HttpServletRequest request) {
        UUID brokerId = UserContextUtils.resolveUserId(request, brokerHeader);
        return ResponseEntity.status(HttpStatus.CREATED).body(service.addPropertyOffer(propertyId, dto, brokerId));
    }

    @PutMapping("/properties/{propertyId}/offers/{propertyOfferId}")
    @PreAuthorize("hasRole('BROKER')")
    public ResponseEntity<PropertyOfferResponseDTO> updatePropertyOffer(
            @PathVariable UUID propertyId,
            @PathVariable UUID propertyOfferId,
            @Valid @RequestBody PropertyOfferRequestDTO dto,
            @RequestHeader(value = "x-broker-id", required = false) String brokerHeader,
            HttpServletRequest request) {
        UUID brokerId = UserContextUtils.resolveUserId(request, brokerHeader);
        return ResponseEntity.ok(service.updatePropertyOffer(propertyId, propertyOfferId, dto, brokerId));
    }

    // ==================== OFFER ENDPOINTS ====================

    @GetMapping("/{transactionId}/offers")
    @PreAuthorize("hasAnyRole('BROKER', 'CLIENT')")
    public ResponseEntity<List<OfferResponseDTO>> getOffers(
            @PathVariable UUID transactionId,
            @RequestHeader(value = "x-broker-id", required = false) String brokerHeader,
            HttpServletRequest request) {
        UUID userId = UserContextUtils.resolveUserId(request, brokerHeader);
        boolean isBroker = UserContextUtils.isBroker(request);
        return ResponseEntity.ok(service.getOffers(transactionId, userId, isBroker));
    }

    @PostMapping("/{transactionId}/offers")
    @PreAuthorize("hasRole('BROKER')")
    public ResponseEntity<OfferResponseDTO> addOffer(
            @PathVariable UUID transactionId,
            @Valid @RequestBody OfferRequestDTO dto,
            @RequestHeader(value = "x-broker-id", required = false) String brokerHeader,
            HttpServletRequest request) {
        UUID brokerId = UserContextUtils.resolveUserId(request, brokerHeader);
        return ResponseEntity.status(HttpStatus.CREATED).body(service.addOffer(transactionId, dto, brokerId));
    }

    @PutMapping("/{transactionId}/offers/{offerId}")
    @PreAuthorize("hasRole('BROKER')")
    public ResponseEntity<OfferResponseDTO> updateOffer(
            @PathVariable UUID transactionId,
            @PathVariable UUID offerId,
            @Valid @RequestBody OfferRequestDTO dto,
            @RequestHeader(value = "x-broker-id", required = false) String brokerHeader,
            HttpServletRequest request) {
        UUID brokerId = UserContextUtils.resolveUserId(request, brokerHeader);
        return ResponseEntity.ok(service.updateOffer(transactionId, offerId, dto, brokerId));
    }

    @DeleteMapping("/{transactionId}/offers/{offerId}")
    @PreAuthorize("hasRole('BROKER')")
    public ResponseEntity<Void> removeOffer(
            @PathVariable UUID transactionId,
            @PathVariable UUID offerId,
            @RequestHeader(value = "x-broker-id", required = false) String brokerHeader,
            HttpServletRequest request) {
        UUID brokerId = UserContextUtils.resolveUserId(request, brokerHeader);
        service.removeOffer(transactionId, offerId, brokerId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{transactionId}/offers/{offerId}")
    @PreAuthorize("hasAnyRole('BROKER', 'CLIENT')")
    public ResponseEntity<OfferResponseDTO> getOfferById(
            @PathVariable UUID transactionId,
            @PathVariable UUID offerId,
            @RequestHeader(value = "x-broker-id", required = false) String brokerHeader,
            HttpServletRequest request) {
        UUID userId = UserContextUtils.resolveUserId(request, brokerHeader);
        boolean isBroker = UserContextUtils.isBroker(request);
        return ResponseEntity.ok(service.getOfferById(offerId, userId, isBroker));
    }

    @GetMapping("/{transactionId}/offers/{offerId}/revisions")
    @PreAuthorize("hasAnyRole('BROKER', 'CLIENT')")
    public ResponseEntity<List<OfferRevisionResponseDTO>> getOfferRevisions(
            @PathVariable UUID transactionId,
            @PathVariable UUID offerId,
            @RequestHeader(value = "x-broker-id", required = false) String brokerHeader,
            HttpServletRequest request) {
        UUID userId = UserContextUtils.resolveUserId(request, brokerHeader);
        boolean isBroker = UserContextUtils.isBroker(request);
        return ResponseEntity.ok(service.getOfferRevisions(offerId, userId, isBroker));
    }

    // ==================== OFFER DOCUMENT ENDPOINTS ====================

    @PostMapping("/{transactionId}/offers/{offerId}/documents")
    @PreAuthorize("hasRole('BROKER')")
    public ResponseEntity<OfferDocumentResponseDTO> uploadOfferDocument(
            @PathVariable UUID transactionId,
            @PathVariable UUID offerId,
            @RequestParam("file") MultipartFile file,
            @RequestHeader(value = "x-broker-id", required = false) String brokerHeader,
            HttpServletRequest request) {
        UUID brokerId = UserContextUtils.resolveUserId(request, brokerHeader);
        return ResponseEntity.status(HttpStatus.CREATED).body(service.uploadOfferDocument(offerId, file, brokerId));
    }

    @PostMapping("/properties/{propertyId}/offers/{propertyOfferId}/documents")
    @PreAuthorize("hasRole('BROKER')")
    public ResponseEntity<OfferDocumentResponseDTO> uploadPropertyOfferDocument(
            @PathVariable UUID propertyId,
            @PathVariable UUID propertyOfferId,
            @RequestParam("file") MultipartFile file,
            @RequestHeader(value = "x-broker-id", required = false) String brokerHeader,
            HttpServletRequest request) {
        UUID brokerId = UserContextUtils.resolveUserId(request, brokerHeader);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.uploadPropertyOfferDocument(propertyOfferId, file, brokerId));
    }

    @GetMapping("/{transactionId}/offers/{offerId}/documents")
    @PreAuthorize("hasAnyRole('BROKER', 'CLIENT')")
    public ResponseEntity<List<OfferDocumentResponseDTO>> getOfferDocuments(
            @PathVariable UUID transactionId,
            @PathVariable UUID offerId,
            @RequestHeader(value = "x-broker-id", required = false) String brokerHeader,
            HttpServletRequest request) {
        UUID userId = UserContextUtils.resolveUserId(request, brokerHeader);
        boolean isBroker = UserContextUtils.isBroker(request);
        return ResponseEntity.ok(service.getOfferDocuments(offerId, userId, isBroker));
    }

    @GetMapping("/properties/{propertyId}/offers/{propertyOfferId}/documents")
    @PreAuthorize("hasAnyRole('BROKER', 'CLIENT')")
    public ResponseEntity<List<OfferDocumentResponseDTO>> getPropertyOfferDocuments(
            @PathVariable UUID propertyId,
            @PathVariable UUID propertyOfferId,
            @RequestHeader(value = "x-broker-id", required = false) String brokerHeader,
            HttpServletRequest request) {
        UUID userId = UserContextUtils.resolveUserId(request, brokerHeader);
        boolean isBroker = UserContextUtils.isBroker(request);
        return ResponseEntity.ok(service.getPropertyOfferDocuments(propertyOfferId, userId, isBroker));
    }

    @GetMapping("/documents/{documentId}/download")
    @PreAuthorize("hasAnyRole('BROKER', 'CLIENT')")
    public ResponseEntity<String> getOfferDocumentDownloadUrl(
            @PathVariable UUID documentId,
            @RequestHeader(value = "x-broker-id", required = false) String brokerHeader,
            HttpServletRequest request) {
        UUID userId = UserContextUtils.resolveUserId(request, brokerHeader);
        return ResponseEntity.ok(service.getOfferDocumentDownloadUrl(documentId, userId));
    }

    @DeleteMapping("/documents/{documentId}")
    @PreAuthorize("hasRole('BROKER')")
    public ResponseEntity<Void> deleteOfferDocument(
            @PathVariable UUID documentId,
            @RequestHeader(value = "x-broker-id", required = false) String brokerHeader,
            HttpServletRequest request) {
        UUID brokerId = UserContextUtils.resolveUserId(request, brokerHeader);
        service.deleteOfferDocument(documentId, brokerId);
        return ResponseEntity.noContent().build();
    }

    // ==================== CONDITION ENDPOINTS ====================

    @GetMapping("/{transactionId}/conditions")
    @PreAuthorize("hasAnyRole('BROKER', 'CLIENT')")
    public ResponseEntity<List<ConditionResponseDTO>> getConditions(
            @PathVariable UUID transactionId,
            @RequestHeader(value = "x-broker-id", required = false) String brokerHeader,
            HttpServletRequest request) {
        UUID userId = UserContextUtils.resolveUserId(request, brokerHeader);
        boolean isBroker = UserContextUtils.isBroker(request);
        return ResponseEntity.ok(service.getConditions(transactionId, userId, isBroker));
    }

    @PostMapping("/{transactionId}/conditions")
    @PreAuthorize("hasRole('BROKER')")
    public ResponseEntity<ConditionResponseDTO> addCondition(
            @PathVariable UUID transactionId,
            @Valid @RequestBody ConditionRequestDTO dto,
            @RequestHeader(value = "x-broker-id", required = false) String brokerHeader,
            HttpServletRequest request) {
        UUID brokerId = UserContextUtils.resolveUserId(request, brokerHeader);
        return ResponseEntity.status(HttpStatus.CREATED).body(service.addCondition(transactionId, dto, brokerId));
    }

    @PutMapping("/{transactionId}/conditions/{conditionId}")
    @PreAuthorize("hasRole('BROKER')")
    public ResponseEntity<ConditionResponseDTO> updateCondition(
            @PathVariable UUID transactionId,
            @PathVariable UUID conditionId,
            @Valid @RequestBody ConditionRequestDTO dto,
            @RequestHeader(value = "x-broker-id", required = false) String brokerHeader,
            HttpServletRequest request) {
        UUID brokerId = UserContextUtils.resolveUserId(request, brokerHeader);
        return ResponseEntity.ok(service.updateCondition(transactionId, conditionId, dto, brokerId));
    }

    @DeleteMapping("/{transactionId}/conditions/{conditionId}")
    @PreAuthorize("hasRole('BROKER')")
    public ResponseEntity<Void> removeCondition(
            @PathVariable UUID transactionId,
            @PathVariable UUID conditionId,
            @RequestHeader(value = "x-broker-id", required = false) String brokerHeader,
            HttpServletRequest request) {
        UUID brokerId = UserContextUtils.resolveUserId(request, brokerHeader);
        service.removeCondition(transactionId, conditionId, brokerId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{transactionId}/conditions/{conditionId}/status")
    @PreAuthorize("hasRole('BROKER')")
    public ResponseEntity<ConditionResponseDTO> updateConditionStatus(
            @PathVariable UUID transactionId,
            @PathVariable UUID conditionId,
            @RequestParam ConditionStatus status,
            @RequestHeader(value = "x-broker-id", required = false) String brokerHeader,
            HttpServletRequest request) {
        UUID brokerId = UserContextUtils.resolveUserId(request, brokerHeader);
        return ResponseEntity.ok(service.updateConditionStatus(transactionId, conditionId, status, brokerId));
    }

    // ==================== UNIFIED DOCUMENTS ENDPOINT ====================

    @GetMapping("/{transactionId}/all-documents")
    @PreAuthorize("hasAnyRole('BROKER', 'CLIENT')")
    public ResponseEntity<List<com.example.courtierprobackend.transactions.datalayer.dto.UnifiedDocumentDTO>> getAllTransactionDocuments(
            @PathVariable UUID transactionId,
            @RequestHeader(value = "x-broker-id", required = false) String brokerHeader,
            HttpServletRequest request) {
        UUID userId = UserContextUtils.resolveUserId(request, brokerHeader);
        boolean isBroker = UserContextUtils.isBroker(request);
        return ResponseEntity.ok(service.getAllTransactionDocuments(transactionId, userId, isBroker));
    }
}
