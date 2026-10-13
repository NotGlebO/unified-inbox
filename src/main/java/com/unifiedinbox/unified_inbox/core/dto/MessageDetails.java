package com.unifiedinbox.unified_inbox.core.dto;

import java.time.Instant;

public record MessageDetails(
        Long id,
        String senderName,
        String text,
        Instant sentAt,
        MessageDirection direction
) {
}