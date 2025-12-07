package com.example.courtierprobackend.Organization.dataccesslayer;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "organization_settings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrganizationSettings {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "default_language", nullable = false, length = 2)
    private String defaultLanguage;

    @Column(name = "invite_subject_en", nullable = false, length = 255)
    private String inviteSubjectEn;

    @Column(name = "invite_body_en", nullable = false, columnDefinition = "TEXT")
    private String inviteBodyEn;

    @Column(name = "invite_subject_fr", nullable = false, length = 255)
    private String inviteSubjectFr;

    @Column(name = "invite_body_fr", nullable = false, columnDefinition = "TEXT")
    private String inviteBodyFr;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
