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

    @Column(name = "user_id")
    private UUID userId;

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

    @ElementCollection(fetch = FetchType.EAGER)
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "participant_permissions", joinColumns = @JoinColumn(name = "participant_id"))
    @Column(name = "permission")
    private java.util.Set<com.example.courtierprobackend.transactions.datalayer.enums.ParticipantPermission> permissions;

    /**
     * Indique si ce participant est un participant système (client ou broker principal).
     * Les participants système ne peuvent pas être modifiés ou supprimés via l'UI ou l'API.
     */
    @Builder.Default
    private boolean isSystem = false;

}
