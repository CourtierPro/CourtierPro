package com.example.courtierprobackend.appointments.businesslayer;

import com.example.courtierprobackend.appointments.datalayer.Appointment;
import com.example.courtierprobackend.appointments.datalayer.AppointmentRepository;
import com.example.courtierprobackend.appointments.datalayer.dto.AppointmentResponseDTO;
import com.example.courtierprobackend.appointments.datalayer.enums.AppointmentStatus;
import com.example.courtierprobackend.common.exceptions.ForbiddenException;
import com.example.courtierprobackend.common.exceptions.NotFoundException;
import com.example.courtierprobackend.transactions.datalayer.Transaction;
import com.example.courtierprobackend.transactions.datalayer.repositories.TransactionRepository;
import com.example.courtierprobackend.user.dataaccesslayer.UserAccount;
import com.example.courtierprobackend.user.dataaccesslayer.UserAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import java.util.stream.Collectors;

/**
 * Implementation of AppointmentService for managing appointment operations.
 */
@Service
@Transactional(readOnly = true)
public class AppointmentServiceImpl implements AppointmentService {

        @Override
        public List<AppointmentResponseDTO> getTopUpcomingAppointmentsForBroker(UUID brokerId, int limit) {
                List<Appointment> appointments = appointmentRepository.findUpcomingByBrokerId(brokerId,
                                LocalDateTime.now());
                return mapToDTOs(appointments.stream()
                                .filter(appointment -> appointment.getStatus() == AppointmentStatus.CONFIRMED
                                                || appointment.getStatus() == AppointmentStatus.PROPOSED)
                                .limit(limit)
                                .toList());
        }

        @Override
        public List<AppointmentResponseDTO> getTopUpcomingAppointmentsForClient(UUID clientId, int limit) {
                List<Appointment> appointments = appointmentRepository.findUpcomingByClientId(clientId,
                                LocalDateTime.now());
                return mapToDTOs(appointments.stream()
                                .filter(appointment -> appointment.getStatus() == AppointmentStatus.CONFIRMED
                                                || appointment.getStatus() == AppointmentStatus.PROPOSED)
                                .limit(limit)
                                .toList());
        }

        private final AppointmentRepository appointmentRepository;
        private final UserAccountRepository userAccountRepository;
        private final TransactionRepository transactionRepository;
        private final com.example.courtierprobackend.audit.appointment_audit.businesslayer.AppointmentAuditService appointmentAuditService;
        private final com.example.courtierprobackend.email.EmailService emailService;
        private final com.example.courtierprobackend.audit.timeline_audit.businesslayer.TimelineService timelineService;
        private final com.example.courtierprobackend.notifications.businesslayer.NotificationService notificationService;
        private final com.example.courtierprobackend.transactions.datalayer.repositories.TransactionParticipantRepository transactionParticipantRepository;
        private final com.example.courtierprobackend.transactions.datalayer.repositories.PropertyRepository propertyRepository;
        private final com.example.courtierprobackend.transactions.datalayer.repositories.VisitorRepository visitorRepository;

        public AppointmentServiceImpl(AppointmentRepository appointmentRepository,
                        UserAccountRepository userAccountRepository,
                        TransactionRepository transactionRepository,
                        com.example.courtierprobackend.audit.appointment_audit.businesslayer.AppointmentAuditService appointmentAuditService,
                        com.example.courtierprobackend.email.EmailService emailService,
                        com.example.courtierprobackend.audit.timeline_audit.businesslayer.TimelineService timelineService,
                        com.example.courtierprobackend.notifications.businesslayer.NotificationService notificationService,
                        com.example.courtierprobackend.transactions.datalayer.repositories.TransactionParticipantRepository transactionParticipantRepository,
                        com.example.courtierprobackend.transactions.datalayer.repositories.PropertyRepository propertyRepository,
                        com.example.courtierprobackend.transactions.datalayer.repositories.VisitorRepository visitorRepository) {
                this.appointmentRepository = appointmentRepository;
                this.userAccountRepository = userAccountRepository;
                this.transactionRepository = transactionRepository;
                this.appointmentAuditService = appointmentAuditService;
                this.emailService = emailService;
                this.timelineService = timelineService;
                this.notificationService = notificationService;
                this.transactionParticipantRepository = transactionParticipantRepository;
                this.propertyRepository = propertyRepository;
                this.visitorRepository = visitorRepository;
        }

        @Override
        public List<AppointmentResponseDTO> getAppointmentsForBroker(UUID brokerId) {
                List<Appointment> appointments = appointmentRepository
                                .findByBrokerIdOrderByFromDateTimeAsc(brokerId);
                return mapToDTOs(appointments);
        }

