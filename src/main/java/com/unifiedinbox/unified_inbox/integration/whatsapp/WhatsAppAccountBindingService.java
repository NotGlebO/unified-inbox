
package com.unifiedinbox.unified_inbox.integration.whatsapp;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class WhatsAppAccountBindingService {
    private static final Logger log =
        LoggerFactory.getLogger(WhatsAppAccountBindingService.class);

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
                log.info(
                "[WA-BIND] START instance={}, jidHash={}",
                instanceName,
                Integer.toHexString(jid.hashCode())
        );

        try {
            return transactions.execute(tx -> {
                WhatsAppConnectionEntity connection = repository
                        .findByInstanceName(instanceName)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Unknown WhatsApp instance"
                                )
                        );

                log.info(
                    "[WA-BIND] NEW connectionId={}, integrationId={}, enabled={}, status={}, hasJid={}",
                    connection.getId(),
                    connection.getIntegration().getId(),
                    connection.getIntegration().isEnabled(),
                    connection.getStatus(),
                    connection.getWhatsappJid() != null
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
                log.warn(
                        "[WA-DIAG] NEW id={}, instance={}, status={}; OLD found={}",
                        connection.getId(),
                        instanceName,
                        connection.getStatus(),
                        existing.isPresent()
                );

                existing.ifPresent(old -> log.warn(
                        "[WA-DIAG] OLD id={}, integrationId={}, enabled={}, status={}",
                        old.getId(),
                        old.getIntegration().getId(),
                        old.getIntegration().isEnabled(),
                        old.getStatus()
                ));

                if (existing.isPresent()
                        && !existing.get().getId().equals(connection.getId())) {

                    WhatsAppConnectionEntity oldConnection = existing.get();

                    boolean isActive =
                            oldConnection.getIntegration().isEnabled()
                            && oldConnection.getStatus() != WhatsAppConnectionStatus.DISCONNECTED
                            && oldConnection.getStatus() != WhatsAppConnectionStatus.FAILED
                            && oldConnection.getStatus() != WhatsAppConnectionStatus.CONFLICT;

                    if (isActive) {
                        return WhatsAppBindingResult.CONFLICT;
                    }

                    // Освобождаем WhatsApp JID старого подключения.
                    oldConnection.setWhatsappJid(null);
                    repository.flush();
                }

                if (existing.isPresent()
                        && !existing.get().getId().equals(connection.getId())) {
                    return WhatsAppBindingResult.CONFLICT;
                }

                connection.setWhatsappJid(jid);
                connection.setStatus(WhatsAppConnectionStatus.CONNECTED);

                connection.setErrorMessage(null);

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

        
    public void markConflict(
            String instanceName,
            String reason
    ) {
        transactions.executeWithoutResult(tx -> {
            WhatsAppConnectionEntity connection = repository
                    .findByInstanceName(instanceName)
                    .orElseThrow();

            connection.setStatus(WhatsAppConnectionStatus.CONFLICT);
            connection.setErrorMessage(reason);
            connection.getIntegration().setEnabled(false);

            repository.flush();
        });
    }

    public String describeConflict(String whatsappJid) {
    String jid = normalizeJid(whatsappJid);

    return repository.findByWhatsappJid(jid)
            .map(existing ->
                    "This WhatsApp account is already connected "
                    + "to integration #"
                    + existing.getIntegration().getId()
                    + ". Disconnect the existing integration "
                    + "before connecting this account again."
            )
            .orElse(
                    "This WhatsApp account is already in use. "
                    + "Please check existing integrations."
            );
}

    
    public void markDisconnected(String instanceName) {
        transactions.executeWithoutResult(tx -> {
            WhatsAppConnectionEntity connection = repository
                    .findByInstanceName(instanceName)
                    .orElseThrow();

            // Do not change terminal or blocked connections.
            if (!connection.getIntegration().isEnabled()
                    || connection.getStatus() == WhatsAppConnectionStatus.CONFLICT
                    || connection.getStatus() == WhatsAppConnectionStatus.FAILED) {
                return;
            }

            if (connection.getStatus() == WhatsAppConnectionStatus.CONNECTED) {
                connection.setStatus(
                        WhatsAppConnectionStatus.DISCONNECTED
                );
            }
        });
    }


}
