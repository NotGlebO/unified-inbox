
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
import org.springframework.transaction.annotation.Transactional;

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

    @Transactional(readOnly = true)
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

            // Verify the actual connection state in Evolution API.
            if (integration.isEnabled()
                    && !"FAILED".equals(status)
                    && !"CONFLICT".equals(status)) {

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
                    } else if ("CONNECTED".equals(status)) {
                        status = "WAITING_FOR_QR";
                    }

                } catch (Exception exception) {
                    // Keep the connection pending if Evolution API
                    // is temporarily unavailable.
                    status = "WAITING_FOR_QR";
                }
            }

            // Preserve terminal errors and disabled integrations.
            if (!integration.isEnabled()
                    && !"CONFLICT".equals(status)
                    && !"FAILED".equals(status)) {
                status = "DISCONNECTED";
            }

            String message = status;

            if ("CONFLICT".equals(status)) {
                message = connection.getErrorMessage() != null
                        ? connection.getErrorMessage()
                        : "This WhatsApp account is already connected to another integration.";
            }

            if ("FAILED".equals(status)) {
                message = connection.getErrorMessage() != null
                        ? connection.getErrorMessage()
                        : "Failed to connect WhatsApp.";
            }

            // Return QR configuration while waiting for a scan.
            if ("WAITING_FOR_QR".equals(status)) {

                String qrUrl = "/api/organizations/"
                        + organizationId
                        + "/whatsapp/"
                        + integrationId
                        + "/qr";

                String statusUrl = "/api/organizations/"
                        + organizationId
                        + "/integrations/"
                        + integrationId
                        + "/connection-status";

                return new IntegrationConnectionResult(
                        integrationId,
                        status,
                        new IntegrationView(
                                "QR",
                                qrUrl,
                                "Scan this QR code in WhatsApp linked devices."
                        ),
                        new IntegrationNextAction(
                                "POLL",
                                statusUrl,
                                2000
                        )
                );
            }

            // All other states.
            return new IntegrationConnectionResult(
                    integrationId,
                    status,
                    new IntegrationView(
                            "MESSAGE",
                            null,
                            message
                    ),
                    null
            );
        }

        // Other integrations: Telegram, Gmail, etc.
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
