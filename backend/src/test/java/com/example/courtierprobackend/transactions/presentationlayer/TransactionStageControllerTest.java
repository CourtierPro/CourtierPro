package com.example.courtierprobackend.transactions.presentationlayer;

import com.example.courtierprobackend.transactions.datalayer.enums.BuyerStage;
import com.example.courtierprobackend.transactions.datalayer.enums.SellerStage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for TransactionStageController.
 */
class TransactionStageControllerTest {

    private TransactionStageController controller;

    @BeforeEach
    void setUp() {
        controller = new TransactionStageController();
    }

    @Test
    void getAllStages_ReturnsBuyerAndSellerStages() {
        // Act
        Map<String, List<String>> stages = controller.getAllStages();

        // Assert
        assertThat(stages).containsKeys("BUYER_STAGES", "SELLER_STAGES");
    }

    @Test
    void getAllStages_BuyerStagesMatchesEnum() {
        // Act
        Map<String, List<String>> stages = controller.getAllStages();

        // Assert
        List<String> buyerStages = stages.get("BUYER_STAGES");
        assertThat(buyerStages).hasSize(BuyerStage.values().length);
    }

    @Test
    void getAllStages_SellerStagesMatchesEnum() {
        // Act
        Map<String, List<String>> stages = controller.getAllStages();

        // Assert
        List<String> sellerStages = stages.get("SELLER_STAGES");
        assertThat(sellerStages).hasSize(SellerStage.values().length);
    }

    @Test
    void getAllStages_ContainsExpectedBuyerStage() {
        // Act
        Map<String, List<String>> stages = controller.getAllStages();

        // Assert - should contain at least one known stage
        assertThat(stages.get("BUYER_STAGES")).contains(BuyerStage.values()[0].name());
    }

    @Test
    void getAllStages_ContainsAllBuyerStageNames() {
        // Act
        Map<String, List<String>> stages = controller.getAllStages();
        List<String> buyerStages = stages.get("BUYER_STAGES");

        // Assert
        assertThat(buyerStages).contains(
                "BUYER_FINANCIAL_PREPARATION",
                "BUYER_PROPERTY_SEARCH",
                "BUYER_OFFER_AND_NEGOTIATION",
                "BUYER_FINANCING_AND_CONDITIONS",
                "BUYER_NOTARY_AND_SIGNING",
                "BUYER_POSSESSION"
        );
    }

    @Test
    void getAllStages_ContainsAllSellerStageNames() {
        // Act
        Map<String, List<String>> stages = controller.getAllStages();
        List<String> sellerStages = stages.get("SELLER_STAGES");

        // Assert
        assertThat(sellerStages).contains(
                "SELLER_INITIAL_CONSULTATION",
                "SELLER_PUBLISH_LISTING",
                "SELLER_OFFER_AND_NEGOTIATION",
                "SELLER_FINANCING_AND_CONDITIONS",
                "SELLER_NOTARY_AND_SIGNING",
                "SELLER_HANDOVER"
        );
    }

    @Test
    void getAllStages_BuyerStagesInEnumOrder() {
        // Act
        Map<String, List<String>> stages = controller.getAllStages();
        List<String> buyerStages = stages.get("BUYER_STAGES");

        // Assert - first and last stages should match enum order
        assertThat(buyerStages.get(0)).isEqualTo("BUYER_FINANCIAL_PREPARATION");
        assertThat(buyerStages.get(buyerStages.size() - 1)).isEqualTo("BUYER_POSSESSION");
    }

    @Test
    void getAllStages_SellerStagesInEnumOrder() {
        // Act
        Map<String, List<String>> stages = controller.getAllStages();
        List<String> sellerStages = stages.get("SELLER_STAGES");

        // Assert - first and last stages should match enum order
        assertThat(sellerStages.get(0)).isEqualTo("SELLER_INITIAL_CONSULTATION");
        assertThat(sellerStages.get(sellerStages.size() - 1)).isEqualTo("SELLER_HANDOVER");
    }
}
