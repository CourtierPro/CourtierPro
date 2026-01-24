package com.example.courtierprobackend.appointments.datalayer.dto;

import com.example.courtierprobackend.appointments.datalayer.enums.AppointmentStatus;
import com.example.courtierprobackend.appointments.datalayer.enums.InitiatorType;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for appointment data.
 */
public record AppointmentResponseDTO(
        UUID appointmentId,
        String title,
        UUID transactionId,
        UUID brokerId,
        String brokerName,
        UUID clientId,
        String clientName,
        LocalDateTime fromDateTime,
        LocalDateTime toDateTime,
        AppointmentStatus status,
        InitiatorType initiatedBy,
        String location,
        Double latitude,
        Double longitude,
        String notes,
        String refusalReason,
        String cancellationReason,
        UUID cancelledBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
