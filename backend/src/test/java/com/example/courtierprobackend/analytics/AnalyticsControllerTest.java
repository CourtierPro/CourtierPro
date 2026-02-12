package com.example.courtierprobackend.analytics;

import com.example.courtierprobackend.transactions.datalayer.enums.TransactionSide;
import com.example.courtierprobackend.security.UserContextFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsControllerTest {

    @Mock
    private AnalyticsService analyticsService;

    private AnalyticsController controller;

    private UUID brokerId;

    @BeforeEach
    void setUp() {
        controller = new AnalyticsController(analyticsService);
        brokerId = UUID.randomUUID();
    }

    private MockHttpServletRequest createBrokerRequest(UUID internalId) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(UserContextFilter.INTERNAL_USER_ID_ATTR, internalId);
        request.setAttribute(UserContextFilter.USER_ROLE_ATTR, "BROKER");
        return request;
    }

    private Jwt createJwt() {
        return Jwt.withTokenValue("token")
                .header("alg", "HS256")
                .claim("sub", "auth0|123")
                .build();
    }

    @Test
    void getAnalytics_ShouldReturnAnalyticsDTO() {
        MockHttpServletRequest request = createBrokerRequest(brokerId);
        Jwt jwt = createJwt();
        
        AnalyticsDTO mockDto = new AnalyticsDTO(
            10, 5, 5, 0, 5, 5, 50.0, 30.0, 60, 10,
            null, null, null, null, 
            100, 2.5, 50, 2.0, 100, 
            20, 4.0, 50.0, 5, 15, 5, 
            25, 60.0, 2.0, 500000.0, 2, 10.0, 
            30, 70.0, 480000.0, 550000.0, 450000.0, 6.0, 
            5, 15.0, 
            50, 10, 80.0, 5, 2.5, 
            60, 90.0, 5.0, 5.0, 
            10, 3.0, 
            40, 95.0, 2, 1, 2.0, 
            25, 5, 20, 40, 
            "October", 2
        );

        when(analyticsService.getAnalytics(eq(brokerId), any(AnalyticsFilterRequest.class))).thenReturn(mockDto);

        ResponseEntity<AnalyticsDTO> response = controller.getAnalytics(
                null, jwt, request, LocalDate.parse("2023-01-01"), LocalDate.parse("2023-12-31"), TransactionSide.BUY_SIDE, "John");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().totalTransactions()).isEqualTo(10);
        verify(analyticsService).getAnalytics(eq(brokerId), any(AnalyticsFilterRequest.class));
    }

    @Test
    void exportAnalyticsCsv_ShouldReturnCsvFile() {
        MockHttpServletRequest request = createBrokerRequest(brokerId);
        Jwt jwt = createJwt();
        byte[] csvContent = "Category,Metric,Value\nMeta,Broker,Test".getBytes();

        when(analyticsService.exportAnalyticsCsv(eq(brokerId), any(AnalyticsFilterRequest.class))).thenReturn(csvContent);

        ResponseEntity<byte[]> response = controller.exportAnalyticsCsv(
                null, jwt, request, LocalDate.parse("2023-01-01"), null, null, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getFirst("Content-Type")).isEqualTo("text/csv");
        assertThat(response.getHeaders().getFirst("Content-Disposition")).contains("analytics_report.csv");
        assertThat(response.getBody()).isEqualTo(csvContent);
        
        verify(analyticsService).exportAnalyticsCsv(eq(brokerId), any(AnalyticsFilterRequest.class));
    }

    @Test
    void exportAnalyticsPdf_ShouldReturnPdfFile() {
        MockHttpServletRequest request = createBrokerRequest(brokerId);
        Jwt jwt = createJwt();
        byte[] pdfContent = "%PDF-1.4...".getBytes();

        when(analyticsService.exportAnalyticsPdf(eq(brokerId), any(AnalyticsFilterRequest.class))).thenReturn(pdfContent);

        ResponseEntity<byte[]> response = controller.exportAnalyticsPdf(
                null, jwt, request, null, null, null, "Smith");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getFirst("Content-Type")).isEqualTo("application/pdf");
        assertThat(response.getHeaders().getFirst("Content-Disposition")).contains("analytics_report.pdf");
        assertThat(response.getBody()).isEqualTo(pdfContent);

        verify(analyticsService).exportAnalyticsPdf(eq(brokerId), any(AnalyticsFilterRequest.class));
    }
}
