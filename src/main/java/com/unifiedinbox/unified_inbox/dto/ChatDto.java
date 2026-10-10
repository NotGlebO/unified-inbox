package com.unifiedinbox.unified_inbox.dto;

import java.time.Instant;

public record ChatDto(
        String chatId,
        String sender,
        String source,
        String lastMessage,
        Instant lastMessageAt
) {}