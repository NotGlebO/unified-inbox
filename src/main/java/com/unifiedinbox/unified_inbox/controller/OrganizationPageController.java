
package com.unifiedinbox.unified_inbox.controller;

import com.unifiedinbox.unified_inbox.service.OrganizationService;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class OrganizationPageController {

    private final OrganizationService organizationService;

    public OrganizationPageController(
            OrganizationService organizationService
    ) {
        this.organizationService = organizationService;
    }

    @GetMapping("/organizations")
    public String organizationsPage(Model model) {

        model.addAttribute(
                "organizations",
                organizationService.getMyOrganizations()
        );

        return "organizations";
    }

    @PostMapping("/organizations")
    public String createOrganization(
            @RequestParam String name,
            RedirectAttributes redirectAttributes
    ) {
        try {
            organizationService.createOrganization(name);

            redirectAttributes.addFlashAttribute(
                    "success",
                    "Organization created successfully."
            );

        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute(
                    "error",
                    e.getMessage()
            );
        }

        return "redirect:/organizations";
    }
}
