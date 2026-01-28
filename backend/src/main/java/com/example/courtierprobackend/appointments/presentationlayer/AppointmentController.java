package com.example.courtierprobackend.appointments.presentationlayer;

import com.example.courtierprobackend.appointments.businesslayer.AppointmentService;
import com.example.courtierprobackend.appointments.datalayer.dto.AppointmentResponseDTO;
import com.example.courtierprobackend.appointments.datalayer.enums.AppointmentStatus;
import com.example.courtierprobackend.common.exceptions.BadRequestException;
import com.example.courtierprobackend.security.UserContextUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * REST controller for appointment operations.
 * Currently implements GET endpoints only (CP-23).
 */
@RestController
@RequestMapping("/appointments")
@RequiredArgsConstructor
public class AppointmentController {
    /**
     * Get top 3 upcoming appointments for the authenticated user (broker or client).
     * Used for dashboard widget.
     */
    @GetMapping("/top-upcoming")
    @PreAuthorize("hasAnyRole('BROKER', 'CLIENT')")
    public ResponseEntity<List<AppointmentResponseDTO>> getTopUpcomingAppointments(
            @RequestHeader(value = "x-broker-id", required = false) String brokerHeader,
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest request) {
        try {
            UUID userId = UserContextUtils.resolveUserId(request, brokerHeader);
            boolean isBroker = UserContextUtils.isBroker(request);
            Object roleAttr = request.getAttribute("userRole");
            if (userId == null) {
                return ResponseEntity.badRequest().build();
            }
            List<AppointmentResponseDTO> appointments = isBroker
                    ? appointmentService.getTopUpcomingAppointmentsForBroker(userId, 3)
                    : appointmentService.getTopUpcomingAppointmentsForClient(userId, 3);
            return ResponseEntity.ok(appointments);
        } catch (com.example.courtierprobackend.common.exceptions.ForbiddenException fe) {
            return ResponseEntity.status(403).build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // ...existing code...
    private final AppointmentService appointmentService;

    /**
     * Get all appointments for the authenticated user.
     * Brokers see their appointments, clients see theirs.
     * Supports optional filtering by date range and status.
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('BROKER', 'CLIENT')")
    public ResponseEntity<List<AppointmentResponseDTO>> getAppointments(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(required = false) AppointmentStatus status,
            @RequestHeader(value = "x-broker-id", required = false) String brokerHeader,
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest request) {

        UUID userId = UserContextUtils.resolveUserId(request, brokerHeader);
        boolean isBroker = UserContextUtils.isBroker(request);

        if ((from != null && to == null) || (from == null && to != null)) {
            throw new BadRequestException("Both 'from' and 'to' parameters must be provided for date filtering.");
        }

        List<AppointmentResponseDTO> appointments;

        String requesterEmail = null;
        if (jwt != null) {
            Object emailClaim = jwt.getClaims().get("email");
            if (emailClaim instanceof String) {
                requesterEmail = (String) emailClaim;
            }
        }
        if (from != null && to != null && status != null) {
            // Filter by date range AND status
            appointments = isBroker
                    ? appointmentService.getAppointmentsForBrokerByDateRangeAndStatus(userId, from, to, status)
                    : appointmentService.getAppointmentsForClientByDateRangeAndStatus(userId, from, to, status, userId, requesterEmail);
        } else if (from != null && to != null) {
            // Filter by date range
            appointments = isBroker
                    ? appointmentService.getAppointmentsForBrokerByDateRange(userId, from, to)
                    : appointmentService.getAppointmentsForClientByDateRange(userId, from, to, userId, requesterEmail);
        } else if (status != null) {
            // Filter by status
            appointments = isBroker
                    ? appointmentService.getAppointmentsForBrokerByStatus(userId, status)
                    : appointmentService.getAppointmentsForClientByStatus(userId, status, userId, requesterEmail);
        } else {
            // Get all appointments
            appointments = isBroker
                ? appointmentService.getAppointmentsForBroker(userId)
                : appointmentService.getAppointmentsForClient(userId, userId, null);
        }

        return ResponseEntity.ok(appointments);
    }

    /**
     * Get a specific appointment by its public ID.
     */
    @GetMapping("/{appointmentId}")
    @PreAuthorize("hasAnyRole('BROKER', 'CLIENT')")
    public ResponseEntity<AppointmentResponseDTO> getAppointmentById(
            @PathVariable UUID appointmentId,
            @RequestHeader(value = "x-broker-id", required = false) String brokerHeader,
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest request) {

        UUID userId = UserContextUtils.resolveUserId(request, brokerHeader);
        AppointmentResponseDTO appointment = appointmentService.getAppointmentById(appointmentId, userId);
        return ResponseEntity.ok(appointment);
    }

    /**
     * Get all appointments for a specific transaction.
     */
    @GetMapping("/transaction/{transactionId}")
    @PreAuthorize("hasAnyRole('BROKER', 'CLIENT')")
    public ResponseEntity<List<AppointmentResponseDTO>> getAppointmentsForTransaction(
            @PathVariable UUID transactionId,
            @RequestHeader(value = "x-broker-id", required = false) String brokerHeader,
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest request) {

        UUID userId = UserContextUtils.resolveUserId(request, brokerHeader);
        List<AppointmentResponseDTO> appointments = appointmentService.getAppointmentsForTransaction(transactionId,
                userId);
        return ResponseEntity.ok(appointments);
    }

    /**
     * Request a new appointment.
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('BROKER', 'CLIENT')")
    public ResponseEntity<AppointmentResponseDTO> requestAppointment(
            @RequestBody com.example.courtierprobackend.appointments.datalayer.dto.AppointmentRequestDTO appointmentRequest,
            @RequestHeader(value = "x-broker-id", required = false) String brokerHeader,
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest request) {

        UUID userId = UserContextUtils.resolveUserId(request, brokerHeader);
        AppointmentResponseDTO createdAppointment = appointmentService.requestAppointment(appointmentRequest, userId);

        return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED)
                .body(createdAppointment);
    }

    /**
     * Review an appointment (Confirm, Decline, or Reschedule).
     */
    @PatchMapping("/{appointmentId}/review")
    @PreAuthorize("hasAnyRole('BROKER', 'CLIENT')")
    public ResponseEntity<AppointmentResponseDTO> reviewAppointment(
            @PathVariable UUID appointmentId,
            @RequestBody com.example.courtierprobackend.appointments.datalayer.dto.AppointmentReviewDTO reviewDTO,
            @RequestHeader(value = "x-broker-id", required = false) String brokerHeader,
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest request) {

        UUID userId = UserContextUtils.resolveUserId(request, brokerHeader);
        AppointmentResponseDTO updatedAppointment = appointmentService.reviewAppointment(appointmentId, reviewDTO,
                userId);

        return ResponseEntity.ok(updatedAppointment);
    }

    /**
     * Cancel an appointment.
     */
    @PatchMapping("/{appointmentId}/cancel")
    @PreAuthorize("hasAnyRole('BROKER', 'CLIENT')")
    public ResponseEntity<AppointmentResponseDTO> cancelAppointment(
            @PathVariable UUID appointmentId,
            @RequestBody com.example.courtierprobackend.appointments.datalayer.dto.AppointmentCancellationDTO cancelDTO,
            @RequestHeader(value = "x-broker-id", required = false) String brokerHeader,
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest request) {

        UUID userId = UserContextUtils.resolveUserId(request, brokerHeader);
        AppointmentResponseDTO updatedAppointment = appointmentService.cancelAppointment(appointmentId, cancelDTO,
                userId);

        return ResponseEntity.ok(updatedAppointment);
    }

    @GetMapping("/client/{clientId}")
    @PreAuthorize("hasAnyRole('BROKER', 'ADMIN')")
    public ResponseEntity<List<AppointmentResponseDTO>> getAppointmentsForClient(
            @PathVariable UUID clientId,
            @RequestHeader(value = "x-broker-id", required = false) String brokerHeader,
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest request) {
        UUID requesterId = UserContextUtils.resolveUserId(request, brokerHeader);
        // If you have a way to get the email, set it here. Otherwise, pass null for now.
        String requesterEmail = null;
        if (jwt != null) {
            Object emailClaim = jwt.getClaims().get("email");
            if (emailClaim instanceof String) {
                requesterEmail = (String) emailClaim;
            }
        }
        List<AppointmentResponseDTO> appointments = appointmentService.getAppointmentsForClient(clientId, requesterId, requesterEmail);
        return ResponseEntity.ok(appointments);
    }
}
