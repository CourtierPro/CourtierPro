package com.example.courtierprobackend.documents.businesslayer;

import com.example.courtierprobackend.documents.datalayer.enums.DocumentFlowEnum;
import com.example.courtierprobackend.documents.datalayer.enums.DocumentPartyEnum;
import com.example.courtierprobackend.documents.datalayer.enums.DocumentStatusEnum;
import com.example.courtierprobackend.documents.datalayer.enums.DocumentTypeEnum;
import com.example.courtierprobackend.documents.datalayer.enums.StageEnum;
import com.example.courtierprobackend.transactions.datalayer.enums.TransactionSide;

import java.util.List;
import java.util.Map;

/**
 * Single source of truth for stage-driven auto-generated document templates.
 */
public final class StageDocumentTemplateRegistry {

    private StageDocumentTemplateRegistry() {
    }

    public record TemplateSpec(
            String itemKey,
            String label,
            DocumentTypeEnum docType,
            DocumentFlowEnum flow,
            boolean requiresSignature) {
    }

    private static final List<StageEnum> BUY_STAGES = List.of(
            StageEnum.BUYER_FINANCIAL_PREPARATION,
            StageEnum.BUYER_PROPERTY_SEARCH,
            StageEnum.BUYER_OFFER_AND_NEGOTIATION,
            StageEnum.BUYER_FINANCING_AND_CONDITIONS,
            StageEnum.BUYER_NOTARY_AND_SIGNING,
            StageEnum.BUYER_POSSESSION);

    private static final List<StageEnum> SELL_STAGES = List.of(
            StageEnum.SELLER_INITIAL_CONSULTATION,
            StageEnum.SELLER_PUBLISH_LISTING,
            StageEnum.SELLER_OFFER_AND_NEGOTIATION,
            StageEnum.SELLER_FINANCING_AND_CONDITIONS,
            StageEnum.SELLER_NOTARY_AND_SIGNING,
            StageEnum.SELLER_HANDOVER);

    private static final Map<StageEnum, List<TemplateSpec>> BUY_TEMPLATES = Map.of(
            StageEnum.BUYER_FINANCIAL_PREPARATION, List.of(
                    new TemplateSpec("mortgage_pre_approval_letter", "Mortgage pre approval letter",
                            DocumentTypeEnum.MORTGAGE_PRE_APPROVAL, DocumentFlowEnum.REQUEST, false),
                    new TemplateSpec("proof_of_funds", "Proof of funds",
                            DocumentTypeEnum.PROOF_OF_FUNDS, DocumentFlowEnum.REQUEST, false),
                    new TemplateSpec("proof_of_income", "Proof of income",
                            DocumentTypeEnum.PROOF_OF_INCOME, DocumentFlowEnum.REQUEST, false),
                    new TemplateSpec("government_id_1", "Government Id 1",
                            DocumentTypeEnum.GOVERNMENT_ID_1, DocumentFlowEnum.REQUEST, false),
                    new TemplateSpec("government_id_2", "Government Id 2",
                            DocumentTypeEnum.GOVERNMENT_ID_2, DocumentFlowEnum.REQUEST, false),
                    new TemplateSpec("brokerage_contract", "Brokerage Contract",
                            DocumentTypeEnum.BROKERAGE_CONTRACT, DocumentFlowEnum.REQUEST, true)),
            StageEnum.BUYER_OFFER_AND_NEGOTIATION, List.of(
                    new TemplateSpec("promise_to_purchase", "Promise to Purchase",
                            DocumentTypeEnum.PROMISE_TO_PURCHASE, DocumentFlowEnum.REQUEST, true),
                    new TemplateSpec("acknowledged_sellers_declaration", "Acknowledged Seller's Declaration",
                            DocumentTypeEnum.ACKNOWLEDGED_SELLERS_DECLARATION, DocumentFlowEnum.REQUEST, true)),
            StageEnum.BUYER_FINANCING_AND_CONDITIONS, List.of(
                    new TemplateSpec("inspection_report", "Inspection Report",
                            DocumentTypeEnum.INSPECTION_REPORT, DocumentFlowEnum.UPLOAD, false),
                    new TemplateSpec("final_mortgage_approval_letter", "Final Mortgage Approval Letter",
                            DocumentTypeEnum.MORTGAGE_APPROVAL, DocumentFlowEnum.UPLOAD, false),
                    new TemplateSpec("insurance_confirmation_letter", "Insurance Confirmation Letter",
                            DocumentTypeEnum.INSURANCE_LETTER, DocumentFlowEnum.REQUEST, false)),
            StageEnum.BUYER_NOTARY_AND_SIGNING, List.of(
                    new TemplateSpec("notary_contact_sheet", "Notary Contact Sheet",
                            DocumentTypeEnum.NOTARY_CONTACT_SHEET, DocumentFlowEnum.REQUEST, true)));

