package com.example.courtierprobackend.infrastructure.storage;

import com.example.courtierprobackend.documents.datalayer.valueobjects.StorageObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for S3StorageService.
 */
@ExtendWith(MockitoExtension.class)
class S3StorageServiceTest {

    @Mock
    private S3Client s3Client;

    private S3StorageService storageService;

    @BeforeEach
    void setUp() {
        storageService = new S3StorageService(s3Client);
        ReflectionTestUtils.setField(storageService, "bucketName", "test-bucket");
    }

    @Test
    void uploadFile_success_returnsStorageObject() throws IOException {
        MultipartFile mockFile = mock(MultipartFile.class);
        when(mockFile.getOriginalFilename()).thenReturn("document.pdf");
        when(mockFile.getContentType()).thenReturn("application/pdf");
        when(mockFile.getSize()).thenReturn(12345L);
        when(mockFile.getInputStream()).thenReturn(new ByteArrayInputStream("content".getBytes()));

        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());

        StorageObject result = storageService.uploadFile(mockFile, "TX-123", "REQ-001");

        assertThat(result.getFileName()).isEqualTo("document.pdf");
        assertThat(result.getMimeType()).isEqualTo("application/pdf");
        assertThat(result.getSizeBytes()).isEqualTo(12345L);
        assertThat(result.getS3Key()).startsWith("transactions/TX-123/documents/REQ-001/");
        assertThat(result.getS3Key()).endsWith("-document.pdf");
    }

    @Test
    void uploadFile_setsCorrectBucketAndContentType() throws IOException {
        MultipartFile mockFile = mock(MultipartFile.class);
        when(mockFile.getOriginalFilename()).thenReturn("image.png");
        when(mockFile.getContentType()).thenReturn("image/png");
        when(mockFile.getSize()).thenReturn(5000L);
        when(mockFile.getInputStream()).thenReturn(new ByteArrayInputStream("image".getBytes()));

        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());

        storageService.uploadFile(mockFile, "TX-456", "REQ-002");

        ArgumentCaptor<PutObjectRequest> captor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(captor.capture(), any(RequestBody.class));

        PutObjectRequest request = captor.getValue();
        assertThat(request.bucket()).isEqualTo("test-bucket");
        assertThat(request.contentType()).isEqualTo("image/png");
        assertThat(request.key()).startsWith("transactions/TX-456/documents/REQ-002/");
    }

    @Test
    void uploadFile_propagatesIOException() throws IOException {
        MultipartFile mockFile = mock(MultipartFile.class);
        when(mockFile.getInputStream()).thenThrow(new IOException("Stream closed"));

        assertThatThrownBy(() -> storageService.uploadFile(mockFile, "TX-123", "REQ-001"))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("Stream closed");
    }

    @Test
    void uploadFile_handlesS3Exception() throws IOException {
        MultipartFile mockFile = mock(MultipartFile.class);
        when(mockFile.getOriginalFilename()).thenReturn("doc.pdf");
        when(mockFile.getContentType()).thenReturn("application/pdf");
        when(mockFile.getSize()).thenReturn(100L);
        when(mockFile.getInputStream()).thenReturn(new ByteArrayInputStream("test".getBytes()));

        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenThrow(S3Exception.builder().message("Access Denied").build());

        assertThatThrownBy(() -> storageService.uploadFile(mockFile, "TX-123", "REQ-001"))
                .isInstanceOf(S3Exception.class)
                .hasMessageContaining("Access Denied");
    }
}
