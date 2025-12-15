package com.example.courtierprobackend.security;

import com.example.courtierprobackend.common.exceptions.ForbiddenException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserContextUtilsTest {

    @Mock
    private HttpServletRequest request;

    @Test
    void resolveUserId_WithOverrideHeader_ReturnsOverrideId() {
        UUID overrideId = UUID.randomUUID();
        UUID result = UserContextUtils.resolveUserId(request, overrideId.toString());
        assertThat(result).isEqualTo(overrideId);
    }

    @Test
    void resolveUserId_WithInternalIdAttribute_ReturnsInternalId() {
        UUID internalId = UUID.randomUUID();
        when(request.getAttribute(UserContextFilter.INTERNAL_USER_ID_ATTR)).thenReturn(internalId);

        UUID result = UserContextUtils.resolveUserId(request);
        assertThat(result).isEqualTo(internalId);
    }

    @Test
    void resolveUserId_WithStringAttribute_ReturnsParsedId() {
        UUID internalId = UUID.randomUUID();
        when(request.getAttribute(UserContextFilter.INTERNAL_USER_ID_ATTR)).thenReturn(internalId.toString());

        UUID result = UserContextUtils.resolveUserId(request);
        assertThat(result).isEqualTo(internalId);
    }

    @Test
    void resolveUserId_MissingAttribute_ThrowsForbiddenException() {
        when(request.getAttribute(UserContextFilter.INTERNAL_USER_ID_ATTR)).thenReturn(null);

        assertThatThrownBy(() -> UserContextUtils.resolveUserId(request))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("Unable to resolve user id from security context");
    }

    @Test
    void resolveUserId_NullRequest_ThrowsForbiddenException() {
        assertThatThrownBy(() -> UserContextUtils.resolveUserId(null))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("Unable to resolve user id from security context");
    }
}
