package com.example.courtierprobackend.user.presentationlayer.controller;

import com.example.courtierprobackend.user.businesslayer.UserProvisioningService;
import com.example.courtierprobackend.user.presentationlayer.response.UserResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/broker/clients")
@PreAuthorize("hasRole('BROKER')")
public class BrokerController {

    private final UserProvisioningService service;

    public BrokerController(UserProvisioningService service) {
        this.service = service;
    }

    @GetMapping
    public List<UserResponse> getClients() {
        return service.getClients();
    }
}
