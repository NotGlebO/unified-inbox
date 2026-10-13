
package com.unifiedinbox.unified_inbox.controller;

import com.unifiedinbox.unified_inbox.core.dto.OrganizationDetails;
import com.unifiedinbox.unified_inbox.service.OrganizationService;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class ConversationPageController {

    private final OrganizationService organizationService;

    public ConversationPageController(
            OrganizationService organizationService
    ) {
        this.organizationService = organizationService;
    }

    @GetMapping("/organizations/{id}/conversations")
    public String conversationsPage(
            @PathVariable Long id,
            Model model
    ) {
        OrganizationDetails organization =
                organizationService.getOrganization(id);

        model.addAttribute("organization", organization);

        return "conversations";
    }
}
