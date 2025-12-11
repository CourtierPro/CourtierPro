package com.example.courtierprobackend.transactions.businesslayer;

import com.example.courtierprobackend.common.exceptions.BadRequestException;
import com.example.courtierprobackend.common.exceptions.NotFoundException;
import com.example.courtierprobackend.transactions.datalayer.TimelineEntry;
import com.example.courtierprobackend.transactions.datalayer.Transaction;
import com.example.courtierprobackend.transactions.datalayer.dto.TransactionRequestDTO;
import com.example.courtierprobackend.transactions.datalayer.dto.TransactionResponseDTO;
import com.example.courtierprobackend.transactions.datalayer.enums.*;
import com.example.courtierprobackend.transactions.datalayer.repositories.TransactionRepository;
import com.example.courtierprobackend.transactions.util.EntityDtoUtil;
import com.example.courtierprobackend.user.dataaccesslayer.UserAccount;
import com.example.courtierprobackend.user.dataaccesslayer.UserAccountRepository;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import com.example.courtierprobackend.transactions.datalayer.dto.NoteRequestDTO;
import com.example.courtierprobackend.transactions.datalayer.dto.TimelineEntryDTO;
import com.example.courtierprobackend.transactions.datalayer.enums.TimelineEntryType;
import com.example.courtierprobackend.transactions.datalayer.dto.StageUpdateRequestDTO;

