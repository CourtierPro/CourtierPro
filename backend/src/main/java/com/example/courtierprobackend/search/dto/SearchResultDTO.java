package com.example.courtierprobackend.search.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SearchResultDTO {
    private String id;
    private SearchResultType type;
    private String title;
    private String subtitle;
    private String url;

    /**
     * Defines the category of search result.
     * 
     * <p>TRANSACTION, DOCUMENT, USER are returned by the backend.
     * PAGE is included for frontend-backend type compatibility, allowing
     * the frontend to use the same type definition for client-side
     * static route results (e.g., Dashboard, Settings). The backend
     * does not return PAGE type results.</p>
     */
    public enum SearchResultType {
        TRANSACTION,
        DOCUMENT,
        USER,
        /** Used by frontend only for static page navigation results. */
        PAGE
    }
}
