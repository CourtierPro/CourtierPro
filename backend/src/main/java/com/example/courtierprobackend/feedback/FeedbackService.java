package com.example.courtierprobackend.feedback;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class FeedbackService {

    private final GitHubService gitHubService;

    /**
     * Process user feedback and create a GitHub issue
     * 
     * @param request The feedback request containing type and message
     * @param userEmail The email of the authenticated user (optional)
     * @return FeedbackResponse with success status and issue details
     */
    public FeedbackResponse submitFeedback(FeedbackRequest request, String userEmail) {
        log.info("Processing feedback submission - Type: {}, User: {}", request.getType(), userEmail);
        
        try {
            GitHubService.GitHubIssueResponse issueResponse = gitHubService.createIssue(
                    request.getType(),
                    request.getMessage(),
                    userEmail
            );

            return FeedbackResponse.builder()
                    .success(true)
                    .issueUrl(issueResponse.getHtmlUrl())
                    .issueNumber(issueResponse.getNumber())
                    .build();
                    
        } catch (Exception e) {
            log.error("Failed to submit feedback: {}", e.getMessage(), e);
            
            String errorMessage;
            if (e instanceof java.net.ConnectException || e instanceof java.net.SocketTimeoutException) {
                errorMessage = "GitHub service unavailable. Please try again later.";
            } else if (e instanceof IllegalArgumentException) {
                errorMessage = "Invalid configuration or request data.";
            } else {
                errorMessage = "An unexpected error occurred while submitting feedback.";
            }
            
            return FeedbackResponse.builder()
                    .success(false)
                    .errorMessage(errorMessage)
                    .build();
        }
    }
}
