package com.example.courtierprobackend.transactions.datalayer.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TerminateRequestDTOTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void setReason_TrimsValue() {
        TerminateRequestDTO dto = new TerminateRequestDTO();
        dto.setReason("   Client changed plans   ");

        assertThat(dto.getReason()).isEqualTo("Client changed plans");
    }

    @Test
    void validation_UsesTrimmedReasonLength() {
        TerminateRequestDTO dto = new TerminateRequestDTO();
        dto.setReason("   1234567890   ");

        var violations = validator.validate(dto);

        assertThat(violations).isEmpty();
        assertThat(dto.getReason()).isEqualTo("1234567890");
    }

    @Test
    void validation_FailsWhenTrimmedReasonTooShort() {
        TerminateRequestDTO dto = new TerminateRequestDTO();
        dto.setReason("   123456789   ");

        var violations = validator.validate(dto);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).contains("between 10 and 500");
    }
}
