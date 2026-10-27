package com.unifiedinbox.unified_inbox.integration.gmail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class GmailPollingService {

    private static final Logger log =
            LoggerFactory.getLogger(GmailPollingService.class);

    private final GmailConnectionRepository connectionRepository;
    private final GmailIntegrationService integrationService;
    private final GmailApiClient apiClient;
    private final GmailSyncService syncService;

    public GmailPollingService(
            GmailConnectionRepository connectionRepository,
            GmailIntegrationService integrationService,
            GmailApiClient apiClient,
            GmailSyncService syncService
    ) {
        this.connectionRepository = connectionRepository;
        this.integrationService = integrationService;
        this.apiClient = apiClient;
        this.syncService = syncService;
    }

    @Scheduled(fixedDelay = 30000, initialDelay = 10000)
    public void poll() {

        for (GmailConnectionEntity connection
            : connectionRepository.findAllWithIntegration()) {

            Long integrationId = connection
                    .getIntegration()
                    .getId();

            try {
                if (!connection.getIntegration().isEnabled()) {
                    continue;
                }

                if (connection.getHistoryId() == null
                        || connection.getHistoryId().isBlank()) {

                    String refreshToken = integrationService
                            .getRefreshToken(integrationId);

                    String accessToken = apiClient
                            .refreshAccessToken(refreshToken)
                            .accessToken();

                    String historyId = apiClient
                            .getCurrentHistoryId(accessToken);

                    // Save the initial cursor for accounts
                    // connected before polling was implemented.
                    connectionRepository.initializeHistoryId(
                            connection.getId(),
                            historyId
                    );

                    log.info(
                            "Initialized Gmail history for integration {}",
                            integrationId
                    );

                    continue;
                }

                syncService.sync(integrationId);

            } catch (Exception exception) {
                log.error(
                        "Gmail polling failed for integration {}",
                        integrationId,
                        exception
                );
            }
        }
    }
}