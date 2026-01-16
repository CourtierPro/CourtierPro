package com.example.courtierprobackend.transactions.presentationlayer;

import com.example.courtierprobackend.security.UserContextFilter;
import com.example.courtierprobackend.transactions.businesslayer.TransactionService;
import com.example.courtierprobackend.transactions.datalayer.dto.PropertyAddressDTO;
import com.example.courtierprobackend.transactions.datalayer.dto.PropertyResponseDTO;
import com.example.courtierprobackend.transactions.datalayer.dto.ClientOfferDecisionDTO;
import com.example.courtierprobackend.transactions.datalayer.dto.OfferResponseDTO;
import com.example.courtierprobackend.transactions.datalayer.dto.TransactionResponseDTO;
import com.example.courtierprobackend.transactions.datalayer.enums.ClientOfferDecision;
import com.example.courtierprobackend.transactions.datalayer.enums.ReceivedOfferStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ClientTransactionController.
 * Tests client-facing transaction endpoints and authorization logic.
 * Updated to use HttpServletRequest with internal UUID from UserContextFilter.
 */
@ExtendWith(MockitoExtension.class)
class ClientTransactionControllerTest {

    @Test
    void getTransactionOffers_NotUuid_AccountMatchesInternalId_Succeeds() {
        String notUuid = "not-a-uuid";
        UUID internalId = UUID.randomUUID();
        MockHttpServletRequest request = createRequestWithInternalId(internalId);
        com.example.courtierprobackend.user.dataaccesslayer.UserAccount account = mock(com.example.courtierprobackend.user.dataaccesslayer.UserAccount.class);
        when(account.getId()).thenReturn(internalId);
        when(userAccountRepository.findByAuth0UserId(notUuid)).thenReturn(java.util.Optional.of(account));
        com.example.courtierprobackend.transactions.datalayer.dto.OfferResponseDTO offer = com.example.courtierprobackend.transactions.datalayer.dto.OfferResponseDTO.builder()
                .offerId(UUID.randomUUID())
                .transactionId(UUID.randomUUID())
                .build();
        when(transactionService.getOffers(any(), eq(internalId), eq(false))).thenReturn(List.of(offer));

        ResponseEntity<List<com.example.courtierprobackend.transactions.datalayer.dto.OfferResponseDTO>> response = controller.getTransactionOffers(notUuid, offer.getTransactionId(), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().get(0).getOfferId()).isEqualTo(offer.getOfferId());
    }

    @Test
    void getOfferById_NotUuid_AccountMatchesInternalId_Succeeds() {
        String notUuid = "not-a-uuid";
        UUID internalId = UUID.randomUUID();
        MockHttpServletRequest request = createRequestWithInternalId(internalId);
        com.example.courtierprobackend.user.dataaccesslayer.UserAccount account = mock(com.example.courtierprobackend.user.dataaccesslayer.UserAccount.class);
        when(account.getId()).thenReturn(internalId);
        when(userAccountRepository.findByAuth0UserId(notUuid)).thenReturn(java.util.Optional.of(account));
        com.example.courtierprobackend.transactions.datalayer.dto.OfferResponseDTO offer = com.example.courtierprobackend.transactions.datalayer.dto.OfferResponseDTO.builder()
                .offerId(UUID.randomUUID())
                .transactionId(UUID.randomUUID())
                .build();
        when(transactionService.getOfferById(eq(offer.getOfferId()), eq(internalId), eq(false))).thenReturn(offer);

        ResponseEntity<com.example.courtierprobackend.transactions.datalayer.dto.OfferResponseDTO> response = controller.getOfferById(notUuid, offer.getTransactionId(), offer.getOfferId(), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getOfferId()).isEqualTo(offer.getOfferId());
    }

