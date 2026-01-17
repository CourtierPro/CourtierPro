package com.example.courtierprobackend.transactions.presentationlayer;

import com.example.courtierprobackend.audit.timeline_audit.businesslayer.TimelineService;
import com.example.courtierprobackend.security.UserContextFilter;
import com.example.courtierprobackend.transactions.businesslayer.TransactionService;
import com.example.courtierprobackend.transactions.datalayer.dto.TransactionResponseDTO;
import com.example.courtierprobackend.transactions.datalayer.dto.AddParticipantRequestDTO;
import com.example.courtierprobackend.transactions.datalayer.dto.ParticipantResponseDTO;
import com.example.courtierprobackend.transactions.datalayer.enums.ParticipantRole;
import com.example.courtierprobackend.transactions.datalayer.enums.TransactionSide;
import com.example.courtierprobackend.user.dataaccesslayer.UserAccountRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for TransactionController REST endpoints.
 * Uses MockMvc with mocked TransactionService to test controller behavior.
 */
@WebMvcTest(TransactionController.class)
@AutoConfigureMockMvc(addFilters = false)
class TransactionControllerTest {
        @MockBean
        private TimelineService timelineService;

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @MockBean
        private TransactionService transactionService;

        @MockBean
        private UserContextFilter userContextFilter;

        @MockBean
        private UserAccountRepository userAccountRepository;

        // ========== createTransaction Tests ==========

        // ========== getNotes Tests ==========

        @Test
        @WithMockUser(roles = "BROKER")
        void getNotes_withValidTransactionId_returnsNotesList() throws Exception {
                // Arrange
                UUID txId = UUID.randomUUID();
                UUID brokerUuid = UUID.randomUUID();
                String brokerId = brokerUuid.toString();

                var note1 = new com.example.courtierprobackend.audit.timeline_audit.presentationlayer.TimelineEntryDTO();
                note1.setTitle("Note 1");
                var note2 = new com.example.courtierprobackend.audit.timeline_audit.presentationlayer.TimelineEntryDTO();
                note2.setTitle("Note 2");
                when(transactionService.getNotes(txId, brokerUuid)).thenReturn(List.of(note1, note2));

                // Act & Assert
                mockMvc.perform(get("/transactions/" + txId + "/notes")
                                .with(jwt().jwt(jwt -> jwt.claim("sub", "auth0|broker-1")))
                                .header("x-broker-id", brokerId))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.length()").value(2))
                                .andExpect(jsonPath("$[0].title").value("Note 1"))
                                .andExpect(jsonPath("$[1].title").value("Note 2"));

                verify(transactionService).getNotes(txId, brokerUuid);
        }

        @Test
        @WithMockUser(roles = "BROKER")
        void getNotes_withHeaderBrokerId_usesHeader() throws Exception {
                // Arrange
                UUID txId = UUID.randomUUID();
                UUID brokerUuid = UUID.randomUUID();
                String brokerId = brokerUuid.toString();

                when(transactionService.getNotes(txId, brokerUuid))
                                .thenReturn(List.of());

                // Act & Assert
                mockMvc.perform(get("/transactions/" + txId + "/notes")
                                .with(jwt())
                                .header("x-broker-id", brokerId))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.length()").value(0));

                verify(transactionService).getNotes(txId, brokerUuid);
        }

        // ========== getBrokerTransactions Tests ==========

        @Test
        @WithMockUser(roles = "BROKER")
        void getBrokerTransactions_returnsTransactionsList() throws Exception {
                // Arrange
                UUID brokerUuid = UUID.randomUUID();
                String brokerId = brokerUuid.toString();
                UUID tx1Id = UUID.randomUUID();
                UUID tx2Id = UUID.randomUUID();

                TransactionResponseDTO tx1 = TransactionResponseDTO.builder()
                                .transactionId(tx1Id)
                                .brokerId(brokerUuid)
                                .build();

                TransactionResponseDTO tx2 = TransactionResponseDTO.builder()
                                .transactionId(tx2Id)
                                .brokerId(brokerUuid)
                                .build();

                when(transactionService.getBrokerTransactions(eq(brokerUuid), any(), any(), any()))
                                .thenReturn(List.of(tx1, tx2));

                // Act & Assert
                mockMvc.perform(get("/transactions")
                                .with(jwt().jwt(jwt -> jwt.claim("sub", "auth0|broker-1"))) // JWT sub ignored if header
                                                                                            // present
                                .header("x-broker-id", brokerId))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.length()").value(2))
                                .andExpect(jsonPath("$[0].transactionId").value(tx1Id.toString()))
                                .andExpect(jsonPath("$[1].transactionId").value(tx2Id.toString()));

                verify(transactionService).getBrokerTransactions(eq(brokerUuid), any(), any(), any());
        }

