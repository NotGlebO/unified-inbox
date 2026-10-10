
package com.unifiedinbox.unified_inbox.integration.gmail;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class GmailOAuthController {

    @GetMapping("/gmail/connect")
    public String connectGmail() {
        return "redirect:/oauth2/authorization/google";
    }
}
