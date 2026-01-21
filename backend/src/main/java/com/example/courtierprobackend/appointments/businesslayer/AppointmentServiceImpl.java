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
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Implementation of AppointmentService for managing appointment operations.
 */
@Service
@Transactional(readOnly = true)
public class AppointmentServiceImpl implements AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final UserAccountRepository userAccountRepository;
    private final TransactionRepository transactionRepository;

    public AppointmentServiceImpl(AppointmentRepository appointmentRepository,
                                  UserAccountRepository userAccountRepository,
                                  TransactionRepository transactionRepository) {
        this.appointmentRepository = appointmentRepository;
        this.userAccountRepository = userAccountRepository;
        this.transactionRepository = transactionRepository;
    }

    @Override
    public List<AppointmentResponseDTO> getAppointmentsForBroker(UUID brokerId) {
        List<Appointment> appointments = appointmentRepository
                .findByBrokerIdAndDeletedAtIsNullOrderByFromDateTimeAsc(brokerId);
        return mapToDTOs(appointments);
    }

    @Override
    public List<AppointmentResponseDTO> getAppointmentsForClient(UUID clientId) {
        List<Appointment> appointments = appointmentRepository
                .findByClientIdAndDeletedAtIsNullOrderByFromDateTimeAsc(clientId);
        return mapToDTOs(appointments);
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
            UUID clientId, LocalDateTime from, LocalDateTime to) {
        List<Appointment> appointments = appointmentRepository
                .findByClientIdAndDateRange(clientId, from, to);
        return mapToDTOs(appointments);
    }

    @Override
    public List<AppointmentResponseDTO> getAppointmentsForBrokerByStatus(
            UUID brokerId, AppointmentStatus status) {
        List<Appointment> appointments = appointmentRepository
                .findByBrokerIdAndStatusAndDeletedAtIsNullOrderByFromDateTimeAsc(brokerId, status);
        return mapToDTOs(appointments);
    }

    @Override
    public List<AppointmentResponseDTO> getAppointmentsForClientByStatus(
            UUID clientId, AppointmentStatus status) {
        List<Appointment> appointments = appointmentRepository
                .findByClientIdAndStatusAndDeletedAtIsNullOrderByFromDateTimeAsc(clientId, status);
        return mapToDTOs(appointments);
    }

    @Override
    public List<AppointmentResponseDTO> getAppointmentsForTransaction(UUID transactionId, UUID requesterId) {
        Transaction transaction = transactionRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new NotFoundException("Transaction not found: " + transactionId));

        if (!transaction.getBrokerId().equals(requesterId) && !transaction.getClientId().equals(requesterId)) {
            throw new ForbiddenException("You do not have permission to view appointments for this transaction");
        }

        List<Appointment> appointments = appointmentRepository
                .findByTransactionIdAndDeletedAtIsNullOrderByFromDateTimeAsc(transactionId);
        return mapToDTOs(appointments);
    }

    @Override
    public AppointmentResponseDTO getAppointmentById(UUID appointmentId, UUID requesterId) {
        Appointment appointment = appointmentRepository
                .findByAppointmentIdAndDeletedAtIsNull(appointmentId)
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

        return userAccountRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(
                        UserAccount::getId,
                        this::getFullName,
                        (existing, replacement) -> existing
                ));
    }

    private String getFullName(UserAccount user) {
        String firstName = user.getFirstName() != null ? user.getFirstName() : "";
        String lastName = user.getLastName() != null ? user.getLastName() : "";
        return (firstName + " " + lastName).trim();
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
                apt.getCreatedAt(),
                apt.getUpdatedAt()
        );
    }
}