        @Override
        public List<AppointmentResponseDTO> getAppointmentsForClient(UUID clientId, UUID requesterId,
                        String requesterEmail) {
                List<Appointment> allAppointments = appointmentRepository
                                .findByClientIdOrderByFromDateTimeAsc(clientId);
                final UUID finalRequesterId = requesterId;
                final String finalRequesterEmail = requesterEmail;
                List<Appointment> filtered = allAppointments.stream().filter(apt -> {
                        if (finalRequesterId == null)
                                return false;
                        if (apt.getBrokerId() != null && apt.getBrokerId().equals(finalRequesterId))
                                return true;
                        if (apt.getClientId() != null && apt.getClientId().equals(finalRequesterId))
                                return true;
                        if (apt.getTransactionId() != null) {
                                var participants = transactionParticipantRepository
                                                .findByTransactionId(apt.getTransactionId());
                                return participants.stream().anyMatch(p -> (p.getUserId() != null
                                                && p.getUserId().equals(finalRequesterId))
                                                || (finalRequesterEmail != null && p.getEmail() != null
                                                                && finalRequesterEmail.equalsIgnoreCase(p.getEmail())));
                        }
                        return false;
                }).collect(java.util.stream.Collectors.toList());
                return mapToDTOs(filtered);
        }

        @Override
        public List<AppointmentResponseDTO> getAppointmentsForBrokerByDateRange(
                        UUID brokerId, LocalDateTime from, LocalDateTime to) {
                List<Appointment> appointments = appointmentRepository
                                .findByBrokerIdAndDateRange(brokerId, from, to);
                return mapToDTOs(appointments);
        }

        @Override
        public List<AppointmentResponseDTO> getAppointmentsForClientByDateRange(
                        UUID clientId, LocalDateTime from, LocalDateTime to, UUID requesterId, String requesterEmail) {
                List<Appointment> appointments = appointmentRepository
                                .findByClientIdAndDateRange(clientId, from, to);
                final UUID finalRequesterId = requesterId;
                final String finalRequesterEmail = requesterEmail;
                List<Appointment> filtered = appointments.stream().filter(apt -> {
                        if (finalRequesterId == null)
                                return false;
                        if (apt.getBrokerId() != null && apt.getBrokerId().equals(finalRequesterId))
                                return true;
                        if (apt.getClientId() != null && apt.getClientId().equals(finalRequesterId))
                                return true;
                        if (apt.getTransactionId() != null) {
                                var participants = transactionParticipantRepository
                                                .findByTransactionId(apt.getTransactionId());
                                return participants.stream().anyMatch(p -> (p.getUserId() != null
                                                && p.getUserId().equals(finalRequesterId))
                                                || (finalRequesterEmail != null && p.getEmail() != null
                                                                && finalRequesterEmail.equalsIgnoreCase(p.getEmail())));
                        }
                        return false;
                }).collect(java.util.stream.Collectors.toList());
                return mapToDTOs(filtered);
        }

        @Override
        public List<AppointmentResponseDTO> getAppointmentsForBrokerByStatus(
                        UUID brokerId, AppointmentStatus status) {
                List<Appointment> appointments = appointmentRepository
                                .findByBrokerIdAndStatusOrderByFromDateTimeAsc(brokerId, status);
                return mapToDTOs(appointments);
        }

        @Override
        public List<AppointmentResponseDTO> getAppointmentsForClientByStatus(
                        UUID clientId, AppointmentStatus status, UUID requesterId, String requesterEmail) {
                List<Appointment> appointments = appointmentRepository
                                .findByClientIdAndStatusOrderByFromDateTimeAsc(clientId, status);
                final UUID finalRequesterId = requesterId;
                final String finalRequesterEmail = requesterEmail;
                List<Appointment> filtered = appointments.stream().filter(apt -> {
                        if (finalRequesterId == null)
                                return false;
                        if (apt.getBrokerId() != null && apt.getBrokerId().equals(finalRequesterId))
                                return true;
                        if (apt.getClientId() != null && apt.getClientId().equals(finalRequesterId))
                                return true;
                        if (apt.getTransactionId() != null) {
                                var participants = transactionParticipantRepository
                                                .findByTransactionId(apt.getTransactionId());
                                return participants.stream().anyMatch(p -> (p.getUserId() != null
                                                && p.getUserId().equals(finalRequesterId))
                                                || (finalRequesterEmail != null && p.getEmail() != null
                                                                && finalRequesterEmail.equalsIgnoreCase(p.getEmail())));
                        }
                        return false;
                }).collect(java.util.stream.Collectors.toList());
                return mapToDTOs(filtered);
        }

        @Override
        public List<AppointmentResponseDTO> getAppointmentsForBrokerByDateRangeAndStatus(
                        UUID brokerId, LocalDateTime from, LocalDateTime to, AppointmentStatus status) {
                List<Appointment> appointments = appointmentRepository
                                .findByBrokerIdAndDateRangeAndStatus(brokerId, from, to, status);
                return mapToDTOs(appointments);
        }

        @Override
        public List<AppointmentResponseDTO> getAppointmentsForClientByDateRangeAndStatus(
                        UUID clientId, LocalDateTime from, LocalDateTime to, AppointmentStatus status, UUID requesterId,
                        String requesterEmail) {
                List<Appointment> appointments = appointmentRepository
                                .findByClientIdAndDateRangeAndStatus(clientId, from, to, status);
                final UUID finalRequesterId = requesterId;
                final String finalRequesterEmail = requesterEmail;
                List<Appointment> filtered = appointments.stream().filter(apt -> {
                        if (finalRequesterId == null)
                                return false;
                        if (apt.getBrokerId() != null && apt.getBrokerId().equals(finalRequesterId))
                                return true;
                        if (apt.getClientId() != null && apt.getClientId().equals(finalRequesterId))
                                return true;
                        if (apt.getTransactionId() != null) {
                                var participants = transactionParticipantRepository
                                                .findByTransactionId(apt.getTransactionId());
                                return participants.stream().anyMatch(p -> (p.getUserId() != null
                                                && p.getUserId().equals(finalRequesterId))
                                                || (finalRequesterEmail != null && p.getEmail() != null
                                                                && finalRequesterEmail.equalsIgnoreCase(p.getEmail())));
                        }
                        return false;
                }).collect(java.util.stream.Collectors.toList());
                return mapToDTOs(filtered);
        }

