
package com.unifiedinbox.unified_inbox.integration.telegram;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class TelegramIntegrationController {

    private final TelegramIntegrationService telegramService;

    public TelegramIntegrationController(
            TelegramIntegrationService telegramService
    ) {
        this.telegramService = telegramService;
    }

    @PostMapping("/organizations/{id}/integrations/telegram")
    public String connectTelegram(
            @PathVariable Long id,
            @RequestParam String token,
            RedirectAttributes redirectAttributes
    ) {
        try {
            telegramService.connect(id, token);

            redirectAttributes.addFlashAttribute(
                    "success",
                    "Telegram bot connected successfully."
            );

        } catch (IllegalArgumentException exception) {

            redirectAttributes.addFlashAttribute(
                    "error",
                    exception.getMessage()
            );

        } catch (DataIntegrityViolationException exception) {

            redirectAttributes.addFlashAttribute(
                    "error",
                    "This Telegram bot is already connected."
            );

        } catch (IllegalStateException exception) {

            redirectAttributes.addFlashAttribute(
                    "error",
                    "Could not connect to Telegram. Please try again."
            );
        }

        return "redirect:/organizations/" + id + "/settings";
    }
}
