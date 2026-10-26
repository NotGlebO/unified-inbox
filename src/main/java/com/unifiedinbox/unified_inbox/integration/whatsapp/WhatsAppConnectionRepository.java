
package com.unifiedinbox.unified_inbox.integration.whatsapp;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import java.util.Optional;

public interface WhatsAppConnectionRepository
        extends JpaRepository<WhatsAppConnectionEntity, Long> {

    @EntityGraph(attributePaths = "integration")
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
