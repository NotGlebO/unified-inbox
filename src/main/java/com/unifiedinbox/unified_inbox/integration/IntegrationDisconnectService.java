
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class IntegrationDisconnectService {

    private static final Logger log =
        LoggerFactory.getLogger(IntegrationDisconnectService.class);
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

        log.info(
                "[DISCONNECT] Service started. organizationId={}, integrationId={}",
                organizationId,
                integrationId
        );

        organizationService.requireOwner(organizationId);

        log.info("[DISCONNECT] Owner permission verified.");

        IntegrationEntity integration = integrationRepository
                .findById(integrationId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Integration not found."
                ));

        log.info(
                "[DISCONNECT] Integration found. id={}, type={}, enabled={}",
                integration.getId(),
                integration.getType(),
                integration.isEnabled()
        );

        if (!integration.getOrganization().getId().equals(organizationId)) {
            log.warn("[DISCONNECT] Organization mismatch.");

            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Integration not found."
            );
        }

        if (!integration.isEnabled()) {
            log.info("[DISCONNECT] Integration already disabled.");
            return;
        }

        IntegrationDisconnectHandler handler =
                handlers.get(integration.getType());

        if (handler == null) {
            log.error(
                    "[DISCONNECT] No handler registered for type={}",
                    integration.getType()
            );

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Disconnect is not supported for this integration."
            );
        }

        log.info(
                "[DISCONNECT] Calling handler={}",
                handler.getClass().getSimpleName()
        );

        handler.disconnect(integration);

        log.info("[DISCONNECT] Handler completed.");

        integration.setEnabled(false);

        // Force database synchronization to detect SQL errors here.
        integrationRepository.flush();

        log.info("[DISCONNECT] Integration marked disabled and flushed.");
    }

}
