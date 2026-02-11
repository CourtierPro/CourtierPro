package com.example.courtierprobackend.transactions.util;

import com.example.courtierprobackend.transactions.datalayer.PropertyAddress;
import com.example.courtierprobackend.audit.timeline_audit.dataaccesslayer.TimelineEntry;
import com.example.courtierprobackend.audit.timeline_audit.dataaccesslayer.Enum.TimelineEntryType;
import com.example.courtierprobackend.transactions.datalayer.Transaction;
import com.example.courtierprobackend.audit.timeline_audit.presentationlayer.TimelineEntryDTO;
import com.example.courtierprobackend.transactions.datalayer.dto.TransactionRequestDTO;
import com.example.courtierprobackend.transactions.datalayer.dto.TransactionResponseDTO;
import com.example.courtierprobackend.transactions.datalayer.enums.*;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for EntityDtoUtil utility class.
 * Tests mapping between Transaction entities and DTOs for API communication.
 */
class EntityDtoUtilTest {

    // ========== toResponse Tests ==========

    @Test
    void toResponse_withBuyerSideTransaction_mapsCorrectly() {
        // Arrange
        UUID txId = UUID.randomUUID();
        UUID clientId = UUID.randomUUID();
        UUID brokerId = UUID.randomUUID();
        Transaction tx = new Transaction();
        tx.setTransactionId(txId);
        tx.setClientId(clientId);
        tx.setBrokerId(brokerId);
        tx.setSide(TransactionSide.BUY_SIDE);
        tx.setBuyerStage(BuyerStage.BUYER_PROPERTY_SEARCH);
        tx.setStatus(TransactionStatus.ACTIVE);
        tx.setOpenedAt(LocalDateTime.of(2025, 1, 15, 10, 0));
        tx.setLastUpdated(LocalDateTime.of(2025, 2, 10, 14, 30));

        PropertyAddress address = new PropertyAddress();
        address.setStreet("123 Main St");
        tx.setPropertyAddress(address);

        // Act
        TransactionResponseDTO result = EntityDtoUtil.toResponse(tx, "Test Client");

        // Assert
        assertThat(result.getTransactionId()).isEqualTo(txId);
        assertThat(result.getClientId()).isEqualTo(clientId);
        assertThat(result.getBrokerId()).isEqualTo(brokerId);
        assertThat(result.getSide()).isEqualTo(TransactionSide.BUY_SIDE);
        assertThat(result.getCurrentStage()).isEqualTo("BUYER_PROPERTY_SEARCH");
        assertThat(result.getStatus()).isEqualTo(TransactionStatus.ACTIVE);
        // openedDate should now be the full string representation of LocalDateTime
        assertThat(result.getOpenedDate()).isEqualTo("2025-01-15T10:00");
        // lastUpdated should be mapped
        assertThat(result.getLastUpdated()).isEqualTo("2025-02-10T14:30");
        assertThat(result.getPropertyAddress().getStreet()).isEqualTo("123 Main St");
    }

    @Test
    void toResponse_withLastUpdatedNull_mapsToNull() {
        // Arrange
        Transaction tx = new Transaction();
        tx.setTransactionId(UUID.randomUUID());
        tx.setSide(TransactionSide.BUY_SIDE);
        tx.setOpenedAt(LocalDateTime.of(2025, 1, 15, 10, 0));
        tx.setLastUpdated(null);

        // Act
        TransactionResponseDTO result = EntityDtoUtil.toResponse(tx, "Client");

        // Assert
        assertThat(result.getLastUpdated()).isNull();
    }

