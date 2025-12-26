
package com.example.courtierprobackend.transactions.businesslayer;

import com.example.courtierprobackend.audit.timeline_audit.presentationlayer.TimelineEntryDTO;
import com.example.courtierprobackend.audit.timeline_audit.dataaccesslayer.Enum.TimelineEntryType;
import com.example.courtierprobackend.audit.timeline_audit.businesslayer.TimelineService;
import com.example.courtierprobackend.audit.timeline_audit.dataaccesslayer.value_object.TransactionInfo;
import com.example.courtierprobackend.common.exceptions.BadRequestException;
import com.example.courtierprobackend.common.exceptions.NotFoundException;
import com.example.courtierprobackend.common.exceptions.ConflictException;
import org.springframework.dao.OptimisticLockingFailureException;
import com.example.courtierprobackend.shared.utils.StageTranslationUtil;
import com.example.courtierprobackend.transactions.datalayer.PinnedTransaction;
import com.example.courtierprobackend.transactions.datalayer.Transaction;
import com.example.courtierprobackend.transactions.datalayer.dto.TransactionRequestDTO;
import com.example.courtierprobackend.transactions.datalayer.dto.TransactionResponseDTO;
import com.example.courtierprobackend.transactions.datalayer.enums.*;
import com.example.courtierprobackend.transactions.datalayer.repositories.PinnedTransactionRepository;
import com.example.courtierprobackend.transactions.datalayer.repositories.TransactionRepository;
import com.example.courtierprobackend.transactions.util.EntityDtoUtil;
import com.example.courtierprobackend.user.dataaccesslayer.UserAccount;
import com.example.courtierprobackend.user.dataaccesslayer.UserAccountRepository;

import com.example.courtierprobackend.email.EmailService;
import com.example.courtierprobackend.notifications.businesslayer.NotificationService;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import com.example.courtierprobackend.transactions.datalayer.dto.NoteRequestDTO;
import com.example.courtierprobackend.transactions.datalayer.dto.StageUpdateRequestDTO;
import com.example.courtierprobackend.transactions.datalayer.dto.AddParticipantRequestDTO;
import com.example.courtierprobackend.transactions.datalayer.dto.ParticipantResponseDTO;
import com.example.courtierprobackend.transactions.datalayer.repositories.TransactionParticipantRepository;
import com.example.courtierprobackend.transactions.datalayer.TransactionParticipant;

