
package com.unifiedinbox.unified_inbox.integration.whatsapp;

import com.unifiedinbox.unified_inbox.integration.webhook.WebhookHandler;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;

@Component
public class WhatsAppWebhookHandler implements WebhookHandler {

    private final ObjectMapper objectMapper;
    private final WhatsAppConnectionRepository connectionRepository;
    private final String webhookSecret;
    
    private final EvolutionApiClient evolutionApiClient;
    private final WhatsAppAccountBindingService bindingService;


    public WhatsAppWebhookHandler(
            ObjectMapper objectMapper,
            WhatsAppConnectionRepository connectionRepository,
            @Value("${evolution.webhook.secret}") String webhookSecret,
            EvolutionApiClient evolutionApiClient,
            WhatsAppAccountBindingService bindingService
    ) {
        this.objectMapper = objectMapper;
        this.connectionRepository = connectionRepository;
        this.webhookSecret = webhookSecret;
        this.evolutionApiClient = evolutionApiClient;
        this.bindingService = bindingService;

    }

    @Override
    public String getProvider() {
        return "WHATSAPP";
    }

    @Override
    public ResponseEntity<Void> handle(
            String body,
            Map<String, String> headers
    ) {
        String suppliedSecret = headers.get("x-webhook-secret");

        if (suppliedSecret == null
                || !MessageDigest.isEqual(
                        suppliedSecret.getBytes(StandardCharsets.UTF_8),
                        webhookSecret.getBytes(StandardCharsets.UTF_8)
                )) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        try {
            JsonNode payload = objectMapper.readTree(body);

            String instanceName =
                    payload.path("instance").asString("");

            if (instanceName.isBlank()) {
                return ResponseEntity.badRequest().build();
            }

            var connection =
                    connectionRepository.findByInstanceName(instanceName);

            if (connection.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }

            String event = payload.path("event").asString("");

            if ("connection.update".equalsIgnoreCase(event)) {

                String state = payload.path("data")
                        .path("state")
                        .asString("");

                if ("open".equalsIgnoreCase(state)) {
                    // Next step:
                    // Resolve the verified account JID from Evolution API,
                    // then call WhatsAppAccountBindingService.
                }
            }

            return ResponseEntity.ok().build();

        } catch (JacksonException exception) {
            return ResponseEntity.badRequest().build();
        }
    }
}
