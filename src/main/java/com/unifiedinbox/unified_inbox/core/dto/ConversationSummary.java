
package com.unifiedinbox.unified_inbox.core.dto;

import java.time.Instant;

public record ConversationSummary(
        Long id,
        Long integrationId,
        String integrationType,
        String platformName,
        String platformIcon,
        String integrationName,
        String customerName,
        Instant updatedAt
) {
}