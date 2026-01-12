
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
import com.example.courtierprobackend.shared.utils.PostalCodeUtil;
import com.example.courtierprobackend.transactions.datalayer.PinnedTransaction;
import com.example.courtierprobackend.transactions.datalayer.Transaction;
import com.example.courtierprobackend.transactions.datalayer.dto.TransactionRequestDTO;
import com.example.courtierprobackend.transactions.datalayer.dto.TransactionResponseDTO;
import com.example.courtierprobackend.transactions.datalayer.enums.BuyerStage;
import com.example.courtierprobackend.transactions.datalayer.enums.PropertyOfferStatus;
import com.example.courtierprobackend.transactions.datalayer.enums.ReceivedOfferStatus;
import com.example.courtierprobackend.transactions.datalayer.enums.SellerStage;
import com.example.courtierprobackend.transactions.datalayer.enums.TransactionSide;
import com.example.courtierprobackend.transactions.datalayer.enums.TransactionStatus;
import com.example.courtierprobackend.transactions.datalayer.repositories.PinnedTransactionRepository;
import com.example.courtierprobackend.transactions.datalayer.repositories.TransactionRepository;
import com.example.courtierprobackend.transactions.datalayer.PropertyAddress;
import com.example.courtierprobackend.transactions.datalayer.dto.PropertyAddressDTO;
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
import com.example.courtierprobackend.transactions.datalayer.repositories.PropertyRepository;
import com.example.courtierprobackend.transactions.datalayer.repositories.OfferRepository;
import com.example.courtierprobackend.transactions.datalayer.TransactionParticipant;
import com.example.courtierprobackend.transactions.datalayer.Property;
import com.example.courtierprobackend.transactions.datalayer.Offer;
import com.example.courtierprobackend.transactions.datalayer.dto.PropertyRequestDTO;
import com.example.courtierprobackend.transactions.datalayer.dto.PropertyResponseDTO;
import com.example.courtierprobackend.transactions.datalayer.dto.OfferRequestDTO;
import com.example.courtierprobackend.transactions.datalayer.dto.OfferResponseDTO;
import com.example.courtierprobackend.transactions.datalayer.Condition;
import com.example.courtierprobackend.transactions.datalayer.repositories.ConditionRepository;
import com.example.courtierprobackend.transactions.datalayer.dto.ConditionRequestDTO;
import com.example.courtierprobackend.transactions.datalayer.dto.ConditionResponseDTO;
import com.example.courtierprobackend.transactions.datalayer.enums.ConditionStatus;
import com.example.courtierprobackend.transactions.datalayer.enums.ConditionType;

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
    private final PropertyRepository propertyRepository;
    private final OfferRepository offerRepository;
    private final ConditionRepository conditionRepository;


    private String lookupUserName(UUID userId) {
        if (userId == null) {
            return "Unknown User";
        }
        var byId = userAccountRepository.findById(userId);
        if (byId.isPresent()) {
            UserAccount u = byId.get();
            String f = u.getFirstName();
            String l = u.getLastName();
            log.debug("lookupUserName: found UserAccount for userId={} firstName='{}' lastName='{}'", userId, f,
                    l);
            String name = ((f == null ? "" : f) + " " + (l == null ? "" : l)).trim();
            if (name.isEmpty())
                name = "Unknown User";
            log.debug("lookupUserName: returning '{}' for userId={}", name, userId);
            return name;
        }
        return "Unknown User";
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
        // Validate property address (required for SELL_SIDE, optional for BUY_SIDE)
        if (dto.getSide() == TransactionSide.SELL_SIDE) {
            if (dto.getPropertyAddress() == null ||
                    dto.getPropertyAddress().getStreet() == null ||
                    dto.getPropertyAddress().getStreet().isBlank()) {
                throw new BadRequestException("propertyAddress.street is required for SELL_SIDE transactions");
            }
        }

        UUID clientId = dto.getClientId();
        UUID brokerId = dto.getBrokerId();
        String street = dto.getPropertyAddress().getStreet();

        // 2) Prevent duplicate ACTIVE transactions
        // 2) Prevent duplicate ACTIVE transactions (only if address is provided)
        if (street != null && !street.isBlank()) {
            repo.findByClientIdAndPropertyAddress_StreetAndStatus(
                    clientId,
                    street,
                    TransactionStatus.ACTIVE).ifPresent(t -> {
                        throw new BadRequestException(
                                "duplicate: Client already has an active transaction for this property");
                    });
        }

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
        
        // Normalize postal code before saving
        var address = dto.getPropertyAddress();
        if (address != null && address.getPostalCode() != null) {
            address.setPostalCode(PostalCodeUtil.normalize(address.getPostalCode()));
        }
        tx.setPropertyAddress(address);

        Transaction saved = repo.save(tx);

        // Create timeline entry for transaction creation
        String clientName = lookupUserName(saved.getClientId());
        String property = saved.getPropertyAddress() != null ? saved.getPropertyAddress().getStreet() : "";
        String actorName = lookupUserName(saved.getBrokerId());
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

        // CP-48: Send Transaction Created Notification to Client
        try {
            notificationService.createNotification(
                    clientId.toString(),
                    "notifications.transactionCreated.title",
                    "notifications.transactionCreated.message",
                    java.util.Map.of("brokerName", actorName),
                    saved.getTransactionId().toString(),
                    com.example.courtierprobackend.notifications.datalayer.enums.NotificationCategory.GENERAL);
        } catch (Exception e) {
            log.error("Failed to send transaction created notification for transaction {}", saved.getTransactionId(), e);
        }

        return EntityDtoUtil.toResponse(saved, lookupUserName(saved.getClientId()));
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
                .map(tx -> EntityDtoUtil.toResponse(tx, lookupUserName(tx.getClientId())))
                .toList();
    }

    @Override
    public List<TransactionResponseDTO> getClientTransactions(UUID clientId) {

        List<Transaction> transactions = repo.findAllByClientId(clientId);

        return transactions.stream()
                .map(tx -> EntityDtoUtil.toResponse(tx, lookupUserName(tx.getClientId())))
                .toList();
    }

    @Override
    public List<TransactionResponseDTO> getBrokerClientTransactions(UUID brokerId, UUID clientId) {
        // Get all transactions for this client where this broker is the broker
        List<Transaction> transactions = repo.findAllByClientId(clientId).stream()
                .filter(tx -> brokerId.equals(tx.getBrokerId()))
                .toList();

        return transactions.stream()
                .map(tx -> EntityDtoUtil.toResponse(tx, lookupUserName(tx.getClientId())))
                .toList();
    }

    @Override
    public List<TransactionResponseDTO> getAllClientTransactions(UUID clientId) {
        // Get ALL transactions for this client (across all brokers)
        List<Transaction> transactions = repo.findAllByClientId(clientId);

        String clientName = lookupUserName(clientId);

        return transactions.stream()
                .map(tx -> EntityDtoUtil.toResponse(
                        tx, 
                        clientName, 
                        tx.getCentrisNumber(),
                        lookupUserName(tx.getBrokerId())
                ))
                .toList();
    }

    @Override
    public TransactionResponseDTO getByTransactionId(UUID transactionId, UUID userId) {

        Transaction tx = repo.findByTransactionId(transactionId)
                .orElseThrow(() -> new NotFoundException("Transaction not found"));

        // Allow access if the user is the broker OR the client
        TransactionAccessUtils.verifyTransactionAccess(tx, userId);

        // For buy-side transactions, get centris number from accepted property
        String centrisNumber = tx.getCentrisNumber();
        if (tx.getSide() == com.example.courtierprobackend.transactions.datalayer.enums.TransactionSide.BUY_SIDE) {
            var acceptedProperty = propertyRepository.findByTransactionIdOrderByCreatedAtDesc(transactionId).stream()
                    .filter(p -> p.getOfferStatus() == com.example.courtierprobackend.transactions.datalayer.enums.PropertyOfferStatus.ACCEPTED)
                    .findFirst();
            if (acceptedProperty.isPresent() && acceptedProperty.get().getCentrisNumber() != null) {
                centrisNumber = acceptedProperty.get().getCentrisNumber();
            }
        }

        return EntityDtoUtil.toResponse(tx, lookupUserName(tx.getClientId()), centrisNumber);
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

        String stageChangeActorName = lookupUserName(tx.getBrokerId());
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

        return EntityDtoUtil.toResponse(saved, lookupUserName(saved.getClientId()));
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

    // ==================== PROPERTY METHODS ====================

    @Override
    public List<PropertyResponseDTO> getProperties(UUID transactionId, UUID userId, boolean isBroker) {
        Transaction tx = repo.findByTransactionId(transactionId)
                .orElseThrow(() -> new NotFoundException("Transaction not found"));

        TransactionAccessUtils.verifyTransactionAccess(tx, userId);

        // Only BUY_SIDE transactions can have multiple properties
        if (tx.getSide() != TransactionSide.BUY_SIDE) {
            return List.of();
        }

        List<Property> properties = propertyRepository.findByTransactionIdOrderByCreatedAtDesc(transactionId);

        return properties.stream()
                .map(p -> toPropertyResponseDTO(p, isBroker))
                .toList();
    }

    @Override
    @Transactional
    public PropertyResponseDTO addProperty(UUID transactionId, PropertyRequestDTO dto, UUID brokerId) {
        Transaction tx = repo.findByTransactionId(transactionId)
                .orElseThrow(() -> new NotFoundException("Transaction not found"));

        TransactionAccessUtils.verifyBrokerAccess(tx, brokerId);

        // Only BUY_SIDE transactions can have multiple properties
        if (tx.getSide() != TransactionSide.BUY_SIDE) {
            throw new BadRequestException("Properties can only be added to buyer-side transactions");
        }

        // Normalize postal code in address before saving
        var propertyAddress = dto.getAddress();
        if (propertyAddress != null && propertyAddress.getPostalCode() != null) {
            propertyAddress.setPostalCode(PostalCodeUtil.normalize(propertyAddress.getPostalCode()));
        }
        
        Property property = Property.builder()
                .propertyId(UUID.randomUUID())
                .transactionId(transactionId)
                .address(propertyAddress)
                .askingPrice(dto.getAskingPrice())
                .offerStatus(dto.getOfferStatus() != null ? dto.getOfferStatus() : PropertyOfferStatus.OFFER_TO_BE_MADE)
                .offerAmount(dto.getOfferAmount())
                .centrisNumber(dto.getCentrisNumber())
                .notes(dto.getNotes())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Property saved = propertyRepository.save(property);

        // Add timeline entry for property addition
        String address = saved.getAddress() != null ? saved.getAddress().getStreet() : "Unknown";
        String actorName = lookupUserName(brokerId);
        TransactionInfo info = TransactionInfo.builder()
                .actorName(actorName)
                .address(address)
                .build();
        timelineService.addEntry(
                transactionId,
                brokerId,
                TimelineEntryType.PROPERTY_ADDED,
                "Property added: " + address,
                null,
                info);

        // Notify client about new property
        try {
            notificationService.createNotification(
                    tx.getClientId().toString(),
                    "notifications.propertyAdded.title",
                    "notifications.propertyAdded.message",
                    java.util.Map.of("address", address),
                    transactionId.toString(),
                    com.example.courtierprobackend.notifications.datalayer.enums.NotificationCategory.PROPERTY_ADDED);
        } catch (Exception e) {
            log.error("Failed to send property notification for transaction {}", transactionId, e);
        }

        return toPropertyResponseDTO(saved, true);
    }

    @Override
    @Transactional
    public PropertyResponseDTO updateProperty(UUID transactionId, UUID propertyId, PropertyRequestDTO dto, UUID brokerId) {
        Transaction tx = repo.findByTransactionId(transactionId)
                .orElseThrow(() -> new NotFoundException("Transaction not found"));

        TransactionAccessUtils.verifyBrokerAccess(tx, brokerId);

        Property property = propertyRepository.findByPropertyId(propertyId)
                .orElseThrow(() -> new NotFoundException("Property not found"));

        if (!property.getTransactionId().equals(transactionId)) {
            throw new BadRequestException("Property does not belong to this transaction");
        }

        // Track if offer status changed for timeline
        PropertyOfferStatus previousStatus = property.getOfferStatus();
        boolean statusChanged = dto.getOfferStatus() != null && dto.getOfferStatus() != previousStatus;

        // Update fields
        if (dto.getAddress() != null) {
            var updatedAddress = dto.getAddress();
            // Normalize postal code
            if (updatedAddress.getPostalCode() != null) {
                updatedAddress.setPostalCode(PostalCodeUtil.normalize(updatedAddress.getPostalCode()));
            }
            property.setAddress(updatedAddress);
        }
        if (dto.getAskingPrice() != null) {
            property.setAskingPrice(dto.getAskingPrice());
        }
        if (dto.getOfferStatus() != null) {
            property.setOfferStatus(dto.getOfferStatus());
        }
        // offerAmount can be null (to clear it)
        property.setOfferAmount(dto.getOfferAmount());
        property.setCentrisNumber(dto.getCentrisNumber());
        property.setNotes(dto.getNotes());
        property.setUpdatedAt(LocalDateTime.now());

        Property saved = propertyRepository.save(property);

        // Add timeline entry for status changes
        if (statusChanged) {
            String address = saved.getAddress() != null ? saved.getAddress().getStreet() : "Unknown";
            String actorName = lookupUserName(brokerId);
            TransactionInfo info = TransactionInfo.builder()
                    .actorName(actorName)
                    .address(address)
                    .build();
            timelineService.addEntry(
                    transactionId,
                    brokerId,
                    TimelineEntryType.PROPERTY_UPDATED,
                    "Property " + address + " offer status changed to " + dto.getOfferStatus(),
                    null,
                    info);
        }

        return toPropertyResponseDTO(saved, true);
    }

    @Override
    @Transactional
    public void removeProperty(UUID transactionId, UUID propertyId, UUID brokerId) {
        Transaction tx = repo.findByTransactionId(transactionId)
                .orElseThrow(() -> new NotFoundException("Transaction not found"));

        TransactionAccessUtils.verifyBrokerAccess(tx, brokerId);

        Property property = propertyRepository.findByPropertyId(propertyId)
                .orElseThrow(() -> new NotFoundException("Property not found"));

        if (!property.getTransactionId().equals(transactionId)) {
            throw new BadRequestException("Property does not belong to this transaction");
        }

        String address = property.getAddress() != null ? property.getAddress().getStreet() : "Unknown";

        propertyRepository.delete(property);

        // Add timeline entry for property removal
        String actorName = lookupUserName(brokerId);
        TransactionInfo info = TransactionInfo.builder()
                .actorName(actorName)
                .address(address)
                .build();
        timelineService.addEntry(
                transactionId,
                brokerId,
                TimelineEntryType.PROPERTY_REMOVED,
                "Property removed: " + address,
                null,
                info);
    }

    @Override
    public PropertyResponseDTO getPropertyById(UUID propertyId, UUID userId, boolean isBroker) {
        Property property = propertyRepository.findByPropertyId(propertyId)
                .orElseThrow(() -> new NotFoundException("Property not found"));

        Transaction tx = repo.findByTransactionId(property.getTransactionId())
                .orElseThrow(() -> new NotFoundException("Transaction not found"));

        TransactionAccessUtils.verifyTransactionAccess(tx, userId);

        return toPropertyResponseDTO(property, isBroker);
    }

    private PropertyResponseDTO toPropertyResponseDTO(Property property, boolean includeBrokerNotes) {
        return PropertyResponseDTO.builder()
                .propertyId(property.getPropertyId())
                .transactionId(property.getTransactionId())
                .address(toAddressDTO(property.getAddress()))
                .askingPrice(property.getAskingPrice())
                .offerStatus(property.getOfferStatus())
                .offerAmount(property.getOfferAmount())
                .centrisNumber(property.getCentrisNumber())
                .notes(property.getNotes())
                .createdAt(property.getCreatedAt())
                .updatedAt(property.getUpdatedAt())
                .build();
    }
    
    private PropertyAddressDTO toAddressDTO(PropertyAddress address) {
        if (address == null) return null;
        return PropertyAddressDTO.builder()
                .street(address.getStreet())
                .city(address.getCity())
                .province(address.getProvince())
                .postalCode(address.getPostalCode())
                .build();
    }

    @Override
    @Transactional
    public void setActiveProperty(UUID transactionId, UUID propertyId, UUID brokerId) {
        Transaction tx = repo.findByTransactionId(transactionId)
                .orElseThrow(() -> new NotFoundException("Transaction not found"));

        TransactionAccessUtils.verifyBrokerAccess(tx, brokerId);

        // If propertyId is null, clear the active property
        if (propertyId == null) {
            tx.setPropertyAddress(null);
            repo.save(tx);
            return;
        }

        Property property = propertyRepository.findByPropertyId(propertyId)
                .orElseThrow(() -> new NotFoundException("Property not found"));

        if (!property.getTransactionId().equals(transactionId)) {
            throw new BadRequestException("Property does not belong to this transaction");
        }

        if (property.getAddress() == null) {
            throw new BadRequestException("Property must have an address to be set as active");
        }

        // Update transaction address
        tx.setPropertyAddress(property.getAddress());
        repo.save(tx);
        // No timeline entry for active property change on buy-side
    }

    // ==================== OFFER METHODS ====================

    @Override
    public List<OfferResponseDTO> getOffers(UUID transactionId, UUID userId, boolean isBroker) {
        Transaction tx = repo.findByTransactionId(transactionId)
                .orElseThrow(() -> new NotFoundException("Transaction not found"));

        TransactionAccessUtils.verifyTransactionAccess(tx, userId);

        // Only SELL_SIDE transactions can have offers
        if (tx.getSide() != TransactionSide.SELL_SIDE) {
            return List.of();
        }

        List<Offer> offers = offerRepository.findByTransactionIdOrderByCreatedAtDesc(transactionId);

        return offers.stream()
                .map(o -> toOfferResponseDTO(o, isBroker))
                .toList();
    }

    @Override
    @Transactional
    public OfferResponseDTO addOffer(UUID transactionId, OfferRequestDTO dto, UUID brokerId) {
        Transaction tx = repo.findByTransactionId(transactionId)
                .orElseThrow(() -> new NotFoundException("Transaction not found"));

        TransactionAccessUtils.verifyBrokerAccess(tx, brokerId);

        // Only SELL_SIDE transactions can have offers
        if (tx.getSide() != TransactionSide.SELL_SIDE) {
            throw new BadRequestException("Offers can only be added to seller-side transactions");
        }

        Offer offer = Offer.builder()
                .offerId(UUID.randomUUID())
                .transactionId(transactionId)
                .buyerName(dto.getBuyerName())
                .offerAmount(dto.getOfferAmount())
                .status(dto.getStatus() != null ? dto.getStatus() : ReceivedOfferStatus.PENDING)
                .notes(dto.getNotes())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Offer saved = offerRepository.save(offer);

        // Add timeline entry for offer received
        String actorName = lookupUserName(brokerId);
        TransactionInfo info = TransactionInfo.builder()
                .actorName(actorName)
                .buyerName(saved.getBuyerName())
                .offerAmount(saved.getOfferAmount())
                .offerStatus(saved.getStatus().name())
                .build();
        timelineService.addEntry(
                transactionId,
                brokerId,
                TimelineEntryType.OFFER_RECEIVED,
                null,
                null,
                info);

        // Notify client about new offer
        try {
            notificationService.createNotification(
                    tx.getClientId().toString(),
                    "notifications.offerReceived.title",
                    "notifications.offerReceived.message",
                    java.util.Map.of(
                            "buyerName", saved.getBuyerName(),
                            "offerAmount", String.format("$%,.0f", saved.getOfferAmount())
                    ),
                    transactionId.toString(),
                    com.example.courtierprobackend.notifications.datalayer.enums.NotificationCategory.OFFER_RECEIVED);
        } catch (Exception e) {
            log.error("Failed to send offer notification for transaction {}", transactionId, e);
        }

        return toOfferResponseDTO(saved, true);
    }

    @Override
    @Transactional
    public OfferResponseDTO updateOffer(UUID transactionId, UUID offerId, OfferRequestDTO dto, UUID brokerId) {
        Transaction tx = repo.findByTransactionId(transactionId)
                .orElseThrow(() -> new NotFoundException("Transaction not found"));

        TransactionAccessUtils.verifyBrokerAccess(tx, brokerId);

        Offer offer = offerRepository.findByOfferId(offerId)
                .orElseThrow(() -> new NotFoundException("Offer not found"));

        if (!offer.getTransactionId().equals(transactionId)) {
            throw new BadRequestException("Offer does not belong to this transaction");
        }

        // Track if status changed for timeline
        ReceivedOfferStatus previousStatus = offer.getStatus();
        boolean statusChanged = dto.getStatus() != null && dto.getStatus() != previousStatus;

        // Update fields
        if (dto.getBuyerName() != null) {
            offer.setBuyerName(dto.getBuyerName());
        }
        offer.setOfferAmount(dto.getOfferAmount());
        if (dto.getStatus() != null) {
            offer.setStatus(dto.getStatus());
        }
        offer.setNotes(dto.getNotes());
        offer.setUpdatedAt(LocalDateTime.now());

        Offer saved = offerRepository.save(offer);

        // Add timeline entry for status changes
        if (statusChanged) {
            String actorName = lookupUserName(brokerId);
            TransactionInfo info = TransactionInfo.builder()
                    .actorName(actorName)
                    .buyerName(saved.getBuyerName())
                    .offerAmount(saved.getOfferAmount())
                    .offerStatus(dto.getStatus().name())
                    .build();
            timelineService.addEntry(
                    transactionId,
                    brokerId,
                    TimelineEntryType.OFFER_UPDATED,
                    null,
                    null,
                    info);
        }

        return toOfferResponseDTO(saved, true);
    }

    @Override
    @Transactional
    public void removeOffer(UUID transactionId, UUID offerId, UUID brokerId) {
        Transaction tx = repo.findByTransactionId(transactionId)
                .orElseThrow(() -> new NotFoundException("Transaction not found"));

        TransactionAccessUtils.verifyBrokerAccess(tx, brokerId);

        Offer offer = offerRepository.findByOfferId(offerId)
                .orElseThrow(() -> new NotFoundException("Offer not found"));

        if (!offer.getTransactionId().equals(transactionId)) {
            throw new BadRequestException("Offer does not belong to this transaction");
        }

        String buyerName = offer.getBuyerName();

        offerRepository.delete(offer);

        // Add timeline entry for offer removal
        String actorName = lookupUserName(brokerId);
        TransactionInfo info = TransactionInfo.builder()
                .actorName(actorName)
                .buyerName(buyerName)
                .build();
        timelineService.addEntry(
                transactionId,
                brokerId,
                TimelineEntryType.OFFER_REMOVED,
                null,
                null,
                info);
    }

    @Override
    public OfferResponseDTO getOfferById(UUID offerId, UUID userId, boolean isBroker) {
        Offer offer = offerRepository.findByOfferId(offerId)
                .orElseThrow(() -> new NotFoundException("Offer not found"));

        Transaction tx = repo.findByTransactionId(offer.getTransactionId())
                .orElseThrow(() -> new NotFoundException("Transaction not found"));

        TransactionAccessUtils.verifyTransactionAccess(tx, userId);

        return toOfferResponseDTO(offer, isBroker);
    }

    private OfferResponseDTO toOfferResponseDTO(Offer offer, boolean includeBrokerNotes) {
        return OfferResponseDTO.builder()
                .offerId(offer.getOfferId())
                .transactionId(offer.getTransactionId())
                .buyerName(offer.getBuyerName())
                .offerAmount(offer.getOfferAmount())
                .status(offer.getStatus())
                .notes(offer.getNotes())
                .createdAt(offer.getCreatedAt())
                .updatedAt(offer.getUpdatedAt())
                .build();
    }

    // ==================== CONDITION METHODS ====================

    @Override
    public List<ConditionResponseDTO> getConditions(UUID transactionId, UUID userId, boolean isBroker) {
        Transaction tx = repo.findByTransactionId(transactionId)
                .orElseThrow(() -> new NotFoundException("Transaction not found"));

        TransactionAccessUtils.verifyTransactionAccess(tx, userId);

        List<Condition> conditions = conditionRepository.findByTransactionIdOrderByDeadlineDateAsc(transactionId);

        return conditions.stream()
                .map(c -> toConditionResponseDTO(c, isBroker))
                .toList();
    }

    @Override
    @Transactional
    public ConditionResponseDTO addCondition(UUID transactionId, ConditionRequestDTO dto, UUID brokerId) {
        Transaction tx = repo.findByTransactionId(transactionId)
                .orElseThrow(() -> new NotFoundException("Transaction not found"));

        TransactionAccessUtils.verifyBrokerAccess(tx, brokerId);

        // Validate: OTHER type requires customTitle
        if (dto.getType() == ConditionType.OTHER && 
                (dto.getCustomTitle() == null || dto.getCustomTitle().isBlank())) {
            throw new BadRequestException("Custom title is required for condition type OTHER");
        }

        Condition condition = Condition.builder()
                .conditionId(UUID.randomUUID())
                .transactionId(transactionId)
                .type(dto.getType())
                .customTitle(dto.getType() == ConditionType.OTHER ? dto.getCustomTitle() : null)
                .description(dto.getDescription())
                .deadlineDate(dto.getDeadlineDate())
                .status(dto.getStatus() != null ? dto.getStatus() : ConditionStatus.PENDING)
                .notes(dto.getNotes())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Condition saved = conditionRepository.save(condition);

        // Add timeline entry for condition added
        String typeName = getConditionTypeName(saved);
        String actorName = lookupUserName(brokerId);
        String description = "Condition added: " + typeName + " - " + saved.getDescription() + 
                " (Deadline: " + saved.getDeadlineDate() + ")";
        TransactionInfo txInfo = TransactionInfo.builder()
                .actorName(actorName)
                .conditionType(saved.getType().name())
                .conditionDescription(saved.getDescription())
                .conditionDeadline(saved.getDeadlineDate() != null ? saved.getDeadlineDate().toString() : null)
                .build();
        timelineService.addEntry(
                transactionId,
                brokerId,
                TimelineEntryType.CONDITION_ADDED,
                description,
                null,
                txInfo);

        // Notify client about new condition
        if (tx.getClientId() != null) {
            try {
                notificationService.createNotification(
                        tx.getClientId().toString(),
                        "notifications.conditionAdded.title",
                        "notifications.conditionAdded.message",
                        java.util.Map.of(
                                "conditionType", typeName,
                                "deadline", saved.getDeadlineDate().toString()
                        ),
                        transactionId.toString(),
                        com.example.courtierprobackend.notifications.datalayer.enums.NotificationCategory.GENERAL);
            } catch (Exception e) {
                log.error("Failed to send condition notification for transaction {}", transactionId, e);
            }
        }

        return toConditionResponseDTO(saved, true);
    }

    @Override
    @Transactional
    public ConditionResponseDTO updateCondition(UUID transactionId, UUID conditionId, ConditionRequestDTO dto, UUID brokerId) {
        Transaction tx = repo.findByTransactionId(transactionId)
                .orElseThrow(() -> new NotFoundException("Transaction not found"));

        TransactionAccessUtils.verifyBrokerAccess(tx, brokerId);

        Condition condition = conditionRepository.findByConditionId(conditionId)
                .orElseThrow(() -> new NotFoundException("Condition not found"));

        if (!condition.getTransactionId().equals(transactionId)) {
            throw new BadRequestException("Condition does not belong to this transaction");
        }

        // Validate: OTHER type requires customTitle
        if (dto.getType() == ConditionType.OTHER && 
                (dto.getCustomTitle() == null || dto.getCustomTitle().isBlank())) {
            throw new BadRequestException("Custom title is required for condition type OTHER");
        }

        // Track status change for timeline
        ConditionStatus previousStatus = condition.getStatus();
        boolean statusChanged = dto.getStatus() != null && dto.getStatus() != previousStatus;

        // Update fields
        condition.setType(dto.getType());
        condition.setCustomTitle(dto.getType() == ConditionType.OTHER ? dto.getCustomTitle() : null);
        condition.setDescription(dto.getDescription());
        condition.setDeadlineDate(dto.getDeadlineDate());
        if (dto.getStatus() != null) {
            condition.setStatus(dto.getStatus());
            if (dto.getStatus() == ConditionStatus.SATISFIED && condition.getSatisfiedAt() == null) {
                condition.setSatisfiedAt(LocalDateTime.now());
            } else if (dto.getStatus() != ConditionStatus.SATISFIED) {
                // Clear satisfiedAt when status changes away from SATISFIED
                condition.setSatisfiedAt(null);
            }
        }
        condition.setNotes(dto.getNotes());
        condition.setUpdatedAt(LocalDateTime.now());

        Condition saved = conditionRepository.save(condition);

        // Add timeline entry for condition update
        String typeName = getConditionTypeName(saved);
        String description = "Condition updated: " + typeName;
        if (statusChanged) {
            description += " - Status changed to " + dto.getStatus();
        }
        TransactionInfo txInfo = TransactionInfo.builder()
                .conditionType(saved.getType().name())
                .conditionDescription(saved.getDescription())
                .conditionDeadline(saved.getDeadlineDate() != null ? saved.getDeadlineDate().toString() : null)
                .conditionPreviousStatus(statusChanged ? previousStatus.name() : null)
                .conditionNewStatus(statusChanged ? dto.getStatus().name() : null)
                .build();
        timelineService.addEntry(
                transactionId,
                brokerId,
                TimelineEntryType.CONDITION_UPDATED,
                description,
                null,
                txInfo);

        return toConditionResponseDTO(saved, true);
    }

    @Override
    @Transactional
    public void removeCondition(UUID transactionId, UUID conditionId, UUID brokerId) {
        Transaction tx = repo.findByTransactionId(transactionId)
                .orElseThrow(() -> new NotFoundException("Transaction not found"));

        TransactionAccessUtils.verifyBrokerAccess(tx, brokerId);

        Condition condition = conditionRepository.findByConditionId(conditionId)
                .orElseThrow(() -> new NotFoundException("Condition not found"));

        if (!condition.getTransactionId().equals(transactionId)) {
            throw new BadRequestException("Condition does not belong to this transaction");
        }

        String typeName = getConditionTypeName(condition);
        String conditionTypeName = condition.getType().name();

        conditionRepository.delete(condition);

        // Add timeline entry for condition removal
        String description = "Condition removed: " + typeName;
        TransactionInfo txInfo = TransactionInfo.builder()
                .conditionType(conditionTypeName)
                .build();
        timelineService.addEntry(
                transactionId,
                brokerId,
                TimelineEntryType.CONDITION_REMOVED,
                description,
                null,
                txInfo);
    }

    @Override
    @Transactional
    public ConditionResponseDTO updateConditionStatus(UUID transactionId, UUID conditionId, 
            ConditionStatus status, UUID brokerId) {
        Transaction tx = repo.findByTransactionId(transactionId)
                .orElseThrow(() -> new NotFoundException("Transaction not found"));

        TransactionAccessUtils.verifyBrokerAccess(tx, brokerId);

        Condition condition = conditionRepository.findByConditionId(conditionId)
                .orElseThrow(() -> new NotFoundException("Condition not found"));

        if (!condition.getTransactionId().equals(transactionId)) {
            throw new BadRequestException("Condition does not belong to this transaction");
        }

        ConditionStatus previousStatus = condition.getStatus();
        condition.setStatus(status);
        condition.setUpdatedAt(LocalDateTime.now());

        if (status == ConditionStatus.SATISFIED && condition.getSatisfiedAt() == null) {
            condition.setSatisfiedAt(LocalDateTime.now());
        } else if (status != ConditionStatus.SATISFIED) {
            // Clear satisfiedAt when status changes away from SATISFIED
            condition.setSatisfiedAt(null);
        }

        Condition saved = conditionRepository.save(condition);

        // Add timeline entry - use CONDITION_SATISFIED or CONDITION_FAILED for status
        String typeName = getConditionTypeName(saved);
        TimelineEntryType timelineType;
        if (status == ConditionStatus.SATISFIED) {
            timelineType = TimelineEntryType.CONDITION_SATISFIED;
        } else if (status == ConditionStatus.FAILED) {
            timelineType = TimelineEntryType.CONDITION_FAILED;
        } else {
            timelineType = TimelineEntryType.CONDITION_UPDATED;
        }
        String description = "Condition " + typeName + " status changed: " + previousStatus + " → " + status;
        TransactionInfo txInfo = TransactionInfo.builder()
                .conditionType(saved.getType().name())
                .conditionDescription(saved.getDescription())
                .conditionPreviousStatus(previousStatus.name())
                .conditionNewStatus(status.name())
                .build();
        timelineService.addEntry(
                transactionId,
                brokerId,
                timelineType,
                description,
                null,
                txInfo);

        // Notify client about condition status change
        if (tx.getClientId() != null) {
            try {
                String notifTitle;
                String notifMessage;
                if (status == ConditionStatus.SATISFIED) {
                    notifTitle = "notifications.conditionSatisfied.title";
                    notifMessage = "notifications.conditionSatisfied.message";
                } else if (status == ConditionStatus.FAILED) {
                    notifTitle = "notifications.conditionFailed.title";
                    notifMessage = "notifications.conditionFailed.message";
                } else {
                    // PENDING status change (e.g., reverting from SATISFIED)
                    notifTitle = "notifications.conditionStatusChanged.title";
                    notifMessage = "notifications.conditionStatusChanged.message";
                }
                notificationService.createNotification(
                        tx.getClientId().toString(),
                        notifTitle,
                        notifMessage,
                        java.util.Map.of(
                                "conditionType", typeName,
                                "status", status.name()
                        ),
                        transactionId.toString(),
                        com.example.courtierprobackend.notifications.datalayer.enums.NotificationCategory.GENERAL);
            } catch (Exception e) {
                log.error("Failed to send condition status notification for transaction {}", transactionId, e);
            }
        }

        return toConditionResponseDTO(saved, true);
    }

    private String getConditionTypeName(Condition condition) {
        if (condition.getType() == ConditionType.OTHER && condition.getCustomTitle() != null) {
            return condition.getCustomTitle();
        }
        return condition.getType().name();
    }

    private ConditionResponseDTO toConditionResponseDTO(Condition condition, boolean includeBrokerNotes) {
        return ConditionResponseDTO.builder()
                .conditionId(condition.getConditionId())
                .transactionId(condition.getTransactionId())
                .type(condition.getType())
                .customTitle(condition.getCustomTitle())
                .description(condition.getDescription())
                .deadlineDate(condition.getDeadlineDate())
                .status(condition.getStatus())
                .satisfiedAt(condition.getSatisfiedAt())
                .notes(includeBrokerNotes ? condition.getNotes() : null)
                .createdAt(condition.getCreatedAt())
                .updatedAt(condition.getUpdatedAt())
                .build();
    }
}
