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
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.util.UUID;
import java.net.MalformedURLException;
import java.net.URL;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ObjectStorageServiceTest {

    @Mock
    private S3Client s3Client;

    @Mock
    private S3Presigner s3Presigner;

    @InjectMocks
    private ObjectStorageService objectStorageService;

    private final String BUCKET_NAME = "test-bucket";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(objectStorageService, "bucketName", BUCKET_NAME);
    }

    @Test
    void uploadFile_ShouldUploadAndReturnMetadata() throws IOException {
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
        StorageObject result = objectStorageService.uploadFile(file, transactionId, requestId);

        // Assert - The service should still create a valid object key
        assertThat(result).isNotNull();
        assertThat(result.getFileName()).isNotNull();
        assertThat(result.getS3Key()).contains(String.format("documents/%s/%s/", transactionId, requestId));
    }

    @Test
    void uploadFile_GeneratesUniqueObjectKey() throws IOException {
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
        StorageObject result = objectStorageService.uploadFile(file, transactionId, requestId);

        // Assert
        assertNotNull(result);
        assertEquals("document.pdf", result.getFileName());
        assertEquals("application/pdf", result.getMimeType());
        assertTrue(result.getS3Key().contains(transactionId.toString()));
        assertTrue(result.getS3Key().contains(requestId.toString()));
        assertTrue(result.getS3Key().endsWith("_document.pdf"));
    }

    @Test
    void uploadFile_WithDifferentMimeTypes_PreservesMimeType() throws IOException {
        // Arrange
        UUID transactionId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.pdf",
                "application/pdf",
                "content".getBytes()
        );
        ArgumentCaptor<PutObjectRequest> captor = ArgumentCaptor.forClass(PutObjectRequest.class);

        // Act
        StorageObject result = objectStorageService.uploadFile(file, transactionId, requestId);

        // Assert
        verify(s3Client).putObject(captor.capture(), any(RequestBody.class));

        PutObjectRequest request = captor.getValue();
        assertEquals(BUCKET_NAME, request.bucket());
        assertEquals(result.getS3Key(), request.key());
        assertEquals("application/pdf", request.contentType());
    }

    @Test
    void uploadFile_WhenOriginalFilenameIsNull_UsesUnnamedFallback() throws IOException {
        UUID transactionId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();
        MultipartFile file = mock(MultipartFile.class);
        when(file.getOriginalFilename()).thenReturn(null);
        when(file.getContentType()).thenReturn("application/pdf");
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream("content".getBytes()));
        when(file.getSize()).thenReturn(7L);

        StorageObject result = objectStorageService.uploadFile(file, transactionId, requestId);

        assertThat(result.getFileName()).isEqualTo("unnamed");
        assertThat(result.getS3Key()).endsWith("_unnamed");
    }

    @Test
    void generatePresignedUrl_ShouldReturnUrl() throws MalformedURLException {
        // Arrange
        String objectKey = "documents/tx-123/req-456/file.pdf";
        String expectedUrl = "https://test-bucket.s3.amazonaws.com/" + objectKey + "?signature=123";

        PresignedGetObjectRequest presignedRequest = mock(PresignedGetObjectRequest.class);
        when(presignedRequest.url()).thenReturn(new URL(expectedUrl));
        when(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class))).thenReturn(presignedRequest);

        // Act
        String result = objectStorageService.generatePresignedUrl(objectKey);

        // Assert
        assertEquals(expectedUrl, result);

        ArgumentCaptor<GetObjectPresignRequest> captor = ArgumentCaptor.forClass(GetObjectPresignRequest.class);
        verify(s3Presigner).presignGetObject(captor.capture());

        GetObjectRequest objectRequest = captor.getValue().getObjectRequest();
        assertEquals(BUCKET_NAME, objectRequest.bucket());
        assertEquals(objectKey, objectRequest.key());
    }

    @Test
    void generatePresignedUrl_WhenKeyIsNull_ShouldReturnNull() {
        assertNull(objectStorageService.generatePresignedUrl(null));
        assertNull(objectStorageService.generatePresignedUrl(""));
    }

    @Test
    void generatePresignedUrl_WithDownloadFileName_SetsContentDisposition() throws MalformedURLException {
        String objectKey = "documents/tx-123/req-456/file.pdf";
        String expectedUrl = "https://test-bucket.s3.amazonaws.com/" + objectKey + "?signature=abc";

        PresignedGetObjectRequest presignedRequest = mock(PresignedGetObjectRequest.class);
        when(presignedRequest.url()).thenReturn(new URL(expectedUrl));
        when(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class))).thenReturn(presignedRequest);

        String result = objectStorageService.generatePresignedUrl(objectKey, "my\"fi;le\r\n\t.pdf");

        assertEquals(expectedUrl, result);
        ArgumentCaptor<GetObjectPresignRequest> captor = ArgumentCaptor.forClass(GetObjectPresignRequest.class);
        verify(s3Presigner).presignGetObject(captor.capture());
        GetObjectRequest objectRequest = captor.getValue().getObjectRequest();
        assertThat(objectRequest.responseContentDisposition())
                .isEqualTo("attachment; filename=\"myfi_le.pdf\"");
    }

    @Test
    void deleteFile_ShouldDeleteFromStorage() {
        // Arrange
        String objectKey = "documents/tx-123/req-456/file.pdf";
        ArgumentCaptor<DeleteObjectRequest> captor = ArgumentCaptor.forClass(DeleteObjectRequest.class);

        // Act
        objectStorageService.deleteFile(objectKey);

        // Assert
        verify(s3Client).deleteObject(captor.capture());
        DeleteObjectRequest request = captor.getValue();
        assertEquals(BUCKET_NAME, request.bucket());
        assertEquals(objectKey, request.key());
    }

    @Test
    void deleteFile_WhenKeyIsNull_ShouldNotCallStorage() {
        // Act
        objectStorageService.deleteFile(null);
        objectStorageService.deleteFile("");

        // Assert - S3 client should never be called
        verify(s3Client, never()).deleteObject(any(DeleteObjectRequest.class));
    }

    @Test
    void deleteFile_WhenStorageFails_ShouldThrowRuntimeException() {
        // Arrange
        String objectKey = "documents/tx-123/req-456/file.pdf";
        when(s3Client.deleteObject(any(DeleteObjectRequest.class)))
                .thenThrow(new RuntimeException("Storage error"));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> objectStorageService.deleteFile(objectKey));
    }

    @Test
    void generatePresignedUrl_WhenStorageFails_ShouldThrowRuntimeException() {
        // Arrange
        String objectKey = "documents/tx-123/req-456/file.pdf";
        when(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class)))
                .thenThrow(new RuntimeException("Presign error"));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> objectStorageService.generatePresignedUrl(objectKey));
    }
}
