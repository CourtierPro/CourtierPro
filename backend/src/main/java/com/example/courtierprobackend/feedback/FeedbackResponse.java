package com.example.courtierprobackend.feedback;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackResponse {
    private boolean success;
    private String issueUrl;
    private Integer issueNumber;
    private String errorMessage;
}
