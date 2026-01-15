package com.example.courtierprobackend.transactions.datalayer.dto;

import com.example.courtierprobackend.transactions.datalayer.enums.ClientOfferDecision;
import com.example.courtierprobackend.transactions.datalayer.enums.ReceivedOfferStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for OfferResponseDTO.
 * Tests builder pattern and client decision fields.
 */
class OfferResponseDTOTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Nested
    @DisplayName("Builder Tests")
    class BuilderTests {

        @Test
        @DisplayName("should create DTO with all fields including client decision")
        void shouldCreateDtoWithAllFields() {
            UUID offerId = UUID.randomUUID();
            UUID transactionId = UUID.randomUUID();
            LocalDateTime now = LocalDateTime.now();

            OfferResponseDTO dto = OfferResponseDTO.builder()
                    .offerId(offerId)
                    .transactionId(transactionId)
                    .buyerName("John Doe")
                    .offerAmount(BigDecimal.valueOf(500000))
                    .status(ReceivedOfferStatus.PENDING)
                    .expiryDate(LocalDate.now().plusDays(7))
                    .notes("Test notes")
                    .clientDecision(ClientOfferDecision.ACCEPT)
                    .clientDecisionAt(now)
                    .clientDecisionNotes("I agree to this offer")
                    .createdAt(now)
                    .updatedAt(now)
                    .documents(List.of())
                    .conditions(List.of())
                    .build();

            assertThat(dto.getOfferId()).isEqualTo(offerId);
            assertThat(dto.getTransactionId()).isEqualTo(transactionId);
            assertThat(dto.getBuyerName()).isEqualTo("John Doe");
            assertThat(dto.getOfferAmount()).isEqualByComparingTo(BigDecimal.valueOf(500000));
            assertThat(dto.getStatus()).isEqualTo(ReceivedOfferStatus.PENDING);
            assertThat(dto.getClientDecision()).isEqualTo(ClientOfferDecision.ACCEPT);
            assertThat(dto.getClientDecisionAt()).isEqualTo(now);
            assertThat(dto.getClientDecisionNotes()).isEqualTo("I agree to this offer");
        }

        @Test
        @DisplayName("should create DTO without client decision fields")
        void shouldCreateDtoWithoutClientDecision() {
            OfferResponseDTO dto = OfferResponseDTO.builder()
                    .offerId(UUID.randomUUID())
                    .transactionId(UUID.randomUUID())
                    .buyerName("Jane Smith")
                    .offerAmount(BigDecimal.valueOf(450000))
                    .status(ReceivedOfferStatus.PENDING)
                    .build();

            assertThat(dto.getBuyerName()).isEqualTo("Jane Smith");
            assertThat(dto.getClientDecision()).isNull();
            assertThat(dto.getClientDecisionAt()).isNull();
            assertThat(dto.getClientDecisionNotes()).isNull();
        }
    }

    @Nested
    @DisplayName("Client Decision Fields Tests")
    class ClientDecisionFieldsTests {

        @Test
        @DisplayName("should set client decision ACCEPT")
        void shouldSetClientDecisionAccept() {
            OfferResponseDTO dto = OfferResponseDTO.builder()
                    .clientDecision(ClientOfferDecision.ACCEPT)
                    .build();

            assertThat(dto.getClientDecision()).isEqualTo(ClientOfferDecision.ACCEPT);
        }

        @Test
        @DisplayName("should set client decision DECLINE")
        void shouldSetClientDecisionDecline() {
            OfferResponseDTO dto = OfferResponseDTO.builder()
                    .clientDecision(ClientOfferDecision.DECLINE)
                    .build();

            assertThat(dto.getClientDecision()).isEqualTo(ClientOfferDecision.DECLINE);
        }

        @Test
        @DisplayName("should set client decision COUNTER")
        void shouldSetClientDecisionCounter() {
            OfferResponseDTO dto = OfferResponseDTO.builder()
                    .clientDecision(ClientOfferDecision.COUNTER)
                    .build();

            assertThat(dto.getClientDecision()).isEqualTo(ClientOfferDecision.COUNTER);
        }

        @Test
        @DisplayName("should set clientDecisionAt timestamp")
        void shouldSetClientDecisionAt() {
            LocalDateTime timestamp = LocalDateTime.of(2024, 1, 15, 10, 30);

            OfferResponseDTO dto = OfferResponseDTO.builder()
                    .clientDecisionAt(timestamp)
                    .build();

            assertThat(dto.getClientDecisionAt()).isEqualTo(timestamp);
        }

        @Test
        @DisplayName("should set clientDecisionNotes")
        void shouldSetClientDecisionNotes() {
            String notes = "I would like to accept this offer as it meets our asking price";

            OfferResponseDTO dto = OfferResponseDTO.builder()
                    .clientDecisionNotes(notes)
                    .build();

            assertThat(dto.getClientDecisionNotes()).isEqualTo(notes);
        }
    }

    @Nested
    @DisplayName("JSON Serialization Tests")
    class JsonSerializationTests {

        @Test
        @DisplayName("should serialize DTO with client decision to JSON")
        void shouldSerializeDtoWithClientDecision() throws Exception {
            LocalDateTime decisionTime = LocalDateTime.of(2024, 1, 15, 10, 30, 0);
            
            OfferResponseDTO dto = OfferResponseDTO.builder()
                    .offerId(UUID.fromString("11111111-1111-1111-1111-111111111111"))
                    .buyerName("John Doe")
                    .offerAmount(BigDecimal.valueOf(500000))
                    .status(ReceivedOfferStatus.PENDING)
                    .clientDecision(ClientOfferDecision.ACCEPT)
                    .clientDecisionAt(decisionTime)
                    .clientDecisionNotes("Agreed")
                    .build();

            String json = objectMapper.writeValueAsString(dto);

            assertThat(json).contains("\"clientDecision\":\"ACCEPT\"");
            assertThat(json).contains("\"clientDecisionNotes\":\"Agreed\"");
            assertThat(json).contains("\"clientDecisionAt\"");
        }

        @Test
        @DisplayName("should deserialize JSON with client decision to DTO")
        void shouldDeserializeJsonWithClientDecision() throws Exception {
            String json = """
                    {
                        "offerId": "11111111-1111-1111-1111-111111111111",
                        "buyerName": "John Doe",
                        "offerAmount": 500000,
                        "status": "PENDING",
                        "clientDecision": "DECLINE",
                        "clientDecisionNotes": "Price too low"
                    }
                    """;

            OfferResponseDTO dto = objectMapper.readValue(json, OfferResponseDTO.class);

            assertThat(dto.getClientDecision()).isEqualTo(ClientOfferDecision.DECLINE);
            assertThat(dto.getClientDecisionNotes()).isEqualTo("Price too low");
        }

        @Test
        @DisplayName("should serialize DTO without client decision (null values)")
        void shouldSerializeDtoWithoutClientDecision() throws Exception {
            OfferResponseDTO dto = OfferResponseDTO.builder()
                    .offerId(UUID.fromString("11111111-1111-1111-1111-111111111111"))
                    .buyerName("John Doe")
                    .status(ReceivedOfferStatus.PENDING)
                    .build();

            String json = objectMapper.writeValueAsString(dto);

            // Should not throw and should contain expected fields
            assertThat(json).contains("\"buyerName\":\"John Doe\"");
            assertThat(json).contains("\"status\":\"PENDING\"");
        }
    }
}
