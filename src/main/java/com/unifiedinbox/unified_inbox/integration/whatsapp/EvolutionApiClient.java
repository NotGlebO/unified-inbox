
package com.unifiedinbox.unified_inbox.integration.whatsapp;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

@Component
public class EvolutionApiClient {

    private final String webhookUrl;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public EvolutionApiClient(
            @Value("${evolution.api.url}") String baseUrl,
            @Value("${evolution.api.key}") String apiKey,
            @Value("${evolution.webhook.url}") String webhookUrl,
            ObjectMapper objectMapper
    ) {
        this.objectMapper = objectMapper;
        this.webhookUrl = webhookUrl;
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("apikey", apiKey)
                .build();
    }


    public String createInstance(String instanceName) {

        Map<String, Object> webhook = Map.of(
                "enabled", false,
                "url", webhookUrl,
                "webhookByEvents", false,
                "webhookBase64", false,
                "events", new String[]{
                        "CONNECTION_UPDATE",
                        "QRCODE_UPDATED",
                        "MESSAGES_UPSERT"
                }
        );

        Map<String, Object> request = Map.of(
                "instanceName", instanceName,
                "integration", "WHATSAPP-BAILEYS",
                "qrcode", true,
                "webhook", webhook
        );

        return restClient.post()
                .uri("/instance/create")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(String.class);
    }

    public String getConnectionState(String instanceName) {

        return restClient.get()
                .uri("/instance/connectionState/{instanceName}",
                        instanceName)
                .retrieve()
                .body(String.class);
    }

    
    public void deleteInstance(String instanceName) {

        restClient.delete()
                .uri("/instance/delete/{instanceName}", instanceName)
                .retrieve()
                .toBodilessEntity();
    }

    

    public void configureWebhook(
            String instanceName,
            String webhookSecret
    ) {
        Map<String, Object> webhook = Map.of(
                "enabled", true,
                "url", webhookUrl,
                "webhookByEvents", false,
                "webhookBase64", false,
                "events", new String[]{
                        "CONNECTION_UPDATE",
                        "QRCODE_UPDATED",
                        "MESSAGES_UPSERT"
                },
                "headers", Map.of(
                        "x-webhook-secret", webhookSecret
                )
        );

        Map<String, Object> request = Map.of(
                "webhook", webhook
        );

        restClient.post()
                .uri("/webhook/set/{instanceName}", instanceName)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .toBodilessEntity();
    }

    
    public String getInstanceInfo(String instanceName) {

        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/instance/fetchInstances")
                        .queryParam("instanceName", instanceName)
                        .build())
                .retrieve()
                .body(String.class);
    }


    public EvolutionInstanceInfo getVerifiedInstanceInfo(
            String instanceName
    ) {
        String response = getInstanceInfo(instanceName);

        if (response == null || response.isBlank()) {
            throw new IllegalStateException(
                    "Evolution API returned an empty response"
            );
        }

        try {
            JsonNode root = objectMapper.readTree(response);

            if (!root.isArray()) {
                throw new IllegalStateException(
                        "Unexpected Evolution API response format"
                );
            }

            for (JsonNode instance : root) {
                String name = instance.path("name").asString("");

                if (!instanceName.equals(name)) {
                    continue;
                }

                return new EvolutionInstanceInfo(
                        name,
                        instance.path("ownerJid").asString(""),
                        instance.path("connectionStatus").asString("")
                );
            }

            throw new IllegalStateException(
                    "WhatsApp instance not found in Evolution API"
            );

        } catch (JacksonException exception) {
            throw new IllegalStateException(
                    "Cannot parse Evolution API response",
                    exception
            );
        }
    }

    
    public String getQrCode(String instanceName) {
        return restClient.get()
                .uri("/instance/connect/{instanceName}", instanceName)
                .retrieve()
                .body(String.class);
    }


}