    private static final Map<StageEnum, List<TemplateSpec>> SELL_TEMPLATES = Map.of(
            StageEnum.SELLER_INITIAL_CONSULTATION, List.of(
                    new TemplateSpec("comparative_market_analysis", "Comparative Market Analysis",
                            DocumentTypeEnum.COMPARATIVE_MARKET_ANALYSIS, DocumentFlowEnum.UPLOAD, false),
                    new TemplateSpec("sellers_declaration", "Seller's Declaration",
                            DocumentTypeEnum.SELLERS_DECLARATION, DocumentFlowEnum.REQUEST, true),
                    new TemplateSpec("certificate_of_location", "Certificate of Location",
                            DocumentTypeEnum.CERTIFICATE_OF_LOCATION, DocumentFlowEnum.REQUEST, false),
                    new TemplateSpec("municipal_tax_bills", "Municipal Tax Bills",
                            DocumentTypeEnum.MUNICIPAL_TAX_BILLS, DocumentFlowEnum.REQUEST, false),
                    new TemplateSpec("mortgage_balance_statement", "Mortgage Balance Statement",
                            DocumentTypeEnum.MORTGAGE_BALANCE_STATEMENT, DocumentFlowEnum.REQUEST, false),
                    new TemplateSpec("school_tax_bills", "School Tax Bills",
                            DocumentTypeEnum.SCHOOL_TAX_BILLS, DocumentFlowEnum.REQUEST, false)),
            StageEnum.SELLER_OFFER_AND_NEGOTIATION, List.of(
                    new TemplateSpec("accepted_promise_to_purchase", "Accepted Promise to Purchase",
                            DocumentTypeEnum.ACCEPTED_PROMISE_TO_PURCHASE, DocumentFlowEnum.REQUEST, true)),
            StageEnum.SELLER_FINANCING_AND_CONDITIONS, List.of(
                    new TemplateSpec("inspection_report", "Inspection Report",
                            DocumentTypeEnum.INSPECTION_REPORT, DocumentFlowEnum.UPLOAD, false),
                    new TemplateSpec("final_mortgage_approval_letter", "Final Mortgage Approval Letter",
                            DocumentTypeEnum.MORTGAGE_APPROVAL, DocumentFlowEnum.UPLOAD, false)),
            StageEnum.SELLER_NOTARY_AND_SIGNING, List.of(
                    new TemplateSpec("notary_contact_sheet", "Notary Contact Sheet",
                            DocumentTypeEnum.NOTARY_CONTACT_SHEET, DocumentFlowEnum.UPLOAD, false),
                    new TemplateSpec("current_deed_of_sale", "Current Deed of Sale",
                            DocumentTypeEnum.CURRENT_DEED_OF_SALE, DocumentFlowEnum.REQUEST, false),
                    new TemplateSpec("mortgage_balance_statement", "Mortgage Balance Statement",
                            DocumentTypeEnum.MORTGAGE_BALANCE_STATEMENT, DocumentFlowEnum.REQUEST, false)));

    public static List<StageEnum> orderedStages(TransactionSide side) {
        return side == TransactionSide.BUY_SIDE ? BUY_STAGES : SELL_STAGES;
    }

    public static boolean stageBelongsToSide(TransactionSide side, StageEnum stage) {
        return orderedStages(side).contains(stage);
    }

    public static int stageIndex(TransactionSide side, StageEnum stage) {
        return orderedStages(side).indexOf(stage);
    }

    public static StageEnum parseAndValidateStage(TransactionSide side, String stageName) {
        StageEnum parsed;
        try {
            parsed = StageEnum.valueOf(stageName);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid stage: " + stageName);
        }
        if (!stageBelongsToSide(side, parsed)) {
            throw new IllegalArgumentException("Stage does not belong to side: " + stageName);
        }
        return parsed;
    }

    public static List<TemplateSpec> templatesFor(TransactionSide side, StageEnum stage) {
        if (side == TransactionSide.BUY_SIDE) {
            return BUY_TEMPLATES.getOrDefault(stage, List.of());
        }
        return SELL_TEMPLATES.getOrDefault(stage, List.of());
    }

    public static String templateKey(StageEnum stage, TemplateSpec template) {
        return stage.name() + ":" + template.itemKey();
    }

    public static boolean isAutoChecked(TemplateSpec template, DocumentStatusEnum status) {
        if (status == null) {
            return false;
        }
        if (template.flow() == DocumentFlowEnum.UPLOAD) {
            return status == DocumentStatusEnum.SUBMITTED;
        }
        return status == DocumentStatusEnum.APPROVED;
    }

    public static DocumentPartyEnum expectedFrom(TransactionSide side, TemplateSpec template) {
        if (template.flow() == DocumentFlowEnum.UPLOAD) {
            return DocumentPartyEnum.BROKER;
        }
        return side == TransactionSide.BUY_SIDE ? DocumentPartyEnum.BUYER : DocumentPartyEnum.SELLER;
    }
}
