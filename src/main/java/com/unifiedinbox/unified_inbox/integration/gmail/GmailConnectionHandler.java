package com.unifiedinbox.unified_inbox.integration.gmail;

import com.unifiedinbox.unified_inbox.integration.*;
import org.springframework.stereotype.Component;
import java.util.Map;

@Component
public class GmailConnectionHandler implements IntegrationConnectionHandler {

    private final GmailIntegrationService service;

    public GmailConnectionHandler(GmailIntegrationService service) {
        this.service = service;
    }

    @Override
    public String getType() {
        return "GMAIL";
    }

    @Override
    public IntegrationConnectionResult connect(
            Long organizationId,
            Map<String, String> credentials
    ) {
        String authorizationCode = credentials.get("code");

        Long integrationId = service.connect(organizationId, authorizationCode);

        return new IntegrationConnectionResult(
                integrationId,
                "CONNECTED",
                new IntegrationView(
                        "MESSAGE",
                        null,
                        "Gmail connected successfully"
                ),
                null
        );
    }
}
