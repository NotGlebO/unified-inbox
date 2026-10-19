
package com.unifiedinbox.unified_inbox.integration.whatsapp;

import com.unifiedinbox.unified_inbox.integration.IntegrationProvider;
import com.unifiedinbox.unified_inbox.integration.IntegrationSetup;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class WhatsAppIntegrationProvider
        implements IntegrationProvider {

    @Override
    public String getType() {
        return "WHATSAPP";
    }

    @Override
    public String getDisplayName() {
        return "WhatsApp";
    }

    @Override
    public String getIconPath() {
        return "/icons/whatsapp.svg";
    }

    @Override
    public IntegrationSetup getSetup() {
        return new IntegrationSetup(
                "Connect WhatsApp",
                "Link your WhatsApp account by scanning a QR code.",
                "QR",
                "/organizations/{id}/integrations/whatsapp",
                List.of(),
                List.of(
                        "Click Connect WhatsApp.",
                        "Open WhatsApp on your phone.",
                        "Go to Linked devices.",
                        "Scan the QR code displayed on this page."
                )
        );
    }
}