@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {

    @Override
    public void saveInternalNotes(UUID transactionId, String notes, UUID brokerId) {
        // Optionally: persist notes on the transaction entity if needed (not required
        // per user)
        // Always: create a timeline NOTE event
        if (notes != null && !notes.isBlank()) {
            timelineService.addEntry(
                    transactionId,
                    brokerId,
                    TimelineEntryType.NOTE,
                    notes,
                    null);
        }
    }

    private static final Logger log = LoggerFactory.getLogger(TransactionServiceImpl.class);

    private final TransactionRepository repo;
    private final PinnedTransactionRepository pinnedTransactionRepository;
    private final UserAccountRepository userAccountRepository;
    private final EmailService emailService;
    private final NotificationService notificationService;
    private final TimelineService timelineService;
    private final TransactionParticipantRepository participantRepository;

    private String lookupClientName(UUID clientId) {
        if (clientId == null) {
            return "Unknown Client";
        }
        var byId = userAccountRepository.findById(clientId);
        if (byId.isPresent()) {
            UserAccount u = byId.get();
            String f = u.getFirstName();
            String l = u.getLastName();
            log.debug("lookupClientName: found UserAccount for clientId={} firstName='{}' lastName='{}'", clientId, f,
                    l);
            String name = ((f == null ? "" : f) + " " + (l == null ? "" : l)).trim();
            if (name.isEmpty())
                name = "Unknown Client";
            log.debug("lookupClientName: returning '{}' for clientId={}", name, clientId);
            return name;
        }
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
                TransactionStatus.ACTIVE).ifPresent(t -> {
                    throw new BadRequestException(
                            "duplicate: Client already has an active transaction for this property");
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
                throw new BadRequestException("initialStage '" + initial
                        + "' is not a valid buyer stage. Allowed values: " + Arrays.toString(BuyerStage.values()));
            }
        } else if (dto.getSide() == TransactionSide.SELL_SIDE) {
            try {
                SellerStage sellerStage = SellerStage.valueOf(initial);
                tx.setSellerStage(sellerStage);
                tx.setBuyerStage(null);
            } catch (IllegalArgumentException ex) {
                throw new BadRequestException("initialStage '" + initial
                        + "' is not a valid seller stage. Allowed values: " + Arrays.toString(SellerStage.values()));
            }
        } else {
            throw new BadRequestException("side is not supported: " + dto.getSide());
        }
        tx.setStatus(TransactionStatus.ACTIVE);
        tx.setOpenedAt(LocalDateTime.now());
        tx.setPropertyAddress(dto.getPropertyAddress());

        Transaction saved = repo.save(tx);

        // Create timeline entry for transaction creation
        String clientName = lookupClientName(saved.getClientId());
        String property = saved.getPropertyAddress() != null ? saved.getPropertyAddress().getStreet() : "";
        String actorName = lookupClientName(saved.getBrokerId());
        TransactionInfo info = TransactionInfo.builder()
                .clientName(clientName)
                .address(property)
                .actorName(actorName)
                .build();
        timelineService.addEntry(
                saved.getTransactionId(),
                brokerId,
                TimelineEntryType.CREATED,
                null,
                null,
                info);

        // CP-48: Send Welcome Notification to Client
        try {
            String title = "Welcome to CourtierPro!";
            String message = String.format("A new transaction for %s has been created for you by %s.",
                    street, actorName);
            notificationService.createNotification(
                    clientId.toString(),
                    title,
                    message,
                    saved.getTransactionId().toString(),
                    com.example.courtierprobackend.notifications.datalayer.enums.NotificationCategory.WELCOME);
        } catch (Exception e) {
            log.error("Failed to send welcome notification for transaction {}", saved.getTransactionId(), e);
        }

        return EntityDtoUtil.toResponse(saved, lookupClientName(saved.getClientId()));
    }

    @Override
    public List<TimelineEntryDTO> getNotes(UUID transactionId, UUID brokerId) {
        // 1. Vérifier que la transaction existe
        Transaction tx = repo.findByTransactionId(transactionId)
                .orElseThrow(() -> new NotFoundException("Transaction not found"));

        // 2. Vérifier l'accès du broker
        TransactionAccessUtils.verifyBrokerAccess(tx, brokerId);

        // 3. Récupérer les notes
        List<TimelineEntryDTO> entries = timelineService.getTimelineForTransaction(transactionId);
        return entries.stream()
                .filter(e -> e.getType() == TimelineEntryType.NOTE)
                .toList();
    }

    @Override
    public TimelineEntryDTO createNote(UUID transactionId, NoteRequestDTO note, UUID brokerId) {
        if (note.getTitle() == null || note.getTitle().isBlank()) {
            throw new BadRequestException("title is required");
        }
        if (note.getMessage() == null || note.getMessage().isBlank()) {
            throw new BadRequestException("message is required");
        }
        // Optionally add access checks here
        timelineService.addEntry(
                transactionId,
                brokerId,
                TimelineEntryType.NOTE,
                note.getTitle() + ": " + note.getMessage(),
                null);
        // Return the last note entry for this transaction as DTO
        List<TimelineEntryDTO> entries = timelineService.getTimelineForTransaction(transactionId);
        List<TimelineEntryDTO> notes = entries.stream()
                .filter(e -> e.getType() == TimelineEntryType.NOTE)
                .toList();
        if (notes.isEmpty())
            return null;
        return notes.get(notes.size() - 1);
    }

    @Override
    public List<TransactionResponseDTO> getBrokerTransactions(UUID brokerId, String statusStr, String stageStr,
            String sideStr) {

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
                side = TransactionSide.valueOf(sideStr.toUpperCase() + "_SIDE"); // Frontend sends "buy"/"sell", enum is
                                                                                 // BUY_SIDE/SELL_SIDE
                if (sideStr.equalsIgnoreCase("buy"))
                    side = TransactionSide.BUY_SIDE;
                if (sideStr.equalsIgnoreCase("sell"))
                    side = TransactionSide.SELL_SIDE;
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
    @Transactional
    public TransactionResponseDTO updateTransactionStage(UUID transactionId, StageUpdateRequestDTO dto, UUID brokerId) {

        if (dto == null) {
            throw new BadRequestException("request body is required");
        }

        Transaction tx = repo.findByTransactionId(transactionId)
                .orElseThrow(() -> new NotFoundException("Transaction not found"));

        TransactionAccessUtils.verifyBrokerAccess(tx, brokerId);

        // Guard Clause: Cannot update stage if transaction is already closed or
        // terminated
        if (tx.getStatus() == TransactionStatus.CLOSED_SUCCESSFULLY ||
                tx.getStatus() == TransactionStatus.TERMINATED_EARLY) {
            throw new BadRequestException("Cannot update stage of a closed or terminated transaction.");
        }

        String stageStr = dto.getStage();
        if (stageStr == null || stageStr.isBlank()) {
            throw new BadRequestException("stage is required");
        }

        stageStr = stageStr.trim();

        // Capture previous stage BEFORE update
        String previousStage = null;
        if (tx.getSide() == TransactionSide.BUY_SIDE && tx.getBuyerStage() != null) {
            previousStage = tx.getBuyerStage().name();
        } else if (tx.getSide() == TransactionSide.SELL_SIDE && tx.getSellerStage() != null) {
            previousStage = tx.getSellerStage().name();
        }

        // Validate and apply based on side
        if (tx.getSide() == TransactionSide.BUY_SIDE) {
            try {
                BuyerStage buyerStage = BuyerStage.valueOf(stageStr);
                EntityDtoUtil.updateBuyerStage(tx, buyerStage);
            } catch (IllegalArgumentException ex) {
                throw new BadRequestException("stage '" + stageStr + "' is not a valid buyer stage. Allowed values: "
                        + Arrays.toString(BuyerStage.values()));
            }
        } else if (tx.getSide() == TransactionSide.SELL_SIDE) {
            try {
                SellerStage sellerStage = SellerStage.valueOf(stageStr);
                EntityDtoUtil.updateSellerStage(tx, sellerStage);
            } catch (IllegalArgumentException ex) {
                throw new BadRequestException("stage '" + stageStr + "' is not a valid seller stage. Allowed values: "
                        + Arrays.toString(SellerStage.values()));
            }
        } else {
            throw new BadRequestException("Unsupported transaction side: " + tx.getSide());
        }

        // Auto-Closing Logic
        if (tx.getSide() == TransactionSide.BUY_SIDE) {
            if (tx.getBuyerStage() == BuyerStage.BUYER_OCCUPANCY) {
                tx.setStatus(TransactionStatus.CLOSED_SUCCESSFULLY);
                tx.setClosedAt(LocalDateTime.now());
            } else if (tx.getBuyerStage() == BuyerStage.BUYER_TERMINATED) {
                tx.setStatus(TransactionStatus.TERMINATED_EARLY);
                tx.setClosedAt(LocalDateTime.now());
            }
        } else if (tx.getSide() == TransactionSide.SELL_SIDE) {
            if (tx.getSellerStage() == SellerStage.SELLER_HANDOVER_KEYS) {
                tx.setStatus(TransactionStatus.CLOSED_SUCCESSFULLY);
                tx.setClosedAt(LocalDateTime.now());
            } else if (tx.getSellerStage() == SellerStage.SELLER_TERMINATED) {
                tx.setStatus(TransactionStatus.TERMINATED_EARLY);
                tx.setClosedAt(LocalDateTime.now());
            }
        }

        // Log explicit status change if it happened
        if (tx.getStatus() == TransactionStatus.CLOSED_SUCCESSFULLY ||
                tx.getStatus() == TransactionStatus.TERMINATED_EARLY) {
            timelineService.addEntry(
                    transactionId,
                    brokerId,
                    TimelineEntryType.STATUS_CHANGE,
                    "Transaction automatically updated to " + tx.getStatus(),
                    null);
        }

        String stageChangeActorName = lookupClientName(tx.getBrokerId());
        TransactionInfo stageChangeInfo = TransactionInfo.builder()
                .previousStage(previousStage)
                .newStage(stageStr)
                .stage(stageStr) // pour compatibilité
                .actorName(stageChangeActorName)
                .build();
        timelineService.addEntry(
                transactionId,
                brokerId,
                TimelineEntryType.STAGE_CHANGE,
                null,
                null,
                stageChangeInfo);

        Transaction saved;
        try {
            saved = repo.save(tx);
        } catch (OptimisticLockingFailureException e) {
            throw new ConflictException("The transaction was updated by another user. Please refresh and try again.",
                    e);
        }

        // CP-48: Send Notifications and Emails
        try {
            // Fetch Client details
            Optional<UserAccount> clientOpt = userAccountRepository.findById(saved.getClientId());
            Optional<UserAccount> brokerOpt = userAccountRepository.findById(brokerId);

            if (clientOpt.isPresent() && brokerOpt.isPresent()) {
                UserAccount client = clientOpt.get();
                UserAccount broker = brokerOpt.get();

                String clientName = (client.getFirstName() + " " + client.getLastName()).trim();
                String brokerName = (broker.getFirstName() + " " + broker.getLastName()).trim();

                String address = "Unknown Address";
                if (saved.getPropertyAddress() != null && saved.getPropertyAddress().getStreet() != null) {
                    address = saved.getPropertyAddress().getStreet();
                }

                // 1. Email
                emailService.sendStageUpdateEmail(
                        client.getEmail(),
                        clientName,
                        brokerName,
                        address,
                        stageStr,
                        client.getPreferredLanguage());

                // 2. In-App Notification
                String notifTitle = "Stage Update";
                String notifMessage = StageTranslationUtil.constructNotificationMessage(
                        stageStr,
                        brokerName,
                        address,
                        client.getPreferredLanguage());

                notificationService.createNotification(
                        client.getId().toString(), // Internal UUID
                        notifTitle,
                        notifMessage,
                        saved.getTransactionId().toString(),
                        com.example.courtierprobackend.notifications.datalayer.enums.NotificationCategory.STAGE_UPDATE);

            } else {
                log.warn("Could not send notifications for transaction {}: Client or Broker not found",
                        saved.getTransactionId());
            }

        } catch (Exception e) {
            log.error("Failed to send notifications/emails for transaction {}", saved.getTransactionId(), e);
            // Do not rethrow, transaction is already saved
        }

        return EntityDtoUtil.toResponse(saved, lookupClientName(saved.getClientId()));
    }

    @Override
    @Transactional
    public void pinTransaction(UUID transactionId, UUID brokerId) {
        Transaction tx = repo.findByTransactionId(transactionId)
                .orElseThrow(() -> new NotFoundException("Transaction not found"));

        TransactionAccessUtils.verifyBrokerAccess(tx, brokerId);

        // Check if already pinned
        if (pinnedTransactionRepository.existsByBrokerIdAndTransactionId(brokerId, transactionId)) {
            return; // Already pinned, no-op
        }

        PinnedTransaction pin = PinnedTransaction.builder()
                .brokerId(brokerId)
                .transactionId(transactionId)
                .pinnedAt(LocalDateTime.now())
                .build();

        pinnedTransactionRepository.save(pin);
    }

    @Override
    @Transactional
    public void unpinTransaction(UUID transactionId, UUID brokerId) {
        Transaction tx = repo.findByTransactionId(transactionId)
                .orElseThrow(() -> new NotFoundException("Transaction not found"));

        TransactionAccessUtils.verifyBrokerAccess(tx, brokerId);

        pinnedTransactionRepository.deleteByBrokerIdAndTransactionId(brokerId, transactionId);
    }

    @Override
    public Set<UUID> getPinnedTransactionIds(UUID brokerId) {
        return pinnedTransactionRepository.findAllByBrokerId(brokerId).stream()
                .map(PinnedTransaction::getTransactionId)
                .collect(Collectors.toSet());
    }

    @Override
    public ParticipantResponseDTO addParticipant(UUID transactionId, AddParticipantRequestDTO dto, UUID brokerId) {
        Transaction tx = repo.findByTransactionId(transactionId)
                .orElseThrow(() -> new NotFoundException("Transaction not found"));

        TransactionAccessUtils.verifyBrokerAccess(tx, brokerId);

        TransactionParticipant participant = TransactionParticipant.builder()
                .transactionId(transactionId)
                .name(dto.getName())
                .role(dto.getRole())
                .email(dto.getEmail())
                .phoneNumber(dto.getPhoneNumber())
                .build();

        TransactionParticipant saved = participantRepository.save(participant);

        return ParticipantResponseDTO.builder()
                .id(saved.getId())
                .transactionId(saved.getTransactionId())
                .name(saved.getName())
                .role(saved.getRole())
                .email(saved.getEmail())
                .phoneNumber(saved.getPhoneNumber())
                .build();
    }

    @Override
    public void removeParticipant(UUID transactionId, UUID participantId, UUID brokerId) {
        Transaction tx = repo.findByTransactionId(transactionId)
                .orElseThrow(() -> new NotFoundException("Transaction not found"));

        TransactionAccessUtils.verifyBrokerAccess(tx, brokerId);

        TransactionParticipant participant = participantRepository.findById(participantId)
                .orElseThrow(() -> new NotFoundException("Participant not found"));

        if (!participant.getTransactionId().equals(transactionId)) {
            throw new BadRequestException("Participant does not belong to this transaction");
        }

        participantRepository.delete(participant);
    }

    @Override
    public List<ParticipantResponseDTO> getParticipants(UUID transactionId, UUID userId) {
        Transaction tx = repo.findByTransactionId(transactionId)
                .orElseThrow(() -> new NotFoundException("Transaction not found"));

        TransactionAccessUtils.verifyTransactionAccess(tx, userId);

        return participantRepository.findByTransactionId(transactionId).stream()
                .map(p -> ParticipantResponseDTO.builder()
                        .id(p.getId())
                        .transactionId(p.getTransactionId())
                        .name(p.getName())
                        .role(p.getRole())
                        .email(p.getEmail())
                        .phoneNumber(p.getPhoneNumber())
                        .build())
                .toList();
    }
}