    @Test
    void toResponse_withCentrisNumberOverride_useOverride() {
        // Arrange
        Transaction tx = new Transaction();
        tx.setTransactionId(UUID.randomUUID());
        tx.setClientId(UUID.randomUUID());
        tx.setBrokerId(UUID.randomUUID());
        tx.setSide(TransactionSide.BUY_SIDE);
        tx.setBuyerStage(BuyerStage.BUYER_PROPERTY_SEARCH);
        tx.setStatus(TransactionStatus.ACTIVE);
        tx.setCentrisNumber("12345678"); // Transaction's own centris

        // Act - Use overloaded method with a different centris number
        TransactionResponseDTO result = EntityDtoUtil.toResponse(tx, "Test Client", "87654321");

        // Assert - Should use the override, not the transaction's centris
        assertThat(result.getCentrisNumber()).isEqualTo("87654321");
    }

    @Test
    void toResponse_withCentrisNumberOverrideNull_usesNull() {
        // Arrange
        Transaction tx = new Transaction();
        tx.setTransactionId(UUID.randomUUID());
        tx.setClientId(UUID.randomUUID());
        tx.setBrokerId(UUID.randomUUID());
        tx.setSide(TransactionSide.BUY_SIDE);
        tx.setBuyerStage(BuyerStage.BUYER_PROPERTY_SEARCH);
        tx.setStatus(TransactionStatus.ACTIVE);
        tx.setCentrisNumber("12345678");

        // Act - Pass null as override
        TransactionResponseDTO result = EntityDtoUtil.toResponse(tx, "Test Client", null);

        // Assert - Should use null when override is null
        assertThat(result.getCentrisNumber()).isNull();
    }

    @Test
    void toResponse_withSellerSideTransaction_mapsCorrectly() {
        // Arrange
        Transaction tx = new Transaction();
        tx.setTransactionId(UUID.randomUUID());
        tx.setClientId(UUID.randomUUID());
        tx.setBrokerId(UUID.randomUUID());
        tx.setSide(TransactionSide.SELL_SIDE);
        tx.setSellerStage(SellerStage.SELLER_PUBLISH_LISTING);
        tx.setStatus(TransactionStatus.ACTIVE);
        tx.setOpenedAt(LocalDateTime.of(2025, 2, 20, 14, 30));

        // Act
        TransactionResponseDTO result = EntityDtoUtil.toResponse(tx, "Test Client");

        // Assert
        assertThat(result.getCurrentStage()).isEqualTo("SELLER_PUBLISH_LISTING");
        assertThat(result.getOpenedDate()).isEqualTo("2025-02-20T14:30");
    }

    @Test
    void toResponse_withNullOpenedAt_returnsNullOpenedDate() {
        // Arrange
        Transaction tx = new Transaction();
        tx.setTransactionId(UUID.randomUUID());
        tx.setClientId(UUID.randomUUID());
        tx.setBrokerId(UUID.randomUUID());
        tx.setSide(TransactionSide.BUY_SIDE);
        tx.setOpenedAt(null);

        // Act
        TransactionResponseDTO result = EntityDtoUtil.toResponse(tx, "Test Client");

        // Assert
        assertThat(result.getOpenedDate()).isNull();
    }

    @Test
    void toResponse_withNullBuyerStage_returnsUnknownBuyerStage() {
        // Arrange
        Transaction tx = new Transaction();
        tx.setTransactionId(UUID.randomUUID());
        tx.setClientId(UUID.randomUUID());
        tx.setBrokerId(UUID.randomUUID());
        tx.setSide(TransactionSide.BUY_SIDE);
        tx.setBuyerStage(null);

        // Act
        TransactionResponseDTO result = EntityDtoUtil.toResponse(tx, "Test Client");

        // Assert
        assertThat(result.getCurrentStage()).isEqualTo("UNKNOWN_BUYER_STAGE");
    }

    @Test
    void toResponse_withNullSellerStage_returnsUnknownSellerStage() {
        // Arrange
        Transaction tx = new Transaction();
        tx.setTransactionId(UUID.randomUUID());
        tx.setClientId(UUID.randomUUID());
        tx.setBrokerId(UUID.randomUUID());
        tx.setSide(TransactionSide.SELL_SIDE);
        tx.setSellerStage(null);

        // Act
        TransactionResponseDTO result = EntityDtoUtil.toResponse(tx, "Test Client");

        // Assert
        assertThat(result.getCurrentStage()).isEqualTo("UNKNOWN_SELLER_STAGE");
    }

