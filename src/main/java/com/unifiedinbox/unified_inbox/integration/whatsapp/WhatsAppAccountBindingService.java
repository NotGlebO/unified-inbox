
package com.unifiedinbox.unified_inbox.integration.whatsapp;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Locale;

@Service
public class WhatsAppAccountBindingService {

    private final WhatsAppConnectionRepository connectionRepository;
    private final TransactionTemplate transactionTemplate;

    public WhatsAppAccountBindingService(
            WhatsAppConnectionRepository connectionRepository,
            TransactionTemplate transactionTemplate
    ) {
        this.connectionRepository = connectionRepository;
        this.transactionTemplate = transactionTemplate;
    }

    public boolean bindAccount(
            String instanceName,
            String whatsappJid
    ) {
        String normalizedJid = normalizeJid(whatsappJid);

        try {
            return Boolean.TRUE.equals(
                    transactionTemplate.execute(status -> {

                        WhatsAppConnectionEntity connection =
                                connectionRepository
                                        .findByInstanceName(instanceName)
                                        .orElseThrow(() ->
                                                new IllegalArgumentException(
                                                        "Unknown WhatsApp instance."
                                                )
                                        );

                        if (!connection.getIntegration().isEnabled()) {
                            return false;
                        }

                        if (connection.getStatus()
                                == WhatsAppConnectionStatus.CONFLICT
                                || connection.getStatus()
                                == WhatsAppConnectionStatus.FAILED) {
                            return false;
                        }

                        if (connection.getWhatsappJid() != null
                                && !connection.getWhatsappJid()
                                        .equals(normalizedJid)) {
                            return false;
                        }

                        connection.setWhatsappJid(normalizedJid);
                        connection.setStatus(
                                WhatsAppConnectionStatus.CONNECTED
                        );

                        connectionRepository.flush();

                        return true;
                    })
            );

        } catch (DataIntegrityViolationException exception) {

            transactionTemplate.executeWithoutResult(status -> {

                WhatsAppConnectionEntity connection =
                        connectionRepository
                                .findByInstanceName(instanceName)
                                .orElseThrow();

                connection.setStatus(
                        WhatsAppConnectionStatus.CONFLICT
                );

                connection.getIntegration().setEnabled(false);
            });

            return false;
        }
    }

    private String normalizeJid(String jid) {

        if (jid == null || jid.isBlank()) {
            throw new IllegalArgumentException(
                    "WhatsApp JID cannot be empty."
            );
        }

        String normalized = jid.trim()
                .toLowerCase(Locale.ROOT);

        if (!normalized.matches(
                "^[0-9]+@s\\.whatsapp\\.net$"
        )) {
            throw new IllegalArgumentException(
                    "Invalid WhatsApp account JID."
            );
        }

        return normalized;
    }
}
