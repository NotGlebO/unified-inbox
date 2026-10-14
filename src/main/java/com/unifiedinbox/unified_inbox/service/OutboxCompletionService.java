
package com.unifiedinbox.unified_inbox.service;

import com.unifiedinbox.unified_inbox.core.dto.MessageStatus;
import com.unifiedinbox.unified_inbox.entity.OutboxEntity;
import com.unifiedinbox.unified_inbox.entity.MessageEntity;
import com.unifiedinbox.unified_inbox.repository.OutboxRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;

@Service
public class OutboxCompletionService {

    private final OutboxRepository outboxRepository;

    public OutboxCompletionService(
            OutboxRepository outboxRepository
    ) {
        this.outboxRepository = outboxRepository;
    }

    @Transactional
    public void markSent(
            Long outboxId,
            String externalMessageId
    ) {
        OutboxEntity outbox = outboxRepository
                .findById(outboxId)
                .orElseThrow();

        MessageEntity message = outbox.getMessage();

        if (message.getStatus() != MessageStatus.SENDING) {
            throw new IllegalStateException(
                    "Message is not in SENDING status."
            );
        }

        message.setExternalMessageId(externalMessageId);
        message.setStatus(MessageStatus.SENT);

        outboxRepository.delete(outbox);
    }


    @Transactional
    public void markUnknown(Long outboxId, String error) {

        OutboxEntity outbox = outboxRepository
                .findById(outboxId)
                .orElseThrow();

        MessageEntity message = outbox.getMessage();

        if (message.getStatus() != MessageStatus.SENDING) {
            throw new IllegalStateException(
                    "Message is not in SENDING status."
            );
        }

        outbox.recordUnknown(error);
        message.setStatus(MessageStatus.UNKNOWN);
    }


    
    @Transactional
    public void markFailed(Long outboxId, String error) {

        OutboxEntity outbox = outboxRepository
                .findById(outboxId)
                .orElseThrow();

        MessageEntity message = outbox.getMessage();

        if (message.getStatus() != MessageStatus.SENDING) {
            throw new IllegalStateException(
                    "Message is not in SENDING status."
            );
        }

        outbox.recordFailure(error);
        message.setStatus(MessageStatus.FAILED);
    }



    @Transactional
    public void markRetry(
            Long outboxId,
            String error,
            Long retryAfterSeconds
    ) {
        final int maxAttempts = 5;

        OutboxEntity outbox = outboxRepository
                .findById(outboxId)
                .orElseThrow();

        MessageEntity message = outbox.getMessage();

        if (message.getStatus() != MessageStatus.SENDING) {
            throw new IllegalStateException(
                    "Message is not in SENDING status."
            );
        }

        int nextAttempt = outbox.getAttempts() + 1;

        if (nextAttempt >= maxAttempts) {
            outbox.recordFailure(
                    "Maximum attempts reached: " + error
            );
            message.setStatus(MessageStatus.FAILED);
            return;
        }

        long backoffSeconds = 30L * (1L << (nextAttempt - 1));

        long delaySeconds = backoffSeconds;

        if (retryAfterSeconds != null && retryAfterSeconds > 0) {
            delaySeconds = Math.max(
                    backoffSeconds,
                    retryAfterSeconds
            );
        }

        Instant nextAttemptAt =
                Instant.now().plusSeconds(delaySeconds);

        outbox.scheduleRetry(nextAttemptAt, error);
        message.setStatus(MessageStatus.RETRY);
    }

}
