package com.unifiedinbox.unified_inbox.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertySource;

import java.net.URI;

@Configuration
public class S3Config {

    @Bean
    public S3Client s3Client(
        
            @Value("${b2.endpoint}") String endpoint,
            @Value("${b2.region}") String region,
            @Value("${b2.access-key}") String accessKey,
            @Value("${b2.secret-key}") String secretKey,
            ConfigurableEnvironment environment
    ) {

        System.out.println("===== ENV DEBUG =====");

        System.getenv().forEach((key, value) -> {
            if (key.toUpperCase().contains("B2")
                    || key.toUpperCase().contains("ENDPOINT")) {
                System.out.println(key + " = " + value);
            }
        });

        System.out.println("=====================");
        System.out.println("========== B2 DEBUG ==========");

        System.out.println("Resolved endpoint: " + endpoint);

        for (PropertySource<?> source : environment.getPropertySources()) {
            Object value = source.getProperty("b2.endpoint");

            if (value != null) {
                System.out.println(
                        "Source: " + source.getName()
                        + " | Value: " + value
                );
            }
        }

        System.out.println("==============================");

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