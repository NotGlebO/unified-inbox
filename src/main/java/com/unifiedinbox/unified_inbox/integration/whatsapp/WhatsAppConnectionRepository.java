
package com.unifiedinbox.unified_inbox.integration.whatsapp;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;

public interface WhatsAppConnectionRepository
        extends JpaRepository<WhatsAppConnectionEntity, Long> {

    Optional<WhatsAppConnectionEntity> findByInstanceName(
            String instanceName
    );

    Optional<WhatsAppConnectionEntity> findByIntegrationId(
            Long integrationId
    );


    List<WhatsAppConnectionEntity> findByIntegrationOrganizationId(
            Long organizationId
    );

    Optional<WhatsAppConnectionEntity> findByWhatsappJid(
            String whatsappJid
    );

}
