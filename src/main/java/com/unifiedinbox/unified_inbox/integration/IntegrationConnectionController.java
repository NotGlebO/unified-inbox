
package com.unifiedinbox.unified_inbox.integration;

import com.unifiedinbox.unified_inbox.entity.IntegrationEntity;
import com.unifiedinbox.unified_inbox.repository.IntegrationRepository;
import com.unifiedinbox.unified_inbox.service.OrganizationService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class IntegrationConnectionController {

    private final IntegrationRepository integrationRepository;
    private final OrganizationService organizationService;

    public IntegrationConnectionController(
            IntegrationRepository integrationRepository,
            OrganizationService organizationService
    ) {
        this.integrationRepository = integrationRepository;
        this.organizationService = organizationService;
    }

    @GetMapping("/organizations/{id}/integrations/connected")
    public List<ConnectedIntegration> getConnected(
            @PathVariable Long id
    ) {
        organizationService.requireMember(id);

        return integrationRepository.findByOrganizationId(id)
                .stream()
                .map(this::toDto)
                .toList();
    }

    private ConnectedIntegration toDto(IntegrationEntity integration) {
        return new ConnectedIntegration(
                integration.getId(),
                integration.getType(),
                integration.getDisplayName(),
                integration.getIconPath(),
                integration.isEnabled()
        );
    }
}
