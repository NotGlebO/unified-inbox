package com.unifiedinbox.unified_inbox.core.adapter;

import com.unifiedinbox.unified_inbox.core.dto.IntegrationCapabilities;

public interface MessagingAdapter {

    String getType();

    String getDisplayName();

    String getIconPath();

    String getColor();

    IntegrationCapabilities getCapabilities();

    String sendMessage(
            Long integrationId,
            String externalChatId,
            String text
    );
}