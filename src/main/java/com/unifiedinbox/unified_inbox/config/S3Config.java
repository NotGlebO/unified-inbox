
package com.unifiedinbox.unified_inbox.config;

import java.net.URI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertySource;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
public class S3Config {

    private static final Logger log =
            LoggerFactory.getLogger(S3Config.class);

    @Bean
    public S3Client s3Client(
            @Value("${b2.endpoint}") String endpoint,
            @Value("${b2.region}") String region,
            @Value("${b2.bucket}") String bucket,
            @Value("${b2.access-key}") String accessKey,
            @Value("${b2.secret-key}") String secretKey,
            ConfigurableEnvironment environment
    ) {

        log.info("========== B2 CONFIG DEBUG ==========");

        log.info("Endpoint: {}", endpoint);
        log.info("Region: {}", region);
        log.info("Bucket: {}", bucket);

        log.info("Access Key length: {}", accessKey.length());
        log.info("Access Key last 4: {}", lastFour(accessKey));

        log.info("Secret Key present: {}", !secretKey.isBlank());
        log.info("Secret Key length: {}", secretKey.length());

        for (String property : new String[] {
                "b2.endpoint",
                "b2.region",
                "b2.bucket",
                "b2.access-key",
                "b2.secret-key"
        }) {

            for (PropertySource<?> source : environment.getPropertySources()) {

                Object value = source.getProperty(property);

                if (value != null) {
                    log.info(
                            "Property {} found in source: {}",
                            property,
                            source.getName()
                    );
                }
            }
        }

        log.info("=====================================");

        return S3Client.builder()
                .endpointOverride(URI.create(endpoint.trim()))
                .region(Region.of(region.trim()))
                .credentialsProvider(
                        StaticCredentialsProvider.create(
                                AwsBasicCredentials.create(
                                        accessKey.trim(),
                                        secretKey.trim()
                                )
                        )
                )
                .forcePathStyle(true)
                .build();
    }

    private static String lastFour(String value) {
        if (value == null || value.isBlank()) {
            return "(empty)";
        }

        return value.substring(Math.max(0, value.length() - 4));
    }
}
