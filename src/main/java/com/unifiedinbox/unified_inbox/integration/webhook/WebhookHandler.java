
package com.unifiedinbox.unified_inbox.integration.webhook;

import org.springframework.http.ResponseEntity;
import java.util.Map;

public interface WebhookHandler {

    String getProvider();

    ResponseEntity<Void> handle(
            String body,
            Map<String, String> headers
    );
}
