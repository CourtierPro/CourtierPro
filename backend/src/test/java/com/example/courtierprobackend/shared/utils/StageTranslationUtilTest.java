package com.example.courtierprobackend.shared.utils;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class StageTranslationUtilTest {
    @Test
    void getTranslatedStage_returnsFrenchAndEnglish() {
        assertThat(StageTranslationUtil.getTranslatedStage("BUYER_PREQUALIFY_FINANCIALLY", "fr"))
            .isEqualTo("Pré-qualification Financière");
        assertThat(StageTranslationUtil.getTranslatedStage("BUYER_PREQUALIFY_FINANCIALLY", "en"))
            .isEqualTo("Buyer Prequalify Financially");
        assertThat(StageTranslationUtil.getTranslatedStage("", "fr")).isEqualTo("");
        assertThat(StageTranslationUtil.getTranslatedStage(null, "fr")).isEqualTo("");
    }

    @Test
    void constructNotificationMessage_formatsCorrectly() {
        assertThat(StageTranslationUtil.constructNotificationMessage(
            "BUYER_PREQUALIFY_FINANCIALLY", "Jean", "123 Main", "fr"))
            .contains("Pré-qualification Financière");
        assertThat(StageTranslationUtil.constructNotificationMessage(
            "BUYER_PREQUALIFY_FINANCIALLY", "John", "123 Main", "en"))
            .contains("Buyer Prequalify Financially");
    }

    @Test
    void formatEnglish_handlesNullAndBlank() {
        // formatEnglish is private, but covered via getTranslatedStage
        assertThat(StageTranslationUtil.getTranslatedStage(null, "en")).isEqualTo("");
        assertThat(StageTranslationUtil.getTranslatedStage("", "en")).isEqualTo("");
    }
}
