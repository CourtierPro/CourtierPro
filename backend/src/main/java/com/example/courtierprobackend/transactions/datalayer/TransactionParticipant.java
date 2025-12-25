package com.example.courtierprobackend.transactions.datalayer;

import com.example.courtierprobackend.transactions.datalayer.enums.ParticipantRole;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "transaction_participants")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TransactionParticipant {

    @Id
    @Builder.Default
    private UUID id = UUID.randomUUID();

    @Column(name = "transaction_id", nullable = false)
    private UUID transactionId;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ParticipantRole role;

    private String email;

    @Column(name = "phone_number")
    private String phoneNumber;

}