    @Test
    void getTransactionOffers_WithValidRequest_ReturnsOffers() {
        UUID clientId = UUID.randomUUID();
        UUID transactionId = UUID.randomUUID();
        MockHttpServletRequest request = createRequestWithInternalId(clientId);
        com.example.courtierprobackend.transactions.datalayer.dto.OfferResponseDTO offer = com.example.courtierprobackend.transactions.datalayer.dto.OfferResponseDTO.builder()
                .offerId(UUID.randomUUID())
                .transactionId(transactionId)
                .build();
        when(transactionService.getOffers(eq(transactionId), eq(clientId), eq(false))).thenReturn(List.of(offer));

        ResponseEntity<List<com.example.courtierprobackend.transactions.datalayer.dto.OfferResponseDTO>> response = controller.getTransactionOffers(clientId.toString(), transactionId, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().get(0).getOfferId()).isEqualTo(offer.getOfferId());
        verify(transactionService).getOffers(transactionId, clientId, false);
    }

    @Test
    void getOfferById_WithValidRequest_ReturnsOffer() {
        UUID clientId = UUID.randomUUID();
        UUID transactionId = UUID.randomUUID();
        UUID offerId = UUID.randomUUID();
        MockHttpServletRequest request = createRequestWithInternalId(clientId);
        com.example.courtierprobackend.transactions.datalayer.dto.OfferResponseDTO offer = com.example.courtierprobackend.transactions.datalayer.dto.OfferResponseDTO.builder()
                .offerId(offerId)
                .transactionId(transactionId)
                .build();
        when(transactionService.getOfferById(eq(offerId), eq(clientId), eq(false))).thenReturn(offer);

        ResponseEntity<com.example.courtierprobackend.transactions.datalayer.dto.OfferResponseDTO> response = controller.getOfferById(clientId.toString(), transactionId, offerId, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getOfferId()).isEqualTo(offerId);
        verify(transactionService).getOfferById(offerId, clientId, false);
    }

        @Test
        void getTransactionOffers_NotUuid_NotFound_ThrowsForbidden() {
            String notUuid = "not-a-uuid";
            MockHttpServletRequest request = createRequestWithInternalId(UUID.randomUUID());
            when(userAccountRepository.findByAuth0UserId(notUuid)).thenReturn(java.util.Optional.empty());
            assertThatThrownBy(() -> controller.getTransactionOffers(notUuid, UUID.randomUUID(), request))
                .isInstanceOf(com.example.courtierprobackend.common.exceptions.ForbiddenException.class)
                .hasMessageContaining("Invalid client ID: not found");
        }

        @Test
        void getTransactionOffers_NotUuid_InternalIdMismatch_ThrowsForbidden() {
            String notUuid = "not-a-uuid";
            UUID internalId = UUID.randomUUID();
            MockHttpServletRequest request = createRequestWithInternalId(internalId);
            com.example.courtierprobackend.user.dataaccesslayer.UserAccount account = mock(com.example.courtierprobackend.user.dataaccesslayer.UserAccount.class);
            when(account.getId()).thenReturn(UUID.randomUUID()); // mismatch
            when(userAccountRepository.findByAuth0UserId(notUuid)).thenReturn(java.util.Optional.of(account));
            assertThatThrownBy(() -> controller.getTransactionOffers(notUuid, UUID.randomUUID(), request))
                .isInstanceOf(com.example.courtierprobackend.common.exceptions.ForbiddenException.class)
                .hasMessageContaining("only access your own transactions");
        }

        @Test
        void getOfferById_NotUuid_NotFound_ThrowsForbidden() {
            String notUuid = "not-a-uuid";
            MockHttpServletRequest request = createRequestWithInternalId(UUID.randomUUID());
            when(userAccountRepository.findByAuth0UserId(notUuid)).thenReturn(java.util.Optional.empty());
            assertThatThrownBy(() -> controller.getOfferById(notUuid, UUID.randomUUID(), UUID.randomUUID(), request))
                .isInstanceOf(com.example.courtierprobackend.common.exceptions.ForbiddenException.class)
                .hasMessageContaining("Invalid client ID: not found");
        }

