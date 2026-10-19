package com.unifiedinbox.unified_inbox.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

import java.net.URI;

@Configuration
public class S3Config {

    @Bean
    public S3Client s3Client(
            @Value("${b2.endpoint}") String endpoint,
            @Value("${b2.region}") String region,
            @Value("${b2.access-key}") String accessKey,
            @Value("${b2.secret-key}") String secretKey
            
    ) {
        System.out.println("B2 endpoint: " + endpoint);
        System.out.println("B2 region: " + region);
        System.out.println("B2 Access Key has spaces: "
                + !accessKey.equals(accessKey.trim()));
        System.out.println("Actual B2 endpoint = [" + endpoint + "]");
        return S3Client.builder()
                .endpointOverride(URI.create(endpoint.trim()))
                .region(Region.of(region))
                .credentialsProvider(
                        StaticCredentialsProvider.create(
                                AwsBasicCredentials.create(
                                        accessKey,
                                        secretKey
                                )
                        )
                )
                .forcePathStyle(true)
                .build();
    }
    
}