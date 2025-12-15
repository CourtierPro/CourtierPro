package com.example.courtierprobackend.audit.timeline_audit.datamapperlayer;

import com.example.courtierprobackend.audit.timeline_audit.dataaccesslayer.Enum.TimelineEntryType;
import com.example.courtierprobackend.audit.timeline_audit.dataaccesslayer.TimelineEntry;
import com.example.courtierprobackend.audit.timeline_audit.dataaccesslayer.value_object.TransactionInfo;
import com.example.courtierprobackend.audit.timeline_audit.presentationlayer.TimelineEntryDTO;
import com.example.courtierprobackend.user.dataaccesslayer.UserAccount;
import com.example.courtierprobackend.user.dataaccesslayer.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TimelineEntryMapperTest {

    @Mock
    private UserAccountRepository userAccountRepository;

    private TimelineEntryMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new TimelineEntryMapper(userAccountRepository);
    }

    @Test
    void toDTO_MapsBasicFieldsCorrectly() {
        TimelineEntry entry = new TimelineEntry();
        entry.setId(UUID.randomUUID());
        entry.setType(TimelineEntryType.CREATED);
        entry.setNote("Test Note");
        entry.setDocType("CONTRACT");
        entry.setVisibleToClient(true);
        entry.setTimestamp(Instant.now());
        entry.setTransactionInfo(TransactionInfo.builder()
                .clientName("Client")
                .actorName("Actor")
                .build());

        TimelineEntryDTO dto = mapper.toDTO(entry);

        assertThat(dto.getId()).isEqualTo(entry.getId());
        assertThat(dto.getType()).isEqualTo(entry.getType());
        assertThat(dto.getNote()).isEqualTo(entry.getNote());
        assertThat(dto.getDocType()).isEqualTo(entry.getDocType());
        assertThat(dto.getVisibleToClient()).isTrue();
        assertThat(dto.getOccurredAt()).isEqualTo(entry.getTimestamp());
        assertThat(dto.getTransactionInfo()).isEqualTo(entry.getTransactionInfo());
        assertThat(dto.getTitle()).isNull(); // Explicitly set to null in mapper
    }

    @Test
    void toDTO_WithActorId_ResolvesFullName() {
        UUID actorId = UUID.randomUUID();
        TimelineEntry entry = new TimelineEntry();
        entry.setActorId(actorId);

        UserAccount user = new UserAccount();
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setEmail("john.doe@example.com");

        when(userAccountRepository.findById(actorId)).thenReturn(Optional.of(user));

        TimelineEntryDTO dto = mapper.toDTO(entry);

        assertThat(dto.getAddedByBrokerId()).isEqualTo(actorId);
        assertThat(dto.getActorName()).isEqualTo("John Doe");
    }

    @Test
    void toDTO_WithActorId_ResolvesEmailWhenNameEmpty() {
        UUID actorId = UUID.randomUUID();
        TimelineEntry entry = new TimelineEntry();
        entry.setActorId(actorId);

        UserAccount user = new UserAccount();
        user.setFirstName(null); // or empty
        user.setLastName("");
        user.setEmail("john.doe@example.com");

        when(userAccountRepository.findById(actorId)).thenReturn(Optional.of(user));

        TimelineEntryDTO dto = mapper.toDTO(entry);

        assertThat(dto.getActorName()).isEqualTo("john.doe@example.com");
    }

    @Test
    void toDTO_WithActorId_ResolvesNullWhenUserNotFound() {
        UUID actorId = UUID.randomUUID();
        TimelineEntry entry = new TimelineEntry();
        entry.setActorId(actorId);

        when(userAccountRepository.findById(actorId)).thenReturn(Optional.empty());

        TimelineEntryDTO dto = mapper.toDTO(entry);

        assertThat(dto.getAddedByBrokerId()).isEqualTo(actorId);
        assertThat(dto.getActorName()).isNull();
    }

    @Test
    void toDTO_WithoutActorId_LeavesNameNull() {
        TimelineEntry entry = new TimelineEntry();
        entry.setActorId(null);

        TimelineEntryDTO dto = mapper.toDTO(entry);

        assertThat(dto.getAddedByBrokerId()).isNull();
        assertThat(dto.getActorName()).isNull();
    }

    @Test
    void toDTO_WithNullRepository_LeavesNameNull() {
        // Technically constructor requires it, but testing null safety
        TimelineEntryMapper safeMapper = new TimelineEntryMapper(null);
        TimelineEntry entry = new TimelineEntry();
        entry.setActorId(UUID.randomUUID());

        TimelineEntryDTO dto = safeMapper.toDTO(entry);

        assertThat(dto.getActorName()).isNull();
    }
}
