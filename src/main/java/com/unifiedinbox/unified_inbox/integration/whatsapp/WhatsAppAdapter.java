
package com.unifiedinbox.unified_inbox.integration.whatsapp;

import com.unifiedinbox.unified_inbox.core.adapter.MessagingAdapter;
import com.unifiedinbox.unified_inbox.core.adapter.MessageSendException;
import com.unifiedinbox.unified_inbox.core.dto.IntegrationCapabilities;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component
public class WhatsAppAdapter implements MessagingAdapter {

    private final WhatsAppConnectionRepository connectionRepository;
    private final EvolutionApiClient evolutionApiClient;
    private final ObjectMapper objectMapper;

    public WhatsAppAdapter(
            WhatsAppConnectionRepository connectionRepository,
            EvolutionApiClient evolutionApiClient,
            ObjectMapper objectMapper
    ) {
        this.connectionRepository = connectionRepository;
        this.evolutionApiClient = evolutionApiClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public String getType() {
        return "WHATSAPP";
    }

    @Override
    public String getDisplayName() {
        return "WhatsApp";
    }

    @Override
    public String getIconPath() {
        return "/icons/whatsapp.svg";
    }

    @Override
    public String getColor() {
        return "#25D366";
    }

    @Override
    public IntegrationCapabilities getCapabilities() {
        return new IntegrationCapabilities(
                true, false, false, false, false, false
        );
    }

    @Override
    public String sendMessage(
            Long integrationId,
            String externalChatId,
            String text
    ) {
        WhatsAppConnectionEntity connection = connectionRepository
                .findByIntegrationId(integrationId)
                .orElseThrow(() -> new MessageSendException(
                        MessageSendException.FailureType.PERMANENT,
                        "WhatsApp connection not found"
                ));

        if (connection.getStatus() != WhatsAppConnectionStatus.CONNECTED) {
            throw new MessageSendException(
                    MessageSendException.FailureType.RETRYABLE,
                    "WhatsApp is not connected",
                    30L
            );
        }

        if (externalChatId == null
        || !(externalChatId.endsWith("@s.whatsapp.net")
             || externalChatId.endsWith("@lid"))) {
            throw new MessageSendException(
                    MessageSendException.FailureType.PERMANENT,
                    "Unsupported WhatsApp chat ID: " + externalChatId
            );
        }

        String number = externalChatId;

        try {
            String response = evolutionApiClient.sendText(
                    connection.getInstanceName(),
                    number,
                    text
            );

            if (response == null || response.isBlank()) {
                throw new MessageSendException(
                        MessageSendException.FailureType.UNKNOWN,
                        "Evolution API returned an empty response"
                );
            }

            JsonNode result = objectMapper.readTree(response);
            String messageId = result.path("key").path("id").asString("");

            if (messageId.isBlank()) {
                throw new MessageSendException(
                        MessageSendException.FailureType.UNKNOWN,
                        "Evolution API did not return a message ID"
                );
            }

            return messageId;

        } catch (MessageSendException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw new MessageSendException(
                    MessageSendException.FailureType.UNKNOWN,
                    "Evolution API send request failed",
                    exception
            );
        } catch (Exception exception) {
            throw new MessageSendException(
                    MessageSendException.FailureType.UNKNOWN,
                    "Could not process Evolution API response",
                    exception
            );
        }
    }
}
