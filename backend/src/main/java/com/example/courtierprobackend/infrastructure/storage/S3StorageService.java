package com.example.courtierprobackend.infrastructure.storage;

import com.example.courtierprobackend.documents.datalayer.valueobjects.StorageObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

/**
 * Stub storage service for document uploads.
 * 
 * TODO: Implement actual S3 storage with LocalStack for local development.
 * Currently accepts file uploads but does not persist them to any storage backend.
 * The file metadata is stored in the database, allowing the document flow to work
 * end-to-end without requiring S3 infrastructure.
 */
@Service
@Slf4j
public class S3StorageService {

    /**
     * Accepts a file upload and returns storage metadata.
     * 
     * NOTE: This is a stub implementation - files are NOT actually stored.
     * The metadata is saved to allow the document flow to function.
     *
     * @param file          The file to upload
     * @param transactionId The transaction ID for organizing files
     * @param requestId     The document request ID
     * @return StorageObject containing the S3 key and file metadata
     */
    public StorageObject uploadFile(MultipartFile file, UUID transactionId, UUID requestId) throws IOException {
        String originalFilename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "unnamed";
        String uniqueId = UUID.randomUUID().toString();
        String s3Key = String.format("documents/%s/%s/%s_%s", transactionId, requestId, uniqueId, originalFilename);

        log.warn("STUB: File upload accepted but NOT stored to S3. Key would be: {}", s3Key);
        log.info("File details: name={}, size={} bytes, type={}", originalFilename, file.getSize(), file.getContentType());

        // TODO: Implement actual S3 upload when LocalStack is configured
        // s3Client.putObject(putRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

        return StorageObject.builder()
                .s3Key(s3Key)
                .fileName(originalFilename)
                .mimeType(file.getContentType())
                .sizeBytes(file.getSize())
                .build();
    }

    /**
     * Generates a placeholder URL for downloading a file.
     * 
     * NOTE: This is a stub implementation - the URL is NOT valid for actual downloads.
     *
     * @param s3Key The S3 object key
     * @return A placeholder URL string
     */
    public String generatePresignedUrl(String s3Key) {
        log.warn("STUB: Presigned URL requested but S3 is not configured. Key: {}", s3Key);
        
        // TODO: Implement actual presigned URL generation when LocalStack is configured
        // Return a placeholder URL that indicates the feature is not yet implemented
        return "https://placeholder.local/download/" + s3Key + "?stub=true";
    }
}

