package com.example.courtierprobackend.appointments.datalayer;

import com.example.courtierprobackend.appointments.datalayer.enums.AppointmentStatus;
import com.example.courtierprobackend.appointments.datalayer.enums.InitiatorType;
import jakarta.persistence.*;
import org.hibernate.annotations.Where;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Appointment entity representing a scheduled meeting between broker and
 * client.
 */
@Entity
@Table(name = "appointments")
@Where(clause = "deleted_at IS NULL")
public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "appointment_id", nullable = false, unique = true)
    private UUID appointmentId;

    @Column(nullable = false)
    private String title;

    @Column(name = "transaction_id")
    private UUID transactionId;

    @Column(name = "broker_id", nullable = false)
    private UUID brokerId;

    @Column(name = "client_id", nullable = false)
    private UUID clientId;

    @Column(name = "from_date_time", nullable = false)
    private LocalDateTime fromDateTime;

    @Column(name = "to_date_time", nullable = false)
    private LocalDateTime toDateTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AppointmentStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "initiated_by", nullable = false)
    private InitiatorType initiatedBy;

    @Column(name = "responded_by")
    private UUID respondedBy;

    @Column(name = "responded_at")
    private LocalDateTime respondedAt;

    @Column(length = 500)
    private String location;

    private Double latitude;

    private Double longitude;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "refusal_reason", columnDefinition = "TEXT")
    private String refusalReason;

    @Column(name = "cancellation_reason", columnDefinition = "TEXT")
    private String cancellationReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Version
    private Long version;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "cancelled_by")
    private UUID cancelledBy;

    @Column(name = "deleted_by")
    private UUID deletedBy;

    @Column(name = "reminder_sent", nullable = false)
    private Boolean reminderSent = false;

    public Appointment() {
    }

    @PrePersist
    protected void onCreate() {
        if (appointmentId == null) {
            appointmentId = UUID.randomUUID();
        }
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        validateDates();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        validateDates();
    }

    private void validateDates() {
        if (fromDateTime != null && toDateTime != null) {
            if (!toDateTime.isAfter(fromDateTime)) {
                throw new IllegalArgumentException("Appointment end time must be after start time");
            }
        }
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public UUID getAppointmentId() {
        return appointmentId;
    }

    public void setAppointmentId(UUID appointmentId) {
        this.appointmentId = appointmentId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public UUID getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(UUID transactionId) {
        this.transactionId = transactionId;
    }

    public UUID getBrokerId() {
        return brokerId;
    }

    public void setBrokerId(UUID brokerId) {
        this.brokerId = brokerId;
    }

    public UUID getClientId() {
        return clientId;
    }

    public void setClientId(UUID clientId) {
        this.clientId = clientId;
    }

    public LocalDateTime getFromDateTime() {
        return fromDateTime;
    }

    public void setFromDateTime(LocalDateTime fromDateTime) {
        this.fromDateTime = fromDateTime;
    }

    public LocalDateTime getToDateTime() {
        return toDateTime;
    }

    public void setToDateTime(LocalDateTime toDateTime) {
        this.toDateTime = toDateTime;
    }

    public AppointmentStatus getStatus() {
        return status;
    }

    public void setStatus(AppointmentStatus status) {
        this.status = status;
    }

    public InitiatorType getInitiatedBy() {
        return initiatedBy;
    }

    public void setInitiatedBy(InitiatorType initiatedBy) {
        this.initiatedBy = initiatedBy;
    }

    public UUID getRespondedBy() {
        return respondedBy;
    }

    public void setRespondedBy(UUID respondedBy) {
        this.respondedBy = respondedBy;
    }

    public LocalDateTime getRespondedAt() {
        return respondedAt;
    }

    public void setRespondedAt(LocalDateTime respondedAt) {
        this.respondedAt = respondedAt;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getRefusalReason() {
        return refusalReason;
    }

    public void setRefusalReason(String refusalReason) {
        this.refusalReason = refusalReason;
    }

    public String getCancellationReason() {
        return cancellationReason;
    }

    public void setCancellationReason(String cancellationReason) {
        this.cancellationReason = cancellationReason;
    }

    public UUID getCancelledBy() {
        return cancelledBy;
    }

    public void setCancelledBy(UUID cancelledBy) {
        this.cancelledBy = cancelledBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }

    public UUID getDeletedBy() {
        return deletedBy;
    }

    public void setDeletedBy(UUID deletedBy) {
        this.deletedBy = deletedBy;
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public Boolean getReminderSent() {
        return reminderSent;
    }

    public void setReminderSent(Boolean reminderSent) {
        this.reminderSent = reminderSent;
    }
}
