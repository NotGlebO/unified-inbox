
package com.unifiedinbox.unified_inbox.integration;

import com.unifiedinbox.unified_inbox.entity.IntegrationEntity;
import com.unifiedinbox.unified_inbox.repository.IntegrationRepository;
import com.unifiedinbox.unified_inbox.service.OrganizationService;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class IntegrationDisconnectService {

    private final IntegrationRepository integrationRepository;
    private final OrganizationService organizationService;
    private final Map<String, IntegrationDisconnectHandler> handlers;

    public IntegrationDisconnectService(
            IntegrationRepository integrationRepository,
            OrganizationService organizationService,
            List<IntegrationDisconnectHandler> handlers
    ) {
        this.integrationRepository = integrationRepository;
        this.organizationService = organizationService;

        this.handlers = handlers.stream()
                .collect(Collectors.toUnmodifiableMap(
                        IntegrationDisconnectHandler::getType,
                        Function.identity()
                ));
    }

    public boolean supportsDisconnect(String type) {
        return handlers.containsKey(type);
    }

    @Transactional
    public void disconnect(Long organizationId, Long integrationId) {

        organizationService.requireOwner(organizationId);

        IntegrationEntity integration = integrationRepository
                .findById(integrationId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Integration not found."
                ));

        if (!integration.getOrganization().getId().equals(organizationId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Integration not found."
            );
        }

        if (!integration.isEnabled()) {
            return;
        }

        IntegrationDisconnectHandler handler =
                handlers.get(integration.getType());

        if (handler == null) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Disconnect is not supported for this integration."
            );
        }

        handler.disconnect(integration);
        integration.setEnabled(false);
    }
}