        @Override
        public List<AppointmentResponseDTO> getAppointmentsForTransaction(UUID transactionId, UUID requesterId) {
                Transaction transaction = transactionRepository.findByTransactionId(transactionId)
                                .orElseThrow(() -> new NotFoundException("Transaction not found: " + transactionId));

                if (!transaction.getBrokerId().equals(requesterId) && !transaction.getClientId().equals(requesterId)) {
                        throw new ForbiddenException(
                                        "You do not have permission to view appointments for this transaction");
                }

                List<Appointment> appointments = appointmentRepository
                                .findByTransactionIdOrderByFromDateTimeAsc(transactionId);
                return mapToDTOs(appointments);
        }

        @Override
        public AppointmentResponseDTO getAppointmentById(UUID appointmentId, UUID requesterId) {
                Appointment appointment = appointmentRepository
                                .findByAppointmentId(appointmentId)
                                .orElseThrow(() -> new NotFoundException("Appointment not found: " + appointmentId));

                if (!appointment.getBrokerId().equals(requesterId) && !appointment.getClientId().equals(requesterId)) {
                        throw new ForbiddenException("You do not have permission to view this appointment");
                }

                return mapToDTO(appointment, getUserNamesMap(List.of(appointment)));
        }

        /**
         * Map a list of appointments to DTOs, batch-fetching user names for efficiency.
         */
        private List<AppointmentResponseDTO> mapToDTOs(List<Appointment> appointments) {
                if (appointments.isEmpty()) {
                        return List.of();
                }
                Map<UUID, String> userNames = getUserNamesMap(appointments);
                return appointments.stream()
                                .map(apt -> mapToDTO(apt, userNames))
                                .collect(Collectors.toList());
        }

        /**
         * Batch fetch user names for all brokers and clients in the appointments list.
         */
        private Map<UUID, String> getUserNamesMap(List<Appointment> appointments) {
                Set<UUID> userIds = appointments.stream()
                                .flatMap(apt -> java.util.stream.Stream.of(apt.getBrokerId(), apt.getClientId()))
                                .collect(Collectors.toSet());

                return userAccountRepository.findAllById((Iterable<UUID>) userIds).stream()
                                .collect(Collectors.toMap(
                                                UserAccount::getId,
                                                this::getFullName,
                                                (existing, replacement) -> existing));
        }

        private String getFullName(UserAccount user) {
                String firstName = user.getFirstName() != null ? user.getFirstName() : "";
                String lastName = user.getLastName() != null ? user.getLastName() : "";
                return (firstName + " " + lastName).trim();
        }

