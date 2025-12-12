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

    public enum SearchResultType {
        TRANSACTION,
        DOCUMENT,
        USER,
        PAGE // For frontend static pages, though mostly backend won't return this
    }
}
