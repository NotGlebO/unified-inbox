package com.unifiedinbox.unified_inbox.controller;

import com.unifiedinbox.unified_inbox.integration.whatsapp.WhatsAppWebhookService;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/webhooks/whatsapp")
public class WhatsAppWebhookController {
    private final WhatsAppWebhookService webhookService;

    public WhatsAppWebhookController(
            WhatsAppWebhookService webhookService
    ) {
        this.webhookService = webhookService;
    }
    @Value("${whatsapp.app-secret}")
    private String appSecret;

    @Value("${whatsapp.webhook-verify-token}")
    private String verifyToken;

    @GetMapping
    public ResponseEntity<String> verifyWebhook(
            @RequestParam("hub.mode") String mode,
            @RequestParam("hub.verify_token") String token,
            @RequestParam("hub.challenge") String challenge) {

        if ("subscribe".equals(mode) && verifyToken.equals(token)) {
            return ResponseEntity.ok(challenge);
        }

        return ResponseEntity.status(403).body("Verification failed");
    }
    @PostMapping
    public ResponseEntity<String> receiveWebhook(
            @RequestBody String body,
            @RequestHeader(
                    value = "X-Hub-Signature-256",
                    required = false
            ) String signature
    ) {
        try {
            if (signature == null || !signature.startsWith("sha256=")) {
                return ResponseEntity.status(403).body("Invalid signature");
            }

            Mac mac = Mac.getInstance("HmacSHA256");

            mac.init(new SecretKeySpec(
                    appSecret.getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256"
            ));

            byte[] expected = mac.doFinal(
                    body.getBytes(StandardCharsets.UTF_8)
            );

            byte[] received = HexFormat.of()
                    .parseHex(signature.substring(7));

            if (!MessageDigest.isEqual(expected, received)) {
                return ResponseEntity.status(403).body("Invalid signature");
            }

            System.out.println("Verified WhatsApp webhook received");

            webhookService.processWebhook(body);

            return ResponseEntity.ok("EVENT_RECEIVED");

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body("Webhook processing failed");
        }
    }
}