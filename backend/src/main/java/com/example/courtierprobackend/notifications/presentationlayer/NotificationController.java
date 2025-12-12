package com.example.courtierprobackend.notifications.presentationlayer;

import com.example.courtierprobackend.notifications.businesslayer.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<List<NotificationResponseDTO>> getUserNotifications(@AuthenticationPrincipal Jwt principal) {
        // extract user ID from principal "auth0|..." or similar depending on setup
        // In this project, it seems we use the 'sub' claim or similar.
        // Assuming the auth0UserId is the principal.getSubject()
        String auth0UserId = principal.getSubject();

        return ResponseEntity.ok(notificationService.getUserNotifications(auth0UserId));
    }

    @PutMapping("/{publicId}/read")
    public ResponseEntity<NotificationResponseDTO> markAsRead(@PathVariable String publicId) {
        return ResponseEntity.ok(notificationService.markAsRead(publicId));
    }
}
