package com.unifiedinbox.unified_inbox.integration.gmail;

import com.unifiedinbox.unified_inbox.entity.IntegrationEntity;
import com.unifiedinbox.unified_inbox.entity.OrganizationEntity;
import com.unifiedinbox.unified_inbox.repository.IntegrationRepository;
import com.unifiedinbox.unified_inbox.service.EncryptionService;
import com.unifiedinbox.unified_inbox.service.OrganizationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GmailIntegrationService {

    private final GmailApiClient gmailApiClient;
    private final GmailConnectionRepository connectionRepository;
    private final IntegrationRepository integrationRepository;
    private final OrganizationService organizationService;
    private final EncryptionService encryptionService;
    private final String redirectUri;

    public GmailIntegrationService(
            GmailApiClient gmailApiClient,
            GmailConnectionRepository connectionRepository,
            IntegrationRepository integrationRepository,
            OrganizationService organizationService,
            EncryptionService encryptionService,
            @org.springframework.beans.factory.annotation.Value("${app.base-url:http://localhost:8080}") String baseUrl
    ) {
        this.gmailApiClient = gmailApiClient;
        this.connectionRepository = connectionRepository;
        this.integrationRepository = integrationRepository;
        this.organizationService = organizationService;
        this.encryptionService = encryptionService;
        this.redirectUri = baseUrl.replaceAll("/+$", "") + "/api/integrations/gmail/callback";
    }

    @Transactional
    public Long connect(Long organizationId, String authorizationCode) {
        OrganizationEntity organization = organizationService
                .requireOwner(organizationId)
                .getOrganization();

        if (authorizationCode == null || authorizationCode.isBlank()) {
            throw new IllegalArgumentException("Gmail authorization code is required.");
        }

        GmailApiClient.TokenResponse tokenResponse = gmailApiClient
                .exchangeCodeForTokens(authorizationCode, redirectUri);

        String email = gmailApiClient.getUserInfo(tokenResponse.accessToken());

        if (email == null || email.isBlank()) {
            throw new IllegalStateException("Could not determine Gmail account email.");
        }

        if (connectionRepository.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("This Gmail account is already connected.");
        }

        String encryptedRefreshToken = encryptionService.encrypt(tokenResponse.refreshToken());

        IntegrationEntity integration = new IntegrationEntity(
                organization,
                "GMAIL",
                email,
                "/icons/gmail.svg"
        );

        integration = integrationRepository.save(integration);

        GmailConnectionEntity connection = new GmailConnectionEntity(
                integration,
                email,
                email,
                encryptedRefreshToken
        );

        connectionRepository.save(connection);

        return integration.getId();
    }

    public String getRefreshToken(Long integrationId) {
        GmailConnectionEntity connection = connectionRepository
                .findByIntegrationId(integrationId)
                .orElseThrow(() -> new IllegalStateException("Gmail connection not found."));

        if (!connection.getIntegration().isEnabled()) {
            throw new IllegalStateException("Gmail integration is disabled.");
        }

        try {
            return encryptionService.decrypt(connection.getEncryptedRefreshToken());
        } catch (Exception exception) {
            throw new IllegalStateException("Could not decrypt Gmail credentials.", exception);
        }
    }
}
