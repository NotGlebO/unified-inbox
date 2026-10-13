
package com.unifiedinbox.unified_inbox.integration;

import com.unifiedinbox.unified_inbox.entity.IntegrationEntity;
import com.unifiedinbox.unified_inbox.repository.IntegrationRepository;
import com.unifiedinbox.unified_inbox.service.OrganizationService;
import com.unifiedinbox.unified_inbox.core.dto.ConnectedIntegration;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.List;

@RestController
public class IntegrationConnectionController {
    private final IntegrationRepository integrationRepository;
    private final OrganizationService organizationService;
    private final IntegrationDisconnectService disconnectService;
    

    public IntegrationConnectionController(
            IntegrationRepository integrationRepository,
            OrganizationService organizationService,
            IntegrationDisconnectService disconnectService
    ) {
        this.integrationRepository = integrationRepository;
        this.organizationService = organizationService;
        this.disconnectService = disconnectService;
    }

    @GetMapping("/organizations/{id}/integrations/connected")
    public List<ConnectedIntegration> getConnected(
            @PathVariable Long id
    ) {
        organizationService.requireMember(id);

        return integrationRepository.findByOrganizationIdAndEnabledTrue(id)
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
                integration.isEnabled(),
                disconnectService.supportsDisconnect(integration.getType())
        );
    }
    @PostMapping(
            "/organizations/{id}/integrations/{integrationId}/disconnect"
    )
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void disconnect(
            @PathVariable Long id,
            @PathVariable Long integrationId
    ) {
        disconnectService.disconnect(id, integrationId);
    }
}
