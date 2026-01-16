package com.example.courtierprobackend.user.presentationlayer.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@AllArgsConstructor
public class UpdateStatusRequest {

    @NotNull(message = "Active field is required")
    Boolean active;
}