        @Override
        @Transactional
        public AppointmentResponseDTO requestAppointment(
                        com.example.courtierprobackend.appointments.datalayer.dto.AppointmentRequestDTO request,
                        UUID requesterId) {
                Transaction transaction = transactionRepository.findByTransactionId(request.transactionId())
                                .orElseThrow(() -> new NotFoundException(
                                                "Transaction not found: " + request.transactionId()));

                if (transaction.getBrokerId() == null) {
                        throw new IllegalStateException(
                                        "Cannot create appointment: transaction is missing brokerId. Please ensure the transaction has a broker assigned.");
                }
                if (transaction.getClientId() == null) {
                        throw new IllegalStateException(
                                        "Cannot create appointment: transaction is missing clientId. Please ensure the transaction has a client assigned.");
                }

                boolean isBroker = transaction.getBrokerId().equals(requesterId);
                boolean isClient = transaction.getClientId().equals(requesterId);

                if (!isBroker && !isClient) {
                        throw new ForbiddenException(
                                        "You do not have permission to request an appointment for this transaction");
                }

                Appointment appointment = new Appointment();
                appointment.setTransactionId(transaction.getTransactionId());
                appointment.setBrokerId(transaction.getBrokerId());
                appointment.setClientId(transaction.getClientId());

                // Handle custom title for "other" type, otherwise use the type as title
                if ("other".equalsIgnoreCase(request.type()) && request.title() != null && !request.title().isBlank()) {
                        appointment.setTitle(request.title());
                } else {
                        appointment.setTitle(request.type());
                }

                // Validate house_visit type: requires BUY_SIDE transaction and a valid property
                if ("house_visit".equalsIgnoreCase(request.type())) {
                        if (transaction.getSide() != com.example.courtierprobackend.transactions.datalayer.enums.TransactionSide.BUY_SIDE) {
                                throw new IllegalArgumentException(
                                                "House visit appointments can only be created for buy-side transactions");
                        }
                        if (request.propertyId() == null) {
                                throw new IllegalArgumentException(
                                                "Property must be selected for house visit appointments");
                        }
                        com.example.courtierprobackend.transactions.datalayer.Property property = propertyRepository
                                        .findByPropertyId(request.propertyId())
                                        .orElseThrow(() -> new NotFoundException(
                                                        "Property not found: " + request.propertyId()));
                        if (!property.getTransactionId().equals(transaction.getTransactionId())) {
                                throw new IllegalArgumentException(
                                                "Property does not belong to this transaction");
                        }
                        appointment.setPropertyId(request.propertyId());
                }

                // Validate open_house and private_showing types: requires SELL_SIDE transaction
                if ("open_house".equalsIgnoreCase(request.type()) || "private_showing".equalsIgnoreCase(request.type())) {
                        if (transaction.getSide() != com.example.courtierprobackend.transactions.datalayer.enums.TransactionSide.SELL_SIDE) {
                                throw new IllegalArgumentException(
                                                "Open house and private showing appointments can only be created for sell-side transactions");
                        }

                        if ("private_showing".equalsIgnoreCase(request.type())) {
                                appointment.setNumberOfVisitors(1); // default for private showing
                                if (request.visitorId() != null) {
                                        com.example.courtierprobackend.transactions.datalayer.Visitor visitor = visitorRepository
                                                        .findByVisitorId(request.visitorId())
                                                        .orElseThrow(() -> new NotFoundException(
                                                                        "Visitor not found: " + request.visitorId()));
                                        if (!visitor.getTransactionId().equals(transaction.getTransactionId())) {
                                                throw new IllegalArgumentException(
                                                                "Visitor does not belong to this transaction");
                                        }
                                        appointment.setVisitorId(request.visitorId());
                                }
                        }
                        // open_house: numberOfVisitors left null, set after event concludes
                }

                LocalDateTime start = LocalDateTime.of(request.date(), request.startTime());
                LocalDateTime end = LocalDateTime.of(request.date(), request.endTime());

                if (!end.isAfter(start)) {
                        throw new IllegalArgumentException("End time must be after start time");
                }

                if (start.isBefore(LocalDateTime.now())) {
                        throw new IllegalArgumentException("Appointment start time must be in the future");
                }

                appointment.setFromDateTime(start);
                appointment.setToDateTime(end);

                appointment.setNotes(request.message());
                appointment.setStatus(AppointmentStatus.PROPOSED);
                appointment.setInitiatedBy(isBroker
                                ? com.example.courtierprobackend.appointments.datalayer.enums.InitiatorType.BROKER
                                : com.example.courtierprobackend.appointments.datalayer.enums.InitiatorType.CLIENT);

                Appointment saved = appointmentRepository.save(appointment);

                // Audit
                appointmentAuditService.logAction(saved.getAppointmentId(), "CREATED", requesterId,
                                "Appointment requested");

                // Notification & Email
                UUID recipientId = isBroker ? transaction.getClientId() : transaction.getBrokerId();
                UserAccount recipient = userAccountRepository.findById(recipientId).orElse(null);

                if (recipient != null) {
                        UserAccount requester = userAccountRepository.findById(requesterId).orElse(null);
                        String requesterName = (requester != null) ? getFullName(requester)
                                        : (isBroker ? "Broker" : "Client");

                        // In-App Notification
                        notificationService.createNotification(
                                        recipient.getId().toString(),
                                        "APPOINTMENT_REQUESTED", // Title Key
                                        "NEW_APPOINTMENT_REQUEST", // Message Key
                                        Map.of("name", requesterName),
                                        saved.getAppointmentId().toString(),
                                        com.example.courtierprobackend.notifications.datalayer.enums.NotificationCategory.APPOINTMENT);

                        // Email
                        String language = recipient.getPreferredLanguage() != null ? recipient.getPreferredLanguage()
                                        : "en";
                        emailService.sendAppointmentRequestedNotification(saved, recipient.getEmail(),
                                        getFullName(recipient), language);
                }

                if (saved.getTransactionId() != null) {
                        try {
                                com.example.courtierprobackend.audit.timeline_audit.dataaccesslayer.value_object.TransactionInfo info = com.example.courtierprobackend.audit.timeline_audit.dataaccesslayer.value_object.TransactionInfo
                                                .builder()
                                                .appointmentTitle(saved.getTitle())
                                                .appointmentDate(saved.getFromDateTime())
                                                .actorName(java.util.Objects.requireNonNullElse(
                                                                getUserNamesMap(List.of(saved)).get(requesterId),
                                                                "Unknown"))
                                                .build();

                                timelineService.addEntry(saved.getTransactionId(), requesterId,
                                                com.example.courtierprobackend.audit.timeline_audit.dataaccesslayer.Enum.TimelineEntryType.APPOINTMENT_REQUESTED,
                                                "Requested", null, info);
                        } catch (Exception e) {
                                org.slf4j.LoggerFactory.getLogger(AppointmentServiceImpl.class)
                                                .error("Failed to add timeline entry", e);
                        }
                }

                return mapToDTO(saved, getUserNamesMap(List.of(saved)));
        }

