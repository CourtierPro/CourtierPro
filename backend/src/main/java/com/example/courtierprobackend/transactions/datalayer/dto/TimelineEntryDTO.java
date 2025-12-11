package com.example.courtierprobackend.transactions.datalayer.dto;

import com.example.courtierprobackend.transactions.datalayer.enums.TimelineEntryType;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TimelineEntryDTO {

    private TimelineEntryType type;
    private String note;
    private String title;
    private Boolean visibleToClient;
    private LocalDateTime occurredAt;
    private UUID addedByBrokerId;
}
