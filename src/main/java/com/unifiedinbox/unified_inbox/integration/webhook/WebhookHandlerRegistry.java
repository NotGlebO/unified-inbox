
package com.unifiedinbox.unified_inbox.integration.webhook;

import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class WebhookHandlerRegistry {

    private final Map<String, WebhookHandler> handlers;

    public WebhookHandlerRegistry(List<WebhookHandler> handlerList) {
        this.handlers = handlerList.stream()
                .collect(Collectors.toUnmodifiableMap(
                        WebhookHandler::getProvider,
                        Function.identity()
                ));
    }

    public WebhookHandler getHandler(String provider) {
        WebhookHandler handler = handlers.get(provider);

        if (handler == null) {
            throw new IllegalArgumentException(
                    "Unsupported webhook provider: " + provider
            );
        }

        return handler;
    }
}
