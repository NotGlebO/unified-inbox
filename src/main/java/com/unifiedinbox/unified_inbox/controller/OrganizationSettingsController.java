
package com.unifiedinbox.unified_inbox.controller;

import com.unifiedinbox.unified_inbox.entity.OrganizationMemberEntity;
import com.unifiedinbox.unified_inbox.entity.OrganizationRole;
import com.unifiedinbox.unified_inbox.service.OrganizationMemberService;
import com.unifiedinbox.unified_inbox.service.OrganizationService;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.unifiedinbox.unified_inbox.service.IntegrationService;

@Controller
public class OrganizationSettingsController {

    private final OrganizationService organizationService;
    private final OrganizationMemberService memberService;
    private final IntegrationService integrationService;

    public OrganizationSettingsController(
            OrganizationService organizationService,
            OrganizationMemberService memberService,
            IntegrationService integrationService
    ) {
        this.organizationService = organizationService;
        this.memberService = memberService;
        this.integrationService = integrationService;
    }

    @GetMapping("/organizations/{organizationId}/settings")
    public String settingsPage(
            @PathVariable Long organizationId,
            Model model
    ) {
        OrganizationMemberEntity membership =
                organizationService.requireMember(organizationId);

        model.addAttribute(
                "organization",
                membership.getOrganization()
        );

        model.addAttribute(
                "members",
                memberService.getMembers(organizationId)
        );

        model.addAttribute(
                "isOwner",
                membership.getRole() == OrganizationRole.OWNER
        );
        model.addAttribute(
                "integrations",
                integrationService.getIntegrations(organizationId)
        );

        return "settings";
    }

    @PostMapping("/organizations/{organizationId}/members")
    public String addMember(
            @PathVariable Long organizationId,
            @RequestParam String username,
            RedirectAttributes redirectAttributes
    ) {
        // Check access before processing the request.
        organizationService.requireOwner(organizationId);

        try {
            memberService.addMember(organizationId, username);

            redirectAttributes.addFlashAttribute(
                    "success",
                    "Member added successfully."
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
