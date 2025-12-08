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
                "BUYER_PREQUALIFY_FINANCIALLY",
                "BUYER_SHOP_FOR_PROPERTY",
                "BUYER_SUBMIT_OFFER",
                "BUYER_OFFER_ACCEPTED",
                "BUYER_HOME_INSPECTION",
                "BUYER_FINANCING_FINALIZED",
                "BUYER_FIRST_NOTARY_APPOINTMENT",
                "BUYER_SECOND_NOTARY_APPOINTMENT",
                "BUYER_OCCUPANCY",
                "BUYER_TERMINATED"
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
                "SELLER_LISTING_PUBLISHED",
                "SELLER_REVIEW_OFFERS",
                "SELLER_ACCEPT_BEST_OFFER",
                "SELLER_CONDITIONS_MET",
                "SELLER_NOTARY_COORDINATION",
                "SELLER_NOTARY_APPOINTMENT",
                "SELLER_HANDOVER_KEYS",
                "SELLER_TERMINATED"
        );
    }

    @Test
    void getAllStages_BuyerStagesInEnumOrder() {
        // Act
        Map<String, List<String>> stages = controller.getAllStages();
        List<String> buyerStages = stages.get("BUYER_STAGES");

        // Assert - first and last stages should match enum order
        assertThat(buyerStages.get(0)).isEqualTo("BUYER_PREQUALIFY_FINANCIALLY");
        assertThat(buyerStages.get(buyerStages.size() - 1)).isEqualTo("BUYER_TERMINATED");
    }

    @Test
    void getAllStages_SellerStagesInEnumOrder() {
        // Act
        Map<String, List<String>> stages = controller.getAllStages();
        List<String> sellerStages = stages.get("SELLER_STAGES");

        // Assert - first and last stages should match enum order
        assertThat(sellerStages.get(0)).isEqualTo("SELLER_INITIAL_CONSULTATION");
        assertThat(sellerStages.get(sellerStages.size() - 1)).isEqualTo("SELLER_TERMINATED");
    }
}

