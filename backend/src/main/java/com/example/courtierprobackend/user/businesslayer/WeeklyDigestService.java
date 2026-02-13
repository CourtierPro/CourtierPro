package com.example.courtierprobackend.user.businesslayer;

import com.example.courtierprobackend.appointments.datalayer.Appointment;
import com.example.courtierprobackend.appointments.datalayer.AppointmentRepository;
import com.example.courtierprobackend.documents.datalayer.Document;
import com.example.courtierprobackend.documents.datalayer.DocumentRepository;
import com.example.courtierprobackend.documents.datalayer.enums.DocumentStatusEnum;
import com.example.courtierprobackend.email.EmailService;
import com.example.courtierprobackend.transactions.datalayer.Transaction;
import com.example.courtierprobackend.transactions.datalayer.repositories.TransactionRepository;
import com.example.courtierprobackend.user.dataaccesslayer.UserAccount;
import com.example.courtierprobackend.user.dataaccesslayer.UserAccountRepository;
import com.example.courtierprobackend.user.dataaccesslayer.UserRole;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Service
@Slf4j
public class WeeklyDigestService {

    private final UserAccountRepository userAccountRepository;
    private final AppointmentRepository appointmentRepository;
    private final DocumentRepository documentRepository;
    private final TransactionRepository transactionRepository;
    private final EmailService emailService;

    public WeeklyDigestService(UserAccountRepository userAccountRepository,
                               AppointmentRepository appointmentRepository,
                               DocumentRepository documentRepository,
                               TransactionRepository transactionRepository,
                               EmailService emailService) {
        this.userAccountRepository = userAccountRepository;
        this.appointmentRepository = appointmentRepository;
        this.documentRepository = documentRepository;
        this.transactionRepository = transactionRepository;
        this.emailService = emailService;
    }

    /**
     * Scheduled task to send weekly digests every Monday at 8 AM.
     */
    @Scheduled(cron = "0 0 8 * * MON")
    public void sendWeeklyDigests() {
        log.info("Starting weekly digest job...");
        
        List<UserAccount> brokers = userAccountRepository.findByRoleAndWeeklyDigestEnabledTrueAndActiveTrue(UserRole.BROKER);
        
        for (UserAccount broker : brokers) {
            try {
                processWeeklyDigestForBroker(broker);
            } catch (Exception e) {
                log.error("Failed to process weekly digest for broker {}", broker.getEmail(), e);
            }
        }
        
        log.info("Weekly digest job completed.");
    }

    private void processWeeklyDigestForBroker(UserAccount broker) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nextWeek = now.plusDays(7);
        LocalDateTime thresholdStalled = now.minusDays(14);

        // 1. Upcoming appointments (next 7 days, excluding CANCELLED/DECLINED)
        List<Appointment> upcomingAppointments = appointmentRepository.findByBrokerIdAndDateRangeAndStatusIn(
                broker.getId(), now, nextWeek, Arrays.asList(
                        com.example.courtierprobackend.appointments.datalayer.enums.AppointmentStatus.CONFIRMED,
                        com.example.courtierprobackend.appointments.datalayer.enums.AppointmentStatus.PROPOSED));

        // 2. Pending documents (REQUESTED, SUBMITTED, NEEDS_REVISION)
        List<Document> pendingDocuments = documentRepository.findPendingDocumentsForWeeklyDigest(broker.getId());

        // 3. Stalled transactions (IN_PROGRESS, not updated in 14+ days)
        List<Transaction> stalledTransactions = transactionRepository.findStalledTransactions(broker.getId(), thresholdStalled);

        if (upcomingAppointments.isEmpty() && pendingDocuments.isEmpty() && stalledTransactions.isEmpty()) {
            log.info("No activity for broker {}, skipping weekly digest email.", broker.getEmail());
            return;
        }

        emailService.sendWeeklyDigestEmail(broker, upcomingAppointments, pendingDocuments, stalledTransactions);
    }
}
