package com.example.courtierprobackend.transactions;

import com.example.courtierprobackend.audit.timeline_audit.businesslayer.TimelineService;
import com.example.courtierprobackend.common.exceptions.GlobalExceptionHandler;
import com.example.courtierprobackend.common.exceptions.NotFoundException;
import com.example.courtierprobackend.security.UserContextFilter;
import com.example.courtierprobackend.transactions.businesslayer.TransactionService;
import com.example.courtierprobackend.transactions.datalayer.dto.OfferRevisionResponseDTO;
import com.example.courtierprobackend.transactions.datalayer.enums.ReceivedOfferStatus;
import com.example.courtierprobackend.transactions.presentationlayer.TransactionController;
import com.example.courtierprobackend.user.dataaccesslayer.UserAccountRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TransactionController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class OfferRevisionControllerIntegrationTest {

    @MockitoBean
    private TimelineService timelineService;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TransactionService service;

    @MockitoBean
    private UserContextFilter userContextFilter;

    @MockitoBean
    private UserAccountRepository userAccountRepository;

    private static final SimpleGrantedAuthority ROLE_BROKER = new SimpleGrantedAuthority("ROLE_BROKER");

    @Nested
    @DisplayName("GET /transactions/{transactionId}/offers/{offerId}/revisions")
    class GetOfferRevisionsTests {

        @Test
        @DisplayName("should return revisions - 200")
        void getRevisions_validRequest_returns200() throws Exception {
            UUID transactionId = UUID.randomUUID();
            UUID offerId = UUID.randomUUID();
            UUID brokerId = UUID.randomUUID();

            OfferRevisionResponseDTO revision = OfferRevisionResponseDTO.builder()
                    .revisionId(UUID.randomUUID())
                    .offerId(offerId)
                    .revisionNumber(1)
                    .newAmount(new BigDecimal("500000"))
                    .newStatus(ReceivedOfferStatus.PENDING.name())
                    .createdAt(LocalDateTime.now())
                    .build();

            when(service.getOfferRevisions(eq(offerId), eq(brokerId), anyBoolean()))
                    .thenReturn(List.of(revision));

            mockMvc.perform(
                    get("/transactions/{transactionId}/offers/{offerId}/revisions", transactionId, offerId)
                            .with(jwt().authorities(ROLE_BROKER).jwt(jwt -> jwt.claim("sub", brokerId.toString())))
                            .header("x-broker-id", brokerId.toString())
            )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$[0].revisionNumber").value(1));
        }

        @Test
        @DisplayName("should return 404 when offer not found")
        void getRevisions_offerNotFound_returns404() throws Exception {
            UUID transactionId = UUID.randomUUID();
            UUID offerId = UUID.randomUUID();
            UUID brokerId = UUID.randomUUID();

            when(service.getOfferRevisions(eq(offerId), eq(brokerId), anyBoolean()))
                    .thenThrow(new NotFoundException("Offer not found"));

            mockMvc.perform(
                    get("/transactions/{transactionId}/offers/{offerId}/revisions", transactionId, offerId)
                            .with(jwt().authorities(ROLE_BROKER).jwt(jwt -> jwt.claim("sub", brokerId.toString())))
                            .header("x-broker-id", brokerId.toString())
            )
                    .andExpect(status().isNotFound());
        }
    }
}
