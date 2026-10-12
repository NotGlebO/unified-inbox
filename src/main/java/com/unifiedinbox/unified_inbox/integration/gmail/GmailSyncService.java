
package com.unifiedinbox.unified_inbox.integration.gmail;

import com.unifiedinbox.unified_inbox.entity.*;
import com.unifiedinbox.unified_inbox.repository.*;
import com.unifiedinbox.unified_inbox.service.ConversationService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Service
public class GmailSyncService {

    private final GmailClientService gmailClientService;
    private final ConversationService conversationService;
    private final MessageRepository messageRepository;

    public GmailSyncService(
            GmailClientService gmailClientService,
            ConversationService conversationService,
            MessageRepository messageRepository
    ) {
        this.gmailClientService = gmailClientService;
        this.conversationService = conversationService;
        this.messageRepository = messageRepository;
    }

    @Transactional
    public int syncInbox(GmailAccountEntity account) {

        IntegrationEntity integration = account.getIntegration();

        if (!integration.isEnabled()
                || integration.getType() != IntegrationType.GMAIL) {
            throw new IllegalStateException(
                    "Gmail integration is disconnected"
            );
        }

        var inbox = gmailClientService.getInboxMessages(account);

        if (inbox.messages() == null) {
            return 0;
        }

        int imported = 0;

        for (var reference : inbox.messages()) {

            String messageId = reference.id();

            if (messageRepository
                    .existsBySourceAndExternalMessageId(
                            "GMAIL", messageId)) {
                continue;
            }

            Map<String, Object> data =
                    gmailClientService.getMessage(account, messageId);

            if (data == null) {
                continue;
            }

            String threadId = stringValue(data.get("threadId"));

            if (threadId == null || threadId.isBlank()) {
                continue;
            }

            Map<String, Object> payload = asMap(data.get("payload"));

            String sender = getHeader(payload, "From");
            String subject = getHeader(payload, "Subject");

            String body = extractBody(payload);

            if (body.isBlank()) {
                body = stringValue(data.get("snippet"));
            }

            if (body == null) {
                body = "";
            }

            if (sender == null || sender.isBlank()) {
                sender = "Unknown sender";
            }

            String customerName = sender;

            if (subject != null && !subject.isBlank()) {
                customerName = subject + " — " + sender;
            }

            Instant receivedAt = Instant.now();

            String internalDate = stringValue(data.get("internalDate"));

            if (internalDate != null) {
                receivedAt = Instant.ofEpochMilli(
                        Long.parseLong(internalDate)
                );
            }

            ConversationEntity conversation =
                    conversationService.findOrCreateConversation(
                            integration,
                            threadId,
                            customerName
                    );

            MessageEntity message = new MessageEntity(
                    messageId,
                    sender,
                    body,
                    receivedAt
            );

            message.setConversation(conversation);
            messageRepository.save(message);

            imported++;
        }

        return imported;
    }

    

    private String getHeader(
            Map<String, Object> payload,
            String name
    ) {
        Object headersObject = payload.get("headers");

        if (!(headersObject instanceof List<?> headers)) {
            return null;
        }

        for (Object item : headers) {

            Map<String, Object> header = asMap(item);

            String headerName = stringValue(header.get("name"));

            if (name.equalsIgnoreCase(headerName)) {
                return stringValue(header.get("value"));
            }
        }

        return null;
    }

    private String extractBody(Map<String, Object> payload) {

        // Prefer plain text, including nested MIME parts
        String plainText = findBody(payload, "text/plain");

        if (plainText != null) {
            return plainText;
        }

        // HTML-only messages: use Gmail snippet for now
        return "";
    }

    private String findBody(
            Map<String, Object> part,
            String mimeType
    ) {
        String type = stringValue(part.get("mimeType"));

        if (mimeType.equalsIgnoreCase(type)) {

            Map<String, Object> body = asMap(part.get("body"));
            String encoded = stringValue(body.get("data"));

            if (encoded != null && !encoded.isBlank()) {
                byte[] decoded = Base64.getUrlDecoder().decode(encoded);
                return new String(decoded, StandardCharsets.UTF_8);
            }
        }

        Object partsObject = part.get("parts");

        if (partsObject instanceof List<?> parts) {
            for (Object child : parts) {

                String result = findBody(asMap(child), mimeType);

                if (result != null) {
                    return result;
                }
            }
        }

        return null;
    }

    private Map<String, Object> asMap(Object value) {

        if (!(value instanceof Map<?, ?> map)) {
            return Map.of();
        }

        Map<String, Object> result = new java.util.HashMap<>();

        map.forEach((key, item) -> {
            if (key instanceof String name) {
                result.put(name, item);
            }
        });

        return result;
    }

    private String stringValue(Object value) {
        return value == null ? null : value.toString();
    }
}
