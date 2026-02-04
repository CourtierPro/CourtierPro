package com.example.courtierprobackend.transactions.businesslayer;

import com.example.courtierprobackend.audit.timeline_audit.businesslayer.TimelineService;
import com.example.courtierprobackend.common.exceptions.BadRequestException;
import com.example.courtierprobackend.common.exceptions.ForbiddenException;
import com.example.courtierprobackend.common.exceptions.NotFoundException;
import com.example.courtierprobackend.transactions.datalayer.PropertyAddress;
import com.example.courtierprobackend.transactions.datalayer.SearchCriteria;
import com.example.courtierprobackend.transactions.datalayer.Transaction;
import com.example.courtierprobackend.transactions.datalayer.dto.SearchCriteriaRequestDTO;
import com.example.courtierprobackend.transactions.datalayer.dto.SearchCriteriaResponseDTO;
import com.example.courtierprobackend.transactions.datalayer.enums.*;
import com.example.courtierprobackend.transactions.datalayer.repositories.*;
import com.example.courtierprobackend.user.dataaccesslayer.UserAccountRepository;
import com.example.courtierprobackend.email.EmailService;
import com.example.courtierprobackend.notifications.businesslayer.NotificationService;
import com.example.courtierprobackend.infrastructure.storage.ObjectStorageService;
import com.example.courtierprobackend.documents.datalayer.DocumentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for search criteria-related methods in TransactionServiceImpl.
 * Tests CRUD operations, authorization, and validation for buyer transaction search criteria.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SearchCriteriaServiceUnitTest {

    @Mock private TransactionRepository transactionRepository;
    @Mock private PinnedTransactionRepository pinnedTransactionRepository;
    @Mock private UserAccountRepository userAccountRepository;
    @Mock private EmailService emailService;
    @Mock private NotificationService notificationService;
    @Mock private TimelineService timelineService;
    @Mock private TransactionParticipantRepository participantRepository;
    @Mock private PropertyRepository propertyRepository;
    @Mock private OfferRepository offerRepository;
    @Mock private ConditionRepository conditionRepository;
    @Mock private PropertyOfferRepository propertyOfferRepository;
    @Mock private OfferDocumentRepository offerDocumentRepository;
    @Mock private OfferRevisionRepository offerRevisionRepository;
    @Mock private ObjectStorageService objectStorageService;
    @Mock private DocumentRepository documentRequestRepository;
    @Mock private DocumentConditionLinkRepository documentConditionLinkRepository;
    @Mock private SearchCriteriaRepository searchCriteriaRepository;

    private TransactionServiceImpl service;

    private UUID transactionId;
    private UUID brokerId;
    private UUID clientId;
    private UUID searchCriteriaId;
    private Transaction buySideTransaction;
    private Transaction sellSideTransaction;
    private SearchCriteria existingSearchCriteria;
    private SearchCriteriaRequestDTO validRequest;

    @BeforeEach
    void setUp() {
        service = new TransactionServiceImpl(
                transactionRepository, pinnedTransactionRepository, userAccountRepository,
                emailService, notificationService, timelineService, participantRepository,
                propertyRepository, offerRepository, conditionRepository, propertyOfferRepository,
                offerDocumentRepository, offerRevisionRepository, objectStorageService,
                documentRequestRepository, documentConditionLinkRepository, searchCriteriaRepository
        );

        transactionId = UUID.randomUUID();
        brokerId = UUID.randomUUID();
        clientId = UUID.randomUUID();
        searchCriteriaId = UUID.randomUUID();

        buySideTransaction = Transaction.builder()
                .transactionId(transactionId)
                .brokerId(brokerId)
                .clientId(clientId)
                .side(TransactionSide.BUY_SIDE)
                .status(TransactionStatus.ACTIVE)
                .propertyAddress(new PropertyAddress("123 Main St", "Montreal", "QC", "H1A 1A1"))
                .build();

        sellSideTransaction = Transaction.builder()
                .transactionId(transactionId)
                .brokerId(brokerId)
                .clientId(clientId)
                .side(TransactionSide.SELL_SIDE)
                .status(TransactionStatus.ACTIVE)
                .propertyAddress(new PropertyAddress("456 Oak St", "Montreal", "QC", "H2B 2B2"))
                .build();

        existingSearchCriteria = SearchCriteria.builder()
                .id(1L)
                .searchCriteriaId(searchCriteriaId)
                .transactionId(transactionId)
                .minBedrooms(2)
                .minBathrooms(1)
                .minPrice(new BigDecimal("300000"))
                .maxPrice(new BigDecimal("500000"))
                .propertyTypes(new java.util.HashSet<>(Set.of(PropertyType.CONDO, PropertyType.SINGLE_FAMILY_HOME)))
                .regions(new java.util.HashSet<>(Set.of(QuebecRegion.MONTREAL, QuebecRegion.LAVAL)))
                .hasPool(true)
                .createdAt(LocalDateTime.now().minusDays(1))
                .updatedAt(LocalDateTime.now())
                .build();

        validRequest = SearchCriteriaRequestDTO.builder()
                .minBedrooms(2)
                .minBathrooms(1)
                .minPrice(new BigDecimal("300000"))
                .maxPrice(new BigDecimal("500000"))
                .propertyTypes(Set.of(PropertyType.CONDO, PropertyType.SINGLE_FAMILY_HOME))
                .regions(Set.of(QuebecRegion.MONTREAL, QuebecRegion.LAVAL))
                .hasPool(true)
                .build();

        // Default mock for participantRepository to allow access
        lenient().when(participantRepository.findByTransactionId(any())).thenReturn(Collections.emptyList());
    }

    // ==================== getSearchCriteria Tests ====================

    @Nested
    @DisplayName("getSearchCriteria")
    class GetSearchCriteriaTests {

        @Test
        @DisplayName("should return search criteria for broker")
        void getSearchCriteria_asBroker_returnsSearchCriteria() {
            when(transactionRepository.findByTransactionId(transactionId))
                    .thenReturn(Optional.of(buySideTransaction));
            when(searchCriteriaRepository.findByTransactionId(transactionId))
                    .thenReturn(Optional.of(existingSearchCriteria));

            SearchCriteriaResponseDTO result = service.getSearchCriteria(transactionId, brokerId, true);

            assertThat(result).isNotNull();
            assertThat(result.getSearchCriteriaId()).isEqualTo(searchCriteriaId);
            assertThat(result.getMinBedrooms()).isEqualTo(2);
            assertThat(result.getMaxPrice()).isEqualByComparingTo(new BigDecimal("500000"));
            assertThat(result.getPropertyTypes()).containsExactlyInAnyOrder(PropertyType.CONDO, PropertyType.SINGLE_FAMILY_HOME);
            assertThat(result.getRegions()).containsExactlyInAnyOrder(QuebecRegion.MONTREAL, QuebecRegion.LAVAL);
        }

        @Test
        @DisplayName("should return search criteria for client")
        void getSearchCriteria_asClient_returnsSearchCriteria() {
            when(transactionRepository.findByTransactionId(transactionId))
                    .thenReturn(Optional.of(buySideTransaction));
            when(searchCriteriaRepository.findByTransactionId(transactionId))
                    .thenReturn(Optional.of(existingSearchCriteria));

            SearchCriteriaResponseDTO result = service.getSearchCriteria(transactionId, clientId, false);

            assertThat(result).isNotNull();
            assertThat(result.getSearchCriteriaId()).isEqualTo(searchCriteriaId);
        }

        @Test
        @DisplayName("should return null when no search criteria exists")
        void getSearchCriteria_noExistingCriteria_returnsNull() {
            when(transactionRepository.findByTransactionId(transactionId))
                    .thenReturn(Optional.of(buySideTransaction));
            when(searchCriteriaRepository.findByTransactionId(transactionId))
                    .thenReturn(Optional.empty());

            SearchCriteriaResponseDTO result = service.getSearchCriteria(transactionId, brokerId, true);

            assertThat(result).isNull();
        }

        @Test
        @DisplayName("should throw NotFoundException when transaction not found")
        void getSearchCriteria_transactionNotFound_throwsNotFoundException() {
            when(transactionRepository.findByTransactionId(transactionId))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getSearchCriteria(transactionId, brokerId, true))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("Transaction not found");
        }

        @Test
        @DisplayName("should throw BadRequestException for sell-side transaction")
        void getSearchCriteria_sellSideTransaction_throwsBadRequest() {
            when(transactionRepository.findByTransactionId(transactionId))
                    .thenReturn(Optional.of(sellSideTransaction));

            assertThatThrownBy(() -> service.getSearchCriteria(transactionId, brokerId, true))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("buyer-side");
        }

        @Test
        @DisplayName("should throw ForbiddenException for unauthorized user")
        void getSearchCriteria_unauthorizedUser_throwsForbidden() {
            UUID unauthorizedUserId = UUID.randomUUID();
            when(transactionRepository.findByTransactionId(transactionId))
                    .thenReturn(Optional.of(buySideTransaction));

            assertThatThrownBy(() -> service.getSearchCriteria(transactionId, unauthorizedUserId, false))
                    .isInstanceOf(ForbiddenException.class);
        }
    }

    // ==================== createOrUpdateSearchCriteria Tests ====================

    @Nested
    @DisplayName("createOrUpdateSearchCriteria")
    class CreateOrUpdateSearchCriteriaTests {

        @Test
        @DisplayName("should create new search criteria when none exists")
        void createOrUpdate_noExisting_createsNewCriteria() {
            when(transactionRepository.findByTransactionId(transactionId))
                    .thenReturn(Optional.of(buySideTransaction));
            when(searchCriteriaRepository.findByTransactionId(transactionId))
                    .thenReturn(Optional.empty());
            when(searchCriteriaRepository.save(any(SearchCriteria.class)))
                    .thenAnswer(invocation -> {
                        SearchCriteria sc = invocation.getArgument(0);
                        sc.setId(1L);
                        sc.setSearchCriteriaId(UUID.randomUUID());
                        sc.setCreatedAt(LocalDateTime.now());
                        sc.setUpdatedAt(LocalDateTime.now());
                        return sc;
                    });

            SearchCriteriaResponseDTO result = service.createOrUpdateSearchCriteria(
                    transactionId, validRequest, brokerId, true);

            assertThat(result).isNotNull();
            assertThat(result.getMinBedrooms()).isEqualTo(2);
            assertThat(result.getMinPrice()).isEqualByComparingTo(new BigDecimal("300000"));
            assertThat(result.getMaxPrice()).isEqualByComparingTo(new BigDecimal("500000"));
            assertThat(result.getPropertyTypes()).containsExactlyInAnyOrder(PropertyType.CONDO, PropertyType.SINGLE_FAMILY_HOME);
            assertThat(result.getRegions()).containsExactlyInAnyOrder(QuebecRegion.MONTREAL, QuebecRegion.LAVAL);
            assertThat(result.getHasPool()).isTrue();

            verify(searchCriteriaRepository).save(any(SearchCriteria.class));
        }

        @Test
        @DisplayName("should update existing search criteria")
        void createOrUpdate_existingCriteria_updatesCriteria() {
            SearchCriteriaRequestDTO updateRequest = SearchCriteriaRequestDTO.builder()
                    .minBedrooms(3)
                    .minPrice(new BigDecimal("400000"))
                    .maxPrice(new BigDecimal("600000"))
                    .regions(Set.of(QuebecRegion.CAPITALE_NATIONALE))
                    .build();

            when(transactionRepository.findByTransactionId(transactionId))
                    .thenReturn(Optional.of(buySideTransaction));
            when(searchCriteriaRepository.findByTransactionId(transactionId))
                    .thenReturn(Optional.of(existingSearchCriteria));
            when(searchCriteriaRepository.save(any(SearchCriteria.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            SearchCriteriaResponseDTO result = service.createOrUpdateSearchCriteria(
                    transactionId, updateRequest, brokerId, true);

            assertThat(result).isNotNull();
            assertThat(result.getSearchCriteriaId()).isEqualTo(searchCriteriaId);
            assertThat(result.getMinBedrooms()).isEqualTo(3);
            assertThat(result.getMinPrice()).isEqualByComparingTo(new BigDecimal("400000"));
            assertThat(result.getRegions()).containsExactly(QuebecRegion.CAPITALE_NATIONALE);

            verify(searchCriteriaRepository).save(existingSearchCriteria);
        }

        @Test
        @DisplayName("should allow client to create search criteria")
        void createOrUpdate_asClient_succeeds() {
            when(transactionRepository.findByTransactionId(transactionId))
                    .thenReturn(Optional.of(buySideTransaction));
            when(searchCriteriaRepository.findByTransactionId(transactionId))
                    .thenReturn(Optional.empty());
            when(searchCriteriaRepository.save(any(SearchCriteria.class)))
                    .thenAnswer(invocation -> {
                        SearchCriteria sc = invocation.getArgument(0);
                        sc.setId(1L);
                        sc.setSearchCriteriaId(UUID.randomUUID());
                        sc.setCreatedAt(LocalDateTime.now());
                        sc.setUpdatedAt(LocalDateTime.now());
                        return sc;
                    });

            SearchCriteriaResponseDTO result = service.createOrUpdateSearchCriteria(
                    transactionId, validRequest, clientId, false);

            assertThat(result).isNotNull();
            verify(searchCriteriaRepository).save(any(SearchCriteria.class));
        }

        @Test
        @DisplayName("should throw NotFoundException when transaction not found")
        void createOrUpdate_transactionNotFound_throwsNotFoundException() {
            when(transactionRepository.findByTransactionId(transactionId))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.createOrUpdateSearchCriteria(
                    transactionId, validRequest, brokerId, true))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("Transaction not found");
        }

        @Test
        @DisplayName("should throw BadRequestException for sell-side transaction")
        void createOrUpdate_sellSideTransaction_throwsBadRequest() {
            when(transactionRepository.findByTransactionId(transactionId))
                    .thenReturn(Optional.of(sellSideTransaction));

            assertThatThrownBy(() -> service.createOrUpdateSearchCriteria(
                    transactionId, validRequest, brokerId, true))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("buyer-side");
        }

        @Test
        @DisplayName("should throw ForbiddenException for unauthorized user")
        void createOrUpdate_unauthorizedUser_throwsForbidden() {
            UUID unauthorizedUserId = UUID.randomUUID();
            when(transactionRepository.findByTransactionId(transactionId))
                    .thenReturn(Optional.of(buySideTransaction));

            assertThatThrownBy(() -> service.createOrUpdateSearchCriteria(
                    transactionId, validRequest, unauthorizedUserId, false))
                    .isInstanceOf(ForbiddenException.class);
        }

        @Test
        @DisplayName("should handle all optional fields correctly")
        void createOrUpdate_withAllFields_setsAllFields() {
            SearchCriteriaRequestDTO fullRequest = SearchCriteriaRequestDTO.builder()
                    .minBedrooms(2)
                    .minBathrooms(1)
                    .minParkingSpaces(1)
                    .minGarages(0)
                    .hasPool(true)
                    .hasElevator(false)
                    .adaptedForReducedMobility(false)
                    .hasWaterfront(true)
                    .hasAccessToWaterfront(true)
                    .hasNavigableWater(false)
                    .isResort(false)
                    .petsAllowed(true)
                    .smokingAllowed(false)
                    .minLivingArea(new BigDecimal("1500"))
                    .maxLivingArea(new BigDecimal("3000"))
                    .livingAreaUnit(AreaUnit.SQFT)
                    .minYearBuilt(2000)
                    .maxYearBuilt(2024)
                    .minLandArea(new BigDecimal("5000"))
                    .maxLandArea(new BigDecimal("10000"))
                    .landAreaUnit(AreaUnit.SQFT)
                    .newSince(LocalDate.of(2024, 1, 1))
                    .moveInDate(LocalDate.of(2024, 6, 1))
                    .openHousesOnly(true)
                    .repossessionOnly(false)
                    .minPrice(new BigDecimal("300000"))
                    .maxPrice(new BigDecimal("500000"))
                    .propertyTypes(Set.of(PropertyType.CONDO, PropertyType.SINGLE_FAMILY_HOME))
                    .buildingStyles(Set.of(BuildingStyle.NEW_CONSTRUCTION, BuildingStyle.BUNGALOW))
                    .plexTypes(Set.of(PlexType.DUPLEX))
                    .regions(Set.of(QuebecRegion.MONTREAL, QuebecRegion.LAVAL))
                    .build();

            when(transactionRepository.findByTransactionId(transactionId))
                    .thenReturn(Optional.of(buySideTransaction));
            when(searchCriteriaRepository.findByTransactionId(transactionId))
                    .thenReturn(Optional.empty());
            when(searchCriteriaRepository.save(any(SearchCriteria.class)))
                    .thenAnswer(invocation -> {
                        SearchCriteria sc = invocation.getArgument(0);
                        sc.setId(1L);
                        sc.setSearchCriteriaId(UUID.randomUUID());
                        sc.setCreatedAt(LocalDateTime.now());
                        sc.setUpdatedAt(LocalDateTime.now());
                        return sc;
                    });

            SearchCriteriaResponseDTO result = service.createOrUpdateSearchCriteria(
                    transactionId, fullRequest, brokerId, true);

            assertThat(result).isNotNull();
            assertThat(result.getHasPool()).isTrue();
            assertThat(result.getHasWaterfront()).isTrue();
            assertThat(result.getPetsAllowed()).isTrue();
            assertThat(result.getOpenHousesOnly()).isTrue();
            assertThat(result.getMinLivingArea()).isEqualByComparingTo(new BigDecimal("1500"));
            assertThat(result.getMinYearBuilt()).isEqualTo(2000);
            assertThat(result.getBuildingStyles()).containsExactlyInAnyOrder(BuildingStyle.NEW_CONSTRUCTION, BuildingStyle.BUNGALOW);
            assertThat(result.getPlexTypes()).containsExactly(PlexType.DUPLEX);
        }

        @Test
        @DisplayName("should handle empty request (all fields null)")
        void createOrUpdate_emptyRequest_succeeds() {
            SearchCriteriaRequestDTO emptyRequest = SearchCriteriaRequestDTO.builder().build();

            when(transactionRepository.findByTransactionId(transactionId))
                    .thenReturn(Optional.of(buySideTransaction));
            when(searchCriteriaRepository.findByTransactionId(transactionId))
                    .thenReturn(Optional.empty());
            when(searchCriteriaRepository.save(any(SearchCriteria.class)))
                    .thenAnswer(invocation -> {
                        SearchCriteria sc = invocation.getArgument(0);
                        sc.setId(1L);
                        sc.setSearchCriteriaId(UUID.randomUUID());
                        sc.setCreatedAt(LocalDateTime.now());
                        sc.setUpdatedAt(LocalDateTime.now());
                        return sc;
                    });

            SearchCriteriaResponseDTO result = service.createOrUpdateSearchCriteria(
                    transactionId, emptyRequest, brokerId, true);

            assertThat(result).isNotNull();
            assertThat(result.getMinBedrooms()).isNull();
            assertThat(result.getMaxPrice()).isNull();
            assertThat(result.getPropertyTypes()).isEmpty();
        }
    }

    // ==================== deleteSearchCriteria Tests ====================

    @Nested
    @DisplayName("deleteSearchCriteria")
    class DeleteSearchCriteriaTests {

        @Test
        @DisplayName("should delete existing search criteria as broker")
        void delete_asBroker_deletesSearchCriteria() {
            when(transactionRepository.findByTransactionId(transactionId))
                    .thenReturn(Optional.of(buySideTransaction));
            when(searchCriteriaRepository.findByTransactionId(transactionId))
                    .thenReturn(Optional.of(existingSearchCriteria));

            service.deleteSearchCriteria(transactionId, brokerId, true);

            verify(searchCriteriaRepository).delete(existingSearchCriteria);
        }

        @Test
        @DisplayName("should delete existing search criteria as client")
        void delete_asClient_deletesSearchCriteria() {
            when(transactionRepository.findByTransactionId(transactionId))
                    .thenReturn(Optional.of(buySideTransaction));
            when(searchCriteriaRepository.findByTransactionId(transactionId))
                    .thenReturn(Optional.of(existingSearchCriteria));

            service.deleteSearchCriteria(transactionId, clientId, false);

            verify(searchCriteriaRepository).delete(existingSearchCriteria);
        }

        @Test
        @DisplayName("should throw NotFoundException when search criteria not found")
        void delete_noCriteria_throwsNotFoundException() {
            when(transactionRepository.findByTransactionId(transactionId))
                    .thenReturn(Optional.of(buySideTransaction));
            when(searchCriteriaRepository.findByTransactionId(transactionId))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.deleteSearchCriteria(transactionId, brokerId, true))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("Search criteria not found");
        }

        @Test
        @DisplayName("should throw NotFoundException when transaction not found")
        void delete_transactionNotFound_throwsNotFoundException() {
            when(transactionRepository.findByTransactionId(transactionId))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.deleteSearchCriteria(transactionId, brokerId, true))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("Transaction not found");
        }

        @Test
        @DisplayName("should throw BadRequestException for sell-side transaction")
        void delete_sellSideTransaction_throwsBadRequest() {
            when(transactionRepository.findByTransactionId(transactionId))
                    .thenReturn(Optional.of(sellSideTransaction));

            assertThatThrownBy(() -> service.deleteSearchCriteria(transactionId, brokerId, true))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("buyer-side");
        }

        @Test
        @DisplayName("should throw ForbiddenException for unauthorized user")
        void delete_unauthorizedUser_throwsForbidden() {
            UUID unauthorizedUserId = UUID.randomUUID();
            when(transactionRepository.findByTransactionId(transactionId))
                    .thenReturn(Optional.of(buySideTransaction));

            assertThatThrownBy(() -> service.deleteSearchCriteria(transactionId, unauthorizedUserId, false))
                    .isInstanceOf(ForbiddenException.class);
        }
    }
}
