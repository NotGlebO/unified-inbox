
package com.unifiedinbox.unified_inbox.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.unifiedinbox.unified_inbox.repository.IntegrationRepository;
import com.unifiedinbox.unified_inbox.entity.IntegrationEntity;
import com.unifiedinbox.unified_inbox.entity.IntegrationType;
import com.unifiedinbox.unified_inbox.entity.OrganizationEntity;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;

@Service
public class IntegrationService {

    private final IntegrationRepository integrationRepository;
    private final OrganizationService organizationService;
    private final TokenEncryptionService encryptionService;
    private final RestClient restClient;

    public IntegrationService(
            IntegrationRepository integrationRepository,
            OrganizationService organizationService,
            TokenEncryptionService encryptionService
    ) {
        this.integrationRepository = integrationRepository;
        this.organizationService = organizationService;
        this.encryptionService = encryptionService;
        this.restClient = RestClient.create();
    }

    public IntegrationEntity connectTelegram(
            Long organizationId,
            String token
    ) {
        OrganizationEntity organization = organizationService
                .requireOwner(organizationId)
                .getOrganization();

        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException(
                    "Telegram bot token cannot be empty."
            );
        }

        String normalizedToken = token.trim();

        // Check token with Telegram API
        TelegramMeResponse response;

        try {
            response = restClient.get()
                    .uri("https://api.telegram.org/bot{token}/getMe",
                            normalizedToken)
                    .retrieve()
                    .body(TelegramMeResponse.class);

        } catch (RestClientException e) {
            throw new IllegalArgumentException(
                    "Could not verify Telegram bot token."
            );
        }

        if (response == null
                || !response.ok()
                || response.result() == null
                || response.result().username() == null) {

            throw new IllegalArgumentException(
                    "Invalid Telegram bot token."
            );
        }

        String displayName = response.result().username();
        Long botId = response.result().id();

        if (botId == null) {
            throw new IllegalArgumentException(
                    "Telegram did not return a bot ID."
            );
        }
        
        if (integrationRepository
                .findByTypeAndExternalBotIdAndEnabledTrue(
                        IntegrationType.TELEGRAM,
                        botId
                )
                .isPresent()) {

            throw new IllegalArgumentException(
                    "This Telegram bot is already connected to an organization."
            );
        }


        String encryptedToken =
                encryptionService.encrypt(normalizedToken);

        IntegrationEntity integration = new IntegrationEntity(
                organization,
                IntegrationType.TELEGRAM,
                displayName,
                botId,
                encryptedToken
        );

        return integrationRepository.save(integration);
    }

    
    @Transactional(readOnly = true)
    public List<IntegrationEntity> getIntegrations(Long organizationId) {

        organizationService.requireMember(organizationId);

        return integrationRepository
                .findByOrganizationIdAndEnabledTrue(organizationId);
    }


    
    @Transactional
    public void disconnectIntegration(
            Long organizationId,
            Long integrationId
    ) {
        // Only organization owner can disconnect bots
        organizationService.requireOwner(organizationId);

        IntegrationEntity integration = integrationRepository
                .findById(integrationId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Integration not found."
                ));

        // Ensure integration belongs to this organization
        if (!integration.getOrganization().getId().equals(organizationId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Integration not found."
            );
        }

        if (!integration.isEnabled()) {
            throw new IllegalArgumentException(
                    "Integration is already disconnected."
            );
        }

        integration.setEnabled(false);
    }


    @JsonIgnoreProperties(ignoreUnknown = true)
    private record TelegramMeResponse(
            boolean ok,
            TelegramBot result
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record TelegramBot(
            Long id,
            String username,
            String first_name
    ) {}
}
