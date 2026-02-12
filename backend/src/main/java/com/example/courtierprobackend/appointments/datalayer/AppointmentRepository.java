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
         * An appointment overlaps if it starts before the range ends AND ends after the
         * range starts.
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
         * An appointment overlaps if it starts before the range ends AND ends after the
         * range starts.
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
         * Find appointments for a broker that overlap with a date range and have
         * specific status.
         * An appointment overlaps if it starts before the range ends AND ends after the
         * range starts.
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
         * Find appointments for a client that overlap with a date range and have
         * specific status.
         * An appointment overlaps if it starts before the range ends AND ends after the
         * range starts.
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

        /**
         * Find appointments for reminder job:
         * - starting between start and end
         * - reminder not sent yet
         * - status is NOT the excluded status (e.g. CANCELLED / DECLINED)
         * 
         * Note: JPA method naming convention or @Query can be used.
         * We'll use a custom query for clarity and to handle multiple status exclusions
         * if needed,
         * or simply chain the method name if it's just one status. But the requirement
         * asks for "NOT CANCELLED/DECLINED".
         * So better to use a query or a method that takes a list of statuses to exclude
         * (NotInto).
         * 
         * Requirement: "query the repository for appointments starting between 24h and
         * 25h from now that have reminderSent = false and are not CANCELLED/DECLINED"
         */
        List<Appointment> findByFromDateTimeBetweenAndReminderSentFalseAndStatusNotInAndDeletedAtIsNull(
                        LocalDateTime start,
                        LocalDateTime end, java.util.Collection<AppointmentStatus> statuses);

        /**
         * Count confirmed house visit appointments for a specific property.
         */
        @Query("SELECT COUNT(a) FROM Appointment a WHERE a.propertyId = :propertyId " +
                        "AND a.title = 'house_visit' AND a.status = com.example.courtierprobackend.appointments.datalayer.enums.AppointmentStatus.CONFIRMED " +
                        "AND a.deletedAt IS NULL")
        int countConfirmedHouseVisitsByPropertyId(@Param("propertyId") UUID propertyId);

        /**
         * Count confirmed house visit appointments for a specific transaction.
         */
        @Query("SELECT COUNT(a) FROM Appointment a WHERE a.transactionId = :transactionId " +
                        "AND a.title = 'house_visit' AND a.status = com.example.courtierprobackend.appointments.datalayer.enums.AppointmentStatus.CONFIRMED " +
                        "AND a.deletedAt IS NULL")
        int countConfirmedHouseVisitsByTransactionId(@Param("transactionId") UUID transactionId);

        /**
         * Batch count confirmed house visits per property for a list of property IDs.
         * Returns rows of [propertyId, count].
         */
        @Query("SELECT a.propertyId, COUNT(a) FROM Appointment a " +
                        "WHERE a.propertyId IN :propertyIds " +
                        "AND a.title = 'house_visit' AND a.status = com.example.courtierprobackend.appointments.datalayer.enums.AppointmentStatus.CONFIRMED " +
                        "AND a.deletedAt IS NULL " +
                        "GROUP BY a.propertyId")
        List<Object[]> countConfirmedHouseVisitsByPropertyIds(@Param("propertyIds") List<UUID> propertyIds);
}
