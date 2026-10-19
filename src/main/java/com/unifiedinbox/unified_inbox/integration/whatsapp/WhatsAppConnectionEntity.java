
package com.unifiedinbox.unified_inbox.integration.whatsapp;

import com.unifiedinbox.unified_inbox.entity.IntegrationEntity;
import jakarta.persistence.*;


@Entity
@Table(
    name = "whatsapp_connections",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_whatsapp_instance",
            columnNames = "instance_name"
        ),
        @UniqueConstraint(
            name = "uk_whatsapp_integration",
            columnNames = "integration_id"
        )
    }
)
public class WhatsAppConnectionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "integration_id",
        nullable = false
    )
    private IntegrationEntity integration;

    @Column(name = "instance_name", nullable = false)
    private String instanceName;

    @Column(name = "whatsapp_jid")
    private String whatsappJid;

    public String getWhatsappJid() {
        return whatsappJid;
    }

    public void setWhatsappJid(String whatsappJid) {
        this.whatsappJid = whatsappJid;
    }

    


    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WhatsAppConnectionStatus status =
            WhatsAppConnectionStatus.CREATING;

    protected WhatsAppConnectionEntity() {
    }

    public WhatsAppConnectionEntity(
            IntegrationEntity integration,
            String instanceName
    ) {
        this.integration = integration;
        this.instanceName = instanceName;
    }

    public Long getId() {
        return id;
    }

    public IntegrationEntity getIntegration() {
        return integration;
    }

    public String getInstanceName() {
        return instanceName;
    }

    public WhatsAppConnectionStatus getStatus() {
        return status;
    }

    public void setStatus(WhatsAppConnectionStatus status) {
        this.status = status;
    }

}
