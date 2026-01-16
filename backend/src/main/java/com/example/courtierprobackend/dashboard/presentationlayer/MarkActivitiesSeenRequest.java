package com.example.courtierprobackend.dashboard.presentationlayer;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Request body for marking timeline entries as seen.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class MarkActivitiesSeenRequest {
    private List<UUID> activityIds;
}
