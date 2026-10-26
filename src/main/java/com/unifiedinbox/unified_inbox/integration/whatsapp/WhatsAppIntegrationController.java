package com.unifiedinbox.unified_inbox.integration.whatsapp;

import com.unifiedinbox.unified_inbox.service.OrganizationService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RestController
@RequestMapping("/api/organizations/{organizationId}/whatsapp")
public class WhatsAppIntegrationController {

    private final WhatsAppIntegrationService whatsappService;
    private final WhatsAppConnectionRepository connectionRepository;
    private final OrganizationService organizationService;
    private final EvolutionApiClient evolutionApiClient;

    public WhatsAppIntegrationController(
            WhatsAppIntegrationService whatsappService,
            WhatsAppConnectionRepository connectionRepository,
            OrganizationService organizationService,
            EvolutionApiClient evolutionApiClient
    ) {
        this.whatsappService = whatsappService;
        this.connectionRepository = connectionRepository;
        this.organizationService = organizationService;
        this.evolutionApiClient = evolutionApiClient;
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

    @Transactional(readOnly = true)
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

    @Transactional(readOnly = true)
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

    @Transactional(readOnly = true)
    @GetMapping(
            value = "/{integrationId}/qr",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<String> getQrCode(
            @PathVariable Long organizationId,
            @PathVariable Long integrationId
    ) {
        // Only the owner can link a WhatsApp account.
        organizationService.requireOwner(organizationId);

        WhatsAppConnectionEntity connection =
                connectionRepository.findByIntegrationId(integrationId)
                        .orElseThrow(() -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "WhatsApp connection not found"
                        ));

        if (!connection.getIntegration()
                .getOrganization()
                .getId()
                .equals(organizationId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "WhatsApp connection not found"
            );
        }

        if (!connection.getIntegration().isEnabled()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "WhatsApp integration is disabled"
            );
        }

        WhatsAppConnectionStatus status = connection.getStatus();

        if (status != WhatsAppConnectionStatus.WAITING_FOR_QR
                && status != WhatsAppConnectionStatus.CREATING
                && status != WhatsAppConnectionStatus.DISCONNECTED) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "WhatsApp connection cannot request a QR code"
            );
        }

        String response = evolutionApiClient.getQrCode(
                connection.getInstanceName()
        );

        if (response == null || response.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Evolution API returned an empty QR response"
            );
        }

        return ResponseEntity.ok(response);
    }

}
