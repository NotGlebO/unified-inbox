package com.unifiedinbox.unified_inbox.integration.whatsapp;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Service
public class WhatsAppService {

    private final RestClient restClient;

    public WhatsAppService(
            @Value("${whatsapp.access-token}") String accessToken,
            @Value("${whatsapp.phone-number-id}") String phoneNumberId,
            @Value("${whatsapp.api-version}") String apiVersion) {

        this.restClient = RestClient.builder()
                .baseUrl("https://graph.facebook.com/"
                        + apiVersion + "/" + phoneNumberId)
                .defaultHeader("Authorization", "Bearer " + accessToken)
                .build();
    }

    public void sendMessage(String recipientPhone, String text) {

        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException(
                    "Сообщение не может быть пустым");
        }

        Map<String, Object> body = Map.of(
                "messaging_product", "whatsapp",
                "to", recipientPhone,
                "type", "text",
                "text", Map.of("body", text)
        );

        restClient.post()
                .uri("/messages")
                .body(body)
                .retrieve()
                .toBodilessEntity();
    }
}