package com.example.courtierprobackend.transactions;

import com.example.courtierprobackend.transactions.datalayer.DocumentConditionLink;
import com.example.courtierprobackend.transactions.datalayer.dto.OfferRequestDTO;
import com.example.courtierprobackend.transactions.datalayer.enums.*;
import com.example.courtierprobackend.transactions.datalayer.repositories.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for condition linking functionality in offers.
 * Tests the full request-response cycle for linking conditions to offers.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ConditionLinkingIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private ConditionRepository conditionRepository;

    @Autowired
    private DocumentConditionLinkRepository documentConditionLinkRepository;

    private UUID transactionId;
    private UUID conditionId1;
    private UUID conditionId2;
    private UUID brokerId;
    private UUID clientId;

    @BeforeEach
    void setUp() {
        transactionId = UUID.randomUUID();
        brokerId = UUID.randomUUID();
        clientId = UUID.randomUUID();
        conditionId1 = UUID.randomUUID();
        conditionId2 = UUID.randomUUID();

        // Create test transaction
        com.example.courtierprobackend.transactions.datalayer.Transaction tx = 
                com.example.courtierprobackend.transactions.datalayer.Transaction.builder()
                        .transactionId(transactionId)
                        .brokerId(brokerId)
                        .clientId(clientId)
                        .side(TransactionSide.SELL_SIDE)
                        .status(TransactionStatus.ACTIVE)
                        .build();
        transactionRepository.save(tx);

        // Create test conditions
        com.example.courtierprobackend.transactions.datalayer.Condition condition1 = 
                com.example.courtierprobackend.transactions.datalayer.Condition.builder()
                        .conditionId(conditionId1)
                        .transactionId(transactionId)
                        .type(ConditionType.FINANCING)
                        .description("Financing condition")
                        .deadlineDate(LocalDate.now().plusDays(30))
                        .status(ConditionStatus.PENDING)
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build();
        conditionRepository.save(condition1);

        com.example.courtierprobackend.transactions.datalayer.Condition condition2 = 
                com.example.courtierprobackend.transactions.datalayer.Condition.builder()
                        .conditionId(conditionId2)
                        .transactionId(transactionId)
                        .type(ConditionType.INSPECTION)
                        .description("Inspection condition")
                        .deadlineDate(LocalDate.now().plusDays(14))
                        .status(ConditionStatus.PENDING)
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build();
        conditionRepository.save(condition2);
    }

    @Test
    @DisplayName("POST /transactions/{id}/offers - should create offer and link conditions")
    @WithMockUser(roles = "BROKER")
    void addOffer_withConditionIds_createsOfferAndLinks() throws Exception {
        OfferRequestDTO dto = OfferRequestDTO.builder()
                .buyerName("Test Buyer")
                .offerAmount(BigDecimal.valueOf(500000))
                .status(ReceivedOfferStatus.PENDING)
                .expiryDate(LocalDate.now().plusDays(7))
                .conditionIds(Arrays.asList(conditionId1, conditionId2))
                .build();

        mockMvc.perform(post("/transactions/{transactionId}/offers", transactionId)
                        .header("x-broker-id", brokerId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.offerId").exists())
                .andExpect(jsonPath("$.buyerName").value("Test Buyer"))
                .andExpect(jsonPath("$.conditions").isArray())
                .andExpect(jsonPath("$.conditions.length()").value(2));

        // Verify links were created in database
        List<DocumentConditionLink> allLinks = documentConditionLinkRepository.findAll();
        assertThat(allLinks).hasSize(2);
    }

    @Test
    @DisplayName("PUT /transactions/{id}/offers/{offerId} - should update offer and replace condition links")
    @WithMockUser(roles = "BROKER")
    void updateOffer_withConditionIds_replacesLinks() throws Exception {
        // First create an offer with conditions
        OfferRequestDTO createDto = OfferRequestDTO.builder()
                .buyerName("Test Buyer")
                .offerAmount(BigDecimal.valueOf(500000))
                .status(ReceivedOfferStatus.PENDING)
                .conditionIds(Arrays.asList(conditionId1, conditionId2))
                .build();

        String createResponse = mockMvc.perform(post("/transactions/{transactionId}/offers", transactionId)
                        .header("x-broker-id", brokerId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String offerId = objectMapper.readTree(createResponse).get("offerId").asText();

        // Update with only one condition
        OfferRequestDTO updateDto = OfferRequestDTO.builder()
                .buyerName("Updated Buyer")
                .offerAmount(BigDecimal.valueOf(520000))
                .status(ReceivedOfferStatus.UNDER_REVIEW)
                .conditionIds(Arrays.asList(conditionId1)) // Only one condition now
                .build();

        mockMvc.perform(put("/transactions/{transactionId}/offers/{offerId}", transactionId, offerId)
                        .header("x-broker-id", brokerId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conditions.length()").value(1))
                .andExpect(jsonPath("$.conditions[0].conditionId").value(conditionId1.toString()));

        // Verify only one link remains
        List<DocumentConditionLink> links = documentConditionLinkRepository.findByOfferId(UUID.fromString(offerId));
        assertThat(links).hasSize(1);
        assertThat(links.get(0).getConditionId()).isEqualTo(conditionId1);
    }

    @Test
    @DisplayName("GET /transactions/{id}/offers - should return offers with linked conditions")
    @WithMockUser(roles = "BROKER")
    void getOffers_returnsOffersWithConditions() throws Exception {
        // Create an offer with conditions
        OfferRequestDTO dto = OfferRequestDTO.builder()
                .buyerName("Test Buyer")
                .offerAmount(BigDecimal.valueOf(500000))
                .status(ReceivedOfferStatus.PENDING)
                .conditionIds(Arrays.asList(conditionId1))
                .build();

        mockMvc.perform(post("/transactions/{transactionId}/offers", transactionId)
                        .header("x-broker-id", brokerId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated());

        // Get all offers
        mockMvc.perform(get("/transactions/{transactionId}/offers", transactionId)
                        .header("x-broker-id", brokerId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].conditions").isArray())
                .andExpect(jsonPath("$[0].conditions[0].type").value("FINANCING"));
    }

    @Test
    @DisplayName("POST /transactions/{id}/offers - should create offer without conditions when conditionIds is null")
    @WithMockUser(roles = "BROKER")
    void addOffer_noConditionIds_createsOfferWithoutLinks() throws Exception {
        OfferRequestDTO dto = OfferRequestDTO.builder()
                .buyerName("Test Buyer")
                .offerAmount(BigDecimal.valueOf(500000))
                .status(ReceivedOfferStatus.PENDING)
                .expiryDate(LocalDate.now().plusDays(7))
                .conditionIds(null)
                .build();

        mockMvc.perform(post("/transactions/{transactionId}/offers", transactionId)
                        .header("x-broker-id", brokerId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.offerId").exists())
                .andExpect(jsonPath("$.conditions").isEmpty());

        // Verify no links were created
        List<DocumentConditionLink> allLinks = documentConditionLinkRepository.findAll();
        assertThat(allLinks).isEmpty();
    }
}