@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {

    private static final Logger log = LoggerFactory.getLogger(TransactionServiceImpl.class);

    private final TransactionRepository repo;
    private final UserAccountRepository userAccountRepository;

    private String lookupClientName(UUID clientId) {
        log.debug("lookupClientName: called with clientId={}", clientId);
        if (clientId == null) {
            log.debug("lookupClientName: clientId is null, returning 'Unknown Client'");
            return "Unknown Client";
        }

        // Direct lookup by internal UUID - no more fallback lookups needed
        var byId = userAccountRepository.findById(clientId);
        if (byId.isPresent()) {
            UserAccount u = byId.get();
            String f = u.getFirstName();
            String l = u.getLastName();
            log.debug("lookupClientName: found UserAccount for clientId={} firstName='{}' lastName='{}'", clientId, f, l);
            String name = ((f == null ? "" : f) + " " + (l == null ? "" : l)).trim();
            if (name.isEmpty()) name = "Unknown Client";
            log.debug("lookupClientName: returning '{}' for clientId={}", name, clientId);
            return name;
        }

        log.debug("lookupClientName: no UserAccount found for clientId={}; returning 'Unknown Client'", clientId);
        return "Unknown Client";
    }

    @Override
    public TransactionResponseDTO createTransaction(TransactionRequestDTO dto) {

        // 1) Validate required data
        if (dto.getClientId() == null) {
            throw new BadRequestException("clientId is required");
        }
        if (dto.getBrokerId() == null) {
            throw new BadRequestException("brokerId is required");
        }
        if (dto.getSide() == null) {
            throw new BadRequestException("side is required");
        }
        if (dto.getPropertyAddress() == null ||
                dto.getPropertyAddress().getStreet() == null ||
                dto.getPropertyAddress().getStreet().isBlank()) {

            throw new BadRequestException("propertyAddress.street is required");
        }

        UUID clientId = dto.getClientId();
        UUID brokerId = dto.getBrokerId();
        String street = dto.getPropertyAddress().getStreet();

        // 2) Prevent duplicate ACTIVE transactions
        repo.findByClientIdAndPropertyAddress_StreetAndStatus(
                clientId,
                street,
                TransactionStatus.ACTIVE
        ).ifPresent(t -> {
            throw new BadRequestException("duplicate: Client already has an active transaction for this property");
        });

        // 3) Create Transaction entity
        Transaction tx = new Transaction();
        tx.setTransactionId(UUID.randomUUID());
        tx.setClientId(dto.getClientId());
        tx.setBrokerId(dto.getBrokerId());
        tx.setSide(dto.getSide());
        // Validate and apply initialStage based on side
        if (dto.getInitialStage() == null || dto.getInitialStage().isBlank()) {
            throw new BadRequestException("initialStage is required");
        }

        String initial = dto.getInitialStage().trim();
        if (dto.getSide() == TransactionSide.BUY_SIDE) {
            try {
                BuyerStage buyerStage = BuyerStage.valueOf(initial);
                tx.setBuyerStage(buyerStage);
                tx.setSellerStage(null);
            } catch (IllegalArgumentException ex) {
                throw new BadRequestException("initialStage '" + initial + "' is not a valid buyer stage. Allowed values: " + Arrays.toString(BuyerStage.values()));
            }
        } else if (dto.getSide() == TransactionSide.SELL_SIDE) {
            try {
                SellerStage sellerStage = SellerStage.valueOf(initial);
                tx.setSellerStage(sellerStage);
                tx.setBuyerStage(null);
            } catch (IllegalArgumentException ex) {
                throw new BadRequestException("initialStage '" + initial + "' is not a valid seller stage. Allowed values: " + Arrays.toString(SellerStage.values()));
            }
        } else {
            throw new BadRequestException("side is not supported: " + dto.getSide());
        }
        tx.setStatus(TransactionStatus.ACTIVE);
        tx.setOpenedAt(LocalDateTime.now());
        tx.setPropertyAddress(dto.getPropertyAddress());

        Transaction saved = repo.save(tx);

        return EntityDtoUtil.toResponse(saved, lookupClientName(saved.getClientId()));
    }

    @Override
    public java.util.List<TimelineEntryDTO> getNotes(UUID transactionId, UUID brokerId) {
        Transaction tx = repo.findByTransactionId(transactionId)
                .orElseThrow(() -> new NotFoundException("Transaction not found"));

        TransactionAccessUtils.verifyBrokerAccess(tx, brokerId);

        List<TimelineEntry> entries = tx.getTimeline() == null ? List.of() : tx.getTimeline();

        return entries.stream()
                .filter(e -> e.getType() == TimelineEntryType.NOTE)
                .map(EntityDtoUtil::toTimelineDTO)
                .toList();
    }

    @Override
    public TimelineEntryDTO createNote(UUID transactionId, NoteRequestDTO note, UUID brokerId) {
        if (note.getActorId() == null) {
            throw new BadRequestException("actorId is required");
        }
        if (note.getTitle() == null || note.getTitle().isBlank()) {
            throw new BadRequestException("title is required");
        }
        if (note.getMessage() == null || note.getMessage().isBlank()) {
            throw new BadRequestException("message is required");
        }

        Transaction tx = repo.findByTransactionId(transactionId)
                .orElseThrow(() -> new NotFoundException("Transaction not found"));

        TransactionAccessUtils.verifyBrokerAccess(tx, brokerId);

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
    public List<TransactionResponseDTO> getBrokerTransactions(UUID brokerId, String statusStr, String stageStr, String sideStr) {

        TransactionStatus status = null;
        if (statusStr != null && !statusStr.isBlank() && !statusStr.equalsIgnoreCase("all")) {
            try {
                status = TransactionStatus.valueOf(statusStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                // ignore invalid status, treat as null (all)
            }
        }

        TransactionSide side = null;
        if (sideStr != null && !sideStr.isBlank() && !sideStr.equalsIgnoreCase("all")) {
            try {
                side = TransactionSide.valueOf(sideStr.toUpperCase() + "_SIDE"); // Frontend sends "buy"/"sell", enum is BUY_SIDE/SELL_SIDE
                if (sideStr.equalsIgnoreCase("buy")) side = TransactionSide.BUY_SIDE;
                if (sideStr.equalsIgnoreCase("sell")) side = TransactionSide.SELL_SIDE;
            } catch (IllegalArgumentException e) {
                 // try direct match
                 try {
                     side = TransactionSide.valueOf(sideStr.toUpperCase());
                 } catch (IllegalArgumentException ex) {
                     // ignore
                 }
            }
        }

        Enum<?> stage = null;
        if (stageStr != null && !stageStr.isBlank() && !stageStr.equalsIgnoreCase("all")) {
            // Try BuyerStage
            try {
                stage = BuyerStage.valueOf(stageStr);
            } catch (IllegalArgumentException e) {
                // Try SellerStage
                try {
                    stage = SellerStage.valueOf(stageStr);
                } catch (IllegalArgumentException ex) {
                    // ignore
                }
            }
        }

        List<Transaction> transactions = repo.findAllByFilters(brokerId, status, side, stage);

        return transactions.stream()
                .map(tx -> EntityDtoUtil.toResponse(tx, lookupClientName(tx.getClientId())))
                .toList();
    }

    @Override
    public List<TransactionResponseDTO> getClientTransactions(UUID clientId) {

        List<Transaction> transactions = repo.findAllByClientId(clientId);

        return transactions.stream()
                .map(tx -> EntityDtoUtil.toResponse(tx, lookupClientName(tx.getClientId())))
                .toList();
    }

    @Override
    public TransactionResponseDTO getByTransactionId(UUID transactionId, UUID userId) {

        Transaction tx = repo.findByTransactionId(transactionId)
                .orElseThrow(() -> new NotFoundException("Transaction not found"));

        // Allow access if the user is the broker OR the client
        TransactionAccessUtils.verifyTransactionAccess(tx, userId);

        return EntityDtoUtil.toResponse(tx, lookupClientName(tx.getClientId()));
    }

    @Override
    public TransactionResponseDTO updateTransactionStage(UUID transactionId, StageUpdateRequestDTO dto, UUID brokerId) {

        if (dto == null) {
            throw new BadRequestException("request body is required");
        }

        Transaction tx = repo.findByTransactionId(transactionId)
                .orElseThrow(() -> new NotFoundException("Transaction not found"));

        TransactionAccessUtils.verifyBrokerAccess(tx, brokerId);

        String stageStr = dto.getStage();
        if (stageStr == null || stageStr.isBlank()) {
            throw new BadRequestException("stage is required");
        }

        stageStr = stageStr.trim();

        // Validate and apply based on side
        if (tx.getSide() == TransactionSide.BUY_SIDE) {
            try {
                BuyerStage buyerStage = BuyerStage.valueOf(stageStr);
                EntityDtoUtil.updateBuyerStage(tx, buyerStage);
            } catch (IllegalArgumentException ex) {
                throw new BadRequestException("stage '" + stageStr + "' is not a valid buyer stage. Allowed values: " + Arrays.toString(BuyerStage.values()));
            }
        } else if (tx.getSide() == TransactionSide.SELL_SIDE) {
            try {
                SellerStage sellerStage = SellerStage.valueOf(stageStr);
                EntityDtoUtil.updateSellerStage(tx, sellerStage);
            } catch (IllegalArgumentException ex) {
                throw new BadRequestException("stage '" + stageStr + "' is not a valid seller stage. Allowed values: " + Arrays.toString(SellerStage.values()));
            }
        } else {
            throw new BadRequestException("Unsupported transaction side: " + tx.getSide());
        }

        // Create timeline entry for stage change
        String stageName = stageStr;
        TimelineEntry entry = TimelineEntry.builder()
                .type(TimelineEntryType.STAGE_CHANGE)
                .title("Stage updated to " + stageName)
                .note(dto.getNote() != null && !dto.getNote().isBlank() ? dto.getNote() : "Stage moved to " + stageName)
                .visibleToClient(true)
                .occurredAt(LocalDateTime.now())
                .addedByBrokerId(brokerId)
                .transaction(tx)
                .build();

        if (tx.getTimeline() == null) tx.setTimeline(new ArrayList<>());
        tx.getTimeline().add(entry);

        Transaction saved = repo.save(tx);

        return EntityDtoUtil.toResponse(saved, lookupClientName(saved.getClientId()));
    }
}