        @Override
        @Transactional
        public AppointmentResponseDTO reviewAppointment(
                        UUID appointmentId,
                        com.example.courtierprobackend.appointments.datalayer.dto.AppointmentReviewDTO reviewDTO,
                        UUID reviewerId) {
                Appointment appointment = appointmentRepository
                                .findByAppointmentId(appointmentId)
                                .orElseThrow(() -> new NotFoundException("Appointment not found: " + appointmentId));

                boolean isBroker = appointment.getBrokerId().equals(reviewerId);
                boolean isClient = appointment.getClientId().equals(reviewerId);

                if (!isBroker && !isClient) {
                        throw new ForbiddenException("You do not have permission to review this appointment");
                }

                // Prevent the initiator of the current proposal from reviewing it
                boolean isInitiator = (appointment
                                .getInitiatedBy() == com.example.courtierprobackend.appointments.datalayer.enums.InitiatorType.BROKER
                                && isBroker)
                                || (appointment.getInitiatedBy() == com.example.courtierprobackend.appointments.datalayer.enums.InitiatorType.CLIENT
                                                && isClient);

                if (isInitiator) {
                        // Exception: Initiator CAN reschedule if the appointment was cancelled or
                        // declined (reactivating it)
                        if (reviewDTO.action() != com.example.courtierprobackend.appointments.datalayer.dto.AppointmentReviewDTO.ReviewAction.RESCHEDULE) {
                                throw new ForbiddenException("You cannot review an appointment proposal you initiated");
                        }
                }

                if (appointment.getStatus() != AppointmentStatus.PROPOSED &&
                                appointment.getStatus() != AppointmentStatus.CANCELLED &&
                                appointment.getStatus() != AppointmentStatus.DECLINED) {
                        throw new ForbiddenException(
                                        "Appointment can only be reviewed while in PROPOSED status (or rescheduled if Cancelled/Declined)");
                }

                // If rescheduling from Cancelled/Declined, ensure it's a Reschedule action
                if ((appointment.getStatus() == AppointmentStatus.CANCELLED
                                || appointment.getStatus() == AppointmentStatus.DECLINED) &&
                                reviewDTO.action() != com.example.courtierprobackend.appointments.datalayer.dto.AppointmentReviewDTO.ReviewAction.RESCHEDULE) {
                        throw new ForbiddenException("Cancelled or Declined appointments can only be rescheduled");
                }

                // RESTRICTION: Only the person who cancelled the appointment can reschedule it.
                if (appointment.getStatus() == AppointmentStatus.CANCELLED && appointment.getCancelledBy() != null) {
                        if (!appointment.getCancelledBy().equals(reviewerId)) {
                                throw new ForbiddenException(
                                                "Only the party who cancelled the appointment can reschedule it.");
                        }
                }

                switch (reviewDTO.action()) {
                        case CONFIRM:
                                appointment.setStatus(AppointmentStatus.CONFIRMED);
                                break;
                        case DECLINE:
                                String refusalReason = reviewDTO.refusalReason();
                                if (refusalReason == null || refusalReason.trim().isEmpty()) {
                                        throw new IllegalArgumentException(
                                                        "Refusal reason is required when declining an appointment");
                                }
                                appointment.setStatus(AppointmentStatus.DECLINED);
                                appointment.setRefusalReason(refusalReason.trim());
                                break;
                        case RESCHEDULE:
                                if (reviewDTO.newDate() == null || reviewDTO.newStartTime() == null
                                                || reviewDTO.newEndTime() == null) {
                                        throw new IllegalArgumentException(
                                                        "New date, start time, and end time must be provided for rescheduling");
                                }
                                LocalDateTime start = LocalDateTime.of(reviewDTO.newDate(), reviewDTO.newStartTime());
                                LocalDateTime end = LocalDateTime.of(reviewDTO.newDate(), reviewDTO.newEndTime());

                                if (!end.isAfter(start)) {
                                        throw new IllegalArgumentException("New end time must be after start time");
                                }

                                appointment.setFromDateTime(start);
                                appointment.setToDateTime(end);
                                appointment.setStatus(AppointmentStatus.PROPOSED);
                                appointment.setInitiatedBy(isBroker
                                                ? com.example.courtierprobackend.appointments.datalayer.enums.InitiatorType.BROKER
                                                : com.example.courtierprobackend.appointments.datalayer.enums.InitiatorType.CLIENT);

                                // Reset response fields since it's a new proposal
                                appointment.setRespondedBy(null);
                                appointment.setRespondedAt(null);
                                break;
                }

                // Only set response metadata if it's NOT a reschedule (which is a new proposal)
                if (reviewDTO.action() != com.example.courtierprobackend.appointments.datalayer.dto.AppointmentReviewDTO.ReviewAction.RESCHEDULE) {
                        appointment.setRespondedBy(reviewerId);
                        appointment.setRespondedAt(LocalDateTime.now());
                }

                Appointment saved = appointmentRepository.save(appointment);

                // Audit
                String details = "";
                if (reviewDTO.action() == com.example.courtierprobackend.appointments.datalayer.dto.AppointmentReviewDTO.ReviewAction.DECLINE) {
                        details = "Refusal reason: " + reviewDTO.refusalReason();
                } else if (reviewDTO
                                .action() == com.example.courtierprobackend.appointments.datalayer.dto.AppointmentReviewDTO.ReviewAction.RESCHEDULE) {
                        details = "Rescheduled to: " + reviewDTO.newDate() + " " + reviewDTO.newStartTime() + " - "
                                        + reviewDTO.newEndTime();
                } else {
                        details = "Appointment confirmed";
                }

                appointmentAuditService.logAction(saved.getAppointmentId(), reviewDTO.action().name(), reviewerId,
                                details);

                // Timeline Event
                if (saved.getTransactionId() != null) {
                        try {
                                com.example.courtierprobackend.audit.timeline_audit.dataaccesslayer.Enum.TimelineEntryType timelineType = null;
                                String timelineNote = null;

                                if (reviewDTO.action() == com.example.courtierprobackend.appointments.datalayer.dto.AppointmentReviewDTO.ReviewAction.CONFIRM) {
                                        timelineType = com.example.courtierprobackend.audit.timeline_audit.dataaccesslayer.Enum.TimelineEntryType.APPOINTMENT_CONFIRMED;
                                } else if (reviewDTO
                                                .action() == com.example.courtierprobackend.appointments.datalayer.dto.AppointmentReviewDTO.ReviewAction.DECLINE) {
                                        timelineType = com.example.courtierprobackend.audit.timeline_audit.dataaccesslayer.Enum.TimelineEntryType.APPOINTMENT_DECLINED;
                                        timelineNote = appointment.getRefusalReason();
                                } else if (reviewDTO
                                                .action() == com.example.courtierprobackend.appointments.datalayer.dto.AppointmentReviewDTO.ReviewAction.RESCHEDULE) {
                                        timelineType = com.example.courtierprobackend.audit.timeline_audit.dataaccesslayer.Enum.TimelineEntryType.APPOINTMENT_RESCHEDULED;
                                        timelineNote = "Rescheduled to: " + reviewDTO.newDate() + " "
                                                        + reviewDTO.newStartTime();
                                }

                                if (timelineType != null) {
                                        com.example.courtierprobackend.audit.timeline_audit.dataaccesslayer.value_object.TransactionInfo info = com.example.courtierprobackend.audit.timeline_audit.dataaccesslayer.value_object.TransactionInfo
                                                        .builder()
                                                        .appointmentTitle(saved.getTitle())
                                                        .appointmentDate(saved.getFromDateTime())
                                                        .actorName(java.util.Objects.requireNonNullElse(
                                                                        getUserNamesMap(List.of(saved)).get(reviewerId),
                                                                        "Unknown"))
                                                        .build();

                                        timelineService.addEntry(saved.getTransactionId(), reviewerId, timelineType,
                                                        timelineNote, null,
                                                        info);
                                }
                        } catch (Exception e) {
                                // Log but don't fail the request
                                org.slf4j.LoggerFactory.getLogger(AppointmentServiceImpl.class)
                                                .error("Failed to add timeline entry", e);
                        }
                }

                // Notifications
                UUID recipientId = (isBroker) ? appointment.getClientId() : appointment.getBrokerId();
                UserAccount recipient = userAccountRepository.findById(recipientId).orElse(null);

                if (recipient != null) {
                        if (reviewDTO.action() == com.example.courtierprobackend.appointments.datalayer.dto.AppointmentReviewDTO.ReviewAction.CONFIRM) {
                                notificationService.createNotification(
                                                recipient.getId().toString(),
                                                "APPOINTMENT_CONFIRMED",
                                                "APPOINTMENT_HAS_BEEN_CONFIRMED",
                                                Map.of(),
                                                saved.getAppointmentId().toString(),
                                                com.example.courtierprobackend.notifications.datalayer.enums.NotificationCategory.APPOINTMENT);
                                String language = recipient.getPreferredLanguage() != null
                                                ? recipient.getPreferredLanguage()
                                                : "en";
                                emailService.sendAppointmentConfirmedNotification(saved, recipient.getEmail(),
                                                getFullName(recipient), language);

                        } else if (reviewDTO
                                        .action() == com.example.courtierprobackend.appointments.datalayer.dto.AppointmentReviewDTO.ReviewAction.DECLINE) {
                                notificationService.createNotification(
                                                recipient.getId().toString(),
                                                "APPOINTMENT_DECLINED",
                                                "APPOINTMENT_HAS_BEEN_DECLINED",
                                                Map.of("reason", appointment.getRefusalReason()),
                                                saved.getAppointmentId().toString(),
                                                com.example.courtierprobackend.notifications.datalayer.enums.NotificationCategory.APPOINTMENT);
                                String language = recipient.getPreferredLanguage() != null
                                                ? recipient.getPreferredLanguage()
                                                : "en";
                                emailService.sendAppointmentStatusUpdateNotification(saved, recipient.getEmail(),
                                                getFullName(recipient), language, "DECLINED",
                                                appointment.getRefusalReason());

                        } else if (reviewDTO
                                        .action() == com.example.courtierprobackend.appointments.datalayer.dto.AppointmentReviewDTO.ReviewAction.RESCHEDULE) {
                                notificationService.createNotification(
                                                recipient.getId().toString(),
                                                "APPOINTMENT_RESCHEDULED",
                                                "APPOINTMENT_HAS_BEEN_RESCHEDULED",
                                                Map.of(),
                                                saved.getAppointmentId().toString(),
                                                com.example.courtierprobackend.notifications.datalayer.enums.NotificationCategory.APPOINTMENT);
                                String language = recipient.getPreferredLanguage() != null
                                                ? recipient.getPreferredLanguage()
                                                : "en";
                                emailService.sendAppointmentStatusUpdateNotification(saved, recipient.getEmail(),
                                                getFullName(recipient), language, "RESCHEDULED", "New time proposed");
                        }
                }

                return mapToDTO(saved, getUserNamesMap(List.of(saved)));
        }

