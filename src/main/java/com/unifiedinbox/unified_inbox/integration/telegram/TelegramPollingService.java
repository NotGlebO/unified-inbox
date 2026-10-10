
package com.unifiedinbox.unified_inbox.integration.telegram;

import com.unifiedinbox.unified_inbox.entity.IntegrationEntity;
import com.unifiedinbox.unified_inbox.entity.IntegrationType;
import com.unifiedinbox.unified_inbox.repository.IntegrationRepository;
import com.unifiedinbox.unified_inbox.service.ConversationMessageService;
import com.unifiedinbox.unified_inbox.service.TokenEncryptionService;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import tools.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TelegramPollingService {

    private final IntegrationRepository integrationRepository;
    private final ConversationMessageService messageService;
    private final TokenEncryptionService encryptionService;

    private final Map<Long, Long> offsets = new ConcurrentHashMap<>();

    public TelegramPollingService(
            IntegrationRepository integrationRepository,
            ConversationMessageService messageService,
            TokenEncryptionService encryptionService
    ) {
        this.integrationRepository = integrationRepository;
        this.messageService = messageService;
        this.encryptionService = encryptionService;
    }

    @Scheduled(fixedDelay = 3000)
    public void poll() {

        for (IntegrationEntity integration :
                integrationRepository.findAll()) {

            if (!integration.isEnabled()
                    || integration.getType() != IntegrationType.TELEGRAM) {
                continue;
            }

            try {
                pollIntegration(integration);
            } catch (Exception e) {
                System.err.println(
                        "Telegram integration "
                                + integration.getId()
                                + ": " + e.getMessage()
                );
            }
        }
    }

    private void pollIntegration(IntegrationEntity integration) {

        String token = encryptionService.decrypt(
                integration.getEncryptedToken()
        );

        RestClient client = RestClient.create(
                "https://api.telegram.org/bot" + token
        );

        long offset = offsets.getOrDefault(
                integration.getId(), 0L
        );

        JsonNode response = client.get()
                .uri("/getUpdates?offset={offset}&timeout=0", offset)
                .retrieve()
                .body(JsonNode.class);

        if (response == null || !response.path("ok").asBoolean()) {
            return;
        }

        for (JsonNode update : response.path("result")) {

            long updateId = update.path("update_id").asLong();

            JsonNode message = update.path("message");

            if (message.hasNonNull("text")) {

                long messageId =
                        message.path("message_id").asLong();

                long chatId =
                        message.path("chat").path("id").asLong();

                String sender = message.path("from")
                        .path("first_name")
                        .asString("Unknown");

                String text = message.path("text").asString();

                Instant receivedAt = Instant.ofEpochSecond(
                        message.path("date").asLong()
                );

                messageService.saveIncomingTelegramMessage(
                        integration,
                        messageId,
                        chatId,
                        sender,
                        text,
                        receivedAt
                );
            }

            offsets.put(integration.getId(), updateId + 1);
        }
    }
}
