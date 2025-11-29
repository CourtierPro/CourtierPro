package com.example.courtierprobackend.transactions.businesslayer;

import com.example.courtierprobackend.transactions.datalayer.TimelineEntry;
import com.example.courtierprobackend.transactions.datalayer.Transaction;
import com.example.courtierprobackend.transactions.datalayer.dto.TransactionRequestDTO;
import com.example.courtierprobackend.transactions.datalayer.dto.TransactionResponseDTO;
import com.example.courtierprobackend.transactions.datalayer.enums.*;
import com.example.courtierprobackend.transactions.datalayer.repositories.TransactionRepository;
import com.example.courtierprobackend.transactions.exceptions.DuplicateTransactionException;
import com.example.courtierprobackend.transactions.exceptions.InvalidInputException;
import com.example.courtierprobackend.transactions.exceptions.NotFoundException;
import com.example.courtierprobackend.transactions.util.EntityDtoUtil;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import com.example.courtierprobackend.transactions.datalayer.dto.NoteRequestDTO;
import com.example.courtierprobackend.transactions.datalayer.dto.TimelineEntryDTO;
import com.example.courtierprobackend.transactions.datalayer.enums.TimelineEntryType;

@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository repo;

    @Override
    public TransactionResponseDTO createTransaction(TransactionRequestDTO dto) {

        // 1) Validate required data
        if (dto.getClientId() == null || dto.getClientId().isBlank()) {
            throw new InvalidInputException("clientId is required");
        }
        if (dto.getBrokerId() == null || dto.getBrokerId().isBlank()) {
            throw new InvalidInputException("brokerId is required");
        }
        if (dto.getSide() == null) {
            throw new InvalidInputException("side is required");
        }
        if (dto.getPropertyAddress() == null ||
                dto.getPropertyAddress().getStreet() == null ||
                dto.getPropertyAddress().getStreet().isBlank()) {

            throw new InvalidInputException("propertyAddress.street is required");
        }

        String clientId = dto.getClientId();
        String street = dto.getPropertyAddress().getStreet();

        // 2) Prevent duplicate ACTIVE transactions
        repo.findByClientIdAndPropertyAddress_StreetAndStatus(
                clientId,
                street,
                TransactionStatus.ACTIVE
        ).ifPresent(t -> {
            throw new InvalidInputException("duplicate: Client already has an active transaction for this property");
        });

        // 3) Create Transaction entity
        Transaction tx = new Transaction();
        tx.setTransactionId("TX-" + UUID.randomUUID().toString().substring(0, 8));
        tx.setClientId(dto.getClientId());
        tx.setBrokerId(dto.getBrokerId());
        tx.setSide(dto.getSide());
        // Validate and apply initialStage based on side
        if (dto.getInitialStage() == null || dto.getInitialStage().isBlank()) {
            throw new InvalidInputException("initialStage is required");
        }

        String initial = dto.getInitialStage().trim();
        if (dto.getSide() == TransactionSide.BUY_SIDE) {
            try {
                BuyerStage buyerStage = BuyerStage.valueOf(initial);
                tx.setBuyerStage(buyerStage);
                tx.setSellerStage(null);
            } catch (IllegalArgumentException ex) {
                throw new InvalidInputException("initialStage '" + initial + "' is not a valid buyer stage. Allowed values: " + Arrays.toString(BuyerStage.values()));
            }
        } else if (dto.getSide() == TransactionSide.SELL_SIDE) {
            try {
                SellerStage sellerStage = SellerStage.valueOf(initial);
                tx.setSellerStage(sellerStage);
                tx.setBuyerStage(null);
            } catch (IllegalArgumentException ex) {
                throw new InvalidInputException("initialStage '" + initial + "' is not a valid seller stage. Allowed values: " + Arrays.toString(SellerStage.values()));
            }
        } else {
            throw new InvalidInputException("side is not supported: " + dto.getSide());
        }
        tx.setStatus(TransactionStatus.ACTIVE);
        tx.setOpenedAt(LocalDateTime.now());
        tx.setPropertyAddress(dto.getPropertyAddress());

        Transaction saved = repo.save(tx);

        return EntityDtoUtil.toResponse(saved);
    }

    @Override
    public java.util.List<TimelineEntryDTO> getNotes(String transactionId, String brokerId) {
        Transaction tx = repo.findByTransactionId(transactionId)
                .orElseThrow(() -> new NotFoundException("Transaction not found"));

        if (!tx.getBrokerId().equals(brokerId)) {
            throw new NotFoundException("You do not have access to this transaction");
        }

        List<TimelineEntry> entries = tx.getTimeline() == null ? List.of() : tx.getTimeline();

        return entries.stream()
                .filter(e -> e.getType() == TimelineEntryType.NOTE)
                .map(EntityDtoUtil::toTimelineDTO)
                .toList();
    }

    @Override
    public TimelineEntryDTO createNote(String transactionId, NoteRequestDTO note, String brokerId) {
        if (note.getActorId() == null || note.getActorId().isBlank()) {
            throw new InvalidInputException("actorId is required");
        }
        if (note.getTitle() == null || note.getTitle().isBlank()) {
            throw new InvalidInputException("title is required");
        }
        if (note.getMessage() == null || note.getMessage().isBlank()) {
            throw new InvalidInputException("message is required");
        }

        Transaction tx = repo.findByTransactionId(transactionId)
                .orElseThrow(() -> new NotFoundException("Transaction not found"));

        if (!tx.getBrokerId().equals(brokerId)) {
            throw new NotFoundException("You do not have access to this transaction");
        }

        TimelineEntry entry = TimelineEntry.builder()
                .type(TimelineEntryType.NOTE)
                .title(note.getTitle())
                .note(note.getMessage())
                .visibleToClient(note.getVisibleToClient() != null ? note.getVisibleToClient() : false)
                .occurredAt(LocalDateTime.now())
                .addedByBrokerId(note.getActorId())
                .transaction(tx)
                .build();

        if (tx.getTimeline() == null) tx.setTimeline(new ArrayList<>());
        tx.getTimeline().add(entry);

        Transaction saved = repo.save(tx);

        // find the saved entry (last)
        TimelineEntry savedEntry = saved.getTimeline().get(saved.getTimeline().size() - 1);

        return EntityDtoUtil.toTimelineDTO(savedEntry);
    }


    @Override
    public List<TransactionResponseDTO> getBrokerTransactions(String brokerId) {

        List<Transaction> transactions = repo.findAllByBrokerId(brokerId);

        return transactions.stream()
                .map(EntityDtoUtil::toResponse)
                .toList();
    }

    @Override
    public TransactionResponseDTO getByTransactionId(String transactionId, String brokerId) {

        Transaction tx = repo.findByTransactionId(transactionId)
                .orElseThrow(() -> new NotFoundException("Transaction not found"));

        if (!tx.getBrokerId().equals(brokerId)) {
            throw new NotFoundException("You do not have access to this transaction");
        }

        return EntityDtoUtil.toResponse(tx);
    }
}
