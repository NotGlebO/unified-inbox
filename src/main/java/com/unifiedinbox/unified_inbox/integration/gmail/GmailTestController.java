
package com.unifiedinbox.unified_inbox.integration.gmail;

import com.unifiedinbox.unified_inbox.repository.GmailAccountRepository;
import com.unifiedinbox.unified_inbox.service.OrganizationService;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.Map;

@RestController
@RequestMapping("/api/organizations/{organizationId}/gmail")
public class GmailTestController {
    private final GmailSyncService gmailSyncService;
    private final GmailClientService gmailClientService;
    private final GmailAccountRepository gmailAccountRepository;
    private final OrganizationService organizationService;

    public GmailTestController(
            GmailClientService gmailClientService,
            GmailAccountRepository gmailAccountRepository,
            OrganizationService organizationService,
            GmailSyncService gmailSyncService
    ) {
        this.gmailClientService = gmailClientService;
        this.gmailAccountRepository = gmailAccountRepository;
        this.organizationService = organizationService;
        this.gmailSyncService = gmailSyncService;
    }

    @GetMapping("/test")
    public GmailClientService.GmailMessageList test(
            @PathVariable Long organizationId
    ) {
        organizationService.requireOwner(organizationId);

        var account = gmailAccountRepository.findAll()
                .stream()
                .filter(a ->
                        a.getIntegration().getOrganization()
                                .getId().equals(organizationId)
                        && a.getIntegration().isEnabled()
                )
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No connected Gmail account"
                ));

        return gmailClientService.getInboxMessages(account);
    }

    @PostMapping("/sync")
    public Map<String, Object> sync(
            @PathVariable Long organizationId
    ) {
        organizationService.requireOwner(organizationId);

        var account = gmailAccountRepository.findAll()
                .stream()
                .filter(a ->
                        a.getIntegration().getOrganization()
                                .getId().equals(organizationId)
                        && a.getIntegration().isEnabled()
                )
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No connected Gmail account"
                ));

        int imported = gmailSyncService.syncInbox(account);

        return Map.of(
                "status", "success",
                "imported", imported
        );
    }
}