    // ========== toNewTransaction Tests ==========

    @Test
    void toNewTransaction_mapsRequestDtoToEntity() {
        // Arrange
        UUID clientId = UUID.randomUUID();
        UUID brokerId = UUID.randomUUID();
        UUID txId = UUID.randomUUID();
        TransactionRequestDTO dto = new TransactionRequestDTO();
        dto.setClientId(clientId);
        dto.setBrokerId(brokerId);
        dto.setSide(TransactionSide.BUY_SIDE);

        PropertyAddress address = new PropertyAddress();
        address.setStreet("456 Oak Ave");
        dto.setPropertyAddress(address);

        // Act
        Transaction result = EntityDtoUtil.toNewTransaction(dto, txId);

        // Assert
        assertThat(result.getTransactionId()).isEqualTo(txId);
        assertThat(result.getClientId()).isEqualTo(clientId);
        assertThat(result.getBrokerId()).isEqualTo(brokerId);
        assertThat(result.getSide()).isEqualTo(TransactionSide.BUY_SIDE);
        assertThat(result.getPropertyAddress().getStreet()).isEqualTo("456 Oak Ave");
    }

    // ========== toTimelineDTO Tests ==========

    @Test
    void toTimelineDTO_mapsTimelineEntryToDTO() {
        // Arrange
        UUID brokerUuid = UUID.randomUUID();
        TimelineEntry entry = TimelineEntry.builder()
            .type(TimelineEntryType.NOTE)
            .note("This is the note content")
            .visibleToClient(true)
            .timestamp(java.time.Instant.parse("2025-03-10T09:30:00Z"))
            .actorId(brokerUuid)
            .build();

        // Act
        TimelineEntryDTO result = EntityDtoUtil.toTimelineDTO(entry);

        // Assert
        assertThat(result.getType()).isEqualTo(TimelineEntryType.NOTE);
        assertThat(result.getTitle()).isNull();
        assertThat(result.getNote()).isEqualTo("This is the note content");
        assertThat(result.getVisibleToClient()).isTrue();
        assertThat(result.getOccurredAt()).isEqualTo(java.time.Instant.parse("2025-03-10T09:30:00Z"));
        assertThat(result.getAddedByBrokerId()).isEqualTo(brokerUuid);
    }

    @Test
    void toTimelineDTO_withNullValues_handlesGracefully() {
        // Arrange
        TimelineEntry entry = TimelineEntry.builder()
            .type(TimelineEntryType.NOTE)
            .note(null)
            // .visibleToClient(null) // Adapter si champ existe
            // .occurredAt(null) // Adapter si champ existe
            // .addedByBrokerId(null) // Adapter si champ existe
            .build();

        // Act
        TimelineEntryDTO result = EntityDtoUtil.toTimelineDTO(entry);

        // Assert
        assertThat(result.getType()).isEqualTo(TimelineEntryType.NOTE);
        assertThat(result.getTitle()).isNull();
        assertThat(result.getNote()).isNull();
    }

    // ========== toTimelineDTOs Tests ==========

