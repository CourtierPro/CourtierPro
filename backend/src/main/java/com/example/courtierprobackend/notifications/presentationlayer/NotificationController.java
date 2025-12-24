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
    public ResponseEntity<?> markAsRead(@PathVariable("publicId") String publicId) {
        java.util.UUID uuid;
        try {
            uuid = java.util.UUID.fromString(publicId);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body("Invalid UUID format for publicId: " + publicId);
        }
        return ResponseEntity.ok(notificationService.markAsRead(uuid.toString()));
    }

    @PostMapping("/broadcast")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> sendBroadcast(
            @jakarta.validation.Valid @RequestBody com.example.courtierprobackend.notifications.presentationlayer.BroadcastRequestDTO request,
            @AuthenticationPrincipal Jwt principal) {
        // Extract admin ID from principal, e.g. "auth0|..."
        String adminId = principal.getSubject();
        notificationService.sendBroadcast(request, adminId);
        return ResponseEntity.ok().build();
    }
}
