package com.example.courtierprobackend.appointments.datalayer;

import com.example.courtierprobackend.appointments.datalayer.enums.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Appointment entity operations.
 */
@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    /**
     * Find appointment by public UUID.
     */
    Optional<Appointment> findByAppointmentIdAndDeletedAtIsNull(UUID appointmentId);

    /**
     * Find all appointments for a broker (not deleted).
     */
    List<Appointment> findByBrokerIdAndDeletedAtIsNullOrderByFromDateTimeAsc(UUID brokerId);

    /**
     * Find all appointments for a client (not deleted).
     */
    List<Appointment> findByClientIdAndDeletedAtIsNullOrderByFromDateTimeAsc(UUID clientId);

    /**
     * Find appointments for a broker that overlap with a date range.
     * An appointment overlaps if it starts before the range ends AND ends after the range starts.
     */
    @Query("SELECT a FROM Appointment a WHERE a.brokerId = :brokerId " +
           "AND a.deletedAt IS NULL " +
           "AND a.fromDateTime < :toDate AND a.toDateTime > :fromDate " +
           "ORDER BY a.fromDateTime ASC")
    List<Appointment> findByBrokerIdAndDateRange(
            @Param("brokerId") UUID brokerId,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate);

    /**
     * Find appointments for a client that overlap with a date range.
     * An appointment overlaps if it starts before the range ends AND ends after the range starts.
     */
    @Query("SELECT a FROM Appointment a WHERE a.clientId = :clientId " +
           "AND a.deletedAt IS NULL " +
           "AND a.fromDateTime < :toDate AND a.toDateTime > :fromDate " +
           "ORDER BY a.fromDateTime ASC")
    List<Appointment> findByClientIdAndDateRange(
            @Param("clientId") UUID clientId,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate);

    /**
     * Find appointments for a broker with specific status.
     */
    List<Appointment> findByBrokerIdAndStatusAndDeletedAtIsNullOrderByFromDateTimeAsc(
            UUID brokerId, AppointmentStatus status);

    /**
     * Find appointments for a client with specific status.
     */
    List<Appointment> findByClientIdAndStatusAndDeletedAtIsNullOrderByFromDateTimeAsc(
            UUID clientId, AppointmentStatus status);

    /**
     * Find appointments for a broker that overlap with a date range and have specific status.
     * An appointment overlaps if it starts before the range ends AND ends after the range starts.
     */
    @Query("SELECT a FROM Appointment a WHERE a.brokerId = :brokerId " +
           "AND a.deletedAt IS NULL " +
           "AND a.status = :status " +
           "AND a.fromDateTime < :toDate AND a.toDateTime > :fromDate " +
           "ORDER BY a.fromDateTime ASC")
    List<Appointment> findByBrokerIdAndDateRangeAndStatus(
            @Param("brokerId") UUID brokerId,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            @Param("status") AppointmentStatus status);

    /**
     * Find appointments for a client that overlap with a date range and have specific status.
     * An appointment overlaps if it starts before the range ends AND ends after the range starts.
     */
    @Query("SELECT a FROM Appointment a WHERE a.clientId = :clientId " +
           "AND a.deletedAt IS NULL " +
           "AND a.status = :status " +
           "AND a.fromDateTime < :toDate AND a.toDateTime > :fromDate " +
           "ORDER BY a.fromDateTime ASC")
    List<Appointment> findByClientIdAndDateRangeAndStatus(
            @Param("clientId") UUID clientId,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            @Param("status") AppointmentStatus status);

    /**
     * Find appointments for a specific transaction.
     */
    List<Appointment> findByTransactionIdAndDeletedAtIsNullOrderByFromDateTimeAsc(UUID transactionId);

    /**
     * Find upcoming appointments for a broker (from now onwards).
     */
    @Query("SELECT a FROM Appointment a WHERE a.brokerId = :brokerId " +
           "AND a.deletedAt IS NULL " +
           "AND a.fromDateTime >= :now " +
           "ORDER BY a.fromDateTime ASC")
    List<Appointment> findUpcomingByBrokerId(@Param("brokerId") UUID brokerId, @Param("now") LocalDateTime now);

    /**
     * Find upcoming appointments for a client (from now onwards).
     */
    @Query("SELECT a FROM Appointment a WHERE a.clientId = :clientId " +
           "AND a.deletedAt IS NULL " +
           "AND a.fromDateTime >= :now " +
           "ORDER BY a.fromDateTime ASC")
    List<Appointment> findUpcomingByClientId(@Param("clientId") UUID clientId, @Param("now") LocalDateTime now);
}
