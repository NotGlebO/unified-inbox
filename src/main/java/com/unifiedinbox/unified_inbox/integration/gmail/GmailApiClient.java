package com.unifiedinbox.unified_inbox.integration.gmail;

import com.unifiedinbox.unified_inbox.core.adapter.MessageSendException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

@Component
public class GmailApiClient {

    private static final String TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final String GMAIL_API_BASE = "https://gmail.googleapis.com/gmail/v1/users/me";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String clientId;
    private final String clientSecret;

    public GmailApiClient(
            ObjectMapper objectMapper,
            @Value("${spring.security.oauth2.client.registration.google.client-id}") String clientId,
            @Value("${spring.security.oauth2.client.registration.google.client-secret}") String clientSecret
    ) {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = objectMapper;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    public TokenResponse exchangeCodeForTokens(String authorizationCode, String redirectUri) {
        try {
            String body = "code=" + URLEncoder.encode(authorizationCode, StandardCharsets.UTF_8)
                    + "&client_id=" + URLEncoder.encode(clientId, StandardCharsets.UTF_8)
                    + "&client_secret=" + URLEncoder.encode(clientSecret, StandardCharsets.UTF_8)
                    + "&redirect_uri=" + URLEncoder.encode(redirectUri, StandardCharsets.UTF_8)
                    + "&grant_type=authorization_code";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(TOKEN_URL))
                    .timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new MessageSendException(
                        MessageSendException.FailureType.PERMANENT,
                        "Google OAuth2 token exchange failed: HTTP " + response.statusCode() + " " + response.body()
                );
            }

            JsonNode root = objectMapper.readTree(response.body());
            return new TokenResponse(
                    root.path("access_token").asString(""),
                    root.path("refresh_token").asString(""),
                    root.path("expires_in").asLong(0),
                    root.path("scope").asString("")
            );
        } catch (MessageSendException e) {
            throw e;
        } catch (Exception e) {
            throw new MessageSendException(
                    MessageSendException.FailureType.UNKNOWN,
                    "Failed to exchange authorization code for tokens",
                    e
            );
        }
    }

    public AccessTokenResponse refreshAccessToken(String refreshToken) {
        try {
            String body = "refresh_token=" + URLEncoder.encode(refreshToken, StandardCharsets.UTF_8)
                    + "&client_id=" + URLEncoder.encode(clientId, StandardCharsets.UTF_8)
                    + "&client_secret=" + URLEncoder.encode(clientSecret, StandardCharsets.UTF_8)
                    + "&grant_type=refresh_token";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(TOKEN_URL))
                    .timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new MessageSendException(
                        MessageSendException.FailureType.PERMANENT,
                        "Google OAuth2 token refresh failed: HTTP " + response.statusCode() + " " + response.body()
                );
            }

            JsonNode root = objectMapper.readTree(response.body());
            return new AccessTokenResponse(
                    root.path("access_token").asString(""),
                    root.path("expires_in").asLong(0)
            );
        } catch (MessageSendException e) {
            throw e;
        } catch (Exception e) {
            throw new MessageSendException(
                    MessageSendException.FailureType.UNKNOWN,
                    "Failed to refresh Google access token",
                    e
            );
        }
    }

    public String getUserInfo(String accessToken) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://www.googleapis.com/oauth2/v2/userinfo"))
                    .timeout(Duration.ofSeconds(10))
                    .header("Authorization", "Bearer " + accessToken)
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new MessageSendException(
                        MessageSendException.FailureType.PERMANENT,
                        "Failed to fetch Google user info: HTTP " + response.statusCode()
                );
            }

            JsonNode root = objectMapper.readTree(response.body());
            return root.path("email").asString("");
        } catch (MessageSendException e) {
            throw e;
        } catch (Exception e) {
            throw new MessageSendException(
                    MessageSendException.FailureType.UNKNOWN,
                    "Failed to fetch Google user info",
                    e
            );
        }
    }

    public String sendMessage(String accessToken, String to, String subject, String bodyText) {
        try {
            String emailMessage = buildMimeMessage(to, subject, bodyText);
            String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(emailMessage.getBytes(StandardCharsets.UTF_8));

            String jsonBody = "{\"raw\": \"" + raw + "\"}";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(GMAIL_API_BASE + "/messages/send"))
                    .timeout(Duration.ofSeconds(15))
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                String errorDescription = extractErrorDescription(response.body());
                boolean retryable = response.statusCode() == 429 || response.statusCode() >= 500;
                throw new MessageSendException(
                        retryable ? MessageSendException.FailureType.RETRYABLE : MessageSendException.FailureType.UNKNOWN,
                        "Gmail API send failed: " + errorDescription,
                        (long) (retryable ? 30 : 0)
                );
            }

            JsonNode root = objectMapper.readTree(response.body());
            return root.path("id").asString("");
        } catch (MessageSendException e) {
            throw e;
        } catch (Exception e) {
            throw new MessageSendException(
                    MessageSendException.FailureType.UNKNOWN,
                    "Failed to send email via Gmail API",
                    e
            );
        }
    }

    public String getProfile(String accessToken) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(GMAIL_API_BASE + "/profile"))
                    .timeout(Duration.ofSeconds(10))
                    .header("Authorization", "Bearer " + accessToken)
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new MessageSendException(
                        MessageSendException.FailureType.PERMANENT,
                        "Failed to fetch Gmail profile: HTTP " + response.statusCode()
                );
            }

            JsonNode root = objectMapper.readTree(response.body());
            return root.path("emailAddress").asString("");
        } catch (MessageSendException e) {
            throw e;
        } catch (Exception e) {
            throw new MessageSendException(
                    MessageSendException.FailureType.UNKNOWN,
                    "Failed to fetch Gmail profile",
                    e
            );
        }
    }

    private String buildMimeMessage(String to, String subject, String bodyText) {
        StringBuilder sb = new StringBuilder();
        sb.append("From: me\r\n");
        sb.append("To: ").append(to).append("\r\n");
        sb.append("Subject: ").append(subject).append("\r\n");
        sb.append("MIME-Version: 1.0\r\n");
        sb.append("Content-Type: text/plain; charset=UTF-8\r\n");
        sb.append("Content-Transfer-Encoding: 7bit\r\n");
        sb.append("\r\n");
        sb.append(bodyText);
        return sb.toString();
    }

    private String extractErrorDescription(String body) {
        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode error = root.path("error");
            if (!error.isMissingNode()) {
                return error.path("message").asString("Unknown Gmail API error");
            }
        } catch (Exception ignored) {
        }
        return body;
    }

    public record TokenResponse(
        String accessToken,
        String refreshToken,
        long expiresIn,
        String scope
    ) {}

    public record AccessTokenResponse(
            String accessToken,
            long expiresIn
    ) {}

    
    public void revokeToken(String refreshToken) {
        try {
            String body = "token=" + URLEncoder.encode(
                    refreshToken,
                    StandardCharsets.UTF_8
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://oauth2.googleapis.com/revoke"))
                    .timeout(Duration.ofSeconds(10))
                    .header(
                            "Content-Type",
                            "application/x-www-form-urlencoded"
                    )
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            int status = response.statusCode();

            if (status == 200 || status == 400) {
                return;
            }

            throw new IllegalStateException(
                    "Google token revocation failed: HTTP " + status
            );

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(
                    "Google token revocation interrupted", e
            );
        } catch (java.io.IOException e) {
            throw new IllegalStateException(
                    "Could not contact Google OAuth API", e
            );
        }
    }


}
