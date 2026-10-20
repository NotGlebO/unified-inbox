
package com.unifiedinbox.unified_inbox.integration;

import com.unifiedinbox.unified_inbox.core.dto.ConnectedIntegration;
import com.unifiedinbox.unified_inbox.entity.IntegrationEntity;
import com.unifiedinbox.unified_inbox.integration.whatsapp.EvolutionApiClient;
import com.unifiedinbox.unified_inbox.integration.whatsapp.WhatsAppConnectionEntity;
import com.unifiedinbox.unified_inbox.integration.whatsapp.WhatsAppConnectionRepository;
import com.unifiedinbox.unified_inbox.repository.IntegrationRepository;
import com.unifiedinbox.unified_inbox.service.OrganizationService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.unifiedinbox.unified_inbox.integration.whatsapp.WhatsAppConnectionEntity;

import java.util.List;

@RestController
public class IntegrationConnectionController {

    private final IntegrationRepository integrationRepository;
    private final OrganizationService organizationService;
    private final IntegrationDisconnectService disconnectService;
    private final WhatsAppConnectionRepository whatsappRepository;
    private final EvolutionApiClient evolutionApiClient;
    private final ObjectMapper objectMapper;

    public IntegrationConnectionController(
            IntegrationRepository integrationRepository,
            OrganizationService organizationService,
            IntegrationDisconnectService disconnectService,
            WhatsAppConnectionRepository whatsappRepository,
            EvolutionApiClient evolutionApiClient,
            ObjectMapper objectMapper
    ) {
        this.integrationRepository = integrationRepository;
        this.organizationService = organizationService;
        this.disconnectService = disconnectService;
        this.whatsappRepository = whatsappRepository;
        this.evolutionApiClient = evolutionApiClient;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/organizations/{id}/integrations/connected")
    public List<ConnectedIntegration> getConnected(
            @PathVariable Long id
    ) {
        organizationService.requireMember(id);

        return integrationRepository
                .findByOrganizationIdAndEnabledTrue(id)
                .stream()
                .filter(this::isConnected)
                .map(this::toDto)
                .toList();
    }

    private boolean isConnected(IntegrationEntity integration) {
        if (!"WHATSAPP".equals(integration.getType())) {
            return true;
        }

        return whatsappRepository
                .findByIntegrationId(integration.getId())
                .map(connection -> {
                    try {
                        String response = evolutionApiClient
                                .getConnectionState(
                                        connection.getInstanceName()
                                );

                        JsonNode root = objectMapper.readTree(response);

                        return "open".equalsIgnoreCase(
                                root.path("instance")
                                        .path("state")
                                        .asString("")
                        );
                    } catch (Exception exception) {
                        return false;
                    }
                })
                .orElse(false);
    }

    private ConnectedIntegration toDto(
            IntegrationEntity integration
    ) {
    String accountLabel = null;

    if ("WHATSAPP".equals(integration.getType())) {
            accountLabel = whatsappRepository
                    .findByIntegrationId(integration.getId())
                    .map(WhatsAppConnectionEntity::getWhatsappJid)
                    .filter(jid -> jid.endsWith("@s.whatsapp.net"))
                    .map(jid -> "+" + jid.replace("@s.whatsapp.net", ""))
                    .orElse(null);
    }

    return new ConnectedIntegration(
            integration.getId(),
            integration.getType(),
            integration.getDisplayName(),
            integration.getIconPath(),
            integration.isEnabled(),
            disconnectService.supportsDisconnect(
                    integration.getType()
            ),
            accountLabel
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
        organizationService.requireOwner(id);

        IntegrationEntity integration = integrationRepository
                .findById(integrationId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Integration not found"
                        )
                );

        if (!integration.getOrganization().getId().equals(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        disconnectService.disconnect(id, integrationId);
    }
}
