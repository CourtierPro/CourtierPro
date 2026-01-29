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
         * Get top N upcoming appointments for a broker (from now onwards).
         */
        List<AppointmentResponseDTO> getTopUpcomingAppointmentsForBroker(UUID brokerId, int limit);

        /**
         * Get top N upcoming appointments for a client (from now onwards).
         */
        List<AppointmentResponseDTO> getTopUpcomingAppointmentsForClient(UUID clientId, int limit);

        /**
         * Get all appointments for a broker.
         */
        List<AppointmentResponseDTO> getAppointmentsForBroker(UUID brokerId);

        /**
         * Get all appointments for a client.
         *
         * @param clientId the public identifier of the client whose appointments are requested
         * @param requesterId the identifier of the authenticated requester, used to enforce access
         *                   control and ensure only authorized users can view the client's appointments
         * @param requesterEmail the email of the authenticated requester, used to support additional
         *                      access control checks or auditing as part of the filtering logic
         * @return a list of appointments the requester is authorized to view for the given client
         */
        List<AppointmentResponseDTO> getAppointmentsForClient(UUID clientId, UUID requesterId, String requesterEmail);

        /**
         * Get appointments for a broker within a date range.
         */
        List<AppointmentResponseDTO> getAppointmentsForBrokerByDateRange(
                        UUID brokerId, LocalDateTime from, LocalDateTime to);

        /**
         * Get appointments for a client within a date range.
         */
        List<AppointmentResponseDTO> getAppointmentsForClientByDateRange(
                        UUID clientId, LocalDateTime from, LocalDateTime to, UUID requesterId, String requesterEmail);

        /**
         * Get appointments for a broker with specific status.
         */
        List<AppointmentResponseDTO> getAppointmentsForBrokerByStatus(
                        UUID brokerId, AppointmentStatus status);

        /**
         * Get appointments for a client with specific status.
         */
        List<AppointmentResponseDTO> getAppointmentsForClientByStatus(
                        UUID clientId, AppointmentStatus status, UUID requesterId, String requesterEmail);

        /**
         * Get appointments for a broker within a date range and with specific status.
         */
        List<AppointmentResponseDTO> getAppointmentsForBrokerByDateRangeAndStatus(
                        UUID brokerId, LocalDateTime from, LocalDateTime to, AppointmentStatus status);

        /**
         * Get appointments for a client within a date range and with specific status.
         */
        List<AppointmentResponseDTO> getAppointmentsForClientByDateRangeAndStatus(
                        UUID clientId, LocalDateTime from, LocalDateTime to, AppointmentStatus status, UUID requesterId, String requesterEmail);

        /**
         * Get appointments for a specific transaction.
         * Includes transaction ownership verification.
         */
        List<AppointmentResponseDTO> getAppointmentsForTransaction(UUID transactionId, UUID requesterId);

        /**
         * Get a single appointment by its public ID.
         * Includes ownership verification.
         */
        AppointmentResponseDTO getAppointmentById(UUID appointmentId, UUID requesterId);

        /**
         * Request a new appointment.
         */
        AppointmentResponseDTO requestAppointment(
                        com.example.courtierprobackend.appointments.datalayer.dto.AppointmentRequestDTO request,
                        UUID requesterId);

        /**
         * Review an appointment (Confirm, Decline, or Reschedule).
         */
        AppointmentResponseDTO reviewAppointment(
                        UUID appointmentId,
                        com.example.courtierprobackend.appointments.datalayer.dto.AppointmentReviewDTO reviewDTO,
                        UUID reviewerId);

        /**
         * Cancel an appointment (Confirmed or Proposed/Withdraw).
         */
        AppointmentResponseDTO cancelAppointment(
                        UUID appointmentId,
                        com.example.courtierprobackend.appointments.datalayer.dto.AppointmentCancellationDTO cancelDTO,
                        UUID requesterId);

}
