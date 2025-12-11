package com.example.courtierprobackend.infrastructure.storage;

import com.example.courtierprobackend.documents.datalayer.valueobjects.StorageObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.IOException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class S3StorageServiceTest {

    @Mock
    private S3Client s3Client;

    @Mock
    private S3Presigner s3Presigner;

    @InjectMocks
    private S3StorageService s3StorageService;

    private final String BUCKET_NAME = "test-bucket";

    @BeforeEach
    void setUp() {
        storageService = new S3StorageService();
    }

    // ========== uploadFile Tests ==========

    @Test
    void uploadFile_WithValidFile_ReturnsStorageObject() throws IOException {
        // Arrange
        UUID transactionId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test-document.pdf",
                "application/pdf",
                "PDF content".getBytes()
        );

        // Act
        StorageObject result = storageService.uploadFile(file, transactionId, requestId);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getFileName()).isEqualTo("test-document.pdf");
        assertThat(result.getMimeType()).isEqualTo("application/pdf");
        assertThat(result.getSizeBytes()).isEqualTo(11L); // "PDF content".length()
        assertThat(result.getS3Key()).contains(String.format("documents/%s/%s/", transactionId, requestId));
        assertThat(result.getS3Key()).endsWith("_test-document.pdf");
    }

    @Test
    void uploadFile_ShouldUploadToS3AndReturnMetadata() throws IOException {
        // Arrange
        UUID transactionId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();
        // Use a file with empty filename - the service treats empty string same as having a name
        // Test the actual behavior: MockMultipartFile returns empty string for null, not null
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "",  // empty filename (MockMultipartFile converts null to "")
                "text/plain",
                "Some content".getBytes()
        );

        // Act
        StorageObject result = storageService.uploadFile(file, transactionId, requestId);

        // Assert - The service should still create a valid S3 key
        assertThat(result).isNotNull();
        assertThat(result.getFileName()).isNotNull();
        assertThat(result.getS3Key()).contains(String.format("documents/%s/%s/", transactionId, requestId));
    }

    @Test
    void uploadFile_GeneratesUniqueS3Key() throws IOException {
        // Arrange
        UUID transactionId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "document.pdf",
                "application/pdf",
                "content".getBytes()
        );

        // Act
        StorageObject result = s3StorageService.uploadFile(file, transactionId, requestId);

        // Assert
        assertNotNull(result);
        assertEquals("test.pdf", result.getFileName());
        assertEquals("application/pdf", result.getMimeType());
        assertTrue(result.getS3Key().contains(transactionId));
        assertTrue(result.getS3Key().contains(requestId));
        assertTrue(result.getS3Key().endsWith("_test.pdf"));

    @Test
    void uploadFile_WithDifferentMimeTypes_PreservesMimeType() throws IOException {
        // Arrange
        UUID transactionId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();
        
        PutObjectRequest request = captor.getValue();
        assertEquals(BUCKET_NAME, request.bucket());
        assertEquals(result.getS3Key(), request.key());
        assertEquals("application/pdf", request.contentType());
    }

    @Test
    void generatePresignedUrl_ShouldReturnUrl() throws MalformedURLException {
        // Arrange
        String s3Key = "documents/tx-123/req-456/file.pdf";
        String expectedUrl = "https://test-bucket.s3.amazonaws.com/" + s3Key + "?signature=123";

        PresignedGetObjectRequest presignedRequest = mock(PresignedGetObjectRequest.class);
        when(presignedRequest.url()).thenReturn(new URL(expectedUrl));
        when(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class))).thenReturn(presignedRequest);

        // Act
        StorageObject result = storageService.uploadFile(file, UUID.randomUUID(), UUID.randomUUID());

        // Assert
        assertEquals(expectedUrl, result);

        ArgumentCaptor<GetObjectPresignRequest> captor = ArgumentCaptor.forClass(GetObjectPresignRequest.class);
        verify(s3Presigner).presignGetObject(captor.capture());
        
        GetObjectRequest objectRequest = captor.getValue().getObjectRequest();
        assertEquals(BUCKET_NAME, objectRequest.bucket());
        assertEquals(s3Key, objectRequest.key());
    }

    @Test
    void generatePresignedUrl_WhenKeyIsNull_ShouldReturnNull() {
        assertNull(s3StorageService.generatePresignedUrl(null));
        assertNull(s3StorageService.generatePresignedUrl(""));
    }
}
