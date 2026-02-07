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
        // Buyer stages
        assertThat(StageEnum.valueOf("BUYER_FINANCIAL_PREPARATION")).isNotNull();
        assertThat(StageEnum.valueOf("BUYER_PROPERTY_SEARCH")).isNotNull();
        assertThat(StageEnum.valueOf("BUYER_OFFER_AND_NEGOTIATION")).isNotNull();
        assertThat(StageEnum.valueOf("BUYER_FINANCING_AND_CONDITIONS")).isNotNull();
        assertThat(StageEnum.valueOf("BUYER_NOTARY_AND_SIGNING")).isNotNull();
        assertThat(StageEnum.valueOf("BUYER_POSSESSION")).isNotNull();
        // Seller stages
        assertThat(StageEnum.valueOf("SELLER_INITIAL_CONSULTATION")).isNotNull();
        assertThat(StageEnum.valueOf("SELLER_PUBLISH_LISTING")).isNotNull();
        assertThat(StageEnum.valueOf("SELLER_OFFER_AND_NEGOTIATION")).isNotNull();
        assertThat(StageEnum.valueOf("SELLER_FINANCING_AND_CONDITIONS")).isNotNull();
        assertThat(StageEnum.valueOf("SELLER_NOTARY_AND_SIGNING")).isNotNull();
        assertThat(StageEnum.valueOf("SELLER_HANDOVER")).isNotNull();
    }

    @Test
    void enumHasExpectedCount() {
        assertThat(StageEnum.values()).hasSize(12);
    }
}
