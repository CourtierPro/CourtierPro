package com.example.courtierprobackend.documents.datalayer.valueobjects;

import com.example.courtierprobackend.documents.datalayer.enums.DocumentPartyEnum;
import com.example.courtierprobackend.documents.datalayer.enums.UploadedByRefEnum;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Embeddable
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UploadedBy {

    @Enumerated(EnumType.STRING)
    private UploadedByRefEnum uploaderType; // CLIENT, BROKER, etc.

    @Enumerated(EnumType.STRING)
    private DocumentPartyEnum party; // BUYER, SELLER, etc.

    private UUID uploaderId; // clientId or brokerId (internal UUID)
    private String externalName; // if uploaded by external party
}
