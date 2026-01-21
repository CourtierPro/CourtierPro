package com.example.courtierprobackend.appointments.datalayer;

import com.example.courtierprobackend.appointments.datalayer.enums.AppointmentStatus;
import com.example.courtierprobackend.appointments.datalayer.enums.InitiatorType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for Appointment entity.
 * Covers all getters, setters, lifecycle callbacks, and utility methods.
 */
class AppointmentTest {

    private Appointment appointment;

    @BeforeEach
    void setUp() {
        appointment = new Appointment();
    }

    // ========== Lifecycle Callbacks ==========

    @Test
    void onCreate_setsAppointmentIdAndTimestamps() {
        // Arrange
        assertThat(appointment.getAppointmentId()).isNull();
        assertThat(appointment.getCreatedAt()).isNull();
        assertThat(appointment.getUpdatedAt()).isNull();

        // Act
        appointment.onCreate();

        // Assert
        assertThat(appointment.getAppointmentId()).isNotNull();
        assertThat(appointment.getCreatedAt()).isNotNull();
        assertThat(appointment.getUpdatedAt()).isNotNull();
        // Both timestamps are set from LocalDateTime.now() so they should be within 1 second
        assertThat(appointment.getCreatedAt())
                .isCloseTo(appointment.getUpdatedAt(), org.assertj.core.api.Assertions.within(1, java.time.temporal.ChronoUnit.SECONDS));
    }

    @Test
    void onCreate_preservesExistingAppointmentId() {
        // Arrange
        UUID existingId = UUID.randomUUID();
        appointment.setAppointmentId(existingId);

        // Act
        appointment.onCreate();

        // Assert
        assertThat(appointment.getAppointmentId()).isEqualTo(existingId);
    }

    @Test
    void onUpdate_updatesTimestamp() {
        // Arrange
        LocalDateTime originalTime = LocalDateTime.now().minusHours(1);
        appointment.setUpdatedAt(originalTime);

        // Act
        appointment.onUpdate();

        // Assert
        assertThat(appointment.getUpdatedAt()).isAfter(originalTime);
    }

    // ========== isDeleted ==========

    @Test
    void isDeleted_returnsFalseWhenDeletedAtIsNull() {
        // Arrange
        appointment.setDeletedAt(null);

        // Act & Assert
        assertThat(appointment.isDeleted()).isFalse();
    }

    @Test
    void isDeleted_returnsTrueWhenDeletedAtIsSet() {
        // Arrange
        appointment.setDeletedAt(LocalDateTime.now());

        // Act & Assert
        assertThat(appointment.isDeleted()).isTrue();
    }

    // ========== Getters and Setters ==========

    @Test
    void testIdGetterAndSetter() {
        Long id = 1L;
        appointment.setId(id);
        assertThat(appointment.getId()).isEqualTo(id);
    }

    @Test
    void testAppointmentIdGetterAndSetter() {
        UUID uuid = UUID.randomUUID();
        appointment.setAppointmentId(uuid);
        assertThat(appointment.getAppointmentId()).isEqualTo(uuid);
    }

    @Test
    void testTitleGetterAndSetter() {
        String title = "Meeting with Client";
        appointment.setTitle(title);
        assertThat(appointment.getTitle()).isEqualTo(title);
    }

    @Test
    void testTransactionIdGetterAndSetter() {
        UUID transactionId = UUID.randomUUID();
        appointment.setTransactionId(transactionId);
        assertThat(appointment.getTransactionId()).isEqualTo(transactionId);
    }

    @Test
    void testBrokerIdGetterAndSetter() {
        UUID brokerId = UUID.randomUUID();
        appointment.setBrokerId(brokerId);
        assertThat(appointment.getBrokerId()).isEqualTo(brokerId);
    }

    @Test
    void testClientIdGetterAndSetter() {
        UUID clientId = UUID.randomUUID();
        appointment.setClientId(clientId);
        assertThat(appointment.getClientId()).isEqualTo(clientId);
    }

    @Test
    void testFromDateTimeGetterAndSetter() {
        LocalDateTime fromDateTime = LocalDateTime.now();
        appointment.setFromDateTime(fromDateTime);
        assertThat(appointment.getFromDateTime()).isEqualTo(fromDateTime);
    }

    @Test
    void testToDateTimeGetterAndSetter() {
        LocalDateTime toDateTime = LocalDateTime.now().plusHours(1);
        appointment.setToDateTime(toDateTime);
        assertThat(appointment.getToDateTime()).isEqualTo(toDateTime);
    }

    @Test
    void testStatusGetterAndSetter() {
        appointment.setStatus(AppointmentStatus.PROPOSED);
        assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.PROPOSED);

