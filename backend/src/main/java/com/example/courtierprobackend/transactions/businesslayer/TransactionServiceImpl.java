package com.example.courtierprobackend.transactions.businesslayer;

import com.example.courtierprobackend.transactions.datalayer.TimelineEntry;
import com.example.courtierprobackend.transactions.datalayer.Transaction;
import com.example.courtierprobackend.transactions.datalayer.dto.TransactionRequestDTO;
import com.example.courtierprobackend.transactions.datalayer.dto.TransactionResponseDTO;
import com.example.courtierprobackend.transactions.datalayer.enums.*;
import com.example.courtierprobackend.transactions.datalayer.repositories.TransactionRepository;
import com.example.courtierprobackend.transactions.exceptions.DuplicateTransactionException;
import com.example.courtierprobackend.transactions.exceptions.NotFoundException;
import com.example.courtierprobackend.transactions.util.EntityDtoUtil;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository repo;

    @Override
    public TransactionResponseDTO createTransaction(TransactionRequestDTO dto) {

        // Duplicate check (client + same street + active)
        repo.findByClientIdAndPropertyAddress_StreetAndStatus(
                dto.getClientId(),
                dto.getPropertyAddress().getStreet(),
                TransactionStatus.ACTIVE
        ).ifPresent(t -> {
            throw new DuplicateTransactionException("Client already has an active transaction for this property");
        });

        // Build initial transaction
        Transaction t = new Transaction();
        t.setTransactionId(UUID.randomUUID().toString());
        t.setClientId(dto.getClientId());
        t.setBrokerId(dto.getBrokerId());
        t.setSide(dto.getSide());
        t.setPropertyAddress(dto.getPropertyAddress());
        t.setStatus(TransactionStatus.ACTIVE);
        t.setOpenedAt(LocalDateTime.now());

        // Initial stage per buy/sell side
        if (dto.getSide() == TransactionSide.BUY_SIDE) {
            t.setBuyerStage(BuyerStage.BUYER_PREQUALIFY_FINANCIALLY);
            t.setSellerStage(null);
        } else {
            t.setSellerStage(SellerStage.SELLER_INITIAL_CONSULTATION);
            t.setBuyerStage(null);
        }

        // Timeline entry
        TimelineEntry entry = TimelineEntry.builder()
                .type(TimelineEntryType.CREATED)
                .note("Transaction created")
                .occurredAt(LocalDateTime.now())
                .addedByBrokerId(dto.getBrokerId())
                .transaction(t)
                .build();

        t.setTimeline(List.of(entry));

        repo.save(t);

        return EntityDtoUtil.toResponse(t);
    }

    @Override
    public List<TransactionResponseDTO> getBrokerTransactions(String brokerId) {

        List<Transaction> transactions = repo.findAllByBrokerId(brokerId);

        return transactions.stream()
                .map(EntityDtoUtil::toResponse)
                .toList();
    }

    @Override
    public TransactionResponseDTO getByTransactionId(String transactionId, String brokerId) {

        Transaction tx = repo.findByTransactionId(transactionId)
                .orElseThrow(() -> new NotFoundException("Transaction not found"));

        if (!tx.getBrokerId().equals(brokerId)) {
            throw new NotFoundException("You do not have access to this transaction");
        }

        return EntityDtoUtil.toResponse(tx);
    }
}
