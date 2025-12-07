package com.example.courtierprobackend.infrastructure.storage;

import com.example.courtierprobackend.documents.datalayer.valueobjects.StorageObject;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class S3StorageService {

    private final S3Client s3Client;

    @Value("${aws.s3.bucket:courtierpro-documents}")
    private String bucketName;

    public StorageObject uploadFile(MultipartFile file, String transactionId, String requestId) throws IOException {
        String key = "transactions/" + transactionId + "/documents/" + requestId + "/" + UUID.randomUUID() + "-" + file.getOriginalFilename();

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType(file.getContentType())
                .build();

        s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

        return StorageObject.builder()
                .s3Key(key)
                .fileName(file.getOriginalFilename())
                .mimeType(file.getContentType())
                .sizeBytes(file.getSize())
                .build();
    }
}
