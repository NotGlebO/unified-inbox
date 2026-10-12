package com.unifiedinbox.unified_inbox.integration.gmail;

import com.unifiedinbox.unified_inbox.repository.GmailAccountRepository;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class GmailPollingService {

    private final GmailAccountRepository gmailAccountRepository;
    private final GmailSyncService gmailSyncService;

    public GmailPollingService(
            GmailAccountRepository gmailAccountRepository,
            GmailSyncService gmailSyncService
    ) {
        this.gmailAccountRepository = gmailAccountRepository;
        this.gmailSyncService = gmailSyncService;
    }

    @Scheduled(fixedDelay = 30000)
    public void pollGmail() {

        var accounts = gmailAccountRepository.findAllActive();

        for (var account : accounts) {

            if (!account.getIntegration().isEnabled()) {
                continue;
            }

            try {
                gmailSyncService.syncInbox(account);

            } catch (Exception e) {
                System.err.println(
                        "Gmail polling failed: " + e.getMessage()
                );
            }
        }
    }
}