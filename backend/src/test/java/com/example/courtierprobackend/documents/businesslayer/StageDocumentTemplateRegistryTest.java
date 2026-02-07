package com.example.courtierprobackend.documents.businesslayer;

import com.example.courtierprobackend.documents.datalayer.enums.DocumentFlowEnum;
import com.example.courtierprobackend.documents.datalayer.enums.DocumentStatusEnum;
import com.example.courtierprobackend.documents.datalayer.enums.StageEnum;
import com.example.courtierprobackend.transactions.datalayer.enums.TransactionSide;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StageDocumentTemplateRegistryTest {

    @Test
    void parseAndValidateStage_withUnknownValue_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> StageDocumentTemplateRegistry.parseAndValidateStage(
                TransactionSide.BUY_SIDE, "NOT_A_REAL_STAGE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid stage");
    }

    @Test
    void parseAndValidateStage_withSideMismatch_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> StageDocumentTemplateRegistry.parseAndValidateStage(
                TransactionSide.BUY_SIDE, StageEnum.SELLER_HANDOVER.name()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Stage does not belong to side");
    }

    @Test
    void isAutoChecked_withNullStatus_returnsFalse() {
        StageDocumentTemplateRegistry.TemplateSpec template = StageDocumentTemplateRegistry
                .templatesFor(TransactionSide.BUY_SIDE, StageEnum.BUYER_FINANCIAL_PREPARATION)
                .get(0);

        assertThat(StageDocumentTemplateRegistry.isAutoChecked(template, null)).isFalse();
    }

    @Test
    void isAutoChecked_withUploadAndRequestFlows_coversBothBranches() {
        StageDocumentTemplateRegistry.TemplateSpec uploadTemplate = StageDocumentTemplateRegistry
                .templatesFor(TransactionSide.BUY_SIDE, StageEnum.BUYER_FINANCING_AND_CONDITIONS)
                .stream()
                .filter(t -> t.flow() == DocumentFlowEnum.UPLOAD)
                .findFirst()
                .orElseThrow();

        StageDocumentTemplateRegistry.TemplateSpec requestTemplate = StageDocumentTemplateRegistry
                .templatesFor(TransactionSide.BUY_SIDE, StageEnum.BUYER_FINANCIAL_PREPARATION)
                .stream()
                .filter(t -> t.flow() == DocumentFlowEnum.REQUEST)
                .findFirst()
                .orElseThrow();

        assertThat(StageDocumentTemplateRegistry.isAutoChecked(uploadTemplate, DocumentStatusEnum.SUBMITTED)).isTrue();
        assertThat(StageDocumentTemplateRegistry.isAutoChecked(uploadTemplate, DocumentStatusEnum.APPROVED)).isFalse();
        assertThat(StageDocumentTemplateRegistry.isAutoChecked(requestTemplate, DocumentStatusEnum.APPROVED)).isTrue();
        assertThat(StageDocumentTemplateRegistry.isAutoChecked(requestTemplate, DocumentStatusEnum.SUBMITTED)).isFalse();
    }
}
