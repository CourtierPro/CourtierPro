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

    private String lookupClientName(String clientId) {
        log.debug("lookupClientName: called with clientId={}", clientId);
        if (clientId == null) {
            log.debug("lookupClientName: clientId is null, returning 'Unknown Client'");
            return "Unknown Client";
        }

        // 1) Primary lookup by Auth0 id
        var byAuth0 = userAccountRepository.findByAuth0UserId(clientId);
        if (byAuth0.isPresent()) {
            UserAccount u = byAuth0.get();
            String f = u.getFirstName();
            String l = u.getLastName();
            log.debug("lookupClientName: found UserAccount for clientId={} firstName='{}' lastName='{}'", clientId, f, l);
            String name = ((f == null ? "" : f) + " " + (l == null ? "" : l)).trim();
            if (name.isEmpty()) name = "Unknown Client";
            log.debug("lookupClientName: returning '{}' for clientId={}", name, clientId);
            return name;
        }

        // 2) If clientId looks like a UUID, it might be the internal UserAccount id (primary key)
        try {
            UUID uuid = UUID.fromString(clientId);
            var byId = userAccountRepository.findById(uuid);
            if (byId.isPresent()) {
                UserAccount u = byId.get();
                String name = ((u.getFirstName() == null ? "" : u.getFirstName()) + " " + (u.getLastName() == null ? "" : u.getLastName())).trim();
                if (name.isEmpty()) name = "Unknown Client";
                log.debug("lookupClientName: found UserAccount by UUID id={} name='{}'", clientId, name);
                return name;
            }
        } catch (IllegalArgumentException ex) {
            // not a UUID - ignore
        }

        // 3) Try matching by email (some legacy rows may store email)
        var byEmail = userAccountRepository.findByEmail(clientId);
        if (byEmail.isPresent()) {
            UserAccount u = byEmail.get();
            String name = ((u.getFirstName() == null ? "" : u.getFirstName()) + " " + (u.getLastName() == null ? "" : u.getLastName())).trim();
            if (name.isEmpty()) name = "Unknown Client";
            log.debug("lookupClientName: found UserAccount by email='{}' name='{}'", clientId, name);
            return name;
        }

        // 4) Try auth0 id with/without common prefix variations
        if (!clientId.startsWith("auth0|")) {
            var withPrefix = userAccountRepository.findByAuth0UserId("auth0|" + clientId);
            if (withPrefix.isPresent()) {
                UserAccount u = withPrefix.get();
                String name = ((u.getFirstName() == null ? "" : u.getFirstName()) + " " + (u.getLastName() == null ? "" : u.getLastName())).trim();
                if (name.isEmpty()) name = "Unknown Client";
                log.debug("lookupClientName: found UserAccount by auth0-prefixed id for clientId={} -> name='{}'", clientId, name);
                return name;
            }
        } else {
            // if it starts with auth0|, try without prefix
            String stripped = clientId.substring(clientId.indexOf('|') + 1);
            var withoutPrefix = userAccountRepository.findByAuth0UserId(stripped);
            if (withoutPrefix.isPresent()) {
                UserAccount u = withoutPrefix.get();
                String name = ((u.getFirstName() == null ? "" : u.getFirstName()) + " " + (u.getLastName() == null ? "" : u.getLastName())).trim();
                if (name.isEmpty()) name = "Unknown Client";
                log.debug("lookupClientName: found UserAccount for stripped auth0 id={} -> name='{}'", stripped, name);
                return name;
            }
        }

        // 5) Fallbacks for legacy/non-Auth0 client identifiers (readable labels)
        if (clientId.toUpperCase().startsWith("CLI-") || clientId.matches("^[A-Z0-9_-]{3,30}$")) {
            String fallback = "Client " + clientId;
            log.debug("lookupClientName: no UserAccount found; using fallback='{}' for clientId={}", fallback, clientId);
            return fallback;
        }

        if (clientId.matches("^[0-9a-fA-F-]{8,36}$")) {
            String fallback = "Client " + clientId;
            log.debug("lookupClientName: no UserAccount found; clientId looks like UUID, using fallback='{}'", fallback);
            return fallback;
        }

        log.debug("lookupClientName: no UserAccount found for clientId={}; returning 'Unknown Client'", clientId);
        return "Unknown Client";
    }

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

        return EntityDtoUtil.toResponse(saved, lookupClientName(saved.getClientId()));
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
    public List<TransactionResponseDTO> getBrokerTransactions(String brokerId, String statusStr, String stageStr, String sideStr) {

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
    public List<TransactionResponseDTO> getClientTransactions(String clientId) {

        List<Transaction> transactions = repo.findAllByClientId(clientId);

        return transactions.stream()
                .map(tx -> EntityDtoUtil.toResponse(tx, lookupClientName(tx.getClientId())))
                .toList();
    }

    @Override
    public TransactionResponseDTO getByTransactionId(String transactionId, String userId) {

        Transaction tx = repo.findByTransactionId(transactionId)
                .orElseThrow(() -> new NotFoundException("Transaction not found"));

        // Allow access if the user is the broker OR the client
        boolean isBroker = tx.getBrokerId().equals(userId);
        boolean isClient = tx.getClientId().equals(userId);

        if (!isBroker && !isClient) {
            throw new NotFoundException("You do not have access to this transaction");
        }

        return EntityDtoUtil.toResponse(tx, lookupClientName(tx.getClientId()));
    }

    @Override
    public TransactionResponseDTO updateTransactionStage(String transactionId, StageUpdateRequestDTO dto, String brokerId) {

        if (dto == null) {
            throw new InvalidInputException("request body is required");
        }

        Transaction tx = repo.findByTransactionId(transactionId)
                .orElseThrow(() -> new NotFoundException("Transaction not found"));

        if (!tx.getBrokerId().equals(brokerId)) {
            throw new NotFoundException("You do not have access to this transaction");
        }

        String stageStr = dto.getStage();
        if (stageStr == null || stageStr.isBlank()) {
            throw new InvalidInputException("stage is required");
        }

        stageStr = stageStr.trim();

        // Validate and apply based on side
        if (tx.getSide() == TransactionSide.BUY_SIDE) {
            try {
                BuyerStage buyerStage = BuyerStage.valueOf(stageStr);
                EntityDtoUtil.updateBuyerStage(tx, buyerStage);
            } catch (IllegalArgumentException ex) {
                throw new InvalidInputException("stage '" + stageStr + "' is not a valid buyer stage. Allowed values: " + Arrays.toString(BuyerStage.values()));
            }
        } else if (tx.getSide() == TransactionSide.SELL_SIDE) {
            try {
                SellerStage sellerStage = SellerStage.valueOf(stageStr);
                EntityDtoUtil.updateSellerStage(tx, sellerStage);
            } catch (IllegalArgumentException ex) {
                throw new InvalidInputException("stage '" + stageStr + "' is not a valid seller stage. Allowed values: " + Arrays.toString(SellerStage.values()));
            }
        } else {
            throw new InvalidInputException("Unsupported transaction side: " + tx.getSide());
        }

        // Create timeline entry for stage change
        String stageName = stageStr;
        TimelineEntry entry = TimelineEntry.builder()
                .type(TimelineEntryType.STAGE_CHANGE)
                .title("Stage updated to " + stageName)
                .note(dto.getNote() != null && !dto.getNote().isBlank() ? dto.getNote() : "Stage moved to " + stageName)
                .visibleToClient(dto.getVisibleToClient() != null ? dto.getVisibleToClient() : true)
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
