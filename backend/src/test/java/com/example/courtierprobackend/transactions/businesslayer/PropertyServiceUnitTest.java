package com.example.courtierprobackend.transactions.businesslayer;

import com.example.courtierprobackend.audit.timeline_audit.businesslayer.TimelineService;
import com.example.courtierprobackend.common.exceptions.BadRequestException;
import com.example.courtierprobackend.common.exceptions.ForbiddenException;
import com.example.courtierprobackend.common.exceptions.NotFoundException;
import com.example.courtierprobackend.transactions.datalayer.Property;
import com.example.courtierprobackend.transactions.datalayer.PropertyAddress;
import com.example.courtierprobackend.transactions.datalayer.Transaction;
import com.example.courtierprobackend.transactions.datalayer.dto.PropertyRequestDTO;
import com.example.courtierprobackend.transactions.datalayer.dto.PropertyResponseDTO;
import com.example.courtierprobackend.transactions.datalayer.enums.PropertyOfferStatus;
import com.example.courtierprobackend.transactions.datalayer.enums.TransactionSide;
import com.example.courtierprobackend.transactions.datalayer.enums.TransactionStatus;
import com.example.courtierprobackend.transactions.datalayer.repositories.PropertyRepository;
import com.example.courtierprobackend.transactions.datalayer.repositories.TransactionRepository;
import com.example.courtierprobackend.notifications.businesslayer.NotificationService;
import com.example.courtierprobackend.user.dataaccesslayer.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for property-related methods in TransactionServiceImpl.
 * These tests ensure comprehensive coverage for property CRUD operations,
 * authorization checks, and edge cases.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PropertyServiceUnitTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private PropertyRepository propertyRepository;

    @Mock
    private TimelineService timelineService;

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private TransactionServiceImpl service;

    private UUID transactionId;
    private UUID brokerId;
    private UUID clientId;
    private UUID propertyId;
    private Transaction buySideTransaction;
    private Transaction sellSideTransaction;
    private Property testProperty;
    private PropertyRequestDTO validPropertyRequest;

    @BeforeEach
    void setUp() {
        transactionId = UUID.randomUUID();
        brokerId = UUID.randomUUID();
        clientId = UUID.randomUUID();
        propertyId = UUID.randomUUID();

        buySideTransaction = new Transaction();
        buySideTransaction.setTransactionId(transactionId);
        buySideTransaction.setBrokerId(brokerId);
        buySideTransaction.setClientId(clientId);
        buySideTransaction.setSide(TransactionSide.BUY_SIDE);
        buySideTransaction.setStatus(TransactionStatus.ACTIVE);
        buySideTransaction.setPropertyAddress(new PropertyAddress("123 Main St", "Montreal", "QC", "H1A 1A1"));

        sellSideTransaction = new Transaction();
        sellSideTransaction.setTransactionId(transactionId);
        sellSideTransaction.setBrokerId(brokerId);
        sellSideTransaction.setClientId(clientId);
        sellSideTransaction.setSide(TransactionSide.SELL_SIDE);
        sellSideTransaction.setStatus(TransactionStatus.ACTIVE);
        sellSideTransaction.setPropertyAddress(new PropertyAddress("456 Oak St", "Montreal", "QC", "H2B 2B2"));

        testProperty = Property.builder()
                .id(1L)
                .propertyId(propertyId)
                .transactionId(transactionId)
                .address(new PropertyAddress("789 Pine St", "Laval", "QC", "H3C 3C3"))
                .askingPrice(BigDecimal.valueOf(500000))
                .offerStatus(PropertyOfferStatus.OFFER_TO_BE_MADE)
                .offerAmount(null)
                .centrisNumber("12345678")
                .notes("Test notes")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        validPropertyRequest = PropertyRequestDTO.builder()
                .address(new PropertyAddress("789 Pine St", "Laval", "QC", "H3C 3C3"))
                .askingPrice(BigDecimal.valueOf(500000))
                .offerStatus(PropertyOfferStatus.OFFER_TO_BE_MADE)
                .centrisNumber("12345678")
                .notes("Test notes")
                .build();
    }

    // ==================== getProperties Tests ====================

    @Nested
    @DisplayName("getProperties")
    class GetPropertiesTests {

        @Test
        @DisplayName("should return properties with notes for broker")
        void getProperties_asBroker_returnsPropertiesWithNotes() {
            when(transactionRepository.findByTransactionId(transactionId))
                    .thenReturn(Optional.of(buySideTransaction));
            when(propertyRepository.findByTransactionIdOrderByCreatedAtDesc(transactionId))
                    .thenReturn(List.of(testProperty));

            List<PropertyResponseDTO> result = service.getProperties(transactionId, brokerId, true);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getNotes()).isEqualTo("Test notes");
            assertThat(result.get(0).getPropertyId()).isEqualTo(propertyId);
        }

        @Test
        @DisplayName("should return properties with notes for client")
        void getProperties_asClient_returnsPropertiesWithNotes() {
            when(transactionRepository.findByTransactionId(transactionId))
                    .thenReturn(Optional.of(buySideTransaction));
            when(propertyRepository.findByTransactionIdOrderByCreatedAtDesc(transactionId))
                    .thenReturn(List.of(testProperty));

            List<PropertyResponseDTO> result = service.getProperties(transactionId, clientId, false);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getNotes()).isEqualTo(testProperty.getNotes());
            assertThat(result.get(0).getPropertyId()).isEqualTo(propertyId);
        }

        @Test
        @DisplayName("should return empty list for sell-side transactions")
        void getProperties_sellSideTransaction_returnsEmptyList() {
            when(transactionRepository.findByTransactionId(transactionId))
                    .thenReturn(Optional.of(sellSideTransaction));

            List<PropertyResponseDTO> result = service.getProperties(transactionId, brokerId, true);

            assertThat(result).isEmpty();
            verify(propertyRepository, never()).findByTransactionIdOrderByCreatedAtDesc(any());
        }

        @Test
        @DisplayName("should throw NotFoundException when transaction not found")
        void getProperties_transactionNotFound_throwsNotFoundException() {
            when(transactionRepository.findByTransactionId(transactionId))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getProperties(transactionId, brokerId, true))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("Transaction not found");
        }

        @Test
        @DisplayName("should throw ForbiddenException for unauthorized user")
        void getProperties_unauthorizedUser_throwsForbiddenException() {
            UUID unauthorizedUserId = UUID.randomUUID();
            when(transactionRepository.findByTransactionId(transactionId))
                    .thenReturn(Optional.of(buySideTransaction));

            assertThatThrownBy(() -> service.getProperties(transactionId, unauthorizedUserId, false))
                    .isInstanceOf(ForbiddenException.class);
        }
    }

    // ==================== addProperty Tests ====================

    @Nested
    @DisplayName("addProperty")
    class AddPropertyTests {

        @Test
        @DisplayName("should create property successfully")
        void addProperty_validRequest_createsProperty() {
            when(transactionRepository.findByTransactionId(transactionId))
                    .thenReturn(Optional.of(buySideTransaction));
            when(propertyRepository.save(any(Property.class)))
                    .thenAnswer(invocation -> {
                        Property p = invocation.getArgument(0);
                        p.setId(1L);
                        return p;
                    });

            PropertyResponseDTO result = service.addProperty(transactionId, validPropertyRequest, brokerId);

            assertThat(result).isNotNull();
            assertThat(result.getAddress().getStreet()).isEqualTo("789 Pine St");
            assertThat(result.getAskingPrice()).isEqualByComparingTo(BigDecimal.valueOf(500000));
            assertThat(result.getOfferStatus()).isEqualTo(PropertyOfferStatus.OFFER_TO_BE_MADE);
            assertThat(result.getNotes()).isEqualTo("Test notes");

            verify(propertyRepository).save(any(Property.class));
            verify(timelineService).addEntry(any(), any(), any(), any(), any(), any());
            verify(notificationService).createNotification(
                    eq(clientId.toString()),
                    eq("notifications.propertyAdded.title"),
                    eq("notifications.propertyAdded.message"),
                    any(java.util.Map.class),
                    eq(transactionId.toString()),
                    any()
            );
        }

        @Test
        @DisplayName("should throw BadRequestException for sell-side transaction")
        void addProperty_sellSideTransaction_throwsBadRequestException() {
            when(transactionRepository.findByTransactionId(transactionId))
                    .thenReturn(Optional.of(sellSideTransaction));

            assertThatThrownBy(() -> service.addProperty(transactionId, validPropertyRequest, brokerId))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("buyer-side");
        }

        @Test
        @DisplayName("should throw ForbiddenException for non-broker")
        void addProperty_nonBroker_throwsForbiddenException() {
            UUID differentBrokerId = UUID.randomUUID();
            when(transactionRepository.findByTransactionId(transactionId))
                    .thenReturn(Optional.of(buySideTransaction));

            assertThatThrownBy(() -> service.addProperty(transactionId, validPropertyRequest, differentBrokerId))
                    .isInstanceOf(ForbiddenException.class);
        }

        @Test
        @DisplayName("should throw NotFoundException when transaction not found")
        void addProperty_transactionNotFound_throwsNotFoundException() {
            when(transactionRepository.findByTransactionId(transactionId))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.addProperty(transactionId, validPropertyRequest, brokerId))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("Transaction not found");
        }

        @Test
        @DisplayName("should set default offer status when not provided")
        void addProperty_noOfferStatus_setsDefault() {
            PropertyRequestDTO requestWithoutStatus = PropertyRequestDTO.builder()
                    .address(new PropertyAddress("789 Pine St", "Laval", "QC", "H3C 3C3"))
                    .askingPrice(BigDecimal.valueOf(500000))
                    .offerStatus(null)
                    .build();

            when(transactionRepository.findByTransactionId(transactionId))
                    .thenReturn(Optional.of(buySideTransaction));
            when(propertyRepository.save(any(Property.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            PropertyResponseDTO result = service.addProperty(transactionId, requestWithoutStatus, brokerId);

            assertThat(result.getOfferStatus()).isEqualTo(PropertyOfferStatus.OFFER_TO_BE_MADE);
        }
    }

    // ==================== updateProperty Tests ====================

    @Nested
    @DisplayName("updateProperty")
    class UpdatePropertyTests {

        @Test
        @DisplayName("should update property successfully")
        void updateProperty_validRequest_updatesProperty() {
            PropertyRequestDTO updateRequest = PropertyRequestDTO.builder()
                    .address(new PropertyAddress("Updated Street", "Montreal", "QC", "H4D 4D4"))
                    .askingPrice(BigDecimal.valueOf(550000))
                    .offerStatus(PropertyOfferStatus.OFFER_MADE)
                    .offerAmount(BigDecimal.valueOf(525000))
                    .build();

            when(transactionRepository.findByTransactionId(transactionId))
                    .thenReturn(Optional.of(buySideTransaction));
            when(propertyRepository.findByPropertyId(propertyId))
                    .thenReturn(Optional.of(testProperty));
            when(propertyRepository.save(any(Property.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            PropertyResponseDTO result = service.updateProperty(transactionId, propertyId, updateRequest, brokerId);

            assertThat(result.getAddress().getStreet()).isEqualTo("Updated Street");
            assertThat(result.getAskingPrice()).isEqualByComparingTo(BigDecimal.valueOf(550000));
            assertThat(result.getOfferStatus()).isEqualTo(PropertyOfferStatus.OFFER_MADE);
            assertThat(result.getOfferAmount()).isEqualByComparingTo(BigDecimal.valueOf(525000));

            verify(timelineService).addEntry(any(), any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("should throw NotFoundException when property not found")
        void updateProperty_propertyNotFound_throwsNotFoundException() {
            when(transactionRepository.findByTransactionId(transactionId))
                    .thenReturn(Optional.of(buySideTransaction));
            when(propertyRepository.findByPropertyId(propertyId))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.updateProperty(transactionId, propertyId, validPropertyRequest, brokerId))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("Property not found");
        }

        @Test
        @DisplayName("should throw BadRequestException when property does not belong to transaction")
        void updateProperty_propertyWrongTransaction_throwsBadRequestException() {
            UUID differentTransactionId = UUID.randomUUID();
            testProperty.setTransactionId(differentTransactionId);

            when(transactionRepository.findByTransactionId(transactionId))
                    .thenReturn(Optional.of(buySideTransaction));
            when(propertyRepository.findByPropertyId(propertyId))
                    .thenReturn(Optional.of(testProperty));

            assertThatThrownBy(() -> service.updateProperty(transactionId, propertyId, validPropertyRequest, brokerId))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("does not belong to this transaction");
        }

        @Test
        @DisplayName("should throw ForbiddenException for non-broker")
        void updateProperty_nonBroker_throwsForbiddenException() {
            UUID differentBrokerId = UUID.randomUUID();
            when(transactionRepository.findByTransactionId(transactionId))
                    .thenReturn(Optional.of(buySideTransaction));

            assertThatThrownBy(() -> service.updateProperty(transactionId, propertyId, validPropertyRequest, differentBrokerId))
                    .isInstanceOf(ForbiddenException.class);
        }

        @Test
        @DisplayName("should not create timeline entry when status unchanged")
        void updateProperty_statusUnchanged_noTimelineEntry() {
            PropertyRequestDTO sameStatusRequest = PropertyRequestDTO.builder()
                    .address(new PropertyAddress("Updated Street", "Montreal", "QC", "H4D 4D4"))
                    .askingPrice(BigDecimal.valueOf(550000))
                    .offerStatus(PropertyOfferStatus.OFFER_TO_BE_MADE) // Same as current
                    .build();

            when(transactionRepository.findByTransactionId(transactionId))
                    .thenReturn(Optional.of(buySideTransaction));
            when(propertyRepository.findByPropertyId(propertyId))
                    .thenReturn(Optional.of(testProperty));
            when(propertyRepository.save(any(Property.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            service.updateProperty(transactionId, propertyId, sameStatusRequest, brokerId);

            verify(timelineService, never()).addEntry(any(), any(), any(), any(), any(), any());
        }
    }

    // ==================== removeProperty Tests ====================

    @Nested
    @DisplayName("removeProperty")
    class RemovePropertyTests {

        @Test
        @DisplayName("should remove property successfully")
        void removeProperty_validRequest_removesProperty() {
            when(transactionRepository.findByTransactionId(transactionId))
                    .thenReturn(Optional.of(buySideTransaction));
            when(propertyRepository.findByPropertyId(propertyId))
                    .thenReturn(Optional.of(testProperty));

            service.removeProperty(transactionId, propertyId, brokerId);

            verify(propertyRepository).delete(testProperty);
            verify(timelineService).addEntry(any(), any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("should throw NotFoundException when property not found")
        void removeProperty_propertyNotFound_throwsNotFoundException() {
            when(transactionRepository.findByTransactionId(transactionId))
                    .thenReturn(Optional.of(buySideTransaction));
            when(propertyRepository.findByPropertyId(propertyId))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.removeProperty(transactionId, propertyId, brokerId))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("Property not found");
        }

        @Test
        @DisplayName("should throw BadRequestException when property does not belong to transaction")
        void removeProperty_propertyWrongTransaction_throwsBadRequestException() {
            UUID differentTransactionId = UUID.randomUUID();
            testProperty.setTransactionId(differentTransactionId);

            when(transactionRepository.findByTransactionId(transactionId))
                    .thenReturn(Optional.of(buySideTransaction));
            when(propertyRepository.findByPropertyId(propertyId))
                    .thenReturn(Optional.of(testProperty));

            assertThatThrownBy(() -> service.removeProperty(transactionId, propertyId, brokerId))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("does not belong to this transaction");
        }

        @Test
        @DisplayName("should throw ForbiddenException for non-broker")
        void removeProperty_nonBroker_throwsForbiddenException() {
            UUID differentBrokerId = UUID.randomUUID();
            when(transactionRepository.findByTransactionId(transactionId))
                    .thenReturn(Optional.of(buySideTransaction));

            assertThatThrownBy(() -> service.removeProperty(transactionId, propertyId, differentBrokerId))
                    .isInstanceOf(ForbiddenException.class);
        }
    }

    // ==================== getPropertyById Tests ====================

    @Nested
    @DisplayName("getPropertyById")
    class GetPropertyByIdTests {

        @Test
        @DisplayName("should return property with notes for broker")
        void getPropertyById_asBroker_returnsPropertyWithNotes() {
            when(propertyRepository.findByPropertyId(propertyId))
                    .thenReturn(Optional.of(testProperty));
            when(transactionRepository.findByTransactionId(transactionId))
                    .thenReturn(Optional.of(buySideTransaction));

            PropertyResponseDTO result = service.getPropertyById(propertyId, brokerId, true);

            assertThat(result).isNotNull();
            assertThat(result.getNotes()).isEqualTo("Test notes");
        }

        @Test
        @DisplayName("should return property with notes for client")
        void getPropertyById_asClient_returnsPropertyWithNotes() {
            when(propertyRepository.findByPropertyId(propertyId))
                    .thenReturn(Optional.of(testProperty));
            when(transactionRepository.findByTransactionId(transactionId))
                    .thenReturn(Optional.of(buySideTransaction));

            PropertyResponseDTO result = service.getPropertyById(propertyId, clientId, false);

            assertThat(result).isNotNull();
            assertThat(result.getNotes()).isEqualTo(testProperty.getNotes());
        }

        @Test
        @DisplayName("should throw NotFoundException when property not found")
        void getPropertyById_propertyNotFound_throwsNotFoundException() {
            when(propertyRepository.findByPropertyId(propertyId))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getPropertyById(propertyId, brokerId, true))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("Property not found");
        }
    }

    // ==================== setActiveProperty Tests ====================

    @Nested
    @DisplayName("setActiveProperty")
    class SetActivePropertyTests {

        @Test
        @DisplayName("should set active property successfully")
        void setActiveProperty_validRequest_setsActiveProperty() {
            when(transactionRepository.findByTransactionId(transactionId))
                    .thenReturn(Optional.of(buySideTransaction));
            when(propertyRepository.findByPropertyId(propertyId))
                    .thenReturn(Optional.of(testProperty));
            when(transactionRepository.save(any(Transaction.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            service.setActiveProperty(transactionId, propertyId, brokerId);

            ArgumentCaptor<Transaction> txCaptor = ArgumentCaptor.forClass(Transaction.class);
            verify(transactionRepository).save(txCaptor.capture());

            assertThat(txCaptor.getValue().getPropertyAddress().getStreet())
                    .isEqualTo("789 Pine St");
        }

        @Test
        @DisplayName("should throw NotFoundException when transaction not found")
        void setActiveProperty_transactionNotFound_throwsNotFoundException() {
            when(transactionRepository.findByTransactionId(transactionId))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.setActiveProperty(transactionId, propertyId, brokerId))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("Transaction not found");
        }

        @Test
        @DisplayName("should throw NotFoundException when property not found")
        void setActiveProperty_propertyNotFound_throwsNotFoundException() {
            when(transactionRepository.findByTransactionId(transactionId))
                    .thenReturn(Optional.of(buySideTransaction));
            when(propertyRepository.findByPropertyId(propertyId))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.setActiveProperty(transactionId, propertyId, brokerId))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("Property not found");
        }

        @Test
        @DisplayName("should throw BadRequestException when property does not belong to transaction")
        void setActiveProperty_propertyWrongTransaction_throwsBadRequestException() {
            UUID differentTransactionId = UUID.randomUUID();
            testProperty.setTransactionId(differentTransactionId);

            when(transactionRepository.findByTransactionId(transactionId))
                    .thenReturn(Optional.of(buySideTransaction));
            when(propertyRepository.findByPropertyId(propertyId))
                    .thenReturn(Optional.of(testProperty));

            assertThatThrownBy(() -> service.setActiveProperty(transactionId, propertyId, brokerId))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("does not belong to this transaction");
        }

        @Test
        @DisplayName("should throw BadRequestException when property has no address")
        void setActiveProperty_noAddress_throwsBadRequestException() {
            testProperty.setAddress(null);

            when(transactionRepository.findByTransactionId(transactionId))
                    .thenReturn(Optional.of(buySideTransaction));
            when(propertyRepository.findByPropertyId(propertyId))
                    .thenReturn(Optional.of(testProperty));

            assertThatThrownBy(() -> service.setActiveProperty(transactionId, propertyId, brokerId))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("must have an address");
        }

        @Test
        @DisplayName("should throw ForbiddenException for non-broker")
        void setActiveProperty_nonBroker_throwsForbiddenException() {
            UUID differentBrokerId = UUID.randomUUID();
            when(transactionRepository.findByTransactionId(transactionId))
                    .thenReturn(Optional.of(buySideTransaction));

            assertThatThrownBy(() -> service.setActiveProperty(transactionId, propertyId, differentBrokerId))
                    .isInstanceOf(ForbiddenException.class);
        }

        @Test
        @DisplayName("should clear active property when propertyId is null")
        void setActiveProperty_nullPropertyId_clearsActiveProperty() {
            UUID transactionId = UUID.randomUUID();

            Transaction tx = new Transaction();
            tx.setTransactionId(transactionId);
            tx.setBrokerId(brokerId);
            tx.setSide(TransactionSide.BUY_SIDE);
            tx.setPropertyAddress(new PropertyAddress("123 Main St", "Montreal", "QC", "H1A 1A1"));
            tx.setStatus(TransactionStatus.ACTIVE);

            when(transactionRepository.findByTransactionId(transactionId))
                    .thenReturn(Optional.of(tx));
            when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

            // Act - pass null propertyId
            service.setActiveProperty(transactionId, null, brokerId);

            // Assert
            ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
            verify(transactionRepository).save(captor.capture());
            assertThat(captor.getValue().getPropertyAddress()).isNull();
        }
    }
}
