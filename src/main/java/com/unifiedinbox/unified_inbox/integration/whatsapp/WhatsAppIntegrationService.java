
package com.unifiedinbox.unified_inbox.integration.whatsapp;

import com.unifiedinbox.unified_inbox.entity.IntegrationEntity;
import com.unifiedinbox.unified_inbox.entity.OrganizationEntity;
import com.unifiedinbox.unified_inbox.repository.IntegrationRepository;
import com.unifiedinbox.unified_inbox.service.OrganizationService;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.beans.factory.annotation.Value;

import java.util.UUID;

@Service
public class WhatsAppIntegrationService {

    private final String webhookSecret;
    private final OrganizationService organizationService;
    private final IntegrationRepository integrationRepository;
    private final WhatsAppConnectionRepository connectionRepository;
    private final EvolutionApiClient evolutionApiClient;
    private final TransactionTemplate transactionTemplate;

    public WhatsAppIntegrationService(
            OrganizationService organizationService,
            IntegrationRepository integrationRepository,
            WhatsAppConnectionRepository connectionRepository,
            EvolutionApiClient evolutionApiClient,
            TransactionTemplate transactionTemplate,
            @Value("${evolution.webhook.secret}") String webhookSecret
    ) {
        this.organizationService = organizationService;
        this.integrationRepository = integrationRepository;
        this.connectionRepository = connectionRepository;
        this.evolutionApiClient = evolutionApiClient;
        this.transactionTemplate = transactionTemplate;
        this.webhookSecret = webhookSecret;
    }

    public Long connect(Long organizationId) {

        // Only organization owners may connect WhatsApp.
        OrganizationEntity organization = organizationService
                .requireOwner(organizationId)
                .getOrganization();

        String instanceName = "wa-" + UUID.randomUUID();

        Long integrationId = transactionTemplate.execute(status -> {

            IntegrationEntity integration = new IntegrationEntity(
                    organization,
                    "WHATSAPP",
                    "WhatsApp",
                    "/icons/whatsapp.svg"
            );

            integration = integrationRepository.save(integration);

            WhatsAppConnectionEntity connection =
                    new WhatsAppConnectionEntity(
                            integration,
                            instanceName
                    );

            connectionRepository.save(connection);

            return integration.getId();
        });

        try {
            evolutionApiClient.createInstance(instanceName);
            evolutionApiClient.configureWebhook(
                    instanceName,
                    webhookSecret
            );

            updateStatus(
                    integrationId,
                    WhatsAppConnectionStatus.WAITING_FOR_QR
            );

        } catch (Exception exception) {

            updateStatus(
                    integrationId,
                    WhatsAppConnectionStatus.FAILED
            );

            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Failed to create WhatsApp instance.",
                    exception
            );
        }

        return integrationId;
    }

   
    private void updateStatus(
            Long integrationId,
            WhatsAppConnectionStatus newStatus
    ) {
        transactionTemplate.executeWithoutResult(status -> {

            WhatsAppConnectionEntity connection =
                    connectionRepository.findByIntegrationId(integrationId)
                            .orElseThrow();

            connection.setStatus(newStatus);

            IntegrationEntity integration = connection.getIntegration();

            if (newStatus == WhatsAppConnectionStatus.FAILED) {
                integration.setEnabled(false);
            }
        });
    }

}
