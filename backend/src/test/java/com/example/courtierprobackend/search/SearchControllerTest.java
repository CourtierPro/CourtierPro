package com.example.courtierprobackend.search;

import com.example.courtierprobackend.search.dto.SearchResultDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SearchControllerTest {

    @Mock
    private SearchService searchService;

    private SearchController controller;

    @BeforeEach
    void setUp() {
        controller = new SearchController(searchService);
    }

    @Test
    void search_DelegatesToService() {
        String query = "test query";
        
        controller.search(query);
        
        verify(searchService).search(query);
    }

    @Test
    void search_ReturnsServiceResult() {
        String query = "test query";
        List<SearchResultDTO> expectedResults = List.of(
                SearchResultDTO.builder()
                        .id(UUID.randomUUID().toString())
                        .type(SearchResultDTO.SearchResultType.TRANSACTION)
                        .title("123 Main St")
                        .subtitle("Montreal, QC")
                        .url("/transactions/123")
                        .build()
        );
        when(searchService.search(query)).thenReturn(expectedResults);

        List<SearchResultDTO> results = controller.search(query);

        assertThat(results).isEqualTo(expectedResults);
    }

    @Test
    void search_WithEmptyServiceResult_ReturnsEmptyList() {
        String query = "unknown";
        when(searchService.search(query)).thenReturn(List.of());

        List<SearchResultDTO> results = controller.search(query);

        assertThat(results).isEmpty();
    }
}
