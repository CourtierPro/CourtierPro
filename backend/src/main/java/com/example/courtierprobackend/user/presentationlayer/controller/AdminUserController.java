package com.example.courtierprobackend.user.presentationlayer.controller;

import com.example.courtierprobackend.user.businesslayer.UserProvisioningService;
import com.example.courtierprobackend.user.presentationlayer.request.CreateUserRequest;
import com.example.courtierprobackend.user.presentationlayer.request.UpdateStatusRequest;
import com.example.courtierprobackend.user.presentationlayer.response.UserResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
@RestController
@RequestMapping("/api/admin/users")
//  Toute la classe est réservée aux ADMIN
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final UserProvisioningService service;

    public AdminUserController(UserProvisioningService service) {
        this.service = service;
    }


    @GetMapping
    public List<UserResponse> getAllUsers() {
        return service.getAllUsers();
    }

    @PostMapping
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        UserResponse response = service.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }


    @PatchMapping("/{userId}/status")
    public ResponseEntity<UserResponse> updateStatus(@PathVariable UUID userId,
                                                     @Valid @RequestBody UpdateStatusRequest request) {
        UserResponse response = service.updateStatus(userId, request);
        return ResponseEntity.ok(response);
    }
    @PostMapping("/{userId}/password-reset")
    public ResponseEntity<Void> triggerPasswordReset(@PathVariable UUID userId) {
        service.triggerPasswordReset(userId);
        return ResponseEntity.ok().build();
    }
}
