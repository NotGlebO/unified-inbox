
package com.unifiedinbox.unified_inbox.controller;

import com.unifiedinbox.unified_inbox.core.dto.OrganizationDetails;
import com.unifiedinbox.unified_inbox.core.dto.OrganizationMemberDetails;
import com.unifiedinbox.unified_inbox.entity.OrganizationRole;
import com.unifiedinbox.unified_inbox.service.OrganizationService;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.server.ResponseStatusException;


import java.util.List;

@Controller
public class OrganizationSettingsController {

    private final OrganizationService organizationService;

    public OrganizationSettingsController(
            OrganizationService organizationService
    ) {
        this.organizationService = organizationService;
    }

    @GetMapping("/organizations/{id}/settings")
    public String settingsPage(
            @PathVariable Long id,
            Model model
    ) {

        OrganizationDetails organization =
                organizationService.getOrganization(id);

        List<OrganizationMemberDetails> members =
                organizationService.getMembers(id);

        boolean isOwner = organizationService
                .requireMember(id)
                .getRole() == OrganizationRole.OWNER;

        model.addAttribute("organization", organization);
        model.addAttribute("members", members);
        model.addAttribute("isOwner", isOwner);

        return "settings";
    }

    @PostMapping("/organizations/{id}/members/{memberId}/remove")
    public String removeMember(
            @PathVariable Long id,
            @PathVariable Long memberId,
            RedirectAttributes redirectAttributes
    ) {

        try {
            organizationService.removeMember(id, memberId);

            redirectAttributes.addFlashAttribute(
                    "success",
                    "Member removed successfully."
            );

        } catch (ResponseStatusException exception) {

            // Only show an error on the settings page
            // when the current user can access the organization
            organizationService.requireMember(id);

            redirectAttributes.addFlashAttribute(
                    "error",
                    "Unable to remove this member."
            );
        }

        return "redirect:/organizations/" + id + "/settings";
    }

    
    @PostMapping("/organizations/{id}/members/add")
    public String addMember(
            @PathVariable Long id,
            @RequestParam String username,
            RedirectAttributes redirectAttributes
    ) {

    try {
            organizationService.addMember(id, username);

            redirectAttributes.addFlashAttribute(
                    "success",
                    "Member added successfully."
            );

    } catch (IllegalArgumentException exception) {

            redirectAttributes.addFlashAttribute(
                    "error",
                    exception.getMessage()
            );
    }

    return "redirect:/organizations/" + id + "/settings";
    }

}
