
package com.unifiedinbox.unified_inbox.integration;

import com.unifiedinbox.unified_inbox.service.OrganizationService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class IntegrationCatalogController {

    private final IntegrationRegistry integrationRegistry;
    private final OrganizationService organizationService;

    public IntegrationCatalogController(
            IntegrationRegistry integrationRegistry,
            OrganizationService organizationService
    ) {
        this.integrationRegistry = integrationRegistry;
        this.organizationService = organizationService;
    }

    @GetMapping("/organizations/{id}/integrations/catalog")
    public List<IntegrationCatalogItem> getCatalog(
            @PathVariable Long id
    ) {
        organizationService.requireMember(id);

        return integrationRegistry.getAll()
                .stream()
                .map(provider -> new IntegrationCatalogItem(
                        provider.getType(),
                        provider.getDisplayName(),
                        provider.getIconPath(),
                        provider.getSetup()
                ))
                .toList();
    }

    public record IntegrationCatalogItem(
            String type,
            String displayName,
            String iconPath,
            IntegrationSetup setup
    ) {
    }
}
