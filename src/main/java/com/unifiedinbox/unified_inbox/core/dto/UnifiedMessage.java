package com.unifiedinbox.unified_inbox.core.dto;

import java.time.Instant;

public record UnifiedMessage(
        Long integrationId,
        String externalChatId,
        String externalMessageId,
        String senderName,
        String text,
        Instant sentAt,
        MessageDirection direction
) {
}