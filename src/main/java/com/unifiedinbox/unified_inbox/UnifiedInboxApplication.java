package com.unifiedinbox.unified_inbox;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class UnifiedInboxApplication {

    public static void main(String[] args) {
        SpringApplication.run(
                UnifiedInboxApplication.class,
                args
        );
    }
}