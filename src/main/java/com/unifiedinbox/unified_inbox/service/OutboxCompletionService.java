
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
            Instant nextAttemptAt
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

        outbox.scheduleRetry(nextAttemptAt, error);
        message.setStatus(MessageStatus.RETRY);
    }

}
