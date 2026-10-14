
package com.unifiedinbox.unified_inbox.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class OutboxScheduler {

    private static final Logger log =
            LoggerFactory.getLogger(OutboxScheduler.class);

    private static final int MAX_BATCH_SIZE = 20;

    private final OutboxClaimService claimService;
    private final OutboxProcessor processor;
    private final OutboxRecoveryService recoveryService;

    public OutboxScheduler(
            OutboxClaimService claimService,
            OutboxProcessor processor,
            OutboxRecoveryService recoveryService
    ) {
        this.claimService = claimService;
        this.processor = processor;
        this.recoveryService = recoveryService;
    }

    @Scheduled(fixedDelay = 2000)
    public void processMessages() {

        for (int i = 0; i < MAX_BATCH_SIZE; i++) {

            Optional<Long> outboxId;

            try {
                outboxId = claimService.claimNextMessage();
            } catch (Exception exception) {
                log.error("Failed to claim outbox message.", exception);
                return;
            }

            if (outboxId.isEmpty()) {
                return;
            }

            try {
                processor.process(outboxId.get());
            } catch (Exception exception) {
                log.error(
                        "Failed to process outbox message ID {}.",
                        outboxId.get(),
                        exception
                );
            }
        }
    }

    @Scheduled(fixedDelay = 60000)
    public void recoverStuckMessages() {

        for (int i = 0; i < MAX_BATCH_SIZE; i++) {

            try {
                boolean recovered =
                        recoveryService.recoverOneStuckMessage();

                if (!recovered) {
                    return;
                }

            } catch (Exception exception) {
                log.error(
                        "Failed to recover stuck outbox message.",
                        exception
                );
                return;
            }
        }
    }
}