        appointment.setStatus(AppointmentStatus.CONFIRMED);
        assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.CONFIRMED);

        appointment.setStatus(AppointmentStatus.DECLINED);
        assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.DECLINED);

        appointment.setStatus(AppointmentStatus.CANCELLED);
        assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.CANCELLED);
    }

    @Test
    void testInitiatedByGetterAndSetter() {
        appointment.setInitiatedBy(InitiatorType.BROKER);
        assertThat(appointment.getInitiatedBy()).isEqualTo(InitiatorType.BROKER);

        appointment.setInitiatedBy(InitiatorType.CLIENT);
        assertThat(appointment.getInitiatedBy()).isEqualTo(InitiatorType.CLIENT);
    }

    @Test
    void testRespondedByGetterAndSetter() {
        UUID respondedBy = UUID.randomUUID();
        appointment.setRespondedBy(respondedBy);
        assertThat(appointment.getRespondedBy()).isEqualTo(respondedBy);
    }

    @Test
    void testRespondedAtGetterAndSetter() {
        LocalDateTime respondedAt = LocalDateTime.now();
        appointment.setRespondedAt(respondedAt);
        assertThat(appointment.getRespondedAt()).isEqualTo(respondedAt);
    }

    @Test
    void testLocationGetterAndSetter() {
        String location = "123 Main St, Montreal, QC";
        appointment.setLocation(location);
        assertThat(appointment.getLocation()).isEqualTo(location);
    }

    @Test
    void testLatitudeGetterAndSetter() {
        Double latitude = 45.5017;
        appointment.setLatitude(latitude);
        assertThat(appointment.getLatitude()).isEqualTo(latitude);
    }

    @Test
    void testLongitudeGetterAndSetter() {
        Double longitude = -73.5673;
        appointment.setLongitude(longitude);
        assertThat(appointment.getLongitude()).isEqualTo(longitude);
    }

    @Test
    void testNotesGetterAndSetter() {
        String notes = "Important meeting notes";
        appointment.setNotes(notes);
        assertThat(appointment.getNotes()).isEqualTo(notes);
    }

    @Test
    void testCreatedAtGetterAndSetter() {
        LocalDateTime createdAt = LocalDateTime.now();
        appointment.setCreatedAt(createdAt);
        assertThat(appointment.getCreatedAt()).isEqualTo(createdAt);
    }

    @Test
    void testUpdatedAtGetterAndSetter() {
        LocalDateTime updatedAt = LocalDateTime.now();
        appointment.setUpdatedAt(updatedAt);
        assertThat(appointment.getUpdatedAt()).isEqualTo(updatedAt);
    }

    @Test
    void testVersionGetterAndSetter() {
        Long version = 5L;
        appointment.setVersion(version);
        assertThat(appointment.getVersion()).isEqualTo(version);
    }

    @Test
    void testDeletedAtGetterAndSetter() {
        LocalDateTime deletedAt = LocalDateTime.now();
        appointment.setDeletedAt(deletedAt);
        assertThat(appointment.getDeletedAt()).isEqualTo(deletedAt);
    }

    @Test
    void testDeletedByGetterAndSetter() {
        UUID deletedBy = UUID.randomUUID();
        appointment.setDeletedBy(deletedBy);
        assertThat(appointment.getDeletedBy()).isEqualTo(deletedBy);
    }

    @Test
    void testFullAppointmentCreation() {
        // Arrange - create a fully populated appointment
        UUID appointmentId = UUID.randomUUID();
        UUID transactionId = UUID.randomUUID();
        UUID brokerId = UUID.randomUUID();
        UUID clientId = UUID.randomUUID();
        LocalDateTime fromDateTime = LocalDateTime.now();
        LocalDateTime toDateTime = LocalDateTime.now().plusHours(1);

        // Act
        appointment.setAppointmentId(appointmentId);
        appointment.setTitle("Property Showing");
        appointment.setTransactionId(transactionId);
        appointment.setBrokerId(brokerId);
        appointment.setClientId(clientId);
        appointment.setFromDateTime(fromDateTime);
        appointment.setToDateTime(toDateTime);
        appointment.setStatus(AppointmentStatus.CONFIRMED);
        appointment.setInitiatedBy(InitiatorType.BROKER);
        appointment.setLocation("456 Oak Avenue");
        appointment.setLatitude(45.5);
        appointment.setLongitude(-73.5);
        appointment.setNotes("Bring documents");

        // Assert
        assertThat(appointment.getAppointmentId()).isEqualTo(appointmentId);
        assertThat(appointment.getTitle()).isEqualTo("Property Showing");
        assertThat(appointment.getTransactionId()).isEqualTo(transactionId);
        assertThat(appointment.getBrokerId()).isEqualTo(brokerId);
        assertThat(appointment.getClientId()).isEqualTo(clientId);
        assertThat(appointment.getFromDateTime()).isEqualTo(fromDateTime);
        assertThat(appointment.getToDateTime()).isEqualTo(toDateTime);
        assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.CONFIRMED);
        assertThat(appointment.getInitiatedBy()).isEqualTo(InitiatorType.BROKER);
        assertThat(appointment.getLocation()).isEqualTo("456 Oak Avenue");
        assertThat(appointment.getLatitude()).isEqualTo(45.5);
        assertThat(appointment.getLongitude()).isEqualTo(-73.5);
        assertThat(appointment.getNotes()).isEqualTo("Bring documents");
    }
}
