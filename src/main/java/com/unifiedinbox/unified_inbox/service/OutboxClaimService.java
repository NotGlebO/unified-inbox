
package com.unifiedinbox.unified_inbox.service;

import com.unifiedinbox.unified_inbox.core.dto.MessageStatus;
import com.unifiedinbox.unified_inbox.entity.OutboxEntity;
import com.unifiedinbox.unified_inbox.entity.MessageEntity;
import com.unifiedinbox.unified_inbox.repository.OutboxRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class OutboxClaimService {

    private final OutboxRepository outboxRepository;

    public OutboxClaimService(
            OutboxRepository outboxRepository
    ) {
        this.outboxRepository = outboxRepository;
    }

    @Transactional
    public Optional<Long> claimNextMessage() {

        Optional<OutboxEntity> optionalOutbox =
                outboxRepository.findNextPendingForUpdate();

        if (optionalOutbox.isEmpty()) {
            return Optional.empty();
        }

        OutboxEntity outbox = optionalOutbox.get();

        MessageEntity message = outbox.getMessage();

        message.setStatus(MessageStatus.SENDING);

        return Optional.of(outbox.getId());
    }
}
