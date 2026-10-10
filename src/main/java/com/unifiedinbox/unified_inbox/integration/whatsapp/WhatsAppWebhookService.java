package com.unifiedinbox.unified_inbox.integration.whatsapp;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.unifiedinbox.unified_inbox.entity.MessageEntity;
import com.unifiedinbox.unified_inbox.repository.MessageRepository;

import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class WhatsAppWebhookService {

    private final MessageRepository messageRepository;
    private final ObjectMapper objectMapper;

    public WhatsAppWebhookService(
            MessageRepository messageRepository,
            ObjectMapper objectMapper) {

        this.messageRepository = messageRepository;
        this.objectMapper = objectMapper;
    }

    public void processWebhook(String body) throws Exception {

        JsonNode root = objectMapper.readTree(body);

        for (JsonNode entry : root.path("entry")) {
            for (JsonNode change : entry.path("changes")) {

                if (!"messages".equals(
                        change.path("field").asString())) {
                    continue;
                }

                JsonNode value = change.path("value");

                for (JsonNode message : value.path("messages")) {

                    if (!"text".equals(
                            message.path("type").asString())) {
                        continue;
                    }

                    String messageId =
                            message.path("id").asString();

                    String senderId =
                            message.path("from").asString();

                    String text =
                            message.path("text")
                                    .path("body").asString();

                    if (messageId.isBlank()
                            || senderId.isBlank()
                            || text.isBlank()) {
                        continue;
                    }

                    if (messageRepository
                            .existsBySourceAndExternalMessageId(
                                    "WHATSAPP", messageId)) {
                        continue;
                    }

                    long timestamp = message.path("timestamp")
                            .asLong(Instant.now().getEpochSecond());

                    String senderName = senderId;

                    for (JsonNode contact : value.path("contacts")) {
                        if (senderId.equals(
                                contact.path("wa_id").asString())) {

                            String name = contact.path("profile")
                                    .path("name").asString();

                            if (!name.isBlank()) {
                                senderName = name;
                            }
                            break;
                        }
                    }

                    MessageEntity entity = new MessageEntity(
                            senderId,
                            messageId,
                            senderName,
                            text,
                            Instant.ofEpochSecond(timestamp)
                    );

                    messageRepository.save(entity);

                    System.out.println(
                            "WhatsApp message saved: " + messageId);
                }
            }
        }
    }
}