        @Test
        @WithMockUser(roles = "BROKER")
        void getBrokerTransactions_withHeaderBrokerId_usesHeader() throws Exception {
                // Arrange
                UUID brokerUuid = UUID.randomUUID();
                String brokerId = brokerUuid.toString();

                when(transactionService.getBrokerTransactions(eq(brokerUuid), any(), any(), any()))
                                .thenReturn(List.of());

                // Act & Assert
                mockMvc.perform(get("/transactions")
                                .with(jwt())
                                .header("x-broker-id", brokerId))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.length()").value(0));

                verify(transactionService).getBrokerTransactions(eq(brokerUuid), any(), any(), any());
        }

        // ========== getTransactionById Tests ==========

        @Test
        @WithMockUser(roles = "BROKER")
        void getTransactionById_withValidId_returnsTransaction() throws Exception {
                // Arrange
                UUID txId = UUID.randomUUID();
                UUID brokerUuid = UUID.randomUUID();
                String brokerId = brokerUuid.toString();

                TransactionResponseDTO response = TransactionResponseDTO.builder()
                                .transactionId(txId)
                                .clientId(UUID.randomUUID())
                                .brokerId(brokerUuid)
                                .side(TransactionSide.BUY_SIDE)
                                .build();

                when(transactionService.getByTransactionId(eq(txId), eq(brokerUuid)))
                                .thenReturn(response);

                // Act & Assert
                mockMvc.perform(get("/transactions/" + txId)
                                .with(jwt().jwt(jwt -> jwt.claim("sub", "auth0|" + brokerId))) // Auth0 ID can be
                                                                                               // anything, but we use
                                                                                               // internal ID
                                .header("x-broker-id", brokerId))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.transactionId").value(txId.toString()))
                                .andExpect(jsonPath("$.brokerId").value(brokerId));

                verify(transactionService).getByTransactionId(eq(txId), eq(brokerUuid));
        }

        @Test
        @WithMockUser(roles = "BROKER")
        void getTransactionById_withHeaderBrokerId_usesHeader() throws Exception {
                // Arrange
                UUID txId = UUID.randomUUID();
                UUID brokerUuid = UUID.randomUUID();
                String brokerId = brokerUuid.toString();

                TransactionResponseDTO response = TransactionResponseDTO.builder()
                                .transactionId(txId)
                                .brokerId(brokerUuid)
                                .build();

                when(transactionService.getByTransactionId(eq(txId), eq(brokerUuid)))
                                .thenReturn(response);

                // Act & Assert
                mockMvc.perform(get("/transactions/" + txId)
                                .with(jwt())
                                .header("x-broker-id", brokerId))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.transactionId").value(txId.toString()))
                                .andExpect(jsonPath("$.brokerId").value(brokerId));

                verify(transactionService).getByTransactionId(eq(txId), eq(brokerUuid));
        }

        // ========== updateTransactionStage Tests ==========

