
package com.unifiedinbox.unified_inbox.core.dto;

public record OutgoingMessageData(
        Long outboxId,
        Long integrationId,
        String integrationType,
        String externalChatId,
        String text,
        boolean integrationEnabled
) {
}
