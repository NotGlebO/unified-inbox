
package com.unifiedinbox.unified_inbox.integration.whatsapp;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Component
public class EvolutionApiClient {

    private final String webhookUrl;
    private final RestClient restClient;

    public EvolutionApiClient(
            @Value("${evolution.api.url}") String baseUrl,
            @Value("${evolution.api.key}") String apiKey,
            @Value("${evolution.webhook.url}") String webhookUrl
    ) {
        this.webhookUrl = webhookUrl;
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("apikey", apiKey)
                .build();
    }

    public String createInstance(String instanceName) {

        Map<String, Object> request = Map.of(
                "instanceName", instanceName,
                "integration", "WHATSAPP-BAILEYS",
                "qrcode", true
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
        Map<String, Object> request = Map.of(
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



}
