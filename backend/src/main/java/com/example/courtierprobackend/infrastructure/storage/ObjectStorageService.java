package com.example.courtierprobackend.infrastructure.storage;

import com.example.courtierprobackend.documents.datalayer.valueobjects.StorageObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
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
import java.time.Duration;
import java.util.UUID;

/**
 * Service for handling object storage operations using S3-compatible APIs.
 *
 * Supports any S3-compatible storage provider (AWS S3, Cloudflare R2, MinIO, etc.).
 * Persists files to the configured bucket and manages presigned URLs for secure file access.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ObjectStorageService {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    @Value("${aws.s3.bucket-name:courtierpro-dev-files}")
    private String bucketName;

    /**
     * Uploads a file to object storage and returns storage metadata.
     *
     * @param file          The file to upload
     * @param transactionId The transaction ID for organizing files
     * @param documentId    The document ID
     * @return StorageObject containing the object key and file metadata
     */
    public StorageObject uploadFile(MultipartFile file, UUID transactionId, UUID documentId) throws IOException {
        String originalFilename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "unnamed";
        String uniqueId = UUID.randomUUID().toString();
        // Use a clean key structure: documents/{transactionId}/{documentId}/{uniqueId}_{filename}
        String objectKey = String.format("documents/%s/%s/%s_%s", transactionId, documentId, uniqueId, originalFilename);

        log.info("Uploading file to object storage. Bucket: {}, Key: {}", bucketName, objectKey);

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .contentType(file.getContentType())
                .build();

        s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

        log.info("Upload successful. Key: {}", objectKey);

        return StorageObject.builder()
                .s3Key(objectKey)
                .fileName(originalFilename)
                .mimeType(file.getContentType())
                .sizeBytes(file.getSize())
                .build();
    }

    /**
     * Generates a presigned URL for downloading a file.
     *
     * @param objectKey The object key
     * @return A presigned URL string valid for 15 minutes
     */
    public String generatePresignedUrl(String objectKey) {
        return generatePresignedUrl(objectKey, null);
    }

    /**
     * Generates a presigned URL for downloading a file with an optional download filename.
     *
     * @param objectKey The object key
     * @param downloadFileName Optional filename suggested to the browser for download
     * @return A presigned URL string valid for 15 minutes
     */
    public String generatePresignedUrl(String objectKey, String downloadFileName) {
        if (objectKey == null || objectKey.isEmpty()) {
            return null;
        }

        try {
            GetObjectRequest.Builder getObjectRequestBuilder = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey);

            if (downloadFileName != null && !downloadFileName.isBlank()) {
                String safeFileName = downloadFileName
                        .replace("\"", "")
                        .replaceAll("[\\r\\n\\t]", "")
                        .replaceAll("[;]", "_");
                getObjectRequestBuilder.responseContentDisposition(
                        "attachment; filename=\"" + safeFileName + "\"");
            }

            GetObjectRequest getObjectRequest = getObjectRequestBuilder.build();

            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofMinutes(15))
                    .getObjectRequest(getObjectRequest)
                    .build();

            PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);
            return presignedRequest.url().toString();

        } catch (Exception e) {
            log.error("Failed to generate presigned URL for key: {}", objectKey, e);
            throw new RuntimeException("Could not generate download URL", e);
        }
    }

    /**
     * Deletes a file from object storage.
     *
     * @param objectKey The object key to delete
     */
    public void deleteFile(String objectKey) {
        if (objectKey == null || objectKey.isEmpty()) {
            log.warn("Attempted to delete file with null or empty object key");
            return;
        }

        try {
            DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey)
                    .build();

            s3Client.deleteObject(deleteRequest);
            log.info("Deleted file from object storage. Key: {}", objectKey);

        } catch (Exception e) {
            log.error("Failed to delete file from object storage. Key: {}", objectKey, e);
            throw new RuntimeException("Could not delete file from storage", e);
        }
    }
}
