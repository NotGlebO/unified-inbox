package com.unifiedinbox.unified_inbox.integration.gmail;

import com.unifiedinbox.unified_inbox.entity.IntegrationEntity;
import com.unifiedinbox.unified_inbox.entity.OrganizationEntity;
import com.unifiedinbox.unified_inbox.repository.IntegrationRepository;
import com.unifiedinbox.unified_inbox.service.EncryptionService;
import com.unifiedinbox.unified_inbox.service.OrganizationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

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
            @org.springframework.beans.factory.annotation.Value("${app.gmail-redirect-uri}") String redirectUri
    ) {
        this.gmailApiClient = gmailApiClient;
        this.connectionRepository = connectionRepository;
        this.integrationRepository = integrationRepository;
        this.organizationService = organizationService;
        this.encryptionService = encryptionService;
        this.redirectUri = redirectUri;
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


        
        String requiredScope =
                "https://www.googleapis.com/auth/gmail.modify";

        Set<String> grantedScopes = Arrays.stream(
                tokenResponse.scope().trim().split("\\s+")
        ).filter(scope -> !scope.isBlank())
        .collect(Collectors.toSet());

        if (!grantedScopes.contains(requiredScope)) {
            // The user did not grant the permissions required by Unified Inbox.
            // Revoke the temporary token before rejecting the connection.
            if (!tokenResponse.accessToken().isBlank()) {
                try {
                    gmailApiClient.revokeToken(
                            tokenResponse.accessToken()
                    );
                } catch (Exception exception) {
                    // Revocation failure must not allow the connection.
                    org.slf4j.LoggerFactory
                            .getLogger(GmailIntegrationService.class)
                            .warn("Failed to revoke rejected Gmail OAuth token",
                                    exception);
                }
            }

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Required Gmail permissions were not granted. "
                            + "Please allow Gmail access and try again."
            );
        }

        if (tokenResponse.refreshToken() == null
                || tokenResponse.refreshToken().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Google did not provide a refresh token. "
                            + "Please reconnect your Gmail account."
            );
        }

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

        String initialHistoryId = gmailApiClient.getCurrentHistoryId(
                tokenResponse.accessToken()
        );

        connection.setHistoryId(initialHistoryId);

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
