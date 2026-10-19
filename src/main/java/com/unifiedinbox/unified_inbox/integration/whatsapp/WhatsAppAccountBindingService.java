
package com.unifiedinbox.unified_inbox.integration.whatsapp;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Locale;

@Service
public class WhatsAppAccountBindingService {

    private final WhatsAppConnectionRepository repository;
    private final TransactionTemplate transactions;

    public WhatsAppAccountBindingService(
            WhatsAppConnectionRepository repository,
            TransactionTemplate transactions
    ) {
        this.repository = repository;
        this.transactions = transactions;
    }

    public WhatsAppBindingResult bindAccount(
            String instanceName,
            String whatsappJid
    ) {
        String jid = normalizeJid(whatsappJid);

        try {
            return transactions.execute(tx -> {
                WhatsAppConnectionEntity connection = repository
                        .findByInstanceName(instanceName)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Unknown WhatsApp instance"
                                )
                        );

                if (!connection.getIntegration().isEnabled()
                        || connection.getStatus() == WhatsAppConnectionStatus.FAILED
                        || connection.getStatus() == WhatsAppConnectionStatus.CONFLICT) {
                    return WhatsAppBindingResult.REJECTED;
                }

                if (connection.getWhatsappJid() != null
                        && !connection.getWhatsappJid().equals(jid)) {
                    return WhatsAppBindingResult.REJECTED;
                }

                var existing = repository.findByWhatsappJid(jid);

                if (existing.isPresent()
                        && !existing.get().getId().equals(connection.getId())) {
                    return WhatsAppBindingResult.CONFLICT;
                }

                connection.setWhatsappJid(jid);
                connection.setStatus(WhatsAppConnectionStatus.CONNECTED);

                repository.flush();

                return WhatsAppBindingResult.CONNECTED;
            });

        } catch (DataIntegrityViolationException exception) {
            // Another transaction may have claimed this JID
            // between our lookup and the database write.
            boolean jidTaken = Boolean.TRUE.equals(
                    transactions.execute(tx ->
                            repository.findByWhatsappJid(jid)
                                    .map(existing ->
                                            !existing.getInstanceName()
                                                    .equals(instanceName))
                                    .orElse(false)
                    )
            );

            if (jidTaken) {
                return WhatsAppBindingResult.CONFLICT;
            }

            // A different database constraint failed.
            // Do not misreport it as a WhatsApp conflict.
            throw exception;
        }
    }

    private String normalizeJid(String jid) {
        if (jid == null || jid.isBlank()) {
            throw new IllegalArgumentException(
                    "WhatsApp JID cannot be empty"
            );
        }

        String normalized = jid.trim()
                .toLowerCase(Locale.ROOT);

        if (!normalized.matches("^[0-9]+@s\\.whatsapp\\.net$")) {
            throw new IllegalArgumentException(
                    "Invalid WhatsApp account JID"
            );
        }

        return normalized;
    }

        
    public void markConflict(String instanceName) {
        transactions.executeWithoutResult(tx -> {
            WhatsAppConnectionEntity connection = repository
                    .findByInstanceName(instanceName)
                    .orElseThrow();

            connection.setStatus(
                    WhatsAppConnectionStatus.CONFLICT
            );

            connection.getIntegration().setEnabled(false);

            repository.flush();
        });
    }

}