        @Test
        void getOfferById_NotUuid_InternalIdMismatch_ThrowsForbidden() {
            String notUuid = "not-a-uuid";
            UUID internalId = UUID.randomUUID();
            MockHttpServletRequest request = createRequestWithInternalId(internalId);
            com.example.courtierprobackend.user.dataaccesslayer.UserAccount account = mock(com.example.courtierprobackend.user.dataaccesslayer.UserAccount.class);
            when(account.getId()).thenReturn(UUID.randomUUID()); // mismatch
            when(userAccountRepository.findByAuth0UserId(notUuid)).thenReturn(java.util.Optional.of(account));
            assertThatThrownBy(() -> controller.getOfferById(notUuid, UUID.randomUUID(), UUID.randomUUID(), request))
                .isInstanceOf(com.example.courtierprobackend.common.exceptions.ForbiddenException.class)
                .hasMessageContaining("only access your own transactions");
        }
    // ========== getTransactionProperties & getPropertyById Tests ========== 

    @Test
    void getTransactionProperties_WithValidRequest_ReturnsProperties() {
        UUID clientId = UUID.randomUUID();
        UUID transactionId = UUID.randomUUID();
        MockHttpServletRequest request = createRequestWithInternalId(clientId);
        PropertyResponseDTO property = PropertyResponseDTO.builder()
                .propertyId(UUID.randomUUID())
                .transactionId(transactionId)
                .address(PropertyAddressDTO.builder().street("123 Main St").city("Testville").province("QC").postalCode("A1A1A1").build())
                .build();
        when(transactionService.getProperties(eq(transactionId), eq(clientId), eq(false))).thenReturn(List.of(property));

        ResponseEntity<List<PropertyResponseDTO>> response = controller.getTransactionProperties(clientId.toString(), transactionId, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().get(0).getPropertyId()).isEqualTo(property.getPropertyId());
        verify(transactionService).getProperties(transactionId, clientId, false);
    }

    @Test
    void getPropertyById_WithValidRequest_ReturnsProperty() {
        UUID clientId = UUID.randomUUID();
        UUID transactionId = UUID.randomUUID();
        UUID propertyId = UUID.randomUUID();
        MockHttpServletRequest request = createRequestWithInternalId(clientId);
        PropertyResponseDTO property = PropertyResponseDTO.builder()
                .propertyId(propertyId)
                .transactionId(transactionId)
                .address(PropertyAddressDTO.builder().street("123 Main St").city("Testville").province("QC").postalCode("A1A1A1").build())
                .build();
        when(transactionService.getPropertyById(eq(propertyId), eq(clientId), eq(false))).thenReturn(property);

        ResponseEntity<PropertyResponseDTO> response = controller.getPropertyById(clientId.toString(), transactionId, propertyId, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getPropertyId()).isEqualTo(propertyId);
        verify(transactionService).getPropertyById(propertyId, clientId, false);
    }

    @Mock
    private TransactionService transactionService;

    @Mock
    private com.example.courtierprobackend.user.dataaccesslayer.UserAccountRepository userAccountRepository;

    private ClientTransactionController controller;

    @BeforeEach
    void setUp() {
        controller = new ClientTransactionController(transactionService, userAccountRepository);
    }

    // ========== getClientTransactions Tests ==========

