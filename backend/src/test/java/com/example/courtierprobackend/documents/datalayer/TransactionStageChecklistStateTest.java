package com.example.courtierprobackend.documents.datalayer;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class TransactionStageChecklistStateTest {

    @Test
    void onCreate_setsCreatedAndUpdatedWhenMissing() {
        TransactionStageChecklistState state = TransactionStageChecklistState.builder().build();

        state.onCreate();

        assertThat(state.getCreatedAt()).isNotNull();
        assertThat(state.getUpdatedAt()).isNotNull();
    }

    @Test
    void onCreate_doesNotOverrideExistingTimestamps() {
        LocalDateTime createdAt = LocalDateTime.now().minusDays(2);
        LocalDateTime updatedAt = LocalDateTime.now().minusDays(1);
        TransactionStageChecklistState state = TransactionStageChecklistState.builder()
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .build();

        state.onCreate();

        assertThat(state.getCreatedAt()).isEqualTo(createdAt);
        assertThat(state.getUpdatedAt()).isEqualTo(updatedAt);
    }

    @Test
    void onUpdate_refreshesUpdatedAt() {
        LocalDateTime oldUpdatedAt = LocalDateTime.now().minusHours(3);
        TransactionStageChecklistState state = TransactionStageChecklistState.builder()
                .updatedAt(oldUpdatedAt)
                .build();

        state.onUpdate();

        assertThat(state.getUpdatedAt()).isAfter(oldUpdatedAt);
    }
}
