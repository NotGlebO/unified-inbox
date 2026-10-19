
package com.unifiedinbox.unified_inbox.integration.whatsapp;

import com.unifiedinbox.unified_inbox.entity.IntegrationEntity;
import com.unifiedinbox.unified_inbox.integration.IntegrationDisconnectHandler;

import org.springframework.stereotype.Component;

@Component
public class WhatsAppDisconnectHandler
        implements IntegrationDisconnectHandler {

    private final WhatsAppConnectionRepository repository;
    private final EvolutionApiClient evolutionApiClient;

    public WhatsAppDisconnectHandler(
            WhatsAppConnectionRepository repository,
            EvolutionApiClient evolutionApiClient
    ) {
        this.repository = repository;
        this.evolutionApiClient = evolutionApiClient;
    }

    @Override
    public String getType() {
        return "WHATSAPP";
    }

    @Override
    public void disconnect(IntegrationEntity integration) {
        WhatsAppConnectionEntity connection = repository
                .findByIntegrationId(integration.getId())
                .orElseThrow(() ->
                        new IllegalStateException(
                                "WhatsApp connection not found"
                        )
                );

        // If Evolution API fails, stop here.
        // Do not release the JID.
        evolutionApiClient.deleteInstance(
                connection.getInstanceName()
        );

        connection.setStatus(
                WhatsAppConnectionStatus.DISCONNECTED
        );

        connection.setWhatsappJid(null);

        repository.flush();
    }
}