    @Test
    void toTimelineDTOs_withMultipleEntries_mapsAll() {
        // Arrange
        TimelineEntry entry1 = TimelineEntry.builder()
            .type(TimelineEntryType.NOTE)
            .build();

        TimelineEntry entry2 = TimelineEntry.builder()
            .type(TimelineEntryType.STAGE_CHANGE)
            .build();

        List<TimelineEntry> entries = List.of(entry1, entry2);

        // Act
        List<TimelineEntryDTO> result = EntityDtoUtil.toTimelineDTOs(entries);

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getTitle()).isNull();
        assertThat(result.get(1).getTitle()).isNull();
    }

    @Test
    void toTimelineDTOs_withNullList_returnsEmptyList() {
        // Act
        List<TimelineEntryDTO> result = EntityDtoUtil.toTimelineDTOs(null);

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    void toTimelineDTOs_withEmptyList_returnsEmptyList() {
        // Act
        List<TimelineEntryDTO> result = EntityDtoUtil.toTimelineDTOs(new ArrayList<>());

        // Assert
        assertThat(result).isEmpty();
    }

    // ========== updateBuyerStage Tests ==========

    @Test
    void updateBuyerStage_updatesBuyerStageAndClearsSellerStage() {
        // Arrange
        Transaction tx = new Transaction();
        tx.setBuyerStage(BuyerStage.BUYER_FINANCIAL_PREPARATION);
        tx.setSellerStage(SellerStage.SELLER_INITIAL_CONSULTATION);

        // Act
        EntityDtoUtil.updateBuyerStage(tx, BuyerStage.BUYER_OFFER_AND_NEGOTIATION);

        // Assert
        assertThat(tx.getBuyerStage()).isEqualTo(BuyerStage.BUYER_OFFER_AND_NEGOTIATION);
        assertThat(tx.getSellerStage()).isNull();
    }

    // ========== updateSellerStage Tests ==========

    @Test
    void updateSellerStage_updatesSellerStageAndClearsBuyerStage() {
        // Arrange
        Transaction tx = new Transaction();
        tx.setBuyerStage(BuyerStage.BUYER_FINANCIAL_PREPARATION);
        tx.setSellerStage(SellerStage.SELLER_INITIAL_CONSULTATION);

        // Act
        EntityDtoUtil.updateSellerStage(tx, SellerStage.SELLER_OFFER_AND_NEGOTIATION);

        // Assert
        assertThat(tx.getSellerStage()).isEqualTo(SellerStage.SELLER_OFFER_AND_NEGOTIATION);
        assertThat(tx.getBuyerStage()).isNull();
    }

    // ========== copyAddress Tests ==========

    @Test
    void copyAddress_withValidAddress_createsDeepCopy() {
        // Arrange
        PropertyAddress original = new PropertyAddress();
        original.setStreet("123 Main St");
        original.setCity("Montreal");
        original.setProvince("QC");
        original.setPostalCode("H1A 1A1");

        // Act
        PropertyAddress copy = EntityDtoUtil.copyAddress(original);

        // Assert
        assertThat(copy).isNotSameAs(original);
        assertThat(copy.getStreet()).isEqualTo("123 Main St");
        assertThat(copy.getCity()).isEqualTo("Montreal");
        assertThat(copy.getProvince()).isEqualTo("QC");
        assertThat(copy.getPostalCode()).isEqualTo("H1A 1A1");
    }

    @Test
    void copyAddress_withNullAddress_returnsNull() {
        // Act
        PropertyAddress result = EntityDtoUtil.copyAddress(null);

        // Assert
        assertThat(result).isNull();
    }

    // ========== toResponseStub Tests ==========

    @Test
    void toResponseStub_createsStubWithDefaultValues() {
        UUID txId = UUID.randomUUID();
        UUID clientId = UUID.randomUUID();
        UUID brokerId = UUID.randomUUID();

        // Act
        TransactionResponseDTO result = EntityDtoUtil.toResponseStub(
                txId,
                clientId,
                brokerId
        );

        // Assert
        assertThat(result.getTransactionId()).isEqualTo(txId);
        assertThat(result.getClientId()).isEqualTo(clientId);
        assertThat(result.getBrokerId()).isEqualTo(brokerId);
        assertThat(result.getSide()).isEqualTo(TransactionSide.BUY_SIDE);
        assertThat(result.getStatus()).isEqualTo(TransactionStatus.ACTIVE);
        assertThat(result.getCurrentStage()).isEqualTo("BUYER_FINANCIAL_PREPARATION");
        assertThat(result.getPropertyAddress()).isNull();
        assertThat(result.getOpenedDate()).isNotNull();
    }
}