        @Override
        @Transactional
        public AppointmentResponseDTO cancelAppointment(
                        UUID appointmentId,
                        com.example.courtierprobackend.appointments.datalayer.dto.AppointmentCancellationDTO cancelDTO,
                        UUID requesterId) {
                Appointment appointment = appointmentRepository
                                .findByAppointmentId(appointmentId)
                                .orElseThrow(() -> new NotFoundException("Appointment not found: " + appointmentId));

                boolean isBroker = appointment.getBrokerId().equals(requesterId);
                boolean isClient = appointment.getClientId().equals(requesterId);

                if (!isBroker && !isClient) {
                        throw new ForbiddenException("You do not have permission to cancel this appointment");
                }

                if (cancelDTO.reason() == null || cancelDTO.reason().trim().isEmpty()) {
                        throw new IllegalArgumentException("Cancellation reason is required");
                }

                // Logic:
                // 1. If CONFIRMED -> Both can cancel (Emergency/Change of plans).
                // 2. If PROPOSED -> Only Initiator can cancel (Withdraw availability).
                // - If Recipient wants to "cancel" a proposal, they should use DECLINE
                // (reviewAppointment).

                boolean isInitiator = (appointment
                                .getInitiatedBy() == com.example.courtierprobackend.appointments.datalayer.enums.InitiatorType.BROKER
                                && isBroker)
                                || (appointment
                                                .getInitiatedBy() == com.example.courtierprobackend.appointments.datalayer.enums.InitiatorType.CLIENT
                                                && isClient);

                if (appointment.getStatus() == AppointmentStatus.PROPOSED && !isInitiator) {
                        throw new ForbiddenException(
                                        "You cannot cancel a proposal you did not initiate. Please decline it instead.");
                }

                // You cannot cancel if already cancelled or declined
                if (appointment.getStatus() == AppointmentStatus.CANCELLED
                                || appointment.getStatus() == AppointmentStatus.DECLINED) {
                        throw new IllegalArgumentException("Appointment is already cancelled or declined");
                }

                appointment.setStatus(AppointmentStatus.CANCELLED);
                appointment.setCancellationReason(cancelDTO.reason().trim());
                appointment.setCancelledBy(requesterId);

                Appointment saved = appointmentRepository.save(appointment);

                // Audit
                appointmentAuditService.logAction(saved.getAppointmentId(), "CANCELLED", requesterId,
                                "Reason: " + cancelDTO.reason());

                // Timeline Event
                if (saved.getTransactionId() != null) {
                        try {
                                com.example.courtierprobackend.audit.timeline_audit.dataaccesslayer.value_object.TransactionInfo info = com.example.courtierprobackend.audit.timeline_audit.dataaccesslayer.value_object.TransactionInfo
                                                .builder()
                                                .appointmentTitle(saved.getTitle())
                                                .appointmentDate(saved.getFromDateTime())
                                                .actorName(java.util.Objects.requireNonNullElse(
                                                                getUserNamesMap(List.of(saved)).get(requesterId),
                                                                "Unknown"))
                                                .build();

                                timelineService.addEntry(saved.getTransactionId(), requesterId,
                                                com.example.courtierprobackend.audit.timeline_audit.dataaccesslayer.Enum.TimelineEntryType.APPOINTMENT_CANCELLED,
                                                cancelDTO.reason(), null, info);
                        } catch (Exception e) {
                                // Log but don't fail the request
                                org.slf4j.LoggerFactory.getLogger(AppointmentServiceImpl.class)
                                                .error("Failed to add timeline entry", e);
                        }
                }

                // Notifications
                UUID recipientId = (isBroker) ? appointment.getClientId() : appointment.getBrokerId();
                UserAccount recipient = userAccountRepository.findById(recipientId).orElse(null);

                if (recipient != null) {
                        notificationService.createNotification(
                                        recipient.getId().toString(),
                                        "APPOINTMENT_CANCELLED",
                                        "APPOINTMENT_HAS_BEEN_CANCELLED",
                                        Map.of("reason", appointment.getCancellationReason()),
                                        saved.getAppointmentId().toString(),
                                        com.example.courtierprobackend.notifications.datalayer.enums.NotificationCategory.APPOINTMENT);
                        String language = recipient.getPreferredLanguage() != null ? recipient.getPreferredLanguage()
                                        : "en";
                        emailService.sendAppointmentStatusUpdateNotification(saved, recipient.getEmail(),
                                        getFullName(recipient), language, "CANCELLED",
                                        appointment.getCancellationReason());
                }

                return mapToDTO(saved, getUserNamesMap(List.of(saved)));
        }

