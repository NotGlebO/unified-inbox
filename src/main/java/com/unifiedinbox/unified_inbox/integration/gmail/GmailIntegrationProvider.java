package com.unifiedinbox.unified_inbox.integration.gmail;

import com.unifiedinbox.unified_inbox.integration.IntegrationProvider;
import com.unifiedinbox.unified_inbox.integration.IntegrationSetup;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class GmailIntegrationProvider implements IntegrationProvider {

    @Override
    public String getType() {
        return "GMAIL";
    }

    @Override
    public String getDisplayName() {
        return "Gmail";
    }

    @Override
    public String getIconPath() {
        return "/icons/gmail.svg";
    }

    @Override
    public IntegrationSetup getSetup() {
        return new IntegrationSetup(
                "Connect Gmail",
                "Connect your Gmail account to your organization. You will be redirected to Google to authorize access.",
                "OAUTH",
                "/api/integrations/gmail/authorize?organizationId={id}",
                List.of(),
                List.of(
                        "Click Connect Gmail.",
                        "You will be redirected to Google to authorize access to your Gmail account.",
                        "Grant permission to read and send emails.",
                        "You will be redirected back to complete the connection."
                )
        );
    }
}
