package com.unifiedinbox.unified_inbox.integration.gmail;

import com.unifiedinbox.unified_inbox.service.OrganizationService;

import jakarta.servlet.http.HttpSession;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class GmailOAuthController {

    private final OrganizationService organizationService;

    public GmailOAuthController(
            OrganizationService organizationService
    ) {
        this.organizationService = organizationService;
    }

    @GetMapping("/organizations/{organizationId}/gmail/connect")
    public String connectGmail(
            @PathVariable Long organizationId,
            HttpSession session
    ) {
        // Check that the user owns the organization
        organizationService.requireOwner(organizationId);

        // Remember the current Unified Inbox user
        Authentication currentAuthentication =
                SecurityContextHolder.getContext().getAuthentication();

        session.setAttribute(
                "gmailOriginalAuthentication",
                currentAuthentication
        );

        // Remember which organization is connecting Gmail
        session.setAttribute(
                "gmailOrganizationId",
                organizationId
        );

        // Redirect to Google OAuth2
        return "redirect:/oauth2/authorization/google";
    }
}