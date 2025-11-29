package com.example.courtierprobackend.transactions.util;

import com.example.courtierprobackend.transactions.datalayer.*;
import com.example.courtierprobackend.transactions.datalayer.dto.*;
import com.example.courtierprobackend.transactions.datalayer.enums.*;

import java.util.List;
import java.util.stream.Collectors;

public class EntityDtoUtil {

    /* =========================================================
       TRANSACTION -> RESPONSE DTO
       ========================================================= */
    public static TransactionResponseDTO toResponse(Transaction t) {
        return TransactionResponseDTO.builder()
                .transactionId(t.getTransactionId())
                .clientId(t.getClientId())
                .brokerId(t.getBrokerId())
                .side(t.getSide())
                .propertyAddress(t.getPropertyAddress())
                .currentStage(resolveStage(t))
                .status(t.getStatus())
                .build();
    }


    /* =========================================================
       REQUEST DTO -> NEW TRANSACTION (INITIAL VALUES ONLY)
       Used when creating a new Transaction
       ========================================================= */
    public static Transaction toNewTransaction(TransactionRequestDTO dto, String generatedId) {
        Transaction t = new Transaction();
        t.setTransactionId(generatedId);
        t.setClientId(dto.getClientId());
        t.setBrokerId(dto.getBrokerId());
        t.setSide(dto.getSide());
        t.setPropertyAddress(dto.getPropertyAddress());
        return t;
    }


    /* =========================================================
       TIMELINE ENTRY -> TIMELINE DTO
       ========================================================= */
    public static TimelineEntryDTO toTimelineDTO(TimelineEntry entry) {
        return TimelineEntryDTO.builder()
                .type(entry.getType())
                .note(entry.getNote())
                .occurredAt(entry.getOccurredAt())
                .addedByBrokerId(entry.getAddedByBrokerId())
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
    public static TransactionResponseDTO toResponseStub(String id, String clientId, String brokerId) {
        return TransactionResponseDTO.builder()
                .transactionId(id)
                .clientId(clientId)
                .brokerId(brokerId)
                .side(TransactionSide.BUY_SIDE)
                .status(TransactionStatus.ACTIVE)
                .propertyAddress(null)
                .currentStage("TEST_STAGE")
                .build();
    }

}
