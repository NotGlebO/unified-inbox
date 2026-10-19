
package com.unifiedinbox.unified_inbox.integration.whatsapp;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WhatsAppTestController {

    private final EvolutionApiClient evolutionApiClient;

    public WhatsAppTestController(
            EvolutionApiClient evolutionApiClient
    ) {
        this.evolutionApiClient = evolutionApiClient;
    }

    @GetMapping("/api/test/whatsapp")
    public ResponseEntity<String> testConnection() {

        String result = evolutionApiClient
                .getConnectionState("unified-inbox-test");

        return ResponseEntity.ok(result);
    }
}
