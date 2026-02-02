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
 * Service for handling S3 file storage operations.
 *
 * Persists files to the configured S3 bucket and manages presigned URLs
 * for secure file access.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class S3StorageService {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    @Value("${aws.s3.bucket-name:courtierpro-dev-files}")
    private String bucketName;

    /**
     * Uploads a file to S3 and returns storage metadata.
     *
     * @param file          The file to upload
     * @param transactionId The transaction ID for organizing files
     * @param documentId    The document ID
     * @return StorageObject containing the S3 key and file metadata
     */
    public StorageObject uploadFile(MultipartFile file, UUID transactionId, UUID documentId) throws IOException {
        String originalFilename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "unnamed";
        String uniqueId = UUID.randomUUID().toString();
        // Use a clean key structure: documents/{transactionId}/{documentId}/{uniqueId}_{filename}
        String s3Key = String.format("documents/%s/%s/%s_%s", transactionId, documentId, uniqueId, originalFilename);

        log.info("Uploading file to S3. Bucket: {}, Key: {}", bucketName, s3Key);

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(s3Key)
                .contentType(file.getContentType())
                .build();

        s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

        log.info("Upload successful. Key: {}", s3Key);

        return StorageObject.builder()
                .s3Key(s3Key)
                .fileName(originalFilename)
                .mimeType(file.getContentType())
                .sizeBytes(file.getSize())
                .build();
    }

    /**
     * Generates a presigned URL for downloading a file.
     *
     * @param s3Key The S3 object key
     * @return A presigned URL string valid for 15 minutes
     */
    public String generatePresignedUrl(String s3Key) {
        if (s3Key == null || s3Key.isEmpty()) {
            return null;
        }

        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .build();

            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofMinutes(15))
                    .getObjectRequest(getObjectRequest)
                    .build();

            PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);
            return presignedRequest.url().toString();

        } catch (Exception e) {
            log.error("Failed to generate presigned URL for key: {}", s3Key, e);
            throw new RuntimeException("Could not generate download URL", e);
        }
    }

    /**
     * Deletes a file from S3.
     *
     * @param s3Key The S3 object key to delete
     */
    public void deleteFile(String s3Key) {
        if (s3Key == null || s3Key.isEmpty()) {
            log.warn("Attempted to delete file with null or empty S3 key");
            return;
        }

        try {
            DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .build();

            s3Client.deleteObject(deleteRequest);
            log.info("Deleted file from S3. Key: {}", s3Key);

        } catch (Exception e) {
            log.error("Failed to delete file from S3. Key: {}", s3Key, e);
            throw new RuntimeException("Could not delete file from S3", e);
        }
    }
}


