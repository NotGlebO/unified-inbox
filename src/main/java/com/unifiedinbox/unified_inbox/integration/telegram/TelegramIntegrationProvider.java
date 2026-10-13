
package com.unifiedinbox.unified_inbox.integration.telegram;

import com.unifiedinbox.unified_inbox.integration.IntegrationProvider;
import org.springframework.stereotype.Component;

@Component
public class TelegramIntegrationProvider
        implements IntegrationProvider {

    @Override
    public String getType() {
        return "TELEGRAM";
    }

    @Override
    public String getDisplayName() {
        return "Telegram";
    }

    @Override
    public String getIconPath() {
        return "/icons/telegram.svg";
    }
}
