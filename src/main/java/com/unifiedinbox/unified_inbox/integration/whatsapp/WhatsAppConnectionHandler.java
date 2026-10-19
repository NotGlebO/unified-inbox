
package com.unifiedinbox.unified_inbox.integration.whatsapp;

import com.unifiedinbox.unified_inbox.integration.*;
import org.springframework.stereotype.Component;
import java.util.Map;

@Component
public class WhatsAppConnectionHandler implements IntegrationConnectionHandler {

    private final WhatsAppIntegrationService service;

    public WhatsAppConnectionHandler(WhatsAppIntegrationService service) {
        this.service = service;
    }

    @Override
    public String getType() {
        return "WHATSAPP";
    }

    @Override
    public IntegrationConnectionResult connect(
            Long organizationId,
            Map<String, String> credentials
    ) {
        Long id = service.connect(organizationId);

        String base = "/api/organizations/"
                + organizationId
                + "/whatsapp/"
                + id;

        return new IntegrationConnectionResult(
                id,
                "WAITING_FOR_QR",
                new IntegrationView(
                        "QR",
                        base + "/qr",
                        "Scan this QR code in WhatsApp linked devices"
                ),
                new IntegrationNextAction("POLL", base, 2000)
        );
    }
}