        @Override
        @Transactional
        public AppointmentResponseDTO updateVisitorCount(UUID appointmentId, int numberOfVisitors, UUID requesterId) {
                Appointment appointment = appointmentRepository.findByAppointmentIdAndDeletedAtIsNull(appointmentId)
                                .orElseThrow(() -> new NotFoundException("Appointment not found: " + appointmentId));

                // Verify requester is the broker on this appointment
                if (!appointment.getBrokerId().equals(requesterId)) {
                        throw new ForbiddenException("Only the broker can update the visitor count");
                }

                // Validate appointment status - only confirmed appointments can have visitor counts
                if (appointment.getStatus() != com.example.courtierprobackend.appointments.datalayer.enums.AppointmentStatus.CONFIRMED) {
                        throw new IllegalArgumentException("Visitor count can only be updated for confirmed appointments");
                }

                // Validate appointment type
                String title = appointment.getTitle();
                if (!"open_house".equalsIgnoreCase(title) && !"private_showing".equalsIgnoreCase(title)) {
                        throw new IllegalArgumentException("Visitor count can only be updated for open house or private showing appointments");
                }

                // For open_house, event must have concluded
                if ("open_house".equalsIgnoreCase(title) && appointment.getToDateTime().isAfter(LocalDateTime.now())) {
                        throw new IllegalArgumentException("Visitor count for open house can only be set after the event concludes");
                }

                if (numberOfVisitors < 0) {
                        throw new IllegalArgumentException("Number of visitors cannot be negative");
                }

                appointment.setNumberOfVisitors(numberOfVisitors);
                Appointment saved = appointmentRepository.save(appointment);
                return mapToDTO(saved, getUserNamesMap(List.of(saved)));
        }

