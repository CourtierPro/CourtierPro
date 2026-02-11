package com.example.courtierprobackend.transactions.util;

import com.example.courtierprobackend.audit.timeline_audit.dataaccesslayer.TimelineEntry;
import com.example.courtierprobackend.audit.timeline_audit.presentationlayer.TimelineEntryDTO;
import com.example.courtierprobackend.transactions.datalayer.*;
import com.example.courtierprobackend.transactions.datalayer.dto.*;
import com.example.courtierprobackend.transactions.datalayer.enums.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class EntityDtoUtil {

    /* =========================================================
       TRANSACTION -> RESPONSE DTO
       ========================================================= */
    public static TransactionResponseDTO toResponse(Transaction t, String clientName) {
        return toResponse(t, clientName, t.getCentrisNumber(), null);
    }

    /**
     * Overloaded version that accepts a centrisNumber override.
     * Used for buy-side transactions where centris comes from the accepted property.
     */
    public static TransactionResponseDTO toResponse(Transaction t, String clientName, String centrisNumber) {
        return toResponse(t, clientName, centrisNumber, null);
    }

    /**
     * Full version that accepts client name, centris number override, and broker name.
     * Used when listing all transactions for a client with broker attribution.
     */
    public static TransactionResponseDTO toResponse(Transaction t, String clientName, String centrisNumber, String brokerName) {
        return TransactionResponseDTO.builder()
                .transactionId(t.getTransactionId())
                .clientId(t.getClientId())
                .clientName(clientName)
                .brokerId(t.getBrokerId())
                .brokerName(brokerName)
                .side(t.getSide())
                .propertyAddress(t.getPropertyAddress())
                .centrisNumber(centrisNumber)
                .currentStage(resolveStage(t))
                .status(t.getStatus())
                .openedDate(t.getOpenedAt() != null ? t.getOpenedAt().toString() : null)
                .lastUpdated(t.getLastUpdated() != null ? t.getLastUpdated().toString() : null)
                .archived(t.getArchived() != null ? t.getArchived() : false)
                .archivedAt(t.getArchivedAt() != null ? t.getArchivedAt().toString() : null)
                .notes(t.getNotes())
                .build();
    }


    /* =========================================================
       REQUEST DTO -> NEW TRANSACTION (INITIAL VALUES ONLY)
       Used when creating a new Transaction
       ========================================================= */
    public static Transaction toNewTransaction(TransactionRequestDTO dto, UUID generatedId) {
        Transaction t = new Transaction();
        t.setTransactionId(generatedId);
        t.setClientId(dto.getClientId());
        t.setBrokerId(dto.getBrokerId());
        t.setSide(dto.getSide());
        t.setPropertyAddress(dto.getPropertyAddress());
        t.setCentrisNumber(dto.getCentrisNumber());
        return t;
    }


    /* =========================================================
       TIMELINE ENTRY -> TIMELINE DTO
       ========================================================= */
    public static TimelineEntryDTO toTimelineDTO(TimelineEntry entry) {
        return TimelineEntryDTO.builder()
            .id(entry.getId())
            .type(entry.getType())
            .note(entry.getNote())
            .title(null) // Adapter si le champ existe dans TimelineEntry
            .visibleToClient(entry.isVisibleToClient())
            .occurredAt(entry.getTimestamp())
            .addedByBrokerId(entry.getActorId())
            .docType(entry.getDocType())
            .build();
    }


    /* =========================================================
       LIST<TimelineEntry> -> LIST<TimelineEntryDTO>
       ========================================================= */
    public static List<TimelineEntryDTO> toTimelineDTOs(List<TimelineEntry> entries) {
        if (entries == null) return List.of();
        return entries.stream()
                .map(EntityDtoUtil::toTimelineDTO)
                .collect(Collectors.toList());
    }


    /* =========================================================
       HELPER: Resolve current stage as text
       ========================================================= */
    private static String resolveStage(Transaction t) {

        if (t.getSide() == TransactionSide.BUY_SIDE) {
            return t.getBuyerStage() != null
                    ? t.getBuyerStage().name()
                    : "UNKNOWN_BUYER_STAGE";
        }

        return t.getSellerStage() != null
                ? t.getSellerStage().name()
                : "UNKNOWN_SELLER_STAGE";
    }


    /* =========================================================
       STAGE UPDATE UTILITIES (FUTURE PROOFING FOR CP-14+)
       ========================================================= */

    // Ensures buyer side only updates buyer stage
    public static void updateBuyerStage(Transaction t, BuyerStage newStage) {
        t.setBuyerStage(newStage);
        t.setSellerStage(null); // maintain invariant
    }

    // Ensures seller side only updates seller stage
    public static void updateSellerStage(Transaction t, SellerStage newStage) {
        t.setSellerStage(newStage);
        t.setBuyerStage(null); // maintain invariant
    }


    /* =========================================================
       EMBEDDABLE HELPERS FOR FUTURE MAPPINGS
       ========================================================= */
    public static PropertyAddress copyAddress(PropertyAddress p) {
        if (p == null) return null;
        return new PropertyAddress(
                p.getStreet(),
                p.getCity(),
                p.getProvince(),
                p.getPostalCode()
        );
    }
    public static TransactionResponseDTO toResponseStub(UUID id, UUID clientId, UUID brokerId) {
        return TransactionResponseDTO.builder()
                .transactionId(id)
                .clientId(clientId)
                .brokerId(brokerId)
                .side(TransactionSide.BUY_SIDE)
                .status(TransactionStatus.ACTIVE)
                .propertyAddress(null)
                .currentStage(BuyerStage.BUYER_FINANCIAL_PREPARATION.name())
                .openedDate(LocalDate.now().toString())
                .archived(false)
                .archivedAt(null)
                .build();
    }

}
