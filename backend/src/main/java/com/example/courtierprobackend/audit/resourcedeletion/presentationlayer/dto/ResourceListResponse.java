package com.example.courtierprobackend.audit.resourcedeletion.presentationlayer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Response containing a list of resources with their key metadata.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ResourceListResponse {

    private String resourceType;
    private int totalCount;
    private int deletedCount;
    private List<ResourceItem> items;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ResourceItem {
        private UUID id;
        private String summary;
        private LocalDateTime createdAt;
        private LocalDateTime deletedAt;
        private UUID deletedBy;
        private boolean isDeleted;
        
        // Transaction details
        private UUID clientId;
        private String clientEmail;
        private UUID brokerId;
        private String brokerEmail;
        private String status;
        private String side;  // BUY_SIDE or SELL_SIDE
        private String address;
        
        // Document request details
        private UUID transactionId;
        private String docType;
        private int submittedDocCount;
    }
}