        private AppointmentResponseDTO mapToDTO(Appointment apt, Map<UUID, String> userNames) {
                return new AppointmentResponseDTO(
                                apt.getAppointmentId(),
                                apt.getTitle(),
                                apt.getTransactionId(),
                                apt.getBrokerId(),
                                userNames.getOrDefault(apt.getBrokerId(), "Unknown"),
                                apt.getClientId(),
                                userNames.getOrDefault(apt.getClientId(), "Unknown"),
                                apt.getFromDateTime(),
                                apt.getToDateTime(),
                                apt.getStatus(),
                                apt.getInitiatedBy(),
                                apt.getLocation(),
                                apt.getLatitude(),
                                apt.getLongitude(),
                                apt.getNotes(),
                                apt.getRefusalReason(),
                                apt.getCancellationReason(),
                                apt.getCancelledBy(),
                                apt.getCreatedAt(),
                                apt.getUpdatedAt(),
                                apt.getPropertyId(),
                                apt.getNumberOfVisitors(),
                                apt.getVisitorId());
        }

        /**
         * Sends appointment reminders for appointments starting between 24 and 25 hours
         * from now.
         * Runs every hour.
         */
        @org.springframework.scheduling.annotation.Scheduled(cron = "0 0 * * * *") // Runs every hour
        @Transactional
        public void sendAppointmentReminders() {
                LocalDateTime now = LocalDateTime.now();
                LocalDateTime startWindow = now.plusHours(24);
                LocalDateTime endWindow = now.plusHours(25);

                List<AppointmentStatus> excluded = List.of(AppointmentStatus.CANCELLED, AppointmentStatus.DECLINED);
                List<Appointment> appointments = appointmentRepository
                                .findByFromDateTimeBetweenAndReminderSentFalseAndStatusNotIn(
                                                startWindow, endWindow,
                                                excluded);

                List<Appointment> updatedAppointments = new java.util.ArrayList<>();

                for (Appointment apt : appointments) {
                        try {
                                // Determine recipients
                                UserAccount broker = userAccountRepository.findById(apt.getBrokerId()).orElse(null);
                                UserAccount client = userAccountRepository.findById(apt.getClientId()).orElse(null);

                                if (broker != null) {
                                        notificationService.createNotification(
                                                        broker.getId().toString(),
                                                        "APPOINTMENT_REMINDER",
                                                        "APPOINTMENT_REMINDER_MSG",
                                                        Map.of(),
                                                        apt.getAppointmentId().toString(),
                                                        com.example.courtierprobackend.notifications.datalayer.enums.NotificationCategory.APPOINTMENT);
                                        String language = broker.getPreferredLanguage() != null
                                                        ? broker.getPreferredLanguage()
                                                        : "en";
                                        emailService.sendAppointmentReminderNotification(apt, broker.getEmail(),
                                                        getFullName(broker), language);
                                }

                                if (client != null) {
                                        notificationService.createNotification(
                                                        client.getId().toString(),
                                                        "APPOINTMENT_REMINDER",
                                                        "APPOINTMENT_REMINDER_MSG",
                                                        Map.of(),
                                                        apt.getAppointmentId().toString(),
                                                        com.example.courtierprobackend.notifications.datalayer.enums.NotificationCategory.APPOINTMENT);
                                        String language = client.getPreferredLanguage() != null
                                                        ? client.getPreferredLanguage()
                                                        : "en";
                                        emailService.sendAppointmentReminderNotification(apt, client.getEmail(),
                                                        getFullName(client), language);
                                }

                                apt.setReminderSent(true);
                                updatedAppointments.add(apt);

                        } catch (Exception e) {
                                // Log error but continue processing other appointments
                                org.slf4j.LoggerFactory.getLogger(AppointmentServiceImpl.class)
                                                .error("Failed to send reminder for appointment {}",
                                                                apt.getAppointmentId(), e);
                        }
                }

                if (!updatedAppointments.isEmpty()) {
                        appointmentRepository.saveAll(updatedAppointments);
                }
        }
}
