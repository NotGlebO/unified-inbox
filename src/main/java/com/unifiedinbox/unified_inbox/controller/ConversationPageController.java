package com.unifiedinbox.unified_inbox.controller;

import com.unifiedinbox.unified_inbox.service.OrganizationService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class ConversationPageController {

    private final OrganizationService organizationService;

    public ConversationPageController(OrganizationService organizationService) {
        this.organizationService = organizationService;
    }

    @GetMapping("/organizations/{organizationId}/conversations")
    public String conversations(@PathVariable Long organizationId) {
        organizationService.requireMember(organizationId);
        return "conversations";
    }
}