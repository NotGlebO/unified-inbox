package com.unifiedinbox.unified_inbox.core.dto;

import java.util.List;

public record DeleteConversationsRequest(
        List<Long> conversationIds
) {}