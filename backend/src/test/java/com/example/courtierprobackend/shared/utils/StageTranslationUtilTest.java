package com.example.courtierprobackend.shared.utils;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class StageTranslationUtilTest {
    @Test
    void getTranslatedStage_returnsFrenchAndEnglish() {
        assertThat(StageTranslationUtil.getTranslatedStage("BUYER_FINANCIAL_PREPARATION", "fr"))
            .isEqualTo("Préparation Financière");
        assertThat(StageTranslationUtil.getTranslatedStage("BUYER_FINANCIAL_PREPARATION", "en"))
            .isEqualTo("Buyer Financial Preparation");
        assertThat(StageTranslationUtil.getTranslatedStage("", "fr")).isEqualTo("");
        assertThat(StageTranslationUtil.getTranslatedStage(null, "fr")).isEqualTo("");
    }

    @Test
    void constructNotificationMessage_formatsCorrectly() {
        assertThat(StageTranslationUtil.constructNotificationMessage(
            "BUYER_FINANCIAL_PREPARATION", "Jean", "123 Main", "fr"))
            .contains("Préparation Financière");
        assertThat(StageTranslationUtil.constructNotificationMessage(
            "BUYER_FINANCIAL_PREPARATION", "John", "123 Main", "en"))
            .contains("Buyer Financial Preparation");
    }

    @Test
    void formatEnglish_handlesNullAndBlank() {
        // formatEnglish is private, but covered via getTranslatedStage
        assertThat(StageTranslationUtil.getTranslatedStage(null, "en")).isEqualTo("");
        assertThat(StageTranslationUtil.getTranslatedStage("", "en")).isEqualTo("");
    }
}
