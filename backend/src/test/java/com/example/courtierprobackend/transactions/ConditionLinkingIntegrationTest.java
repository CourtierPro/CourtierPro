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
}
