package com.example.courtierprobackend.notifications.presentationlayer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BroadcastRequestDTO {
    @jakarta.validation.constraints.NotBlank
    @jakarta.validation.constraints.Size(max = 100)
    private String title;
    @jakarta.validation.constraints.NotBlank
    @jakarta.validation.constraints.Size(max = 500)
    private String message;
}
