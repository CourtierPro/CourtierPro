package com.example.courtierprobackend.documents.datalayer.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StageEnumTest {
    @Test
    void allStageEnumValues_ArePresentAndUnique() {
        StageEnum[] values = StageEnum.values();
        // Check that all values are unique
        long uniqueCount = java.util.Arrays.stream(values).map(Enum::name).distinct().count();
        assertThat(uniqueCount).isEqualTo(values.length);
    }

    @Test
    void canValueOfEachStageEnum() {
        for (StageEnum stage : StageEnum.values()) {
            assertThat(StageEnum.valueOf(stage.name())).isEqualTo(stage);
        }
    }

    @Test
    void enumContainsExpectedStages() {
        assertThat(StageEnum.valueOf("BUYER_PREQUALIFY_FINANCIALLY")).isNotNull();
        assertThat(StageEnum.valueOf("SELLER_TERMINATED")).isNotNull();
        // Add a few more representative checks
        assertThat(StageEnum.valueOf("BUYER_OFFER_ACCEPTED")).isNotNull();
        assertThat(StageEnum.valueOf("SELLER_LISTING_PUBLISHED")).isNotNull();
    }
}
