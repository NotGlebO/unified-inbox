
package com.unifiedinbox.unified_inbox.integration;

import com.unifiedinbox.unified_inbox.entity.IntegrationEntity;
import com.unifiedinbox.unified_inbox.integration.whatsapp.*;
import com.unifiedinbox.unified_inbox.repository.IntegrationRepository;
import com.unifiedinbox.unified_inbox.service.OrganizationService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/organizations/{organizationId}/integrations")
public class UniversalIntegrationConnectionController {

    private final OrganizationService organizations;
    private final IntegrationRepository integrations;
    private final WhatsAppConnectionRepository whatsappConnections;
    private final EvolutionApiClient evolution;
    private final ObjectMapper objectMapper;
    private final Map<String, IntegrationConnectionHandler> handlers;

    public UniversalIntegrationConnectionController(
            OrganizationService organizations,
            IntegrationRepository integrations,
            WhatsAppConnectionRepository whatsappConnections,
            EvolutionApiClient evolution,
            ObjectMapper objectMapper,
            List<IntegrationConnectionHandler> handlers
    ) {
        this.organizations = organizations;
        this.integrations = integrations;
        this.whatsappConnections = whatsappConnections;
        this.evolution = evolution;
        this.objectMapper = objectMapper;

        this.handlers = handlers.stream().collect(
                Collectors.toUnmodifiableMap(
                        IntegrationConnectionHandler::getType,
                        Function.identity()
                )
        );
    }

    @PostMapping("/connect/{type}")
    public IntegrationConnectionResult connect(
            @PathVariable Long organizationId,
            @PathVariable String type,
            @RequestBody Map<String, String> credentials
    ) {
        organizations.requireOwner(organizationId);

        IntegrationConnectionHandler handler =
                handlers.get(type.toUpperCase(java.util.Locale.ROOT));

        if (handler == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Unsupported integration type"
            );
        }

        return handler.connect(organizationId, credentials);
    }

    @GetMapping("/{integrationId}/connection-status")
    public IntegrationConnectionResult status(
            @PathVariable Long organizationId,
            @PathVariable Long integrationId
    ) {
        organizations.requireOwner(organizationId);

        IntegrationEntity integration = integrations.findById(integrationId)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND)
                );

        if (!integration.getOrganization().getId().equals(organizationId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        if ("WHATSAPP".equals(integration.getType())) {
            var connection = whatsappConnections
                    .findByIntegrationId(integrationId)
                    .orElseThrow(() ->
                            new ResponseStatusException(HttpStatus.NOT_FOUND)
                    );

            String status = connection.getStatus().name();

            if (integration.isEnabled()
                    && status.equals("CONNECTED")) {
                status = "WAITING_FOR_QR";
            }

            if (integration.isEnabled()
                    && !status.equals("FAILED")
                    && !status.equals("CONFLICT")) {
                try {
                    String response = evolution.getConnectionState(
                            connection.getInstanceName()
                    );

                    JsonNode root = objectMapper.readTree(response);
                    String state = root.path("instance")
                            .path("state")
                            .asString("");

                    if ("open".equalsIgnoreCase(state)) {
                        status = "CONNECTED";
                    } else if ("close".equalsIgnoreCase(state)) {
                        status = "WAITING_FOR_QR";
                    }
                } catch (Exception exception) {
                    // Do not report CONNECTED when verification fails.
                    status = "WAITING_FOR_QR";
                }
            }

            if (!integration.isEnabled()) {
                status = "DISCONNECTED";
            }

            return new IntegrationConnectionResult(
                    integrationId,
                    status,
                    new IntegrationView("MESSAGE", null, status),
                    null
            );
        }

        String status = integration.isEnabled()
                ? "CONNECTED"
                : "DISCONNECTED";

        return new IntegrationConnectionResult(
                integrationId,
                status,
                new IntegrationView("MESSAGE", null, status),
                null
        );
    }
}
