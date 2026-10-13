
package com.unifiedinbox.unified_inbox.integration.telegram;

import com.unifiedinbox.unified_inbox.entity.IntegrationEntity;
import com.unifiedinbox.unified_inbox.entity.OrganizationEntity;
import com.unifiedinbox.unified_inbox.repository.IntegrationRepository;
import com.unifiedinbox.unified_inbox.service.EncryptionService;
import com.unifiedinbox.unified_inbox.service.OrganizationService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TelegramIntegrationService {

    private final TelegramApiClient telegramApiClient;
    private final TelegramCredentialsRepository credentialsRepository;
    private final IntegrationRepository integrationRepository;
    private final OrganizationService organizationService;
    private final EncryptionService encryptionService;

    public TelegramIntegrationService(
            TelegramApiClient telegramApiClient,
            TelegramCredentialsRepository credentialsRepository,
            IntegrationRepository integrationRepository,
            OrganizationService organizationService,
            EncryptionService encryptionService
    ) {
        this.telegramApiClient = telegramApiClient;
        this.credentialsRepository = credentialsRepository;
        this.integrationRepository = integrationRepository;
        this.organizationService = organizationService;
        this.encryptionService = encryptionService;
    }

    @Transactional
    public void connect(Long organizationId, String token) {

        OrganizationEntity organization = organizationService
                .requireOwner(organizationId)
                .getOrganization();

        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException(
                    "Telegram bot token is required."
            );
        }

        String normalizedToken = token.trim();

        TelegramApiClient.TelegramBotInfo bot =
                telegramApiClient.getMe(normalizedToken);

        if (bot.id() == null ||
                bot.id() <= 0 ||
                bot.username() == null ||
                bot.username().isBlank()) {
            throw new IllegalArgumentException(
                    "Telegram returned invalid bot information."
            );
        }

        if (credentialsRepository.findByBotId(bot.id()).isPresent()) {
            throw new IllegalArgumentException(
                    "This Telegram bot is already connected."
            );
        }

        String encryptedToken =
                encryptionService.encrypt(normalizedToken);

        IntegrationEntity integration = new IntegrationEntity(
                organization,
                "TELEGRAM",
                "@" + bot.username(),
                "/icons/telegram.svg"
        );

        integrationRepository.save(integration);

        TelegramCredentialsEntity credentials =
                new TelegramCredentialsEntity(
                        integration,
                        bot.id(),
                        bot.username(),
                        encryptedToken
                );

        credentialsRepository.save(credentials);
    }

    @Transactional(readOnly = true)
    public List<String> getConnectedBots(Long organizationId) {

        organizationService.requireMember(organizationId);

        return integrationRepository
                .findByOrganizationId(organizationId)
                .stream()
                .filter(integration ->
                        "TELEGRAM".equals(integration.getType()))
                .map(IntegrationEntity::getDisplayName)
                .toList();
    }
}
