package com.example.courtierprobackend.feedback;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/feedback")
@RequiredArgsConstructor
@Slf4j
public class FeedbackController {

    private final FeedbackService feedbackService;

    /**
     * Submit user feedback which creates a GitHub issue
     * 
     * @param request The feedback request containing type (bug/feature) and message
     * @param jwt The authenticated user's JWT token
     * @return FeedbackResponse with success status and issue details
     */
    @PostMapping
    public ResponseEntity<FeedbackResponse> submitFeedback(
            @Valid @RequestBody FeedbackRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        // Extract user email from JWT if available
        String userEmail = null;
        if (jwt != null) {
            userEmail = jwt.getClaimAsString("email");
            if (userEmail == null) {
                // Try alternative claim names
                userEmail = jwt.getClaimAsString("https://courtierpro.dev/email");
            }
        }
        
        log.info("Received feedback submission - Type: {}, User: {}", request.getType(), userEmail);
        
        FeedbackResponse response = feedbackService.submitFeedback(request, userEmail);
        
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.internalServerError().body(response);
        }
    }
}
