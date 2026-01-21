package com.example.courtierprobackend.appointments.businesslayer;

import com.example.courtierprobackend.appointments.datalayer.dto.AppointmentResponseDTO;
import com.example.courtierprobackend.appointments.datalayer.enums.AppointmentStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Service interface for appointment operations.
 */
public interface AppointmentService {

    /**
     * Get all appointments for a broker.
     */
    List<AppointmentResponseDTO> getAppointmentsForBroker(UUID brokerId);

    /**
     * Get all appointments for a client.
     */
    List<AppointmentResponseDTO> getAppointmentsForClient(UUID clientId);

    /**
     * Get appointments for a broker within a date range.
     */
    List<AppointmentResponseDTO> getAppointmentsForBrokerByDateRange(
            UUID brokerId, LocalDateTime from, LocalDateTime to);

    /**
     * Get appointments for a client within a date range.
     */
    List<AppointmentResponseDTO> getAppointmentsForClientByDateRange(
            UUID clientId, LocalDateTime from, LocalDateTime to);

    /**
     * Get appointments for a broker with specific status.
     */
    List<AppointmentResponseDTO> getAppointmentsForBrokerByStatus(
            UUID brokerId, AppointmentStatus status);

    /**
     * Get appointments for a client with specific status.
     */
    List<AppointmentResponseDTO> getAppointmentsForClientByStatus(
            UUID clientId, AppointmentStatus status);

    /**
     * Get appointments for a specific transaction.
     */
    List<AppointmentResponseDTO> getAppointmentsForTransaction(UUID transactionId);

    /**
     * Get a single appointment by its public ID.
     */
    AppointmentResponseDTO getAppointmentById(UUID appointmentId);
}
