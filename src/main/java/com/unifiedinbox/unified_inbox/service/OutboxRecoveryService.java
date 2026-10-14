
package com.unifiedinbox.unified_inbox.service;

import com.unifiedinbox.unified_inbox.core.dto.MessageStatus;
import com.unifiedinbox.unified_inbox.entity.MessageEntity;
import com.unifiedinbox.unified_inbox.entity.OutboxEntity;
import com.unifiedinbox.unified_inbox.repository.OutboxRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

@Service
public class OutboxRecoveryService {

    private static final Duration STUCK_TIMEOUT =
            Duration.ofMinutes(5);

    private final OutboxRepository outboxRepository;

    public OutboxRecoveryService(
            OutboxRepository outboxRepository
    ) {
        this.outboxRepository = outboxRepository;
    }

    @Transactional
    public boolean recoverOneStuckMessage() {

        Instant cutoff = Instant.now().minus(STUCK_TIMEOUT);

        return outboxRepository
                .findNextStuckForUpdate(cutoff)
                .map(outbox -> {

                    MessageEntity message = outbox.getMessage();

                    message.setStatus(MessageStatus.UNKNOWN);

                    outbox.recordUnknown(
                            "Sending interrupted or timed out. " +
                            "Delivery could not be confirmed."
                    );

                    outbox.clearClaim();

                    return true;
                })
                .orElse(false);
    }
}
