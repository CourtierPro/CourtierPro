package com.example.courtierprobackend.appointments.datalayer.dto;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * DTO for reviewing an appointment request.
 */
public record AppointmentReviewDTO(
        ReviewAction action,
        String refusalReason,
        LocalDate newDate,
        LocalTime newStartTime,
        LocalTime newEndTime) {
    public enum ReviewAction {
        CONFIRM,
        DECLINE,
        RESCHEDULE
    }
}
