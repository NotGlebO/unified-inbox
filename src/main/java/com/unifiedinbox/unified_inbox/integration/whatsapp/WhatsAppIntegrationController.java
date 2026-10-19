package com.unifiedinbox.unified_inbox.integration.whatsapp;

import com.unifiedinbox.unified_inbox.service.OrganizationService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/organizations/{organizationId}/whatsapp")
public class WhatsAppIntegrationController {

    private final WhatsAppIntegrationService whatsappService;
    private final WhatsAppConnectionRepository connectionRepository;
    private final OrganizationService organizationService;

    public WhatsAppIntegrationController(
            WhatsAppIntegrationService whatsappService,
            WhatsAppConnectionRepository connectionRepository,
            OrganizationService organizationService
    ) {
        this.whatsappService = whatsappService;
        this.connectionRepository = connectionRepository;
        this.organizationService = organizationService;
    }

    @PostMapping
    public ResponseEntity<ConnectionResponse> connect(
            @PathVariable Long organizationId
    ) {
        Long integrationId = whatsappService.connect(organizationId);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new ConnectionResponse(
                        integrationId,
                        WhatsAppConnectionStatus.WAITING_FOR_QR
                ));
    }

    @GetMapping("/{integrationId}")
    public ConnectionResponse getConnection(
            @PathVariable Long organizationId,
            @PathVariable Long integrationId
    ) {
        organizationService.requireMember(organizationId);

        WhatsAppConnectionEntity connection =
                connectionRepository.findByIntegrationId(integrationId)
                        .orElseThrow(() -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "WhatsApp connection not found."
                        ));

        if (!connection.getIntegration()
                .getOrganization()
                .getId()
                .equals(organizationId)) {

            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "WhatsApp connection not found."
            );
        }

        return new ConnectionResponse(
                integrationId,
                connection.getStatus()
        );
    }

    @GetMapping
    public List<ConnectionResponse> getConnections(
            @PathVariable Long organizationId
    ) {
        organizationService.requireMember(organizationId);

        return connectionRepository
                .findByIntegrationOrganizationId(organizationId)
                .stream()
                .map(connection -> new ConnectionResponse(
                        connection.getIntegration().getId(),
                        connection.getStatus()
                ))
                .toList();
    }

    public record ConnectionResponse(
            Long integrationId,
            WhatsAppConnectionStatus status
    ) {
    }
}
