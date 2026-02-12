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
        Optional<Appointment> findByAppointmentId(UUID appointmentId);

        /**
         * Find all appointments for a broker (not deleted).
         */
        List<Appointment> findByBrokerIdOrderByFromDateTimeAsc(UUID brokerId);

        /**
         * Find all appointments for a client (not deleted).
         */
        List<Appointment> findByClientIdOrderByFromDateTimeAsc(UUID clientId);

        /**
         * Find appointments for a broker that overlap with a date range.
         * An appointment overlaps if it starts before the range ends AND ends after the
         * range starts.
         */
        @Query("SELECT a FROM Appointment a WHERE a.brokerId = :brokerId " +
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
                        "AND a.fromDateTime < :toDate AND a.toDateTime > :fromDate " +
                        "ORDER BY a.fromDateTime ASC")
        List<Appointment> findByClientIdAndDateRange(
                        @Param("clientId") UUID clientId,
                        @Param("fromDate") LocalDateTime fromDate,
                        @Param("toDate") LocalDateTime toDate);

        /**
         * Find appointments for a broker with specific status.
         */
        List<Appointment> findByBrokerIdAndStatusOrderByFromDateTimeAsc(
                        UUID brokerId, AppointmentStatus status);

        /**
         * Find appointments for a client with specific status.
         */
        List<Appointment> findByClientIdAndStatusOrderByFromDateTimeAsc(
                        UUID clientId, AppointmentStatus status);

        /**
         * Find appointments for a broker that overlap with a date range and have
         * specific status.
         * An appointment overlaps if it starts before the range ends AND ends after the
         * range starts.
         */
        @Query("SELECT a FROM Appointment a WHERE a.brokerId = :brokerId " +
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
        List<Appointment> findByTransactionIdOrderByFromDateTimeAsc(UUID transactionId);

        /**
         * Find upcoming appointments for a broker (from now onwards).
         */
        @Query("SELECT a FROM Appointment a WHERE a.brokerId = :brokerId " +
                        "AND a.fromDateTime >= :now " +
                        "ORDER BY a.fromDateTime ASC")
        List<Appointment> findUpcomingByBrokerId(@Param("brokerId") UUID brokerId, @Param("now") LocalDateTime now);

        /**
         * Find upcoming appointments for a client (from now onwards).
         */
        @Query("SELECT a FROM Appointment a WHERE a.clientId = :clientId " +
                        "AND a.fromDateTime >= :now " +
                        "ORDER BY a.fromDateTime ASC")
        List<Appointment> findUpcomingByClientId(@Param("clientId") UUID clientId, @Param("now") LocalDateTime now);

        /**
         * Find appointments for reminder job:
         * - starting between start and end
         * - reminder not sent yet
         * - status is NOT the excluded status (e.g. CANCELLED / DECLINED)
         */
        List<Appointment> findByFromDateTimeBetweenAndReminderSentFalseAndStatusNotIn(
                        LocalDateTime start,
                        LocalDateTime end, java.util.Collection<AppointmentStatus> statuses);

        @Query(value = "SELECT * FROM appointments", nativeQuery = true)
        List<Appointment> findAllIncludingDeleted();

        /**
         * Find appointment by ID including soft-deleted ones.
         */
        @Query(value = "SELECT * FROM appointments WHERE appointment_id = :appointmentId", nativeQuery = true)
        Optional<Appointment> findByAppointmentIdIncludingDeleted(@Param("appointmentId") UUID appointmentId);

        /**
         * Find appointments for a specific transaction including deleted ones.
         */
        @Query(value = "SELECT * FROM appointments WHERE transaction_id = :transactionId", nativeQuery = true)
        List<Appointment> findByTransactionIdIncludingDeleted(@Param("transactionId") UUID transactionId);

        /**
         * Count confirmed house visit appointments for a specific property.
         */
        @Query("SELECT COUNT(a) FROM Appointment a WHERE a.propertyId = :propertyId " +
                        "AND a.title = 'house_visit' AND a.status = com.example.courtierprobackend.appointments.datalayer.enums.AppointmentStatus.CONFIRMED "
                        +
                        "AND a.deletedAt IS NULL")
        int countConfirmedHouseVisitsByPropertyId(@Param("propertyId") UUID propertyId);

        /**
         * Count confirmed house visit appointments for a specific transaction.
         */
        @Query("SELECT COUNT(a) FROM Appointment a WHERE a.transactionId = :transactionId " +
                        "AND a.title = 'house_visit' AND a.status = com.example.courtierprobackend.appointments.datalayer.enums.AppointmentStatus.CONFIRMED "
                        +
                        "AND a.deletedAt IS NULL")
        int countConfirmedHouseVisitsByTransactionId(@Param("transactionId") UUID transactionId);

        /**
         * Batch count confirmed house visits per property for a list of property IDs.
         * Returns rows of [propertyId, count].
         */
        @Query("SELECT a.propertyId, COUNT(a) FROM Appointment a " +
                        "WHERE a.propertyId IN :propertyIds " +
                        "AND a.title = 'house_visit' AND a.status = com.example.courtierprobackend.appointments.datalayer.enums.AppointmentStatus.CONFIRMED "
                        +
                        "AND a.deletedAt IS NULL " +
                        "GROUP BY a.propertyId")
        List<Object[]> countConfirmedHouseVisitsByPropertyIds(@Param("propertyIds") List<UUID> propertyIds);

        /**
         * Batch count confirmed house visits per transaction for a list of transaction
         * IDs.
         * Returns rows of [transactionId, count].
         */
        @Query("SELECT a.transactionId, COUNT(a) FROM Appointment a " +
                        "WHERE a.transactionId IN :transactionIds " +
                        "AND a.title = 'house_visit' AND a.status = com.example.courtierprobackend.appointments.datalayer.enums.AppointmentStatus.CONFIRMED "
                        +
                        "AND a.deletedAt IS NULL " +
                        "GROUP BY a.transactionId")
        List<Object[]> countConfirmedHouseVisitsByTransactionIds(@Param("transactionIds") List<UUID> transactionIds);

        /**
         * Count confirmed sell-side showings (open_house + private_showing) for a transaction.
         */
        @Query("SELECT COUNT(a) FROM Appointment a WHERE a.transactionId = :transactionId " +
                        "AND a.title IN ('open_house', 'private_showing') " +
                        "AND a.status = com.example.courtierprobackend.appointments.datalayer.enums.AppointmentStatus.CONFIRMED " +
                        "AND a.deletedAt IS NULL")
        int countConfirmedShowingsByTransactionId(@Param("transactionId") UUID transactionId);

        /**
         * Sum numberOfVisitors for confirmed sell-side showings for a transaction.
         */
        @Query("SELECT COALESCE(SUM(a.numberOfVisitors), 0) FROM Appointment a " +
                        "WHERE a.transactionId = :transactionId " +
                        "AND a.title IN ('open_house', 'private_showing') " +
                        "AND a.status = com.example.courtierprobackend.appointments.datalayer.enums.AppointmentStatus.CONFIRMED " +
                        "AND a.deletedAt IS NULL")
        int sumVisitorsByTransactionId(@Param("transactionId") UUID transactionId);

        /**
         * Batch sum numberOfVisitors for confirmed sell-side showings per transaction.
         * Returns rows of [transactionId, visitorSum].
         */
        @Query("SELECT a.transactionId, COALESCE(SUM(a.numberOfVisitors), 0) FROM Appointment a " +
                        "WHERE a.transactionId IN :transactionIds " +
                        "AND a.title IN ('open_house', 'private_showing') " +
                        "AND a.status = com.example.courtierprobackend.appointments.datalayer.enums.AppointmentStatus.CONFIRMED " +
                        "AND a.deletedAt IS NULL " +
                        "GROUP BY a.transactionId")
        List<Object[]> sumVisitorsByTransactionIds(@Param("transactionIds") List<UUID> transactionIds);

        /**
         * Count confirmed private showings for a specific visitor (for timesVisited).
         */
        @Query("SELECT COUNT(a) FROM Appointment a WHERE a.visitorId = :visitorId " +
                        "AND a.title = 'private_showing' " +
                        "AND a.status = com.example.courtierprobackend.appointments.datalayer.enums.AppointmentStatus.CONFIRMED " +
                        "AND a.deletedAt IS NULL")
        int countConfirmedShowingsByVisitorId(@Param("visitorId") UUID visitorId);

        /**
         * Batch count confirmed private showings per visitor for a list of visitor IDs.
         * Returns rows of [visitorId, count].
         */
        @Query("SELECT a.visitorId, COUNT(a) FROM Appointment a " +
                        "WHERE a.visitorId IN :visitorIds " +
                        "AND a.title = 'private_showing' " +
                        "AND a.status = com.example.courtierprobackend.appointments.datalayer.enums.AppointmentStatus.CONFIRMED " +
                        "AND a.deletedAt IS NULL " +
                        "GROUP BY a.visitorId")
        List<Object[]> countConfirmedShowingsByVisitorIds(@Param("visitorIds") List<UUID> visitorIds);

        /**
         * Batch count confirmed sell-side showings per transaction for a list of transaction IDs.
         * Returns rows of [transactionId, count].
         */
        @Query("SELECT a.transactionId, COUNT(a) FROM Appointment a " +
                        "WHERE a.transactionId IN :transactionIds " +
                        "AND a.title IN ('open_house', 'private_showing') " +
                        "AND a.status = com.example.courtierprobackend.appointments.datalayer.enums.AppointmentStatus.CONFIRMED " +
                        "AND a.deletedAt IS NULL " +
                        "GROUP BY a.transactionId")
        List<Object[]> countConfirmedShowingsByTransactionIds(@Param("transactionIds") List<UUID> transactionIds);

        /**
         * Find appointments for analytics filtering.
         */
        @Query("SELECT a FROM Appointment a " +
                "WHERE a.brokerId = :brokerId " +
                "AND (:startDate IS NULL OR a.fromDateTime >= :startDate) " +
                "AND (:endDate IS NULL OR a.fromDateTime <= :endDate) " +
                "ORDER BY a.fromDateTime ASC")
        List<Appointment> findForAnalytics(
                @Param("brokerId") UUID brokerId,
                @Param("startDate") LocalDateTime startDate,
                @Param("endDate") LocalDateTime endDate);

        @Query("SELECT a FROM Appointment a " +
                "WHERE a.brokerId = :brokerId " +
                "AND (:startDate IS NULL OR a.fromDateTime >= :startDate) " +
                "AND (:endDate IS NULL OR a.fromDateTime <= :endDate) " +
                "AND a.clientId IN (:clientIds) " +
                "ORDER BY a.fromDateTime ASC")
        List<Appointment> findForAnalyticsWithClients(
                @Param("brokerId") UUID brokerId,
                @Param("startDate") LocalDateTime startDate,
                @Param("endDate") LocalDateTime endDate,
                @Param("clientIds") java.util.List<UUID> clientIds);
}
