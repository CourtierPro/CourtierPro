package com.example.courtierprobackend.user.presentationlayer.controller;

import com.example.courtierprobackend.security.UserContextUtils;
import com.example.courtierprobackend.transactions.businesslayer.TransactionService;
import com.example.courtierprobackend.transactions.datalayer.dto.TransactionResponseDTO;
import com.example.courtierprobackend.user.businesslayer.UserProvisioningService;
import com.example.courtierprobackend.user.presentationlayer.response.UserResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/broker/clients")
@PreAuthorize("hasRole('BROKER')")
public class BrokerController {

    private final UserProvisioningService userService;
    private final TransactionService transactionService;

    public BrokerController(UserProvisioningService userService, TransactionService transactionService) {
        this.userService = userService;
        this.transactionService = transactionService;
    }

    @GetMapping
    public List<UserResponse> getClients() {
        return userService.getClients();
    }

    /**
     * Get all transactions for a specific client.
     * The broker can only see transactions they are the broker of.
     */
    @GetMapping("/{clientId}/transactions")
    public List<TransactionResponseDTO> getClientTransactions(
            @PathVariable String clientId,
            HttpServletRequest request
    ) {
        UUID brokerId = UserContextUtils.resolveUserId(request);
        // Convert clientId to UUID if possible, else throw error (for now, expect UUID)
        UUID clientUuid;
        try {
            clientUuid = UUID.fromString(clientId);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid clientId format: must be UUID");
        }
        return transactionService.getBrokerClientTransactions(brokerId, clientUuid);
    }

    /**
     * Get ALL transactions for a specific client, across all brokers.
     * Returns broker names with each transaction for display purposes.
     * Note: Access to transaction details is still controlled by ownership.
     */
    @GetMapping("/{clientId}/all-transactions")
    public List<TransactionResponseDTO> getAllClientTransactions(
            @PathVariable String clientId
    ) {
        UUID clientUuid;
        try {
            clientUuid = UUID.fromString(clientId);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid clientId format: must be UUID");
        }
        return transactionService.getAllClientTransactions(clientUuid);
    }
}
