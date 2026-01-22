package com.example.courtierprobackend.appointments.datalayer.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record AppointmentRequestDTO(
        UUID transactionId,
        String type,
        LocalDate date,
        LocalTime time,
        String message) {
}
