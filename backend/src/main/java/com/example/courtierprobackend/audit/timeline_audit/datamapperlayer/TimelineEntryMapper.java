package com.example.courtierprobackend.audit.timeline_audit.datamapperlayer;

import com.example.courtierprobackend.audit.timeline_audit.dataaccesslayer.TimelineEntry;
import com.example.courtierprobackend.audit.timeline_audit.presentationlayer.TimelineEntryDTO;
import com.example.courtierprobackend.user.dataaccesslayer.UserAccountRepository;
import org.springframework.stereotype.Component;

@Component
public class TimelineEntryMapper {
    private final UserAccountRepository userAccountRepository;

    public TimelineEntryMapper(UserAccountRepository userAccountRepository) {
        this.userAccountRepository = userAccountRepository;
    }

    public TimelineEntryDTO toDTO(TimelineEntry entry) {
        String actorName = null;
        if (userAccountRepository != null && entry.getActorId() != null) {
            actorName = userAccountRepository.findById(entry.getActorId())
                .map(u -> {
                    String f = u.getFirstName();
                    String l = u.getLastName();
                    String name = ((f == null ? "" : f) + " " + (l == null ? "" : l)).trim();
                    return name.isEmpty() ? u.getEmail() : name;
                })
                .orElse(null);
        }
        return TimelineEntryDTO.builder()
                .id(entry.getId())
                .type(entry.getType())
                .note(entry.getNote())
                .title(null)
                .docType(entry.getDocType())
                .visibleToClient(entry.isVisibleToClient())
                .occurredAt(entry.getTimestamp())
                .addedByBrokerId(entry.getActorId())
                .actorName(actorName)
                .transactionInfo(entry.getTransactionInfo())
                .build();
    }
}
