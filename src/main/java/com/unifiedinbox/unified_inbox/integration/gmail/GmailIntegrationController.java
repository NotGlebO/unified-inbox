package com.unifiedinbox.unified_inbox.integration.gmail;

import com.unifiedinbox.unified_inbox.service.OrganizationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/integrations/gmail")
public class GmailIntegrationController {

    private final GmailIntegrationService gmailService;
    private final OrganizationService organizationService;
    private final String googleClientId;
    private final String baseUrl;

    public GmailIntegrationController(
            GmailIntegrationService gmailService,
            OrganizationService organizationService,
            @Value("${spring.security.oauth2.client.registration.google.client-id}") String googleClientId,
            @Value("${app.public-url:http://localhost:8080}") String baseUrl
    ) {
        this.gmailService = gmailService;
        this.organizationService = organizationService;
        this.googleClientId = googleClientId;
        this.baseUrl = baseUrl;
    }

    @GetMapping("/authorize")
    public ResponseEntity<Void> authorize(
            @RequestParam Long organizationId
    ) {
        organizationService.requireOwner(organizationId);

        if (googleClientId == null || googleClientId.isBlank()) {
            throw new ResponseStatusException(
                    org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR,
                    "Google OAuth2 client ID is not configured."
            );
        }

        String redirectUri = baseUrl.replaceAll("/+$", "")
                + "/api/integrations/gmail/callback";

        String scope = "openid email https://www.googleapis.com/auth/gmail.modify https://www.googleapis.com/auth/gmail.send";

        String authUrl = "https://accounts.google.com/o/oauth2/v2/auth"
                + "?client_id=" + URLEncoder.encode(googleClientId, StandardCharsets.UTF_8)
                + "&redirect_uri=" + URLEncoder.encode(redirectUri, StandardCharsets.UTF_8)
                + "&response_type=code"
                + "&scope=" + URLEncoder.encode(scope, StandardCharsets.UTF_8)
                + "&access_type=offline"
                + "&prompt=consent"
                + "&state=" + URLEncoder.encode(organizationId.toString(), StandardCharsets.UTF_8);

        return ResponseEntity
                .status(org.springframework.http.HttpStatus.FOUND)
                .location(java.net.URI.create(authUrl))
                .build();
    }

    @GetMapping("/callback")
    public ResponseEntity<Void> handleCallback(
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String error
    ) {
        if (error != null) {
            throw new ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST,
                    "Gmail authorization failed: " + error
            );
        }

        if (state == null || state.isBlank()) {
            throw new ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST,
                    "Organization ID (state) is missing."
            );
        }

        Long organizationId;
        try {
            organizationId = Long.parseLong(state);
        } catch (NumberFormatException exception) {
            throw new ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST,
                    "Invalid organization ID."
            );
        }

        if (code == null || code.isBlank()) {
            throw new ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST,
                    "Authorization code is missing."
            );
        }

        gmailService.connect(organizationId, code);

        return ResponseEntity
                .status(org.springframework.http.HttpStatus.FOUND)
                .location(java.net.URI.create("/organizations/" + organizationId + "/settings?registered=true"))
                .build();
    }
}
