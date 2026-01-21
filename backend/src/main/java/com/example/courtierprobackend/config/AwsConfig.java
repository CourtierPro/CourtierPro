package com.example.courtierprobackend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.ses.SesClient;
import java.net.URI;

@Configuration
public class AwsConfig {

    @Value("${aws.region:ca-central-1}")
    private String region;

    @Value("${aws.s3.endpoint:https://s3.ca-central-1.amazonaws.com}")
    private String endpoint;

    @Value("${aws.ses.region:us-east-1}")
    private String sesRegion;

    @Value("${aws.s3.access-key}")
    private String s3AccessKey;

    @Value("${aws.s3.secret-key}")
    private String s3SecretKey;

    @Value("${aws.ses.access-key}")
    private String sesAccessKey;

    @Value("${aws.ses.secret-key}")
    private String sesSecretKey;

    @Bean
    public S3Client s3Client() {
        var credentials = software.amazon.awssdk.auth.credentials.AwsBasicCredentials.create(s3AccessKey, s3SecretKey);
        var builder = S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(software.amazon.awssdk.auth.credentials.StaticCredentialsProvider.create(credentials));
        
        if (endpoint != null && !endpoint.isEmpty()) {
            builder.endpointOverride(URI.create(endpoint));
        }
        
        return builder.build();
    }

    @Bean
    public SesClient sesClient() {
        var credentials = software.amazon.awssdk.auth.credentials.AwsBasicCredentials.create(sesAccessKey, sesSecretKey);
        return SesClient.builder()
                .region(Region.of(sesRegion))
                .credentialsProvider(software.amazon.awssdk.auth.credentials.StaticCredentialsProvider.create(credentials))
                .build();
    }

    @Bean
    public S3Presigner s3Presigner() {
        var builder = S3Presigner.builder()
                .region(Region.of(region))
                .credentialsProvider(DefaultCredentialsProvider.create());
        
        if (endpoint != null && !endpoint.isEmpty()) {
            builder.endpointOverride(URI.create(endpoint));
        }
        
        return builder.build();
    }
}
