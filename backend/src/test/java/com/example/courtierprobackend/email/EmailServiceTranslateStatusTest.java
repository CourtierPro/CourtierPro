package com.example.courtierprobackend.email;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.assertj.core.api.Assertions.assertThat;

class EmailServiceTranslateStatusTest {

    private final EmailService service = new EmailService(null, null, null, null);

    // ========== translateOfferStatus Tests ==========

    @Test
    void translateOfferStatus_nullReturnsEmpty() {
        assertThat(service.translateOfferStatus(null, true)).isEmpty();
        assertThat(service.translateOfferStatus(null, false)).isEmpty();
    }

    @ParameterizedTest
    @CsvSource({
            "OFFER_MADE, true, Offre soumise",
            "OFFER_MADE, false, Offer Made",
            "PENDING, true, En attente",
            "PENDING, false, Pending",
            "COUNTERED, true, Contre-offre",
            "COUNTERED, false, Countered",
            "ACCEPTED, true, Acceptée",
            "ACCEPTED, false, Accepted",
            "DECLINED, true, Refusée",
            "DECLINED, false, Declined",
            "WITHDRAWN, true, Retirée",
            "WITHDRAWN, false, Withdrawn",
            "EXPIRED, true, Expirée",
            "EXPIRED, false, Expired"
    })
    void translateOfferStatus_allCases(String status, boolean isFrench, String expected) {
        assertThat(service.translateOfferStatus(status, isFrench)).isEqualTo(expected);
    }

    @Test
    void translateOfferStatus_unknownStatusReturnsAsIs() {
        assertThat(service.translateOfferStatus("UNKNOWN_STATUS", true)).isEqualTo("UNKNOWN_STATUS");
        assertThat(service.translateOfferStatus("UNKNOWN_STATUS", false)).isEqualTo("UNKNOWN_STATUS");
    }

    // ========== translateDocumentType Tests ==========

    @Test
    void translateDocumentType_nullReturnsFallback() {
        assertThat(service.translateDocumentType(null, true)).isEqualTo("Autre");
        assertThat(service.translateDocumentType(null, false)).isEqualTo("Other");
    }

    @ParameterizedTest
    @CsvSource({
            "MORTGAGE_PRE_APPROVAL, true, Pré-approbation hypothécaire",
            "MORTGAGE_PRE_APPROVAL, false, Mortgage Pre-Approval",
            "MORTGAGE_APPROVAL, true, Approbation hypothécaire",
            "MORTGAGE_APPROVAL, false, Mortgage Approval",
            "PROOF_OF_FUNDS, true, Preuve de fonds",
            "PROOF_OF_FUNDS, false, Proof of Funds",
            "ID_VERIFICATION, true, Vérification d'identité",
            "ID_VERIFICATION, false, ID Verification",
            "EMPLOYMENT_LETTER, true, Lettre d'emploi",
            "EMPLOYMENT_LETTER, false, Employment Letter",
            "PAY_STUBS, true, Talons de paie",
            "PAY_STUBS, false, Pay Stubs",
            "CREDIT_REPORT, true, Rapport de crédit",
            "CREDIT_REPORT, false, Credit Report",
            "CERTIFICATE_OF_LOCATION, true, Certificat de localisation",
            "CERTIFICATE_OF_LOCATION, false, Certificate of Location",
            "PROMISE_TO_PURCHASE, true, Promesse d'achat",
            "PROMISE_TO_PURCHASE, false, Promise to Purchase",
            "INSPECTION_REPORT, true, Rapport d'inspection",
            "INSPECTION_REPORT, false, Inspection Report",
            "INSURANCE_LETTER, true, Lettre d'assurance",
            "INSURANCE_LETTER, false, Insurance Letter",
            "BANK_STATEMENT, true, Relevé bancaire",
            "BANK_STATEMENT, false, Bank Statement",
            "OTHER, true, Autre",
            "OTHER, false, Other"
    })
    void translateDocumentType_allCases(String docType, boolean isFrench, String expected) {
        assertThat(service.translateDocumentType(docType, isFrench)).isEqualTo(expected);
    }

    @Test
    void translateDocumentType_unknownTypeReturnsAsIs() {
        assertThat(service.translateDocumentType("UNKNOWN_TYPE", true)).isEqualTo("UNKNOWN_TYPE");
        assertThat(service.translateDocumentType("UNKNOWN_TYPE", false)).isEqualTo("UNKNOWN_TYPE");
    }
}
