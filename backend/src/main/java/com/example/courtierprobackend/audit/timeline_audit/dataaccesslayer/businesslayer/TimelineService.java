package com.example.courtierprobackend.audit.timeline_audit.dataaccesslayer.businesslayer;

import com.example.courtierprobackend.audit.timeline_audit.dataaccesslayer.Enum.TimelineEntryType;
import com.example.courtierprobackend.audit.timeline_audit.dataaccesslayer.value_object.TransactionInfo;
import com.example.courtierprobackend.audit.timeline_audit.presentationlayer.TimelineEntryDTO;

import java.util.List;
import java.util.UUID;

public interface TimelineService {
    void addEntry(UUID transactionId, UUID actorId, TimelineEntryType type, String note, String docType);

    // Nouvelle méthode pour supporter TransactionInfo
    void addEntry(UUID transactionId, UUID actorId, TimelineEntryType type, String note, String docType, TransactionInfo transactionInfo);


    List<TimelineEntryDTO> getTimelineForTransaction(UUID transactionId);


    List<TimelineEntryDTO> getTimelineForClient(UUID transactionId);
}
