package com.unifiedinbox.unified_inbox.controller;

import com.unifiedinbox.unified_inbox.entity.OrganizationEntity;
import com.unifiedinbox.unified_inbox.entity.OrganizationMemberEntity;
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

        OrganizationEntity organization =
                organizationService.getOrganization(id);

        List<OrganizationMemberEntity> members =
                organizationService.getMembers(id);

        model.addAttribute("organization", organization);
        model.addAttribute("members", members);

        return "settings";
    }
}