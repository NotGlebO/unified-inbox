
package com.unifiedinbox.unified_inbox.core.dto;

import java.time.Instant;
import java.util.List;

public record MessageDetails(
        Long id,
        String senderName,
        String text,
        Instant sentAt,
        MessageDirection direction,
        MessageStatus status,
        MessageSource source,
        List<AttachmentDetails> attachments
) {}
