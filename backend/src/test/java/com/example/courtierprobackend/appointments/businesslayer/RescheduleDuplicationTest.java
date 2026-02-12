package com.example.courtierprobackend.appointments.businesslayer;

import com.example.courtierprobackend.appointments.datalayer.AppointmentRepository;
import com.example.courtierprobackend.appointments.datalayer.dto.AppointmentCancellationDTO;
import com.example.courtierprobackend.appointments.datalayer.dto.AppointmentRequestDTO;
import com.example.courtierprobackend.appointments.datalayer.dto.AppointmentResponseDTO;
import com.example.courtierprobackend.appointments.datalayer.dto.AppointmentReviewDTO;
import com.example.courtierprobackend.appointments.datalayer.enums.AppointmentStatus;
import com.example.courtierprobackend.transactions.datalayer.Transaction;
import com.example.courtierprobackend.transactions.datalayer.repositories.TransactionRepository;
import com.example.courtierprobackend.user.dataaccesslayer.UserAccount;
import com.example.courtierprobackend.user.dataaccesslayer.UserRole;
import com.example.courtierprobackend.user.dataaccesslayer.UserAccountRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class RescheduleDuplicationTest {

    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Test
    public void testRescheduleUpdatesExistingAppointment() {
        // 1. Setup Data
        UUID brokerId = UUID.randomUUID();
        UUID clientId = UUID.randomUUID();
        UUID transactionId = UUID.randomUUID();

        UserAccount broker = new UserAccount();
        broker.setId(brokerId);
        broker.setAuth0UserId("auth0|broker");
        broker.setEmail("broker@test.com");
        broker.setFirstName("Broker");
        broker.setLastName("Test");
        broker.setRole(UserRole.BROKER);
        broker.setPreferredLanguage("en");
        broker.setActive(true);
        broker.setCreatedAt(java.time.Instant.now());
        broker.setUpdatedAt(java.time.Instant.now());
        userAccountRepository.save(broker);

        UserAccount client = new UserAccount();
        client.setId(clientId);
        client.setAuth0UserId("auth0|client");
        client.setEmail("client@test.com");
        client.setFirstName("Client");
        client.setLastName("Test");
        client.setRole(UserRole.CLIENT);
        client.setPreferredLanguage("en");
        client.setActive(true);
        client.setCreatedAt(java.time.Instant.now());
        client.setUpdatedAt(java.time.Instant.now());
        userAccountRepository.save(client);

        Transaction transaction = new Transaction();
        transaction.setTransactionId(transactionId);
        transaction.setBrokerId(brokerId);
        transaction.setClientId(clientId);
        // Assuming Transaction might need other fields, but let's try with minimal
        // first or check entity if needed.
        // If Transaction has auditing fields they might be nullable or auto-filled.
        transactionRepository.save(transaction);

        // 2. Create Appointment (PROPOSED)
        AppointmentRequestDTO createReq = new AppointmentRequestDTO(
                transactionId, "Property Visit", null, LocalDate.now().plusDays(1), LocalTime.of(10, 0),
                LocalTime.of(11, 0), "Note", null);
        AppointmentResponseDTO created = appointmentService.requestAppointment(createReq, brokerId);
        UUID appointmentId = created.appointmentId();

        System.out.println("CREATED ID: " + appointmentId);

        // 3. Cancel Appointment
        AppointmentCancellationDTO cancelReq = new AppointmentCancellationDTO("Changed mind");
        appointmentService.cancelAppointment(appointmentId, cancelReq, brokerId);

        var cancelledAppt = appointmentRepository.findByAppointmentIdAndDeletedAtIsNull(appointmentId).get();
        Assertions.assertEquals(AppointmentStatus.CANCELLED, cancelledAppt.getStatus());

        // 4. Reschedule Appointment
        AppointmentReviewDTO rescheduleReq = new AppointmentReviewDTO(
                AppointmentReviewDTO.ReviewAction.RESCHEDULE, null, LocalDate.now().plusDays(2), LocalTime.of(14, 0),
                LocalTime.of(15, 0));
        AppointmentResponseDTO rescheduled = appointmentService.reviewAppointment(appointmentId, rescheduleReq,
                brokerId);

        System.out.println("RESCHEDULED ID: " + rescheduled.appointmentId());

        // 5. Verify
        // Ensure ID is same
        Assertions.assertEquals(appointmentId, rescheduled.appointmentId(), "Appointment ID should remain the same");

        // Ensure Status is PROPOSED
        Assertions.assertEquals(AppointmentStatus.PROPOSED, rescheduled.status(), "Status should be PROPOSED");

        // Ensure ONLY 1 appointment exists in DB

        // Depending on existing data in test DB, we might check specifically for this
        // broker
        int countForBroker = appointmentRepository.findByBrokerIdAndDeletedAtIsNullOrderByFromDateTimeAsc(brokerId)
                .size();
        Assertions.assertEquals(1, countForBroker, "Should only be 1 appointment for this broker");
    }
}
