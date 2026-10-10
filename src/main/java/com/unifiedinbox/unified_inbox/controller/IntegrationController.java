
package com.unifiedinbox.unified_inbox.controller;

import com.unifiedinbox.unified_inbox.service.IntegrationService;
import com.unifiedinbox.unified_inbox.service.OrganizationService;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.dao.DataIntegrityViolationException;

@Controller
public class IntegrationController {

    private final IntegrationService integrationService;
    private final OrganizationService organizationService;

    public IntegrationController(
            IntegrationService integrationService,
            OrganizationService organizationService
    ) {
        this.integrationService = integrationService;
        this.organizationService = organizationService;
    }

    @PostMapping("/organizations/{organizationId}/integrations/telegram")
    public String connectTelegram(
            @PathVariable Long organizationId,
            @RequestParam String token,
            RedirectAttributes redirectAttributes
    ) {
        // Only OWNER can connect integrations.
        organizationService.requireOwner(organizationId);

        try {
            integrationService.connectTelegram(
                    organizationId,
                    token
            );

            redirectAttributes.addFlashAttribute(
                    "success",
                    "Telegram bot connected successfully."
            );

        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute(
                    "error",
                    e.getMessage()
            );
        }   catch (DataIntegrityViolationException e) {
                redirectAttributes.addFlashAttribute(
                        "error",
                        "This Telegram bot is already connected to an organization."
                );
    }


        return "redirect:/organizations/"
                + organizationId + "/settings";
    }

    
    @PostMapping("/organizations/{organizationId}/integrations/{integrationId}/disconnect")
    public String disconnectTelegram(
            @PathVariable Long organizationId,
            @PathVariable Long integrationId,
            RedirectAttributes redirectAttributes
    ) {
    organizationService.requireOwner(organizationId);

    try {
            integrationService.disconnectIntegration(
                    organizationId,
                    integrationId
            );

            redirectAttributes.addFlashAttribute(
                    "success",
                    "Telegram bot disconnected successfully."
            );

    } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute(
                    "error",
                    e.getMessage()
            );
    }

    return "redirect:/organizations/"
            + organizationId + "/settings";
    }

}
