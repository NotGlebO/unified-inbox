
package com.unifiedinbox.unified_inbox.integration.gmail;

import com.unifiedinbox.unified_inbox.entity.GmailAccountEntity;
import com.unifiedinbox.unified_inbox.service.TokenEncryptionService;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Service
public class GmailClientService {

    private final TokenEncryptionService encryptionService;
    private final RestClient restClient;

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String clientId;

    @Value("${spring.security.oauth2.client.registration.google.client-secret}")
    private String clientSecret;

    public GmailClientService(
            TokenEncryptionService encryptionService
    ) {
        this.encryptionService = encryptionService;
        this.restClient = RestClient.create();
    }

    // Exchange refresh token for a short-lived access token
    public String getAccessToken(GmailAccountEntity account) {

        String refreshToken = encryptionService.decrypt(
                account.getEncryptedRefreshToken()
        );

        TokenResponse response = restClient.post()
                .uri("https://oauth2.googleapis.com/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(
                        "client_id=" + encode(clientId)
                        + "&client_secret=" + encode(clientSecret)
                        + "&refresh_token=" + encode(refreshToken)
                        + "&grant_type=refresh_token"
                )
                .retrieve()
                .body(TokenResponse.class);

        if (response == null || response.access_token() == null) {
            throw new IllegalStateException(
                    "Failed to refresh Gmail access token"
            );
        }

        return response.access_token();
    }

    // Fetch recent inbox message IDs
    public GmailMessageList getInboxMessages(
            GmailAccountEntity account
    ) {
        String accessToken = getAccessToken(account);

        GmailMessageList result = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .scheme("https")
                        .host("gmail.googleapis.com")
                        .path("/gmail/v1/users/me/messages")
                        .queryParam("labelIds", "INBOX")
                        .queryParam("maxResults", 20)
                        .build())
                .headers(headers ->
                        headers.setBearerAuth(accessToken))
                .retrieve()
                .body(GmailMessageList.class);

        return result == null
                ? new GmailMessageList(List.of(), null)
                : result;
    }

    // Fetch one full Gmail message
    public Map getMessage(
            GmailAccountEntity account,
            String messageId
    ) {
        String accessToken = getAccessToken(account);

        return restClient.get()
                .uri(
                        "https://gmail.googleapis.com/gmail/v1/users/me/messages/{id}?format=full",
                        messageId
                )
                .headers(headers ->
                        headers.setBearerAuth(accessToken))
                .retrieve()
                .body(Map.class);
    }

    private String encode(String value) {
        return java.net.URLEncoder.encode(
                value,
                java.nio.charset.StandardCharsets.UTF_8
        );
    }

    public record TokenResponse(
            String access_token,
            String token_type,
            Integer expires_in
    ) {}

    public record GmailMessageList(
            List<GmailMessageReference> messages,
            String nextPageToken
    ) {}

    public record GmailMessageReference(
            String id,
            String threadId
    ) {}
}
