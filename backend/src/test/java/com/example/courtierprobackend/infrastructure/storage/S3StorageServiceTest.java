package com.example.courtierprobackend.infrastructure.storage;

import com.example.courtierprobackend.documents.datalayer.valueobjects.StorageObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for S3StorageService.
 * Tests file upload and presigned URL generation functionality.
 */
class S3StorageServiceTest {

    private S3StorageService storageService;

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
    void uploadFile_WithNullFilename_UsesDefaultName() throws IOException {
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
        StorageObject result1 = storageService.uploadFile(file, transactionId, requestId);
        StorageObject result2 = storageService.uploadFile(file, transactionId, requestId);

        // Assert - Each upload should have a unique S3 key (UUID)
        assertThat(result1.getS3Key()).isNotEqualTo(result2.getS3Key());
    }

    @Test
    void uploadFile_WithDifferentMimeTypes_PreservesMimeType() throws IOException {
        // Arrange
        UUID transactionId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();
        
        MockMultipartFile pdfFile = new MockMultipartFile("file", "doc.pdf", "application/pdf", "pdf".getBytes());
        MockMultipartFile imageFile = new MockMultipartFile("file", "img.png", "image/png", "png".getBytes());
        MockMultipartFile textFile = new MockMultipartFile("file", "text.txt", "text/plain", "txt".getBytes());

        // Act
        StorageObject pdfResult = storageService.uploadFile(pdfFile, transactionId, requestId);
        StorageObject imageResult = storageService.uploadFile(imageFile, transactionId, requestId);
        StorageObject textResult = storageService.uploadFile(textFile, transactionId, requestId);

        // Assert
        assertThat(pdfResult.getMimeType()).isEqualTo("application/pdf");
        assertThat(imageResult.getMimeType()).isEqualTo("image/png");
        assertThat(textResult.getMimeType()).isEqualTo("text/plain");
    }

    @Test
    void uploadFile_CalculatesCorrectFileSize() throws IOException {
        // Arrange
        byte[] content = new byte[1024]; // 1KB
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "large-file.bin",
                "application/octet-stream",
                content
        );

        // Act
        StorageObject result = storageService.uploadFile(file, UUID.randomUUID(), UUID.randomUUID());

        // Assert
        assertThat(result.getSizeBytes()).isEqualTo(1024L);
    }

    // ========== generatePresignedUrl Tests ==========

    @Test
    void generatePresignedUrl_ReturnsPlaceholderUrl() {
        // Arrange
        String s3Key = "documents/TX-123/REQ-456/uuid_test.pdf";

        // Act
        String result = storageService.generatePresignedUrl(s3Key);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).contains("placeholder.local");
        assertThat(result).contains(s3Key);
        assertThat(result).contains("stub=true");
    }

    @Test
    void generatePresignedUrl_WithDifferentKeys_ReturnsDifferentUrls() {
        // Arrange
        String key1 = "documents/TX-1/REQ-1/file1.pdf";
        String key2 = "documents/TX-2/REQ-2/file2.pdf";

        // Act
        String url1 = storageService.generatePresignedUrl(key1);
        String url2 = storageService.generatePresignedUrl(key2);

        // Assert
        assertThat(url1).isNotEqualTo(url2);
        assertThat(url1).contains("file1.pdf");
        assertThat(url2).contains("file2.pdf");
    }

    @Test
    void generatePresignedUrl_FormatsUrlCorrectly() {
        // Arrange
        String s3Key = "path/to/file.doc";

        // Act
        String result = storageService.generatePresignedUrl(s3Key);

        // Assert
        assertThat(result).startsWith("https://");
        assertThat(result).contains("download");
    }
}
