package com.unifiedinbox.unified_inbox;

import com.unifiedinbox.unified_inbox.core.storage.FileStorageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.nio.charset.StandardCharsets;

@SpringBootTest
class FileStorageTest {

    @Autowired
    private FileStorageService fileStorageService;

    @Test
    void uploadTest() {
        String key = fileStorageService.upload(
                "Hello from Unified Inbox!"
                        .getBytes(StandardCharsets.UTF_8),
                "text/plain"
        );

        System.out.println("Uploaded file: " + key);
    }
}