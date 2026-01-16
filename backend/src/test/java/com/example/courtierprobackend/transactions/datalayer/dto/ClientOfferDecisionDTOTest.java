package com.example.courtierprobackend.transactions.datalayer.dto;

import com.example.courtierprobackend.transactions.datalayer.enums.ClientOfferDecision;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for ClientOfferDecisionDTO.
 * Tests validation, serialization, and builder pattern.
 */
class ClientOfferDecisionDTOTest {

    private static Validator validator;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeAll
    static void setUpValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Nested
    @DisplayName("Builder Tests")
    class BuilderTests {

        @Test
        @DisplayName("should create DTO with all fields using builder")
        void shouldCreateDtoWithAllFields() {
            ClientOfferDecisionDTO dto = ClientOfferDecisionDTO.builder()
                    .decision(ClientOfferDecision.ACCEPT)
                    .notes("Test notes")
                    .build();

            assertThat(dto.getDecision()).isEqualTo(ClientOfferDecision.ACCEPT);
            assertThat(dto.getNotes()).isEqualTo("Test notes");
        }

        @Test
        @DisplayName("should create DTO with only required fields")
        void shouldCreateDtoWithOnlyRequiredFields() {
            ClientOfferDecisionDTO dto = ClientOfferDecisionDTO.builder()
                    .decision(ClientOfferDecision.DECLINE)
                    .build();

            assertThat(dto.getDecision()).isEqualTo(ClientOfferDecision.DECLINE);
            assertThat(dto.getNotes()).isNull();
        }

        @Test
        @DisplayName("should create DTO with no args constructor")
        void shouldCreateDtoWithNoArgsConstructor() {
            ClientOfferDecisionDTO dto = new ClientOfferDecisionDTO();
            
            assertThat(dto.getDecision()).isNull();
            assertThat(dto.getNotes()).isNull();
        }

        @Test
        @DisplayName("should create DTO with all args constructor")
        void shouldCreateDtoWithAllArgsConstructor() {
            ClientOfferDecisionDTO dto = new ClientOfferDecisionDTO(ClientOfferDecision.COUNTER, "Counter notes");
            
            assertThat(dto.getDecision()).isEqualTo(ClientOfferDecision.COUNTER);
            assertThat(dto.getNotes()).isEqualTo("Counter notes");
        }
    }

    @Nested
    @DisplayName("Validation Tests")
    class ValidationTests {

        @Test
        @DisplayName("should pass validation with valid decision")
        void shouldPassValidationWithValidDecision() {
            ClientOfferDecisionDTO dto = ClientOfferDecisionDTO.builder()
                    .decision(ClientOfferDecision.ACCEPT)
                    .build();

            var violations = validator.validate(dto);
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("should fail validation with null decision")
        void shouldFailValidationWithNullDecision() {
            ClientOfferDecisionDTO dto = ClientOfferDecisionDTO.builder()
                    .notes("Some notes")
                    .build();

            var violations = validator.validate(dto);
            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage()).contains("required");
        }

        @Test
        @DisplayName("should pass validation with decision and notes")
        void shouldPassValidationWithDecisionAndNotes() {
            ClientOfferDecisionDTO dto = ClientOfferDecisionDTO.builder()
                    .decision(ClientOfferDecision.DECLINE)
                    .notes("Price is too low for this property")
                    .build();

            var violations = validator.validate(dto);
            assertThat(violations).isEmpty();
        }
    }

    @Nested
    @DisplayName("JSON Serialization Tests")
    class JsonSerializationTests {

        @Test
        @DisplayName("should serialize DTO to JSON")
        void shouldSerializeDtoToJson() throws Exception {
            ClientOfferDecisionDTO dto = ClientOfferDecisionDTO.builder()
                    .decision(ClientOfferDecision.ACCEPT)
                    .notes("Agreed")
                    .build();

            String json = objectMapper.writeValueAsString(dto);

            assertThat(json).contains("\"decision\":\"ACCEPT\"");
            assertThat(json).contains("\"notes\":\"Agreed\"");
        }

        @Test
        @DisplayName("should deserialize JSON to DTO")
        void shouldDeserializeJsonToDto() throws Exception {
            String json = "{\"decision\":\"DECLINE\",\"notes\":\"Too expensive\"}";

            ClientOfferDecisionDTO dto = objectMapper.readValue(json, ClientOfferDecisionDTO.class);

            assertThat(dto.getDecision()).isEqualTo(ClientOfferDecision.DECLINE);
            assertThat(dto.getNotes()).isEqualTo("Too expensive");
        }

        @Test
        @DisplayName("should deserialize JSON without notes")
        void shouldDeserializeJsonWithoutNotes() throws Exception {
            String json = "{\"decision\":\"COUNTER\"}";

            ClientOfferDecisionDTO dto = objectMapper.readValue(json, ClientOfferDecisionDTO.class);

            assertThat(dto.getDecision()).isEqualTo(ClientOfferDecision.COUNTER);
            assertThat(dto.getNotes()).isNull();
        }

        @Test
        @DisplayName("should serialize DTO with null notes")
        void shouldSerializeDtoWithNullNotes() throws Exception {
            ClientOfferDecisionDTO dto = ClientOfferDecisionDTO.builder()
                    .decision(ClientOfferDecision.ACCEPT)
                    .build();

            String json = objectMapper.writeValueAsString(dto);

            assertThat(json).contains("\"decision\":\"ACCEPT\"");
        }
    }

    @Nested
    @DisplayName("Setter Tests")
    class SetterTests {

        @Test
        @DisplayName("should set decision using setter")
        void shouldSetDecisionUsingSetter() {
            ClientOfferDecisionDTO dto = new ClientOfferDecisionDTO();
            dto.setDecision(ClientOfferDecision.ACCEPT);

            assertThat(dto.getDecision()).isEqualTo(ClientOfferDecision.ACCEPT);
        }

        @Test
        @DisplayName("should set notes using setter")
        void shouldSetNotesUsingSetter() {
            ClientOfferDecisionDTO dto = new ClientOfferDecisionDTO();
            dto.setNotes("Test notes");

            assertThat(dto.getNotes()).isEqualTo("Test notes");
        }
    }
}