        @Test
        @WithMockUser(roles = "BROKER")
        void updateTransactionStage_withValidData_returnsUpdatedTransaction() throws Exception {
                UUID txId = UUID.randomUUID();
                UUID brokerUuid = UUID.randomUUID();
                String brokerId = brokerUuid.toString();

                com.example.courtierprobackend.transactions.datalayer.dto.StageUpdateRequestDTO requestDto = new com.example.courtierprobackend.transactions.datalayer.dto.StageUpdateRequestDTO();
                requestDto.setStage("BUYER_OFFER_ACCEPTED");
                requestDto.setNote("Everything looks good");

                TransactionResponseDTO responseDto = TransactionResponseDTO.builder()
                                .transactionId(txId)
                                .brokerId(brokerUuid)
                                .currentStage("BUYER_OFFER_ACCEPTED")
                                .build();

                when(transactionService.updateTransactionStage(eq(txId), any(), eq(brokerUuid)))
                                .thenReturn(responseDto);

                mockMvc.perform(patch("/transactions/" + txId + "/stage")
                                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requestDto))
                                .with(jwt())
                                .header("x-broker-id", brokerId))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.currentStage").value("BUYER_OFFER_ACCEPTED"));

                verify(transactionService).updateTransactionStage(eq(txId), any(), eq(brokerUuid));
        }

        // ========== pin/unpin/getPinned Tests ==========

        @Test
        @WithMockUser(roles = "BROKER")
        void pinTransaction_returnsNoContent() throws Exception {
                UUID txId = UUID.randomUUID();
                UUID brokerUuid = UUID.randomUUID();
                String brokerId = brokerUuid.toString();

                mockMvc.perform(post("/transactions/" + txId + "/pin")
                                .with(jwt())
                                .header("x-broker-id", brokerId))
                                .andExpect(status().isNoContent());

                verify(transactionService).pinTransaction(txId, brokerUuid);
        }

        @Test
        @WithMockUser(roles = "BROKER")
        void unpinTransaction_returnsNoContent() throws Exception {
                UUID txId = UUID.randomUUID();
                UUID brokerUuid = UUID.randomUUID();
                String brokerId = brokerUuid.toString();

                mockMvc.perform(delete("/transactions/" + txId + "/pin")
                                .with(jwt())
                                .header("x-broker-id", brokerId))
                                .andExpect(status().isNoContent());

                verify(transactionService).unpinTransaction(txId, brokerUuid);
        }

        @Test
        @WithMockUser(roles = "BROKER")
        void getPinnedTransactionIds_returnsSetOfIds() throws Exception {
                UUID brokerUuid = UUID.randomUUID();
                String brokerId = brokerUuid.toString();
                UUID txId1 = UUID.randomUUID();

                when(transactionService.getPinnedTransactionIds(brokerUuid))
                                .thenReturn(java.util.Set.of(txId1));

                mockMvc.perform(get("/transactions/pinned")
                                .with(jwt())
                                .header("x-broker-id", brokerId))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0]").value(txId1.toString()));

                verify(transactionService).getPinnedTransactionIds(brokerUuid);
        }

        // ========== Notes & Internal Notes ==========

        @Test
        @WithMockUser(roles = "BROKER")
        void createNote_returnsCreatedEntry() throws Exception {
                UUID txId = UUID.randomUUID();
                UUID brokerUuid = UUID.randomUUID();
                String brokerId = brokerUuid.toString();

                com.example.courtierprobackend.transactions.datalayer.dto.NoteRequestDTO requestDto = new com.example.courtierprobackend.transactions.datalayer.dto.NoteRequestDTO();
                requestDto.setTransactionId(txId); // Required by @NotNull validation
                requestDto.setActorId(brokerUuid); // Required by @NotNull validation
                requestDto.setTitle("Title");
                requestDto.setMessage("Message");

                com.example.courtierprobackend.audit.timeline_audit.presentationlayer.TimelineEntryDTO responseDto = new com.example.courtierprobackend.audit.timeline_audit.presentationlayer.TimelineEntryDTO();
                responseDto.setTitle("Title");

                when(transactionService.createNote(eq(txId), any(), eq(brokerUuid))).thenReturn(responseDto);

                mockMvc.perform(post("/transactions/" + txId + "/notes")
                                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requestDto))
                                .with(jwt())
                                .header("x-broker-id", brokerId))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.title").value("Title"));
        }

        @Test
        @WithMockUser(roles = "BROKER")
        void saveInternalNotes_returnsOk() throws Exception {
                UUID txId = UUID.randomUUID();
                UUID brokerUuid = UUID.randomUUID();
                String brokerId = brokerUuid.toString();
                String notes = "Internal secret note";

                mockMvc.perform(post("/transactions/" + txId + "/internal-notes")
                                .content(notes) // raw string body
                                .with(jwt())
                                .header("x-broker-id", brokerId))
                                .andExpect(status().isOk());

                verify(transactionService).saveInternalNotes(eq(txId), eq(notes), eq(brokerUuid));
        }

        // ========== Client Timeline ==========

        @Test
        @WithMockUser(roles = "CLIENT")
        void getClientTransactionTimeline_returnsList() throws Exception {
                UUID txId = UUID.randomUUID();

                when(timelineService.getTimelineForClient(txId)).thenReturn(List.of());

                mockMvc.perform(get("/transactions/" + txId + "/timeline/client")
                                .with(jwt()))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.length()").value(0));

                verify(timelineService).getTimelineForClient(txId);
        }

        // ========== Participant Tests ==========

        @Test
        @WithMockUser(roles = "BROKER")
        void addParticipant_returnsCreated() throws Exception {
                UUID txId = UUID.randomUUID();
                UUID brokerUuid = UUID.randomUUID();
                String brokerId = brokerUuid.toString();

                AddParticipantRequestDTO requestDto = new AddParticipantRequestDTO();
                requestDto.setName("John Doe");
                requestDto.setRole(ParticipantRole.CO_BROKER);

                ParticipantResponseDTO responseDto = ParticipantResponseDTO.builder()
                                .id(UUID.randomUUID())
                                .name("John Doe")
                                .role(ParticipantRole.CO_BROKER)
                                .build();

                when(transactionService.addParticipant(eq(txId), any(), eq(brokerUuid))).thenReturn(responseDto);

                mockMvc.perform(post("/transactions/" + txId + "/participants")
                                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requestDto))
                                .with(jwt())
                                .header("x-broker-id", brokerId))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.name").value("John Doe"));

                verify(transactionService).addParticipant(eq(txId), any(), eq(brokerUuid));
        }

        @Test
        @WithMockUser(roles = "BROKER")
        void addParticipant_withInvalidEmail_returnsBadRequest() throws Exception {
                UUID txId = UUID.randomUUID();
                AddParticipantRequestDTO requestDto = new AddParticipantRequestDTO();
                requestDto.setName("John Doe");
                requestDto.setRole(ParticipantRole.CO_BROKER);
                requestDto.setEmail("invalid-email");

                mockMvc.perform(post("/transactions/" + txId + "/participants")
                                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requestDto))
                                .with(jwt())
                                .header("x-broker-id", UUID.randomUUID().toString()))
                                .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser(roles = "BROKER")
        void removeParticipant_returnsNoContent() throws Exception {
                UUID txId = UUID.randomUUID();
                UUID participantId = UUID.randomUUID();
                UUID brokerUuid = UUID.randomUUID();
                String brokerId = brokerUuid.toString();

                mockMvc.perform(delete("/transactions/" + txId + "/participants/" + participantId)
                                .with(jwt())
                                .header("x-broker-id", brokerId))
                                .andExpect(status().isNoContent());

                verify(transactionService).removeParticipant(txId, participantId, brokerUuid);
        }

        @Test
        @WithMockUser(roles = "CLIENT")
        void getParticipants_returnsList() throws Exception {
                UUID txId = UUID.randomUUID();

                ParticipantResponseDTO p1 = ParticipantResponseDTO.builder()
                                .name("P1")
                                .role(ParticipantRole.BROKER)
                                .build();

                when(transactionService.getParticipants(eq(txId), any())).thenReturn(List.of(p1));

                mockMvc.perform(get("/transactions/" + txId + "/participants")
                                .with(jwt())
                                .requestAttr(UserContextFilter.INTERNAL_USER_ID_ATTR, UUID.randomUUID()))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.length()").value(1))
                                .andExpect(jsonPath("$[0].name").value("P1"));

                verify(transactionService).getParticipants(eq(txId), any());
        }

        @Test
        @WithMockUser(roles = "CLIENT")
        void addParticipant_withClientRole_returnsForbidden() throws Exception {
                UUID txId = UUID.randomUUID();
                AddParticipantRequestDTO requestDto = new AddParticipantRequestDTO();
                requestDto.setName("John Doe");
                requestDto.setRole(ParticipantRole.CO_BROKER);

                mockMvc.perform(post("/transactions/" + txId + "/participants")
                                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requestDto))
                                .with(jwt()))
                                .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "CLIENT")
        void removeParticipant_withClientRole_returnsForbidden() throws Exception {
                UUID txId = UUID.randomUUID();
                UUID participantId = UUID.randomUUID();

                mockMvc.perform(delete("/transactions/" + txId + "/participants/" + participantId)
                                .with(jwt()))
                                .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "BROKER")
        void getTransactionTimeline_returnsList() throws Exception {
                UUID txId = UUID.randomUUID();
                UUID brokerUuid = UUID.randomUUID();
                String brokerId = brokerUuid.toString();
                
                when(timelineService.getTimelineForTransaction(txId)).thenReturn(List.of());

                mockMvc.perform(get("/transactions/" + txId + "/timeline")
                                .with(jwt())
                                .header("x-broker-id", brokerId))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.length()").value(0));
        }

        // ========== Archive/Unarchive Tests ==========

        @Test
        @WithMockUser(roles = "BROKER")
        void archiveTransaction_returnsNoContent() throws Exception {
                UUID txId = UUID.randomUUID();
                UUID brokerUuid = UUID.randomUUID();
                String brokerId = brokerUuid.toString();

                mockMvc.perform(post("/transactions/" + txId + "/archive")
                                .with(jwt())
                                .header("x-broker-id", brokerId))
                                .andExpect(status().isNoContent());

                verify(transactionService).archiveTransaction(txId, brokerUuid);
        }

        @Test
        @WithMockUser(roles = "BROKER")
        void unarchiveTransaction_returnsNoContent() throws Exception {
                UUID txId = UUID.randomUUID();
                UUID brokerUuid = UUID.randomUUID();
                String brokerId = brokerUuid.toString();

                mockMvc.perform(delete("/transactions/" + txId + "/archive")
                                .with(jwt())
                                .header("x-broker-id", brokerId))
                                .andExpect(status().isNoContent());

                verify(transactionService).unarchiveTransaction(txId, brokerUuid);
        }

        @Test
        @WithMockUser(roles = "BROKER")
        void getArchivedTransactions_returnsTransactionsList() throws Exception {
                UUID brokerUuid = UUID.randomUUID();
                String brokerId = brokerUuid.toString();
                UUID txId = UUID.randomUUID();

                TransactionResponseDTO tx = TransactionResponseDTO.builder()
                                .transactionId(txId)
                                .brokerId(brokerUuid)
                                .archived(true)
                                .build();

                when(transactionService.getArchivedTransactions(brokerUuid)).thenReturn(List.of(tx));

                mockMvc.perform(get("/transactions/archived")
                                .with(jwt())
                                .header("x-broker-id", brokerId))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.length()").value(1))
                                .andExpect(jsonPath("$[0].transactionId").value(txId.toString()));

                verify(transactionService).getArchivedTransactions(brokerUuid);
        }

        // ========== Active Property Tests ==========

        @Test
        @WithMockUser(roles = "BROKER")
        void clearActiveProperty_returnsNoContent() throws Exception {
                UUID txId = UUID.randomUUID();
                UUID brokerUuid = UUID.randomUUID();
                String brokerId = brokerUuid.toString();

                mockMvc.perform(delete("/transactions/" + txId + "/active-property")
                                .with(jwt())
                                .header("x-broker-id", brokerId))
                                .andExpect(status().isNoContent());

                verify(transactionService).clearActiveProperty(txId, brokerUuid);
        }

        // ========== Offer Document Tests ==========

        @Test
        @WithMockUser(roles = "BROKER")
        void uploadOfferDocument_returnsCreated() throws Exception {
                UUID txId = UUID.randomUUID();
                UUID offerId = UUID.randomUUID();
                UUID brokerUuid = UUID.randomUUID();
                String brokerId = brokerUuid.toString();

                org.springframework.mock.web.MockMultipartFile file = 
                        new org.springframework.mock.web.MockMultipartFile(
                                "file", "test.pdf", "application/pdf", "test content".getBytes());

                com.example.courtierprobackend.transactions.datalayer.dto.OfferDocumentResponseDTO response = 
                        com.example.courtierprobackend.transactions.datalayer.dto.OfferDocumentResponseDTO.builder()
                                .documentId(UUID.randomUUID())
                                .fileName("test.pdf")
                                .build();

                when(transactionService.uploadOfferDocument(eq(offerId), any(), eq(brokerUuid))).thenReturn(response);

                mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart("/transactions/" + txId + "/offers/" + offerId + "/documents")
                                .file(file)
                                .with(jwt())
                                .header("x-broker-id", brokerId))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.fileName").value("test.pdf"));

                verify(transactionService).uploadOfferDocument(eq(offerId), any(), eq(brokerUuid));
        }

        @Test
        @WithMockUser(roles = "BROKER")
        void uploadPropertyOfferDocument_returnsCreated() throws Exception {
                UUID propertyId = UUID.randomUUID();
                UUID propertyOfferId = UUID.randomUUID();
                UUID brokerUuid = UUID.randomUUID();
                String brokerId = brokerUuid.toString();

                org.springframework.mock.web.MockMultipartFile file = 
                        new org.springframework.mock.web.MockMultipartFile(
                                "file", "offer.pdf", "application/pdf", "offer content".getBytes());

                com.example.courtierprobackend.transactions.datalayer.dto.OfferDocumentResponseDTO response = 
                        com.example.courtierprobackend.transactions.datalayer.dto.OfferDocumentResponseDTO.builder()
                                .documentId(UUID.randomUUID())
                                .fileName("offer.pdf")
                                .build();

                when(transactionService.uploadPropertyOfferDocument(eq(propertyOfferId), any(), eq(brokerUuid))).thenReturn(response);

                mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart("/transactions/properties/" + propertyId + "/offers/" + propertyOfferId + "/documents")
                                .file(file)
                                .with(jwt())
                                .header("x-broker-id", brokerId))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.fileName").value("offer.pdf"));

                verify(transactionService).uploadPropertyOfferDocument(eq(propertyOfferId), any(), eq(brokerUuid));
        }

        @Test
        @WithMockUser(roles = "BROKER")
        void getOfferDocuments_returnsList() throws Exception {
                UUID txId = UUID.randomUUID();
                UUID offerId = UUID.randomUUID();
                UUID brokerUuid = UUID.randomUUID();
                String brokerId = brokerUuid.toString();

                com.example.courtierprobackend.transactions.datalayer.dto.OfferDocumentResponseDTO doc = 
                        com.example.courtierprobackend.transactions.datalayer.dto.OfferDocumentResponseDTO.builder()
                                .documentId(UUID.randomUUID())
                                .fileName("contract.pdf")
                                .build();

                when(transactionService.getOfferDocuments(eq(offerId), eq(brokerUuid), eq(true))).thenReturn(List.of(doc));

                mockMvc.perform(get("/transactions/" + txId + "/offers/" + offerId + "/documents")
                                .with(jwt())
                                .header("x-broker-id", brokerId))
                                .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(roles = "BROKER")
        void getPropertyOfferDocuments_returnsList() throws Exception {
                UUID propertyId = UUID.randomUUID();
                UUID propertyOfferId = UUID.randomUUID();
                UUID brokerUuid = UUID.randomUUID();
                String brokerId = brokerUuid.toString();

                com.example.courtierprobackend.transactions.datalayer.dto.OfferDocumentResponseDTO doc = 
                        com.example.courtierprobackend.transactions.datalayer.dto.OfferDocumentResponseDTO.builder()
                                .documentId(UUID.randomUUID())
                                .fileName("property-doc.pdf")
                                .build();

                when(transactionService.getPropertyOfferDocuments(eq(propertyOfferId), eq(brokerUuid), eq(true))).thenReturn(List.of(doc));

                mockMvc.perform(get("/transactions/properties/" + propertyId + "/offers/" + propertyOfferId + "/documents")
                                .with(jwt())
                                .header("x-broker-id", brokerId))
                                .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(roles = "BROKER")
        void getOfferDocumentDownloadUrl_returnsUrl() throws Exception {
                UUID documentId = UUID.randomUUID();
                UUID brokerUuid = UUID.randomUUID();
                String brokerId = brokerUuid.toString();
                String expectedUrl = "https://s3.example.com/document.pdf";

                when(transactionService.getOfferDocumentDownloadUrl(documentId, brokerUuid)).thenReturn(expectedUrl);

                mockMvc.perform(get("/transactions/documents/" + documentId + "/download")
                                .with(jwt())
                                .header("x-broker-id", brokerId))
                                .andExpect(status().isOk())
                                .andExpect(content().string(expectedUrl));

                verify(transactionService).getOfferDocumentDownloadUrl(documentId, brokerUuid);
        }

        @Test
        @WithMockUser(roles = "BROKER")
        void deleteOfferDocument_returnsNoContent() throws Exception {
                UUID documentId = UUID.randomUUID();
                UUID brokerUuid = UUID.randomUUID();
                String brokerId = brokerUuid.toString();

                mockMvc.perform(delete("/transactions/documents/" + documentId)
                                .with(jwt())
                                .header("x-broker-id", brokerId))
                                .andExpect(status().isNoContent());

                verify(transactionService).deleteOfferDocument(documentId, brokerUuid);
        }
}

