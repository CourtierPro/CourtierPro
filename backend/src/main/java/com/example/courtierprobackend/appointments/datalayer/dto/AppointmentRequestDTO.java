package com.example.courtierprobackend.appointments.datalayer.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record AppointmentRequestDTO(
        UUID transactionId,
        String type,
        String title,
        LocalDate date,
        LocalTime startTime,
        LocalTime endTime,
        String message,
        UUID propertyId,
        UUID visitorId) {
}
