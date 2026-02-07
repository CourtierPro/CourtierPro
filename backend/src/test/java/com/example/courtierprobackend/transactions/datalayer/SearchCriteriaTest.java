package com.example.courtierprobackend.transactions.datalayer;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SearchCriteriaTest {

    @Test
    void onCreate_generatesIdAndCreatedAtWhenMissing() {
        SearchCriteria criteria = new SearchCriteria();

        criteria.onCreate();

        assertThat(criteria.getSearchCriteriaId()).isNotNull();
        assertThat(criteria.getCreatedAt()).isNotNull();
    }

    @Test
    void onCreate_preservesExistingId() {
        UUID existingId = UUID.randomUUID();
        SearchCriteria criteria = new SearchCriteria();
        criteria.setSearchCriteriaId(existingId);

        criteria.onCreate();

        assertThat(criteria.getSearchCriteriaId()).isEqualTo(existingId);
        assertThat(criteria.getCreatedAt()).isNotNull();
    }

    @Test
    void onUpdate_setsUpdatedAt() {
        SearchCriteria criteria = new SearchCriteria();
        LocalDateTime oldUpdatedAt = LocalDateTime.now().minusHours(1);
        criteria.setUpdatedAt(oldUpdatedAt);

        criteria.onUpdate();

        assertThat(criteria.getUpdatedAt()).isAfter(oldUpdatedAt);
    }
}
