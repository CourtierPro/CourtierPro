package com.example.courtierprobackend.transactions.util;

import com.example.courtierprobackend.transactions.datalayer.PropertyAddress;
import com.example.courtierprobackend.transactions.datalayer.TimelineEntry;
import com.example.courtierprobackend.transactions.datalayer.Transaction;
import com.example.courtierprobackend.transactions.datalayer.dto.TimelineEntryDTO;
import com.example.courtierprobackend.transactions.datalayer.dto.TransactionRequestDTO;
import com.example.courtierprobackend.transactions.datalayer.dto.TransactionResponseDTO;
import com.example.courtierprobackend.transactions.datalayer.enums.*;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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
        Transaction tx = new Transaction();
        tx.setTransactionId("TX-123");
        tx.setClientId("client-1");
        tx.setBrokerId("broker-1");
        tx.setSide(TransactionSide.BUY_SIDE);
        tx.setBuyerStage(BuyerStage.BUYER_SHOP_FOR_PROPERTY);
        tx.setStatus(TransactionStatus.ACTIVE);
        tx.setOpenedAt(LocalDateTime.of(2025, 1, 15, 10, 0));
        
        PropertyAddress address = new PropertyAddress();
        address.setStreet("123 Main St");
        tx.setPropertyAddress(address);

        // Act
        TransactionResponseDTO result = EntityDtoUtil.toResponse(tx, "Test Client");

        // Assert
        assertThat(result.getTransactionId()).isEqualTo("TX-123");
        assertThat(result.getClientId()).isEqualTo("client-1");
        assertThat(result.getBrokerId()).isEqualTo("broker-1");
        assertThat(result.getSide()).isEqualTo(TransactionSide.BUY_SIDE);
        assertThat(result.getCurrentStage()).isEqualTo("BUYER_SHOP_FOR_PROPERTY");
        assertThat(result.getStatus()).isEqualTo(TransactionStatus.ACTIVE);
        assertThat(result.getOpenedDate()).isEqualTo("2025-01-15");
        assertThat(result.getPropertyAddress().getStreet()).isEqualTo("123 Main St");
    }

    @Test
    void toResponse_withSellerSideTransaction_mapsCorrectly() {
        // Arrange
        Transaction tx = new Transaction();
        tx.setTransactionId("TX-456");
        tx.setClientId("client-2");
        tx.setBrokerId("broker-2");
        tx.setSide(TransactionSide.SELL_SIDE);
        tx.setSellerStage(SellerStage.SELLER_LISTING_PUBLISHED);
        tx.setStatus(TransactionStatus.ACTIVE);
        tx.setOpenedAt(LocalDateTime.of(2025, 2, 20, 14, 30));

        // Act
        TransactionResponseDTO result = EntityDtoUtil.toResponse(tx, "Test Client");

        // Assert
        assertThat(result.getCurrentStage()).isEqualTo("SELLER_LISTING_PUBLISHED");
        assertThat(result.getOpenedDate()).isEqualTo("2025-02-20");
    }

    @Test
    void toResponse_withNullOpenedAt_returnsNullOpenedDate() {
        // Arrange
        Transaction tx = new Transaction();
        tx.setTransactionId("TX-789");
        tx.setClientId("client-3");
        tx.setBrokerId("broker-3");
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
        tx.setTransactionId("TX-unknown");
        tx.setClientId("client-4");
        tx.setBrokerId("broker-4");
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
        tx.setTransactionId("TX-unknown-seller");
        tx.setClientId("client-5");
        tx.setBrokerId("broker-5");
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
        TransactionRequestDTO dto = new TransactionRequestDTO();
        dto.setClientId("client-1");
        dto.setBrokerId("broker-1");
        dto.setSide(TransactionSide.BUY_SIDE);
        
        PropertyAddress address = new PropertyAddress();
        address.setStreet("456 Oak Ave");
        dto.setPropertyAddress(address);

        // Act
        Transaction result = EntityDtoUtil.toNewTransaction(dto, "TX-generated");

        // Assert
        assertThat(result.getTransactionId()).isEqualTo("TX-generated");
        assertThat(result.getClientId()).isEqualTo("client-1");
        assertThat(result.getBrokerId()).isEqualTo("broker-1");
        assertThat(result.getSide()).isEqualTo(TransactionSide.BUY_SIDE);
        assertThat(result.getPropertyAddress().getStreet()).isEqualTo("456 Oak Ave");
    }

    // ========== toTimelineDTO Tests ==========

    @Test
    void toTimelineDTO_mapsTimelineEntryToDTO() {
        // Arrange
        TimelineEntry entry = TimelineEntry.builder()
                .type(TimelineEntryType.NOTE)
                .title("Important Note")
                .note("This is the note content")
                .visibleToClient(true)
                .occurredAt(LocalDateTime.of(2025, 3, 10, 9, 30))
                .addedByBrokerId("broker-1")
                .build();

        // Act
        TimelineEntryDTO result = EntityDtoUtil.toTimelineDTO(entry);

        // Assert
        assertThat(result.getType()).isEqualTo(TimelineEntryType.NOTE);
        assertThat(result.getTitle()).isEqualTo("Important Note");
        assertThat(result.getNote()).isEqualTo("This is the note content");
        assertThat(result.getVisibleToClient()).isTrue();
        assertThat(result.getOccurredAt()).isEqualTo(LocalDateTime.of(2025, 3, 10, 9, 30));
        assertThat(result.getAddedByBrokerId()).isEqualTo("broker-1");
    }

    @Test
    void toTimelineDTO_withNullValues_handlesGracefully() {
        // Arrange
        TimelineEntry entry = TimelineEntry.builder()
                .type(TimelineEntryType.NOTE)
                .title(null)
                .note(null)
                .visibleToClient(null)
                .occurredAt(null)
                .addedByBrokerId(null)
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
                .title("Note 1")
                .build();
        
        TimelineEntry entry2 = TimelineEntry.builder()
                .type(TimelineEntryType.STAGE_CHANGE)
                .title("Stage Change")
                .build();

        List<TimelineEntry> entries = List.of(entry1, entry2);

        // Act
        List<TimelineEntryDTO> result = EntityDtoUtil.toTimelineDTOs(entries);

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getTitle()).isEqualTo("Note 1");
        assertThat(result.get(1).getTitle()).isEqualTo("Stage Change");
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
        tx.setBuyerStage(BuyerStage.BUYER_PREQUALIFY_FINANCIALLY);
        tx.setSellerStage(SellerStage.SELLER_INITIAL_CONSULTATION);

        // Act
        EntityDtoUtil.updateBuyerStage(tx, BuyerStage.BUYER_SUBMIT_OFFER);

        // Assert
        assertThat(tx.getBuyerStage()).isEqualTo(BuyerStage.BUYER_SUBMIT_OFFER);
        assertThat(tx.getSellerStage()).isNull();
    }

    // ========== updateSellerStage Tests ==========

    @Test
    void updateSellerStage_updatesSellerStageAndClearsBuyerStage() {
        // Arrange
        Transaction tx = new Transaction();
        tx.setBuyerStage(BuyerStage.BUYER_PREQUALIFY_FINANCIALLY);
        tx.setSellerStage(SellerStage.SELLER_INITIAL_CONSULTATION);

        // Act
        EntityDtoUtil.updateSellerStage(tx, SellerStage.SELLER_ACCEPT_BEST_OFFER);

        // Assert
        assertThat(tx.getSellerStage()).isEqualTo(SellerStage.SELLER_ACCEPT_BEST_OFFER);
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
        // Act
        TransactionResponseDTO result = EntityDtoUtil.toResponseStub(
                "TX-stub",
                "client-stub",
                "broker-stub"
        );

        // Assert
        assertThat(result.getTransactionId()).isEqualTo("TX-stub");
        assertThat(result.getClientId()).isEqualTo("client-stub");
        assertThat(result.getBrokerId()).isEqualTo("broker-stub");
        assertThat(result.getSide()).isEqualTo(TransactionSide.BUY_SIDE);
        assertThat(result.getStatus()).isEqualTo(TransactionStatus.ACTIVE);
        assertThat(result.getCurrentStage()).isEqualTo("BUYER_PREQUALIFY_FINANCIALLY");
        assertThat(result.getPropertyAddress()).isNull();
        assertThat(result.getOpenedDate()).isNotNull();
    }
}
