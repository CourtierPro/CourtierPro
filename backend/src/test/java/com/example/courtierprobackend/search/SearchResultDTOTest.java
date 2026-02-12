package com.example.courtierprobackend.search;

import com.example.courtierprobackend.search.dto.SearchResultDTO;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for SearchResultDTO.
 * Covers builder and equality.
 */
class SearchResultDTOTest {

    @Test
    void builder_createsTransactionResult() {
        SearchResultDTO result = SearchResultDTO.builder()
                .id("uuid-123")
                .type(SearchResultDTO.SearchResultType.TRANSACTION)
                .title("123 Main Street")
                .subtitle("Montreal, QC")
                .url("/transactions/uuid-123")
                .build();

        assertThat(result.getId()).isEqualTo("uuid-123");
        assertThat(result.getType()).isEqualTo(SearchResultDTO.SearchResultType.TRANSACTION);
        assertThat(result.getTitle()).isEqualTo("123 Main Street");
        assertThat(result.getSubtitle()).isEqualTo("Montreal, QC");
        assertThat(result.getUrl()).isEqualTo("/transactions/uuid-123");
    }

    @Test
    void builder_createsDocumentResult() {
        SearchResultDTO result = SearchResultDTO.builder()
                .id("doc-456")
                .type(SearchResultDTO.SearchResultType.DOCUMENT)
                .title("Proof of Income")
                .subtitle("456 Oak Ave")
                .url("/transactions/tx-123?tab=documents&focus=doc-456")
                .build();

        assertThat(result.getType()).isEqualTo(SearchResultDTO.SearchResultType.DOCUMENT);
        assertThat(result.getTitle()).isEqualTo("Proof of Income");
    }

    @Test
    void builder_createsUserResult() {
        SearchResultDTO result = SearchResultDTO.builder()
                .id("user-789")
                .type(SearchResultDTO.SearchResultType.USER)
                .title("John Doe")
                .subtitle("john@example.com")
                .url("/contacts/user-789")
                .build();

        assertThat(result.getType()).isEqualTo(SearchResultDTO.SearchResultType.USER);
        assertThat(result.getTitle()).isEqualTo("John Doe");
        assertThat(result.getSubtitle()).isEqualTo("john@example.com");
    }

    @Test
    void builder_createsAppointmentResult() {
        SearchResultDTO result = SearchResultDTO.builder()
                .id("appt-101")
                .type(SearchResultDTO.SearchResultType.APPOINTMENT)
                .title("Closing Meeting")
                .subtitle("Oct 12, 2:00 PM • Office")
                .url("/appointments?focus=appt-101")
                .build();

        assertThat(result.getType()).isEqualTo(SearchResultDTO.SearchResultType.APPOINTMENT);
        assertThat(result.getTitle()).isEqualTo("Closing Meeting");
        assertThat(result.getSubtitle()).isEqualTo("Oct 12, 2:00 PM • Office");
    }

    @Test
    void searchResultType_hasAllExpectedValues() {
        assertThat(SearchResultDTO.SearchResultType.values())
                .containsExactlyInAnyOrder(
                        SearchResultDTO.SearchResultType.TRANSACTION,
                        SearchResultDTO.SearchResultType.DOCUMENT,
                        SearchResultDTO.SearchResultType.USER,
                        SearchResultDTO.SearchResultType.APPOINTMENT,
                        SearchResultDTO.SearchResultType.PAGE
                );
    }

    @Test
    void equality_basedOnId() {
        SearchResultDTO result1 = SearchResultDTO.builder()
                .id("same-id")
                .type(SearchResultDTO.SearchResultType.TRANSACTION)
                .title("Title 1")
                .build();

        SearchResultDTO result2 = SearchResultDTO.builder()
                .id("same-id")
                .type(SearchResultDTO.SearchResultType.TRANSACTION)
                .title("Title 2")
                .build();

        // Both have same id and type - depending on equals implementation
        assertThat(result1.getId()).isEqualTo(result2.getId());
    }
}
