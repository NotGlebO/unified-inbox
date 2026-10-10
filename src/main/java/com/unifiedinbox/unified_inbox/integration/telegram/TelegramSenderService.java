
package com.unifiedinbox.unified_inbox.integration.telegram;

import com.unifiedinbox.unified_inbox.entity.IntegrationEntity;
import com.unifiedinbox.unified_inbox.service.TokenEncryptionService;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import tools.jackson.databind.JsonNode;

import java.util.Map;

@Service
public class TelegramSenderService {

    private final TokenEncryptionService encryptionService;

    public TelegramSenderService(
            TokenEncryptionService encryptionService
    ) {
        this.encryptionService = encryptionService;
    }

    public Long sendMessage(
            IntegrationEntity integration,
            Long chatId,
            String text
    ) {
        String token = encryptionService.decrypt(
                integration.getEncryptedToken()
        );

        RestClient client = RestClient.create(
                "https://api.telegram.org/bot" + token
        );

        JsonNode response = client.post()
                .uri("/sendMessage")
                .body(Map.of(
                        "chat_id", chatId,
                        "text", text
                ))
                .retrieve()
                .body(JsonNode.class);

        if (response == null || !response.path("ok").asBoolean()) {
            throw new IllegalStateException(
                    "Telegram could not send the message."
            );
        }

        return response.path("result")
                .path("message_id")
                .asLong();
    }
}
