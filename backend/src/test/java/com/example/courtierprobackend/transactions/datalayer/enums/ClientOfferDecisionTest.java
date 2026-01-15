package com.example.courtierprobackend.transactions.datalayer.enums;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for ClientOfferDecision enum.
 * Tests enum values and JSON serialization/deserialization.
 */
class ClientOfferDecisionTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("should have exactly 3 values")
    void shouldHaveThreeValues() {
        assertThat(ClientOfferDecision.values()).hasSize(3);
    }

    @Test
    @DisplayName("should contain ACCEPT value")
    void shouldContainAcceptValue() {
        assertThat(ClientOfferDecision.valueOf("ACCEPT")).isEqualTo(ClientOfferDecision.ACCEPT);
    }

    @Test
    @DisplayName("should contain DECLINE value")
    void shouldContainDeclineValue() {
        assertThat(ClientOfferDecision.valueOf("DECLINE")).isEqualTo(ClientOfferDecision.DECLINE);
    }

    @Test
    @DisplayName("should contain COUNTER value")
    void shouldContainCounterValue() {
        assertThat(ClientOfferDecision.valueOf("COUNTER")).isEqualTo(ClientOfferDecision.COUNTER);
    }

    @Test
    @DisplayName("should throw exception for invalid value")
    void shouldThrowExceptionForInvalidValue() {
        assertThatThrownBy(() -> ClientOfferDecision.valueOf("INVALID"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Nested
    @DisplayName("JSON Serialization")
    class JsonSerializationTests {

        @ParameterizedTest
        @EnumSource(ClientOfferDecision.class)
        @DisplayName("should serialize enum value to JSON")
        void shouldSerializeEnumValue(ClientOfferDecision decision) throws Exception {
            String json = objectMapper.writeValueAsString(decision);
            assertThat(json).isEqualTo("\"" + decision.name() + "\"");
        }

        @ParameterizedTest
        @EnumSource(ClientOfferDecision.class)
        @DisplayName("should deserialize JSON to enum value")
        void shouldDeserializeJsonToEnumValue(ClientOfferDecision decision) throws Exception {
            String json = "\"" + decision.name() + "\"";
            ClientOfferDecision deserialized = objectMapper.readValue(json, ClientOfferDecision.class);
            assertThat(deserialized).isEqualTo(decision);
        }

        @Test
        @DisplayName("should fail to deserialize invalid JSON value")
        void shouldFailToDeserializeInvalidValue() {
            String json = "\"INVALID_VALUE\"";
            assertThatThrownBy(() -> objectMapper.readValue(json, ClientOfferDecision.class))
                    .isInstanceOf(com.fasterxml.jackson.databind.exc.InvalidFormatException.class);
        }
    }
}
