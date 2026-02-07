package com.example.courtierprobackend.documents.datalayer;

import com.example.courtierprobackend.documents.datalayer.enums.StageEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransactionStageChecklistStateRepository extends JpaRepository<TransactionStageChecklistState, Long> {

    List<TransactionStageChecklistState> findByTransactionIdAndStage(UUID transactionId, StageEnum stage);

    Optional<TransactionStageChecklistState> findByTransactionIdAndStageAndItemKey(
            UUID transactionId,
            StageEnum stage,
            String itemKey);
}
