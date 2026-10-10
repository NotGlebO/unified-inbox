package com.unifiedinbox.unified_inbox.controller;

import com.unifiedinbox.unified_inbox.integration.whatsapp.WhatsAppService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/whatsapp")
public class WhatsAppController {

    private final WhatsAppService whatsAppService;

    public WhatsAppController(WhatsAppService whatsAppService) {
        this.whatsAppService = whatsAppService;
    }

    @PostMapping("/test")
    public String testMessage(@RequestBody TestMessageRequest request) {

        whatsAppService.sendMessage(
                request.phone(),
                request.text()
        );

        return "Запрос на отправку принят WhatsApp API";
    }

    public record TestMessageRequest(String phone, String text) {}
}