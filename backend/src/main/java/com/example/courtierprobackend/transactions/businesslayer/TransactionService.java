
package com.example.courtierprobackend.transactions.businesslayer;

import com.example.courtierprobackend.audit.timeline_audit.presentationlayer.TimelineEntryDTO;
import com.example.courtierprobackend.transactions.datalayer.dto.*;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface TransactionService {

    /**
     * Save internal notes for a transaction and create a NOTE event in the
     * timeline.
     */
    void saveInternalNotes(UUID transactionId, String notes, UUID brokerId);

    TransactionResponseDTO createTransaction(TransactionRequestDTO dto);

    java.util.List<TimelineEntryDTO> getNotes(UUID transactionId, UUID brokerId);

    TimelineEntryDTO createNote(UUID transactionId, NoteRequestDTO note, UUID brokerId);

    List<TransactionResponseDTO> getBrokerTransactions(UUID brokerId, String status, String stage, String side);

    List<TransactionResponseDTO> getClientTransactions(UUID clientId);

    List<TransactionResponseDTO> getBrokerClientTransactions(UUID brokerId, UUID clientId);

    /**
     * Get all transactions for a client, regardless of broker.
     * Returns all transactions with broker name for each.
     */
    List<TransactionResponseDTO> getAllClientTransactions(UUID clientId);

    TransactionResponseDTO updateTransactionStage(UUID transactionId, StageUpdateRequestDTO dto, UUID brokerId);

    TransactionResponseDTO getByTransactionId(UUID transactionId, UUID userId);

    void pinTransaction(UUID transactionId, UUID brokerId);

    void unpinTransaction(UUID transactionId, UUID brokerId);

    Set<UUID> getPinnedTransactionIds(UUID brokerId);

    // Participants
    ParticipantResponseDTO addParticipant(UUID transactionId, AddParticipantRequestDTO dto, UUID brokerId);

    void removeParticipant(UUID transactionId, UUID participantId, UUID brokerId);

    List<ParticipantResponseDTO> getParticipants(UUID transactionId, UUID userId);

    // Properties (for buyer transactions)
    List<PropertyResponseDTO> getProperties(UUID transactionId, UUID userId, boolean isBroker);

    PropertyResponseDTO addProperty(UUID transactionId, PropertyRequestDTO dto, UUID brokerId);

    PropertyResponseDTO updateProperty(UUID transactionId, UUID propertyId, PropertyRequestDTO dto, UUID brokerId);

    void removeProperty(UUID transactionId, UUID propertyId, UUID brokerId);

    PropertyResponseDTO getPropertyById(UUID propertyId, UUID userId, boolean isBroker);
    
    void setActiveProperty(UUID transactionId, UUID propertyId, UUID brokerId);

    // Offers (for seller transactions)
    List<OfferResponseDTO> getOffers(UUID transactionId, UUID userId, boolean isBroker);

    OfferResponseDTO addOffer(UUID transactionId, OfferRequestDTO dto, UUID brokerId);

    OfferResponseDTO updateOffer(UUID transactionId, UUID offerId, OfferRequestDTO dto, UUID brokerId);

    void removeOffer(UUID transactionId, UUID offerId, UUID brokerId);

    OfferResponseDTO getOfferById(UUID offerId, UUID userId, boolean isBroker);

    // Conditions (for all transactions)
    List<ConditionResponseDTO> getConditions(UUID transactionId, UUID userId, boolean isBroker);

    ConditionResponseDTO addCondition(UUID transactionId, ConditionRequestDTO dto, UUID brokerId);

    ConditionResponseDTO updateCondition(UUID transactionId, UUID conditionId, ConditionRequestDTO dto, UUID brokerId);

    void removeCondition(UUID transactionId, UUID conditionId, UUID brokerId);

    ConditionResponseDTO updateConditionStatus(UUID transactionId, UUID conditionId, 
            com.example.courtierprobackend.transactions.datalayer.enums.ConditionStatus status, UUID brokerId);
}
