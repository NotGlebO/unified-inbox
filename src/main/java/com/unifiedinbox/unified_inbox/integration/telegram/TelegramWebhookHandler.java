
package com.unifiedinbox.unified_inbox.integration.telegram;

import com.unifiedinbox.unified_inbox.integration.webhook.WebhookHandler;
import com.unifiedinbox.unified_inbox.service.MessageService;
import com.unifiedinbox.unified_inbox.core.dto.MessageDirection;
import com.unifiedinbox.unified_inbox.core.dto.UnifiedMessage;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.Map;

@Component
public class TelegramWebhookHandler implements WebhookHandler {

    private final TelegramCredentialsRepository credentialsRepository;
    private final TelegramWebhookSecretService secretService;
    private final MessageService messageService;
    private final ObjectMapper objectMapper;

    public TelegramWebhookHandler(
            TelegramCredentialsRepository credentialsRepository,
            TelegramWebhookSecretService secretService,
            MessageService messageService,
            ObjectMapper objectMapper
    ) {
        this.credentialsRepository = credentialsRepository;
        this.secretService = secretService;
        this.messageService = messageService;
        this.objectMapper = objectMapper;
    }

    @Override
    public String getProvider() {
        return "TELEGRAM";
    }

    @Override
    public ResponseEntity<Void> handle(
            String body,
            Map<String, String> headers
    ) {
        String secret = headers.get("x-telegram-bot-api-secret-token");

        if (secret == null || !secret.matches("[A-Za-z0-9_-]{1,256}")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        String secretHash = secretService.hash(secret);

        TelegramCredentialsEntity credentials = credentialsRepository
                .findByWebhookSecretHash(secretHash)
                .orElse(null);

        if (credentials == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        var integration = credentials.getIntegration();

        if (!"TELEGRAM".equals(integration.getType())
                || !integration.isEnabled()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        try {
            JsonNode update = objectMapper.readTree(body);
            JsonNode message = update.path("message");

            if (message.isMissingNode() || message.isNull()) {
                return ResponseEntity.ok().build();
            }

            boolean hasAttachment =
                    message.has("photo")
                    || message.has("video")
                    || message.has("animation")
                    || message.has("document")
                    || message.has("audio")
                    || message.has("voice")
                    || message.has("video_note");

            boolean hasText =
                    message.path("text").isTextual()
                    || message.path("caption").isTextual();

            if (!hasText && !hasAttachment) {
                return ResponseEntity.ok().build();
            }

            String chatId = message.path("chat").path("id").asString();
            String messageId = message.path("message_id").asString();
            String text = message.path("text")
                .asString(message.path("caption").asString(""));
            long timestamp = message.path("date").asLong();

            JsonNode from = message.path("from");

            String firstName = from.path("first_name").asString("");
            String lastName = from.path("last_name").asString("");

            String senderName = (firstName + " " + lastName).trim();

            UnifiedMessage unifiedMessage = new UnifiedMessage(
                    integration.getId(),
                    chatId,
                    messageId,
                    senderName,
                    text,
                    Instant.ofEpochSecond(timestamp),
                    MessageDirection.INCOMING
            );

            messageService.saveIncomingMessage(unifiedMessage);

            return ResponseEntity.ok().build();

        } catch (JacksonException exception) {
            return ResponseEntity.badRequest().build();
        }
    }
}
