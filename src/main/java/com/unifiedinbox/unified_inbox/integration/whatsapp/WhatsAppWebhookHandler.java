
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

import com.unifiedinbox.unified_inbox.core.dto.MessageDirection;
import com.unifiedinbox.unified_inbox.core.dto.UnifiedMessage;
import com.unifiedinbox.unified_inbox.service.MessageService;
import java.time.Instant;

@Component
public class WhatsAppWebhookHandler implements WebhookHandler {

    private final MessageService messageService;
    private final ObjectMapper objectMapper;
    private final WhatsAppConnectionRepository connectionRepository;
    private final EvolutionApiClient evolutionApiClient;
    private final WhatsAppAccountBindingService bindingService;
    private final String webhookSecret;

    public WhatsAppWebhookHandler(
            ObjectMapper objectMapper,
            WhatsAppConnectionRepository connectionRepository,
            EvolutionApiClient evolutionApiClient,
            WhatsAppAccountBindingService bindingService,
            @Value("${evolution.webhook.secret}") String webhookSecret,
            MessageService messageService
    ) {
        this.objectMapper = objectMapper;
        this.connectionRepository = connectionRepository;
        this.evolutionApiClient = evolutionApiClient;
        this.bindingService = bindingService;
        this.webhookSecret = webhookSecret;
        this.messageService = messageService;
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

        final JsonNode payload;

        try {
            payload = objectMapper.readTree(body);
        } catch (JacksonException exception) {
            return ResponseEntity.badRequest().build();
        }

        String instanceName =
                payload.path("instance").asString("");

        if (instanceName.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        if (connectionRepository
                .findByInstanceName(instanceName)
                .isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        String event = payload.path("event").asString("");

        if ("messages.upsert".equalsIgnoreCase(event)) {
            return handleIncomingMessage(payload, instanceName);
        }

        if (!"connection.update".equalsIgnoreCase(event)) {
            return ResponseEntity.ok().build();
        }

        String state = payload.path("data")
                .path("state")
                .asString("");

        
        if ("close".equalsIgnoreCase(state)) {

            // Verify current state to avoid processing an old
            // close event after a newer open event.
            EvolutionInstanceInfo info =
                    evolutionApiClient.getVerifiedInstanceInfo(instanceName);

            if ("close".equalsIgnoreCase(info.connectionStatus())) {
                bindingService.markDisconnected(instanceName);
            }

            return ResponseEntity.ok().build();
        }

        if (!"open".equalsIgnoreCase(state)) {
            return ResponseEntity.ok().build();
        }


        // Verify the account through our authenticated
        // Evolution API connection.
        EvolutionInstanceInfo info =
                evolutionApiClient.getVerifiedInstanceInfo(instanceName);

        if (!"open".equalsIgnoreCase(info.connectionStatus())) {
            return ResponseEntity.ok().build();
        }

        if (info.ownerJid() == null
                || info.ownerJid().isBlank()) {
            return ResponseEntity.ok().build();
        }

        WhatsAppBindingResult result =
                bindingService.bindAccount(
                        instanceName,
                        info.ownerJid()
                );

        if (result == WhatsAppBindingResult.CONFLICT) {
            bindingService.markConflict(instanceName);
            evolutionApiClient.deleteInstance(instanceName);
        } else if (result == WhatsAppBindingResult.REJECTED) {
            evolutionApiClient.deleteInstance(instanceName);
        }

        return ResponseEntity.ok().build();
    }

    private ResponseEntity<Void> handleIncomingMessage(
            JsonNode payload,
            String instanceName
    ) {
        WhatsAppConnectionEntity connection = connectionRepository
                .findByInstanceName(instanceName)
                .orElse(null);

        if (connection == null
                || connection.getStatus() != WhatsAppConnectionStatus.CONNECTED
                || !connection.getIntegration().isEnabled()) {
            return ResponseEntity.ok().build();
        }

        JsonNode data = payload.path("data");

        // Evolution API may deliver one message or an array.
        if (data.isArray()) {
            for (JsonNode item : data) {
                saveWhatsAppMessage(connection, item);
            }
        } else if (data.isObject()) {
            saveWhatsAppMessage(connection, data);
        }

        return ResponseEntity.ok().build();
    }

    private void saveWhatsAppMessage(
            WhatsAppConnectionEntity connection,
            JsonNode data
    ) {
        JsonNode key = data.path("key");

        if (key.path("fromMe").asBoolean(false)) {
            return;
        }

        String chatId = key.path("remoteJid").asString("");
        String messageId = key.path("id").asString("");

        if (chatId.isBlank()
                || messageId.isBlank()
                || chatId.endsWith("@g.us")
                || "status@broadcast".equals(chatId)) {
            return;
        }

        JsonNode content = data.path("message");

        String text = content.path("conversation").asString("");

        if (text.isBlank()) {
            text = content.path("extendedTextMessage")
                    .path("text")
                    .asString("");
        }

        // First version supports text messages only.
        if (text.isBlank()) {
            return;
        }

        String senderName = data.path("pushName").asString("");

        if (senderName.isBlank()) {
            senderName = chatId.split("@")[0];
        }

        long timestamp = data.path("messageTimestamp")
                .asLong(Instant.now().getEpochSecond());

        UnifiedMessage message = new UnifiedMessage(
                connection.getIntegration().getId(),
                chatId,
                messageId,
                senderName,
                text,
                Instant.ofEpochSecond(timestamp),
                MessageDirection.INCOMING
        );

        messageService.saveIncomingMessage(message);
    }
}
