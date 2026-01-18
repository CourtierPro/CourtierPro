package com.example.courtierprobackend.email;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class EmailServiceTranslateStatusTest {
    @Test
    void translateOfferStatus_translatesCorrectly() {
        EmailService service = new EmailService(null, null, null, null);
        assertThat(service.translateOfferStatus("OFFER_MADE", true)).isEqualTo("Offre soumise");
        assertThat(service.translateOfferStatus("OFFER_MADE", false)).isEqualTo("Offer Made");
        assertThat(service.translateOfferStatus(null, true)).isEqualTo("");
    }

    @Test
    void translateDocumentType_translatesCorrectly() {
        EmailService service = new EmailService(null, null, null, null);
        assertThat(service.translateDocumentType("MORTGAGE_PRE_APPROVAL", true)).isEqualTo("Pré-approbation hypothécaire");
        assertThat(service.translateDocumentType("MORTGAGE_PRE_APPROVAL", false)).isEqualTo("Mortgage Pre-Approval");
        assertThat(service.translateDocumentType(null, true)).isEqualTo("Autre");
    }
}
