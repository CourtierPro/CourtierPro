package com.example.courtierprobackend.appointments.datalayer.enums;

/**
 * Status of an appointment throughout its lifecycle.
 */
public enum AppointmentStatus {
    PROPOSED,    // Appointment has been requested but not yet confirmed
    CONFIRMED,   // Appointment has been accepted by both parties
    DECLINED,    // Appointment was rejected by the recipient
    CANCELLED    // Appointment was cancelled after being confirmed
}
