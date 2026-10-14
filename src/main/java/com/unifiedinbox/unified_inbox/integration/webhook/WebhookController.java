
package com.unifiedinbox.unified_inbox.integration.webhook;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/api/webhooks")
public class WebhookController {

    private final WebhookHandlerRegistry registry;

    public WebhookController(WebhookHandlerRegistry registry) {
        this.registry = registry;
    }

    @PostMapping("/{provider}")
    public ResponseEntity<Void> receive(
            @PathVariable String provider,
            @RequestBody String body,
            @RequestHeader Map<String, String> headers
    ) {
        WebhookHandler handler;

        try {
            handler = registry.getHandler(
                    provider.toUpperCase(Locale.ROOT)
            );
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.notFound().build();
        }

        Map<String, String> normalized = new HashMap<>();

        headers.forEach((key, value) ->
                normalized.put(
                        key.toLowerCase(Locale.ROOT),
                        value
                )
        );

        return handler.handle(body, normalized);
    }
}
