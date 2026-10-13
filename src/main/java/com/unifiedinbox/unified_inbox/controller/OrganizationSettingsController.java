package com.unifiedinbox.unified_inbox.controller;

import com.unifiedinbox.unified_inbox.core.dto.OrganizationDetails;
import com.unifiedinbox.unified_inbox.core.dto.OrganizationMemberDetails;
import com.unifiedinbox.unified_inbox.service.OrganizationService;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

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

        model.addAttribute("organization", organization);
        model.addAttribute("members", members);

        return "settings";
    }
}