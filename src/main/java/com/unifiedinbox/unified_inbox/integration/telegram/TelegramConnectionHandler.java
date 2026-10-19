
package com.unifiedinbox.unified_inbox.integration.telegram;

import com.unifiedinbox.unified_inbox.integration.*;
import org.springframework.stereotype.Component;
import java.util.Map;

@Component
public class TelegramConnectionHandler implements IntegrationConnectionHandler {

    private final TelegramIntegrationService service;

    public TelegramConnectionHandler(TelegramIntegrationService service) {
        this.service = service;
    }

    @Override
    public String getType() {
        return "TELEGRAM";
    }

    @Override
    public IntegrationConnectionResult connect(
            Long organizationId,
            Map<String, String> credentials
    ) {
        service.connect(organizationId, credentials.get("token"));

        return new IntegrationConnectionResult(
                null,
                "CONNECTED",
                new IntegrationView(
                        "MESSAGE",
                        null,
                        "Telegram connected successfully"
                ),
                null
        );
    }
}
