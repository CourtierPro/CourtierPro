package com.example.courtierprobackend.documents.presentationlayer;

import com.example.courtierprobackend.documents.businesslayer.DocumentRequestService;
import com.example.courtierprobackend.documents.presentationlayer.models.DocumentRequestResponseDTO;
import com.example.courtierprobackend.documents.presentationlayer.models.OutstandingDocumentDTO;
import com.example.courtierprobackend.security.UserContextFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Unit tests for GlobalDocumentController.
 */
@ExtendWith(MockitoExtension.class)
class GlobalDocumentControllerTest {

    @Mock
    private DocumentRequestService service;

    private GlobalDocumentController controller;

    @BeforeEach
    void setUp() {
        controller = new GlobalDocumentController(service);
    }

    @Test
    void getAllDocumentsForUser_ReturnsDocuments() {
        // Arrange
        UUID userId = UUID.randomUUID();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(UserContextFilter.INTERNAL_USER_ID_ATTR, userId);

        List<DocumentRequestResponseDTO> docs = List.of(
                DocumentRequestResponseDTO.builder().requestId(UUID.randomUUID()).build(),
                DocumentRequestResponseDTO.builder().requestId(UUID.randomUUID()).build());
        when(service.getAllDocumentsForUser(userId)).thenReturn(docs);

        // Act
        ResponseEntity<List<DocumentRequestResponseDTO>> response = controller.getAllDocumentsForUser(request);

        // Assert
        assertThat(response.getBody()).hasSize(2);
    }

    @Test
    void getAllDocumentsForUser_WithNoDocuments_ReturnsEmptyList() {
        // Arrange
        UUID userId = UUID.randomUUID();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(UserContextFilter.INTERNAL_USER_ID_ATTR, userId);

        when(service.getAllDocumentsForUser(userId)).thenReturn(List.of());

        // Act
        ResponseEntity<List<DocumentRequestResponseDTO>> response = controller.getAllDocumentsForUser(request);

        // Assert
        assertThat(response.getBody()).isEmpty();
    }

    @Test
    void getAllDocumentsForUser_DelegatesCorrectlyToService() {
        // Arrange
        UUID userId = UUID.randomUUID();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(UserContextFilter.INTERNAL_USER_ID_ATTR, userId);

        when(service.getAllDocumentsForUser(userId)).thenReturn(List.of());

        // Act
        controller.getAllDocumentsForUser(request);

        // Assert
        verify(service).getAllDocumentsForUser(userId);
    }

    @Test
    void getOutstandingDocuments_ReturnsList() {
        UUID userId = UUID.randomUUID();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(UserContextFilter.INTERNAL_USER_ID_ATTR, userId);

        OutstandingDocumentDTO dto = OutstandingDocumentDTO.builder()
                .id(UUID.randomUUID())
                .title("Doc")
                .daysOutstanding(5)
                .build();

        when(service.getOutstandingDocumentSummary(userId)).thenReturn(List.of(dto));

        ResponseEntity<List<OutstandingDocumentDTO>> response = controller.getOutstandingDocuments(request);

        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().get(0).getTitle()).isEqualTo("Doc");
    }

    @Test
    void sendReminder_Success() {
        UUID userId = UUID.randomUUID();
        UUID reqId = UUID.randomUUID();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(UserContextFilter.INTERNAL_USER_ID_ATTR, userId);

        doNothing().when(service).sendDocumentReminder(reqId, userId);

        ResponseEntity<Void> response = controller.sendReminder(reqId, request);

        assertThat(response.getStatusCodeValue()).isEqualTo(200);
        verify(service).sendDocumentReminder(reqId, userId);
    }
}
