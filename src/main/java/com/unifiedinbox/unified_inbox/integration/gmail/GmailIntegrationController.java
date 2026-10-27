
package com.unifiedinbox.unified_inbox.integration.gmail;

import com.unifiedinbox.unified_inbox.service.OrganizationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import org.springframework.stereotype.Controller;

@Controller
@RequestMapping("/api/integrations/gmail")
public class GmailIntegrationController {

    private static final String OAUTH_STATE =
            "GMAIL_OAUTH_STATE";

    private static final String OAUTH_ORGANIZATION =
            "GMAIL_OAUTH_ORGANIZATION";

    private static final SecureRandom SECURE_RANDOM =
            new SecureRandom();

    private final GmailIntegrationService gmailService;
    private final OrganizationService organizationService;
    private final String googleClientId;
    private final String redirectUri;

    public GmailIntegrationController(
            GmailIntegrationService gmailService,
            OrganizationService organizationService,
            @Value("${spring.security.oauth2.client.registration.google.client-id}")
            String googleClientId,
            @Value("${app.gmail-redirect-uri}")
            String redirectUri
    ) {
        this.gmailService = gmailService;
        this.organizationService = organizationService;
        this.googleClientId = googleClientId;
        this.redirectUri = redirectUri;
    }

    @GetMapping("/authorize")
    @ResponseBody
    public ResponseEntity<Void> authorize(
            @RequestParam Long organizationId,
            HttpServletRequest request
    ) {
        organizationService.requireOwner(organizationId);

        if (googleClientId == null || googleClientId.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Google OAuth client ID is not configured."
            );
        }

        byte[] randomBytes = new byte[32];
        SECURE_RANDOM.nextBytes(randomBytes);

        String state = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(randomBytes);

        HttpSession session = request.getSession(true);

        session.setAttribute(OAUTH_STATE, state);
        session.setAttribute(
                OAUTH_ORGANIZATION,
                organizationId
        );

        

        String scope = "openid email https://www.googleapis.com/auth/gmail.modify";

        String authUrl = "https://accounts.google.com/o/oauth2/v2/auth"
                + "?client_id=" + encode(googleClientId)
                + "&redirect_uri=" + encode(redirectUri)
                + "&response_type=code"
                + "&scope=" + encode(scope)
                + "&access_type=offline"
                + "&prompt=consent"
                + "&state=" + encode(state);

        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(authUrl))
                .build();
    }

    @GetMapping("/callback")
    public String handleCallback(
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String error,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes
    ) {
        HttpSession session = request.getSession(false);

        if (session == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "OAuth session not found."
            );
        }

        String expectedState =
                (String) session.getAttribute(OAUTH_STATE);

        Long organizationId =
                (Long) session.getAttribute(OAUTH_ORGANIZATION);

        if (expectedState == null || organizationId == null
                || state == null
                || !expectedState.equals(state)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid OAuth state."
            );
        }

        // State is single-use, even when authorization fails.
        session.removeAttribute(OAUTH_STATE);
        session.removeAttribute(OAUTH_ORGANIZATION);

        
        if (error != null) {

            if ("access_denied".equals(error)) {
                redirectAttributes.addFlashAttribute(
                        "error",
                        "Gmail connection was cancelled. "
                                + "No account was connected."
                );
            } else {
                redirectAttributes.addFlashAttribute(
                        "error",
                        "Gmail authorization failed. Please try again."
                );
            }

            return "redirect:/organizations/"
                    + organizationId
                    + "/settings";
        }


        if (code == null || code.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Authorization code is missing."
            );
        }

        // Recheck authorization before connecting.
        organizationService.requireOwner(organizationId);

        
        try {
        gmailService.connect(organizationId, code);

        redirectAttributes.addFlashAttribute(
                "success",
                "Gmail account connected successfully."
        );

        } catch (ResponseStatusException exception) {

        if (exception.getStatusCode().value() != 403
                || !exception.getReason().equals(
                        "Required Gmail permissions were not granted. "
                        + "Please allow Gmail access and try again."
                )) {
                throw exception;
        }

        redirectAttributes.addFlashAttribute(
                "error",
                "Gmail connection failed. "
                        + "You must grant Gmail permissions "
                        + "to use this integration."
        );
        }

        redirectAttributes.addFlashAttribute(
            "success",
            "Gmail account connected successfully."
        );

        return "redirect:/organizations/"
                + organizationId
                + "/settings";

    }

    private static String encode(String value) {
        return URLEncoder.encode(
                value,
                StandardCharsets.UTF_8
        );
    }
}
