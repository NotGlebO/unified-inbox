package com.unifiedinbox.unified_inbox.integration.gmail;

import com.unifiedinbox.unified_inbox.entity.*;
import com.unifiedinbox.unified_inbox.repository.*;
import com.unifiedinbox.unified_inbox.service.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.*;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;

import java.io.IOException;

@Component
public class GmailOAuthSuccessHandler
        implements AuthenticationSuccessHandler {

    private final OAuth2AuthorizedClientRepository clientRepository;
    private final OrganizationService organizationService;
    private final IntegrationRepository integrationRepository;
    private final GmailAccountRepository gmailAccountRepository;
    private final TokenEncryptionService encryptionService;
    private final TransactionTemplate transactionTemplate;

    public GmailOAuthSuccessHandler(
            OAuth2AuthorizedClientRepository clientRepository,
            OrganizationService organizationService,
            IntegrationRepository integrationRepository,
            GmailAccountRepository gmailAccountRepository,
            TokenEncryptionService encryptionService,
            org.springframework.transaction.PlatformTransactionManager transactionManager
    ) {
        this.clientRepository = clientRepository;
        this.organizationService = organizationService;
        this.integrationRepository = integrationRepository;
        this.gmailAccountRepository = gmailAccountRepository;
        this.encryptionService = encryptionService;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException {

        var session = request.getSession(false);

        if (session == null) {
            response.sendError(403);
            return;
        }

        Long organizationId =
                (Long) session.getAttribute("gmailOrganizationId");

        Authentication originalAuthentication =
                (Authentication) session.getAttribute("gmailOriginalAuthentication");

        session.removeAttribute("gmailOrganizationId");
        session.removeAttribute("gmailOriginalAuthentication");

        if (organizationId == null || originalAuthentication == null) {
            response.sendError(403, "Gmail connection session expired");
            return;
        }

        // Restore the Unified Inbox user instead of keeping Google login.
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(originalAuthentication);
        SecurityContextHolder.setContext(context);

        new HttpSessionSecurityContextRepository()
                .saveContext(context, request, response);

        // Verify ownership again after returning from Google.
        OrganizationEntity organization =
                organizationService.requireOwner(organizationId)
                        .getOrganization();

        OAuth2User googleUser = (OAuth2User) authentication.getPrincipal();

        String googleId = googleUser.getAttribute("sub");
        String email = googleUser.getAttribute("email");

        if (!(authentication instanceof OAuth2AuthenticationToken oauthToken)) {
            response.sendError(400, "Invalid Google authentication");
            return;
        }

        OAuth2AuthorizedClient client =
                clientRepository.loadAuthorizedClient(
                        oauthToken.getAuthorizedClientRegistrationId(),
                        oauthToken,
                        request
                );

        if (client == null || googleId == null || email == null) {
            response.sendError(400, "Google account information unavailable");
            return;
        }

        var refreshToken = client.getRefreshToken();

        if (refreshToken == null) {
            response.sendError(400, "Google did not provide a refresh token");
            return;
        }

        String encryptedRefreshToken =
                encryptionService.encrypt(refreshToken.getTokenValue());

        transactionTemplate.executeWithoutResult(status -> {

            if (gmailAccountRepository.findByGoogleAccountId(googleId).isPresent()) {
                throw new IllegalStateException(
                        "This Gmail account is already connected"
                );
            }

            IntegrationEntity integration = new IntegrationEntity(
                    organization,
                    IntegrationType.GMAIL,
                    email,
                    null,
                    null
            );

            integrationRepository.save(integration);

            GmailAccountEntity account = new GmailAccountEntity(
                    integration,
                    googleId,
                    email,
                    encryptedRefreshToken
            );

            gmailAccountRepository.save(account);
        });

        // Do not retain a second copy of the OAuth tokens in the
        // default Spring authorized-client storage.
        clientRepository.removeAuthorizedClient(
                oauthToken.getAuthorizedClientRegistrationId(),
                oauthToken,
                request,
                response
        );

        response.sendRedirect(
                request.getContextPath()
                        + "/organizations/" + organizationId + "/settings"
        );
    }
}