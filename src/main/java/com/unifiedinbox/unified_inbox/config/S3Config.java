
package com.unifiedinbox.unified_inbox.config;

import java.net.URI;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
public class S3Config {

    @Bean
    public S3Client s3Client(
            @Value("${b2.endpoint}") String endpoint,
            @Value("${b2.region}") String region,
            @Value("${b2.access-key}") String accessKey,
            @Value("${b2.secret-key}") String secretKey
    ) {

        AwsBasicCredentials credentials = AwsBasicCredentials.create(
                accessKey.trim(),
                secretKey.trim()
        );

        return S3Client.builder()
                .endpointOverride(URI.create(endpoint.trim()))
                .region(Region.of(region.trim()))
                .credentialsProvider(
                        StaticCredentialsProvider.create(credentials)
                )
                .forcePathStyle(true)
                .build();
    }
}
