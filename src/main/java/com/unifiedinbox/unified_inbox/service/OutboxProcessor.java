
package com.unifiedinbox.unified_inbox.service;

import com.unifiedinbox.unified_inbox.core.adapter.AdapterRegistry;
import com.unifiedinbox.unified_inbox.core.adapter.MessageSendException;
import com.unifiedinbox.unified_inbox.core.dto.OutgoingMessageData;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class OutboxProcessor {

    private final OutboxDataService dataService;
    private final AdapterRegistry adapterRegistry;
    private final OutboxCompletionService completionService;

    public OutboxProcessor(
            OutboxDataService dataService,
            AdapterRegistry adapterRegistry,
            OutboxCompletionService completionService
    ) {
        this.dataService = dataService;
        this.adapterRegistry = adapterRegistry;
        this.completionService = completionService;
    }

    public void process(Long outboxId) {

        OutgoingMessageData data =
                dataService.getMessageData(outboxId);

        if (!data.integrationEnabled()) {
            completionService.markFailed(
                    outboxId,
                    "Integration is disabled."
            );
            return;
        }

        String externalMessageId;

        try {
            externalMessageId = adapterRegistry
                    .getAdapter(data.integrationType())
                    .sendMessage(
                            data.integrationId(),
                            data.externalChatId(),
                            data.text()
                    );

        } catch (MessageSendException exception) {

            switch (exception.getFailureType()) {
                case UNKNOWN ->
                        completionService.markUnknown(
                                outboxId,
                                exception.getMessage()
                        );

                case PERMANENT ->
                        completionService.markFailed(
                                outboxId,
                                exception.getMessage()
                        );

                case RETRYABLE ->
                completionService.markRetry(
                        outboxId,
                        exception.getMessage(),
                        exception.getRetryAfterSeconds()
                );
            }

            return;
        } catch (RuntimeException exception) {
        completionService.markUnknown(
                outboxId,
                "Unexpected error during message sending: "
                        + exception.getClass().getSimpleName()
        );
            return;
}

        completionService.markSent(
                outboxId,
                externalMessageId
        );
    }
}
