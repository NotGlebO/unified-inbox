
package com.unifiedinbox.unified_inbox.core.dto;

import java.time.Instant;

public record ConversationSummary(
        Long id,
        Long integrationId,
        String integrationType,
        String integrationName,
        String iconPath,
        String integrationColor,
        String integrationDisplayName,
        String customerName,
        Instant updatedAt
) {
}
