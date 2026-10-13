package com.unifiedinbox.unified_inbox.integration.telegram;

import com.unifiedinbox.unified_inbox.integration.IntegrationField;
import com.unifiedinbox.unified_inbox.integration.IntegrationProvider;
import com.unifiedinbox.unified_inbox.integration.IntegrationSetup;

import org.springframework.stereotype.Component;

import java.util.List;

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

    @Override
    public IntegrationSetup getSetup() {

        return new IntegrationSetup(
                "Connect Telegram",
                "Connect a Telegram bot to your organization.",
                "TOKEN",
                "/organizations/{id}/integrations/telegram",

                List.of(
                        new IntegrationField(
                                "token",
                                "Bot API Token",
                                "password",
                                "Enter BotFather token...",
                                true
                        )
                ),

                List.of(
                        "Open @BotFather in Telegram.",
                        "Create a bot using /newbot.",
                        "Copy the API token.",
                        "Paste the token below and click Connect."
                )
        );
    }
}
