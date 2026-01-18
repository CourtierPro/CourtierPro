package com.example.courtierprobackend.email;

import com.example.courtierprobackend.Organization.presentationlayer.model.OrganizationSettingsResponseModel;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.assertThat;

class EmailServiceEmailChangeConfirmationTest {
    @Test
    void sendEmailChangeConfirmation_sendsEmail() {
        EmailService service = spy(new EmailService("a", "b", null, null));
        var user = mock(com.example.courtierprobackend.user.dataaccesslayer.UserAccount.class);
        when(user.getFirstName()).thenReturn("John");
        doNothing().when(service).sendSimpleEmail(anyString(), anyString(), anyString());
        service.sendEmailChangeConfirmation(user, "to@x.com", "token123");
        verify(service).sendSimpleEmail(contains("to@x.com"), contains("Confirm your new email address"), contains("token123"));
    }
}