    @Test
    void getClientTransactions_WithValidClient_ReturnsTransactions() {
        // Arrange
        UUID internalId = UUID.randomUUID();
        MockHttpServletRequest request = createRequestWithInternalId(internalId);
        String clientId = internalId.toString();
        UUID txId = UUID.randomUUID();
        TransactionResponseDTO transaction = TransactionResponseDTO.builder()
                .transactionId(txId)
                .clientId(internalId)
                .build();
        when(transactionService.getClientTransactions(internalId)).thenReturn(List.of(transaction));

        // Act
        ResponseEntity<List<TransactionResponseDTO>> response = controller.getClientTransactions(clientId, request);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().get(0).getTransactionId()).isEqualTo(txId);
        verify(transactionService).getClientTransactions(internalId);
    }

    @Test
    void getClientTransactions_WithMismatchedClientId_ThrowsForbidden() {
        // Arrange
        UUID internalId = UUID.randomUUID();
        String otherClientId = UUID.randomUUID().toString();
        MockHttpServletRequest request = createRequestWithInternalId(internalId);

        // Act & Assert
        assertThatThrownBy(() -> controller.getClientTransactions(otherClientId, request))
                .isInstanceOf(com.example.courtierprobackend.common.exceptions.ForbiddenException.class)
                .hasMessageContaining("only access your own transactions");
        
        verifyNoInteractions(transactionService);
    }

    @Test
    void getClientTransactions_WithNullRequest_ThrowsForbidden() {
        // Arrange
        String clientId = UUID.randomUUID().toString();

        // Act & Assert
        assertThatThrownBy(() -> controller.getClientTransactions(clientId, null))
                .isInstanceOf(com.example.courtierprobackend.common.exceptions.ForbiddenException.class)
                .hasMessageContaining("Unable to resolve user id");
        
        verifyNoInteractions(transactionService);
    }

    @Test
    void getClientTransactions_WithNoInternalIdInRequest_ThrowsForbidden() {
        // Arrange
        String clientId = UUID.randomUUID().toString();
        MockHttpServletRequest request = new MockHttpServletRequest();
        // No internal ID set in request attributes

        // Act & Assert
        assertThatThrownBy(() -> controller.getClientTransactions(clientId, request))
                .isInstanceOf(com.example.courtierprobackend.common.exceptions.ForbiddenException.class)
                .hasMessageContaining("Unable to resolve user id");
        
        verifyNoInteractions(transactionService);
    }

    @Test
    void getClientTransactions_WithNoTransactions_ReturnsEmptyList() {
        // Arrange
        UUID internalId = UUID.randomUUID();
        MockHttpServletRequest request = createRequestWithInternalId(internalId);
        String clientId = internalId.toString();
        when(transactionService.getClientTransactions(internalId)).thenReturn(List.of());

        // Act
        ResponseEntity<List<TransactionResponseDTO>> response = controller.getClientTransactions(clientId, request);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEmpty();
    }

    @Test
    void getClientTransactions_WithMultipleTransactions_ReturnsAll() {
        // Arrange
        UUID internalId = UUID.randomUUID();
        MockHttpServletRequest request = createRequestWithInternalId(internalId);
        String clientId = internalId.toString();
        TransactionResponseDTO tx1 = TransactionResponseDTO.builder().transactionId(UUID.randomUUID()).clientId(internalId).build();
        TransactionResponseDTO tx2 = TransactionResponseDTO.builder().transactionId(UUID.randomUUID()).clientId(internalId).build();
        TransactionResponseDTO tx3 = TransactionResponseDTO.builder().transactionId(UUID.randomUUID()).clientId(internalId).build();
        when(transactionService.getClientTransactions(internalId)).thenReturn(List.of(tx1, tx2, tx3));

        // Act
        ResponseEntity<List<TransactionResponseDTO>> response = controller.getClientTransactions(clientId, request);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(3);
    }

    // ========== submitOfferDecision Tests ==========

    @Nested
    @DisplayName("submitOfferDecision")
    class SubmitOfferDecisionTests {

        @Test
        @DisplayName("should submit offer decision successfully")
        void submitOfferDecision_WithValidData_ReturnsUpdatedOffer() {
            // Arrange
            UUID internalId = UUID.randomUUID();
            UUID transactionId = UUID.randomUUID();
            UUID offerId = UUID.randomUUID();
            MockHttpServletRequest request = createRequestWithInternalId(internalId);

            ClientOfferDecisionDTO decisionDTO = ClientOfferDecisionDTO.builder()
                    .decision(ClientOfferDecision.ACCEPT)
                    .notes("I agree to this offer")
                    .build();

            OfferResponseDTO expectedResponse = OfferResponseDTO.builder()
                    .offerId(offerId)
                    .transactionId(transactionId)
                    .buyerName("John Doe")
                    .offerAmount(BigDecimal.valueOf(500000))
                    .status(ReceivedOfferStatus.PENDING)
                    .clientDecision(ClientOfferDecision.ACCEPT)
                    .clientDecisionAt(LocalDateTime.now())
                    .clientDecisionNotes("I agree to this offer")
                    .build();

            when(transactionService.submitClientOfferDecision(eq(offerId), any(ClientOfferDecisionDTO.class), eq(internalId)))
                    .thenReturn(expectedResponse);

            // Act
            ResponseEntity<OfferResponseDTO> response = controller.submitOfferDecision(
                    internalId.toString(), transactionId, offerId, decisionDTO, request);

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getClientDecision()).isEqualTo(ClientOfferDecision.ACCEPT);
            assertThat(response.getBody().getClientDecisionNotes()).isEqualTo("I agree to this offer");
            verify(transactionService).submitClientOfferDecision(eq(offerId), any(ClientOfferDecisionDTO.class), eq(internalId));
        }

        @Test
        @DisplayName("should submit decline decision successfully")
        void submitOfferDecision_DeclineDecision_ReturnsUpdatedOffer() {
            // Arrange
            UUID internalId = UUID.randomUUID();
            UUID transactionId = UUID.randomUUID();
            UUID offerId = UUID.randomUUID();
            MockHttpServletRequest request = createRequestWithInternalId(internalId);

            ClientOfferDecisionDTO decisionDTO = ClientOfferDecisionDTO.builder()
                    .decision(ClientOfferDecision.DECLINE)
                    .notes("Price is too low")
                    .build();

            OfferResponseDTO expectedResponse = OfferResponseDTO.builder()
                    .offerId(offerId)
                    .clientDecision(ClientOfferDecision.DECLINE)
                    .clientDecisionNotes("Price is too low")
                    .build();

            when(transactionService.submitClientOfferDecision(eq(offerId), any(ClientOfferDecisionDTO.class), eq(internalId)))
                    .thenReturn(expectedResponse);

            // Act
            ResponseEntity<OfferResponseDTO> response = controller.submitOfferDecision(
                    internalId.toString(), transactionId, offerId, decisionDTO, request);

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().getClientDecision()).isEqualTo(ClientOfferDecision.DECLINE);
        }

        @Test
        @DisplayName("should submit counter decision successfully")
        void submitOfferDecision_CounterDecision_ReturnsUpdatedOffer() {
            // Arrange
            UUID internalId = UUID.randomUUID();
            UUID transactionId = UUID.randomUUID();
            UUID offerId = UUID.randomUUID();
            MockHttpServletRequest request = createRequestWithInternalId(internalId);

            ClientOfferDecisionDTO decisionDTO = ClientOfferDecisionDTO.builder()
                    .decision(ClientOfferDecision.COUNTER)
                    .notes("Would accept at $550,000")
                    .build();

            OfferResponseDTO expectedResponse = OfferResponseDTO.builder()
                    .offerId(offerId)
                    .clientDecision(ClientOfferDecision.COUNTER)
                    .clientDecisionNotes("Would accept at $550,000")
                    .build();

            when(transactionService.submitClientOfferDecision(eq(offerId), any(ClientOfferDecisionDTO.class), eq(internalId)))
                    .thenReturn(expectedResponse);

            // Act
            ResponseEntity<OfferResponseDTO> response = controller.submitOfferDecision(
                    internalId.toString(), transactionId, offerId, decisionDTO, request);

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().getClientDecision()).isEqualTo(ClientOfferDecision.COUNTER);
        }

        @Test
        @DisplayName("should throw ForbiddenException when client ID doesn't match")
        void submitOfferDecision_MismatchedClientId_ThrowsForbidden() {
            // Arrange
            UUID internalId = UUID.randomUUID();
            UUID differentClientId = UUID.randomUUID();
            UUID transactionId = UUID.randomUUID();
            UUID offerId = UUID.randomUUID();
            MockHttpServletRequest request = createRequestWithInternalId(internalId);

            ClientOfferDecisionDTO decisionDTO = ClientOfferDecisionDTO.builder()
                    .decision(ClientOfferDecision.ACCEPT)
                    .build();

            // Act & Assert
            assertThatThrownBy(() -> controller.submitOfferDecision(
                    differentClientId.toString(), transactionId, offerId, decisionDTO, request))
                    .isInstanceOf(com.example.courtierprobackend.common.exceptions.ForbiddenException.class)
                    .hasMessageContaining("only access your own transactions");

            verifyNoInteractions(transactionService);
        }

        @Test
        @DisplayName("should throw ForbiddenException when no internal ID in request")
        void submitOfferDecision_NoInternalId_ThrowsForbidden() {
            // Arrange
            UUID clientId = UUID.randomUUID();
            UUID transactionId = UUID.randomUUID();
            UUID offerId = UUID.randomUUID();
            MockHttpServletRequest request = new MockHttpServletRequest(); // No internal ID

            ClientOfferDecisionDTO decisionDTO = ClientOfferDecisionDTO.builder()
                    .decision(ClientOfferDecision.ACCEPT)
                    .build();

            // Act & Assert
            assertThatThrownBy(() -> controller.submitOfferDecision(
                    clientId.toString(), transactionId, offerId, decisionDTO, request))
                    .isInstanceOf(com.example.courtierprobackend.common.exceptions.ForbiddenException.class)
                    .hasMessageContaining("Unable to resolve user id");

            verifyNoInteractions(transactionService);
        }

        @Test
        @DisplayName("should submit decision without notes")
        void submitOfferDecision_NoNotes_ReturnsUpdatedOffer() {
            // Arrange
            UUID internalId = UUID.randomUUID();
            UUID transactionId = UUID.randomUUID();
            UUID offerId = UUID.randomUUID();
            MockHttpServletRequest request = createRequestWithInternalId(internalId);

            ClientOfferDecisionDTO decisionDTO = ClientOfferDecisionDTO.builder()
                    .decision(ClientOfferDecision.ACCEPT)
                    .build();

            OfferResponseDTO expectedResponse = OfferResponseDTO.builder()
                    .offerId(offerId)
                    .clientDecision(ClientOfferDecision.ACCEPT)
                    .clientDecisionNotes(null)
                    .build();

            when(transactionService.submitClientOfferDecision(eq(offerId), any(ClientOfferDecisionDTO.class), eq(internalId)))
                    .thenReturn(expectedResponse);

            // Act
            ResponseEntity<OfferResponseDTO> response = controller.submitOfferDecision(
                    internalId.toString(), transactionId, offerId, decisionDTO, request);

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().getClientDecision()).isEqualTo(ClientOfferDecision.ACCEPT);
            assertThat(response.getBody().getClientDecisionNotes()).isNull();
        }
    }

    // ========== Helper Methods ==========

    private MockHttpServletRequest createRequestWithInternalId(UUID internalId) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(UserContextFilter.INTERNAL_USER_ID_ATTR, internalId);
        return request;
    }

    // ========== Property Offer Tests ==========

    @Test
    void getPropertyOffers_WithValidRequest_ReturnsOffers() {
        UUID clientId = UUID.randomUUID();
        UUID transactionId = UUID.randomUUID();
        UUID propertyId = UUID.randomUUID();
        MockHttpServletRequest request = createRequestWithInternalId(clientId);

        com.example.courtierprobackend.transactions.datalayer.dto.PropertyOfferResponseDTO offer = 
            com.example.courtierprobackend.transactions.datalayer.dto.PropertyOfferResponseDTO.builder()
                .propertyOfferId(UUID.randomUUID())
                .propertyId(propertyId)
                .offerAmount(BigDecimal.valueOf(450000))
                .build();
        when(transactionService.getPropertyOffers(eq(propertyId), eq(clientId), eq(false))).thenReturn(List.of(offer));

        ResponseEntity<List<com.example.courtierprobackend.transactions.datalayer.dto.PropertyOfferResponseDTO>> response = 
            controller.getPropertyOffers(clientId.toString(), transactionId, propertyId, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().get(0).getPropertyOfferId()).isEqualTo(offer.getPropertyOfferId());
        verify(transactionService).getPropertyOffers(propertyId, clientId, false);
    }

    @Test
    void getPropertyOffers_WithMismatchedClientId_ThrowsForbidden() {
        UUID internalId = UUID.randomUUID();
        UUID differentClientId = UUID.randomUUID();
        UUID transactionId = UUID.randomUUID();
        UUID propertyId = UUID.randomUUID();
        MockHttpServletRequest request = createRequestWithInternalId(internalId);

        assertThatThrownBy(() -> controller.getPropertyOffers(differentClientId.toString(), transactionId, propertyId, request))
            .isInstanceOf(com.example.courtierprobackend.common.exceptions.ForbiddenException.class)
            .hasMessageContaining("only access your own transactions");

        verifyNoInteractions(transactionService);
    }

    // ========== Property Offer Document Tests ==========

    @Test
    void getPropertyOfferDocuments_WithValidRequest_ReturnsDocuments() {
        UUID clientId = UUID.randomUUID();
        UUID transactionId = UUID.randomUUID();
        UUID propertyId = UUID.randomUUID();
        UUID propertyOfferId = UUID.randomUUID();
        MockHttpServletRequest request = createRequestWithInternalId(clientId);

        com.example.courtierprobackend.transactions.datalayer.dto.OfferDocumentResponseDTO doc = 
            com.example.courtierprobackend.transactions.datalayer.dto.OfferDocumentResponseDTO.builder()
                .documentId(UUID.randomUUID())
                .fileName("offer-doc.pdf")
                .build();
        when(transactionService.getPropertyOfferDocuments(eq(propertyOfferId), eq(clientId), eq(false))).thenReturn(List.of(doc));

        ResponseEntity<List<com.example.courtierprobackend.transactions.datalayer.dto.OfferDocumentResponseDTO>> response = 
            controller.getPropertyOfferDocuments(clientId.toString(), transactionId, propertyId, propertyOfferId, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().get(0).getFileName()).isEqualTo("offer-doc.pdf");
        verify(transactionService).getPropertyOfferDocuments(propertyOfferId, clientId, false);
    }

    @Test
    void getPropertyOfferDocuments_WithMismatchedClientId_ThrowsForbidden() {
        UUID internalId = UUID.randomUUID();
        UUID differentClientId = UUID.randomUUID();
        UUID transactionId = UUID.randomUUID();
        UUID propertyId = UUID.randomUUID();
        UUID propertyOfferId = UUID.randomUUID();
        MockHttpServletRequest request = createRequestWithInternalId(internalId);

        assertThatThrownBy(() -> controller.getPropertyOfferDocuments(differentClientId.toString(), transactionId, propertyId, propertyOfferId, request))
            .isInstanceOf(com.example.courtierprobackend.common.exceptions.ForbiddenException.class)
            .hasMessageContaining("only access your own transactions");

        verifyNoInteractions(transactionService);
    }

    // ========== Offer Document Tests ==========

    @Test
    void getOfferDocuments_WithValidRequest_ReturnsDocuments() {
        UUID clientId = UUID.randomUUID();
        UUID transactionId = UUID.randomUUID();
        UUID offerId = UUID.randomUUID();
        MockHttpServletRequest request = createRequestWithInternalId(clientId);

        com.example.courtierprobackend.transactions.datalayer.dto.OfferDocumentResponseDTO doc = 
            com.example.courtierprobackend.transactions.datalayer.dto.OfferDocumentResponseDTO.builder()
                .documentId(UUID.randomUUID())
                .fileName("contract.pdf")
                .build();
        when(transactionService.getOfferDocuments(eq(offerId), eq(clientId), eq(false))).thenReturn(List.of(doc));

        ResponseEntity<List<com.example.courtierprobackend.transactions.datalayer.dto.OfferDocumentResponseDTO>> response = 
            controller.getOfferDocuments(clientId.toString(), transactionId, offerId, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().get(0).getFileName()).isEqualTo("contract.pdf");
        verify(transactionService).getOfferDocuments(offerId, clientId, false);
    }

    @Test
    void getOfferDocuments_WithMismatchedClientId_ThrowsForbidden() {
        UUID internalId = UUID.randomUUID();
        UUID differentClientId = UUID.randomUUID();
        UUID transactionId = UUID.randomUUID();
        UUID offerId = UUID.randomUUID();
        MockHttpServletRequest request = createRequestWithInternalId(internalId);

        assertThatThrownBy(() -> controller.getOfferDocuments(differentClientId.toString(), transactionId, offerId, request))
            .isInstanceOf(com.example.courtierprobackend.common.exceptions.ForbiddenException.class)
            .hasMessageContaining("only access your own transactions");

        verifyNoInteractions(transactionService);
    }

    // ========== Auth0 ID Resolution Tests ==========

    @Test
    void getPropertyOffers_NotUuid_AccountMatchesInternalId_Succeeds() {
        String notUuid = "auth0|12345";
        UUID internalId = UUID.randomUUID();
        UUID transactionId = UUID.randomUUID();
        UUID propertyId = UUID.randomUUID();
        MockHttpServletRequest request = createRequestWithInternalId(internalId);

        com.example.courtierprobackend.user.dataaccesslayer.UserAccount account = 
            mock(com.example.courtierprobackend.user.dataaccesslayer.UserAccount.class);
        when(account.getId()).thenReturn(internalId);
        when(userAccountRepository.findByAuth0UserId(notUuid)).thenReturn(java.util.Optional.of(account));

        com.example.courtierprobackend.transactions.datalayer.dto.PropertyOfferResponseDTO offer = 
            com.example.courtierprobackend.transactions.datalayer.dto.PropertyOfferResponseDTO.builder()
                .propertyOfferId(UUID.randomUUID())
                .propertyId(propertyId)
                .build();
        when(transactionService.getPropertyOffers(eq(propertyId), eq(internalId), eq(false))).thenReturn(List.of(offer));

        ResponseEntity<List<com.example.courtierprobackend.transactions.datalayer.dto.PropertyOfferResponseDTO>> response = 
            controller.getPropertyOffers(notUuid, transactionId, propertyId, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
    }

    @Test
    void getPropertyOfferDocuments_NotUuid_AccountMatchesInternalId_Succeeds() {
        String notUuid = "auth0|67890";
        UUID internalId = UUID.randomUUID();
        UUID transactionId = UUID.randomUUID();
        UUID propertyId = UUID.randomUUID();
        UUID propertyOfferId = UUID.randomUUID();
        MockHttpServletRequest request = createRequestWithInternalId(internalId);

        com.example.courtierprobackend.user.dataaccesslayer.UserAccount account = 
            mock(com.example.courtierprobackend.user.dataaccesslayer.UserAccount.class);
        when(account.getId()).thenReturn(internalId);
        when(userAccountRepository.findByAuth0UserId(notUuid)).thenReturn(java.util.Optional.of(account));

        com.example.courtierprobackend.transactions.datalayer.dto.OfferDocumentResponseDTO doc = 
            com.example.courtierprobackend.transactions.datalayer.dto.OfferDocumentResponseDTO.builder()
                .documentId(UUID.randomUUID())
                .fileName("doc.pdf")
                .build();
        when(transactionService.getPropertyOfferDocuments(eq(propertyOfferId), eq(internalId), eq(false))).thenReturn(List.of(doc));

        ResponseEntity<List<com.example.courtierprobackend.transactions.datalayer.dto.OfferDocumentResponseDTO>> response = 
            controller.getPropertyOfferDocuments(notUuid, transactionId, propertyId, propertyOfferId, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
    }

    @Test
    void getOfferDocuments_NotUuid_AccountMatchesInternalId_Succeeds() {
        String notUuid = "auth0|abcde";
        UUID internalId = UUID.randomUUID();
        UUID transactionId = UUID.randomUUID();
        UUID offerId = UUID.randomUUID();
        MockHttpServletRequest request = createRequestWithInternalId(internalId);

        com.example.courtierprobackend.user.dataaccesslayer.UserAccount account = 
            mock(com.example.courtierprobackend.user.dataaccesslayer.UserAccount.class);
        when(account.getId()).thenReturn(internalId);
        when(userAccountRepository.findByAuth0UserId(notUuid)).thenReturn(java.util.Optional.of(account));

        com.example.courtierprobackend.transactions.datalayer.dto.OfferDocumentResponseDTO doc = 
            com.example.courtierprobackend.transactions.datalayer.dto.OfferDocumentResponseDTO.builder()
                .documentId(UUID.randomUUID())
                .fileName("offer.pdf")
                .build();
        when(transactionService.getOfferDocuments(eq(offerId), eq(internalId), eq(false))).thenReturn(List.of(doc));

        ResponseEntity<List<com.example.courtierprobackend.transactions.datalayer.dto.OfferDocumentResponseDTO>> response = 
            controller.getOfferDocuments(notUuid, transactionId, offerId, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
    }
}

