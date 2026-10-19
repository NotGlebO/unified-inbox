
package com.unifiedinbox.unified_inbox.integration.whatsapp;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Component
public class EvolutionApiClient {

    private final RestClient restClient;

    public EvolutionApiClient(
            @Value("${evolution.api.url}") String baseUrl,
            @Value("${evolution.api.key}") String apiKey
    ) {
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

}
