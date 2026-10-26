package com.unifiedinbox.unified_inbox.integration.gmail;

import com.unifiedinbox.unified_inbox.core.dto.MessageDirection;
import com.unifiedinbox.unified_inbox.core.dto.UnifiedMessage;
import com.unifiedinbox.unified_inbox.integration.webhook.WebhookHandler;
import com.unifiedinbox.unified_inbox.service.MessageService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.Map;

@Component
public class GmailWebhookHandler implements WebhookHandler {

    private final GmailConnectionRepository connectionRepository;
    private final GmailIntegrationService gmailService;
    private final MessageService messageService;
    private final ObjectMapper objectMapper;

    public GmailWebhookHandler(
            GmailConnectionRepository connectionRepository,
            GmailIntegrationService gmailService,
            MessageService messageService,
            ObjectMapper objectMapper
    ) {
        this.connectionRepository = connectionRepository;
        this.gmailService = gmailService;
        this.messageService = messageService;
        this.objectMapper = objectMapper;
    }

    @Override
    public String getProvider() {
        return "GMAIL";
    }

    @Override
    public ResponseEntity<Void> handle(
            String body,
            Map<String, String> headers
    ) {
        try {
            JsonNode payload = objectMapper.readTree(body);

            JsonNode message = payload.path("message");
            if (message.isMissingNode() || message.isNull()) {
                return ResponseEntity.ok().build();
            }

            JsonNode data = message.path("data");
            if (data.isMissingNode() || data.isNull()) {
                return ResponseEntity.ok().build();
            }

            String decodedData = new String(
                    java.util.Base64.getDecoder().decode(data.asText()),
                    java.nio.charset.StandardCharsets.UTF_8
            );

            JsonNode notification = objectMapper.readTree(decodedData);
            String emailAddress = notification.path("emailAddress").asString("");

            GmailConnectionEntity connection = connectionRepository.findByEmail(emailAddress)
                    .orElse(null);

            if (connection == null || !connection.getIntegration().isEnabled()) {
                return ResponseEntity.ok().build();
            }

            JsonNode historyId = notification.path("historyId");
            if (!historyId.isMissingNode()) {
                connection.setHistoryId(historyId.asText());
            }

            return ResponseEntity.ok().build();
        } catch (JacksonException exception) {
            return ResponseEntity.badRequest().build();
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    public void processIncomingMessage(
            Long integrationId,
            String messageId,
            String threadId,
            String subject,
            String sender,
            String recipient,
            String snippet,
            Instant receivedAt
    ) {
        UnifiedMessage message = new UnifiedMessage(
                integrationId,
                threadId,
                messageId,
                sender,
                snippet,
                receivedAt,
                MessageDirection.INCOMING
        );

        messageService.saveExternalMessage(message);
    }
}
