package com.unifiedinbox.unified_inbox.integration.telegram;

import java.time.Instant;

public record TelegramMessage(
        Long chatId,
        String sender,
        String text,
        Instant receivedAt
) {}