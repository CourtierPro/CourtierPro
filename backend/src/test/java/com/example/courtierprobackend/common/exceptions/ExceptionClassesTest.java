package com.example.courtierprobackend.common.exceptions;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ExceptionClassesTest {

    @Test
    void badRequestException_Constructors() {
        BadRequestException ex = new BadRequestException("message");
        assertThat(ex.getMessage()).isEqualTo("message");
    }

    @Test
    void forbiddenException_Constructors() {
        ForbiddenException ex = new ForbiddenException("message");
        assertThat(ex.getMessage()).isEqualTo("message");
    }

    @Test
    void internalServerException_Constructors() {
        InternalServerException ex = new InternalServerException("message");
        assertThat(ex.getMessage()).isEqualTo("message");
    }

    @Test
    void notFoundException_Constructors() {
        NotFoundException ex = new NotFoundException("message");
        assertThat(ex.getMessage()).isEqualTo("message");
    }

    @Test
    void notFoundException_DefaultAndCauseConstructors() {
        NotFoundException defaultEx = new NotFoundException();
        assertThat(defaultEx.getMessage()).isNull();

        RuntimeException cause = new RuntimeException("root cause");
        NotFoundException causeEx = new NotFoundException("message", cause);
        assertThat(causeEx.getMessage()).isEqualTo("message");
        assertThat(causeEx.getCause()).isEqualTo(cause);
    }

    @Test
    void unauthorizedException_Constructors() {
        UnauthorizedException ex = new UnauthorizedException("message");
        assertThat(ex.getMessage()).isEqualTo("message");
    }

    @Test
    void unauthorizedException_DefaultAndCauseConstructors() {
        UnauthorizedException defaultEx = new UnauthorizedException();
        assertThat(defaultEx.getMessage()).isNull();

        RuntimeException cause = new RuntimeException("root cause");
        UnauthorizedException causeEx = new UnauthorizedException("message", cause);
        assertThat(causeEx.getMessage()).isEqualTo("message");
        assertThat(causeEx.getCause()).isEqualTo(cause);
    }

    @Test
    void errorResponse_StaticFactories() {
        ErrorResponse er1 = ErrorResponse.of("error", "code");
        assertThat(er1.getError()).isEqualTo("error");
        assertThat(er1.getCode()).isEqualTo("code");
        assertThat(er1.getTimestamp()).isNotNull();
        assertThat(er1.getPath()).isNull();

        ErrorResponse er2 = ErrorResponse.of("error", "code", "/path");
        assertThat(er2.getError()).isEqualTo("error");
        assertThat(er2.getCode()).isEqualTo("code");
        assertThat(er2.getTimestamp()).isNotNull();
        assertThat(er2.getPath()).isEqualTo("/path");
    }
}
