package com.unifiedinbox.unified_inbox.integration.gmail;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;

@Service
public class GmailWatchService {

    private final GmailApiClient apiClient;
    private final GmailIntegrationService integrationService;
    private final GmailConnectionRepository connectionRepository;
    private final String topicName;

    public GmailWatchService(
            GmailApiClient apiClient,
            GmailIntegrationService integrationService,
            GmailConnectionRepository connectionRepository,
            @Value("${app.gmail.pubsub-topic}") String topicName
    ) {
        this.apiClient = apiClient;
        this.integrationService = integrationService;
        this.connectionRepository = connectionRepository;
        this.topicName = topicName;
    }

    @Transactional
    public void registerWatch(Long integrationId) {

        GmailConnectionEntity connection = connectionRepository
                .findByIntegrationId(integrationId)
                .orElseThrow(() -> new IllegalStateException(
                        "Gmail connection not found."
                ));

        if (!connection.getIntegration().isEnabled()) {
            throw new IllegalStateException(
                    "Gmail integration is disabled."
            );
        }

        String refreshToken = integrationService
                .getRefreshToken(integrationId);

        String accessToken = apiClient
                .refreshAccessToken(refreshToken)
                .accessToken();

        JsonNode response = apiClient.watchMailbox(
                accessToken,
                topicName
        );

        String historyId = response.path("historyId").asString("");

        if (historyId.isBlank()) {
            throw new IllegalStateException(
                    "Gmail watch returned no historyId."
            );
        }

        // Only initialize the cursor if this is a new mailbox.
        // Never overwrite an existing sync cursor here.
        if (connection.getHistoryId() == null
                || connection.getHistoryId().isBlank()) {
            connection.setHistoryId(historyId);
        }
    }
}