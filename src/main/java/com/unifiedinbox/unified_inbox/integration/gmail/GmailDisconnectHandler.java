
package com.unifiedinbox.unified_inbox.integration.gmail;

import com.unifiedinbox.unified_inbox.entity.IntegrationEntity;
import com.unifiedinbox.unified_inbox.integration.IntegrationDisconnectHandler;
import com.unifiedinbox.unified_inbox.service.EncryptionService;
import org.springframework.stereotype.Component;

@Component
public class GmailDisconnectHandler
        implements IntegrationDisconnectHandler {

    private final GmailConnectionRepository connectionRepository;
    private final GmailApiClient gmailApiClient;
    private final EncryptionService encryptionService;

    public GmailDisconnectHandler(
            GmailConnectionRepository connectionRepository,
            GmailApiClient gmailApiClient,
            EncryptionService encryptionService
    ) {
        this.connectionRepository = connectionRepository;
        this.gmailApiClient = gmailApiClient;
        this.encryptionService = encryptionService;
    }

    @Override
    public String getType() {
        return "GMAIL";
    }

    @Override
    public void disconnect(IntegrationEntity integration) {

        GmailConnectionEntity connection = connectionRepository
                .findByIntegrationId(integration.getId())
                .orElse(null);

        if (connection == null) {
            return;
        }

        String refreshToken = encryptionService.decrypt(
                connection.getEncryptedRefreshToken()
        );

        gmailApiClient.revokeToken(refreshToken);

        connectionRepository.delete(connection);
        connectionRepository.flush();
    }
}
