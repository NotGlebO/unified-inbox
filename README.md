# Unified Inbox

A Java/Spring Boot web application for managing customer conversations from multiple communication channels in one place.

> **Project status:** Portfolio project under active development. This is not a production-ready hosted service. Integrations require your own provider accounts and credentials.

## Features

- Organization-based inbox with conversations and messages.
- Telegram bot integration via webhooks.
- Gmail integration using Google OAuth 2.0.
- WhatsApp integration using a self-hosted Evolution API instance.
- File attachments stored in Backblaze B2 (S3-compatible object storage).
- PostgreSQL persistence and a Spring Boot/Thymeleaf web interface.

Some functionality is still under development; verify each integration independently before using it with real customer data.

## Technology stack

- Java 21, Spring Boot 4.1.1, Maven
- Spring MVC, Spring Security, OAuth 2.0, Thymeleaf
- Spring Data JPA / Hibernate, PostgreSQL
- AWS SDK for Java v2 (Backblaze B2 S3 API)
- Evolution API for WhatsApp connectivity

## Prerequisites

Install Java 21, Docker Desktop (or PostgreSQL installed locally), and Git. You will also need accounts/credentials for the integrations you intend to configure:

- A Google Cloud OAuth application for Gmail sign-in/authorization.
- A Telegram bot created using BotFather and a public HTTPS webhook endpoint.
- A Backblaze B2 bucket and application key with read/write access.
- A running Evolution API server for WhatsApp integration.

The repository's Spring Boot application does **not** automatically provision PostgreSQL, Evolution API, or a public HTTPS tunnel. The exact setup for those services depends on your environment.

## 1. Clone the repository

```powershell
git clone https://github.com/NotGlebO/unified-inbox.git
cd unified-inbox
```

## 2. Start PostgreSQL

If you don't have PostgreSQL already, you can start a local development container:

```powershell
docker run --name unified-inbox-postgres -e POSTGRES_USER=postgres -e POSTGRES_PASSWORD=change-me -e POSTGRES_DB=unified_inbox -p 5432:5432 -v unified-inbox-pgdata:/var/lib/postgresql/data -d postgres:15
```

Set `DB_PASSWORD=change-me` (or your own chosen password) in your local configuration. If a container with this name already exists, start it instead of running `docker run` again.

**Note:** This command is for local development. Use stronger credentials and a managed secret mechanism for deployments.

## 3. Configure application secrets

The committed `src/main/resources/application.properties` contains property placeholders such as:

```properties
b2.access-key=${B2_ACCESS_KEY}
b2.secret-key=${B2_SECRET_KEY}
```

`B2_ACCESS_KEY` and `B2_SECRET_KEY` are environment variable names, **not** literal credentials. Never commit actual credentials to Git.

### Option A: Windows PowerShell environment variables

Set variables in the same PowerShell session in which you start Spring Boot:

```powershell
$env:DB_PASSWORD="your-postgres-password"
$env:APP_ENCRYPTION_KEY="your-existing-valid-encryption-key"
$env:GOOGLE_CLIENT_ID="your-google-client-id"
$env:GOOGLE_CLIENT_SECRET="your-google-client-secret"
$env:B2_ACCESS_KEY="your-b2-key-id"
$env:B2_SECRET_KEY="your-b2-application-key"
$env:EVOLUTION_API_KEY="your-evolution-api-key"
$env:EVOLUTION_WEBHOOK_SECRET="your-webhook-secret"
$env:APP_PUBLIC_URL="https://your-public-https-domain"
```

Then start the app **from that same terminal**. VS Code launched earlier may not inherit newly configured permanent Windows variables; restart it if necessary.

### Option B: Local Spring profile (recommended for local development)

Create `src/main/resources/application-local.properties` (already ignored by `.gitignore`):

```properties
spring.datasource.password=your-postgres-password
app.encryption.key=your-existing-valid-encryption-key
spring.security.oauth2.client.registration.google.client-id=your-google-client-id
spring.security.oauth2.client.registration.google.client-secret=your-google-client-secret
b2.access-key=your-b2-key-id
b2.secret-key=your-b2-application-key
evolution.api.key=your-evolution-api-key
evolution.webhook.secret=your-webhook-secret
app.public-url=https://your-public-https-domain
```

Activate the profile when launching:

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"
```

**Important:** `application-local.properties` is not loaded unless the `local` profile is active. Keep existing encryption keys for existing encrypted data; changing the encryption key may prevent decryption of previously saved credentials.

### What is `.env.example`?

The root `.env.example` is a **reference checklist** for environment variables. Copying it to `.env` does not automatically load it into Spring Boot. Use Option A or B above, or explicitly configure your deployment environment to load an `.env` file.

## 4. Environment variable reference

| Variable | Purpose | Required / default |
| --- | --- | --- |
| `DB_URL` | JDBC connection URL | Optional; defaults to `jdbc:postgresql://localhost:5432/unified_inbox` |
| `DB_USERNAME` | PostgreSQL user | Optional; defaults to `postgres` |
| `DB_PASSWORD` | PostgreSQL password | Required |
| `PORT` | Application HTTP port | Optional; defaults to `8080` |
| `APP_ENCRYPTION_KEY` | Encrypts integration credentials | Required; format must match the application's encryption service |
| `GOOGLE_CLIENT_ID` | Google OAuth client ID | Required by current Google OAuth configuration |
| `GOOGLE_CLIENT_SECRET` | Google OAuth client secret | Required by current Google OAuth configuration |
| `APP_PUBLIC_URL` | Public base URL for webhook callbacks | Defaults to `http://localhost:8080`; use public HTTPS for real webhooks |
| `B2_ENDPOINT` | S3-compatible B2 endpoint | Defaults to `https://s3.eu-central-003.backblazeb2.com` |
| `B2_REGION` | B2 bucket region | Defaults to `eu-central-003` |
| `B2_BUCKET` | B2 bucket name | Defaults to `unified-inbox-attachments` |
| `B2_ACCESS_KEY` | Backblaze application key ID | Required |
| `B2_SECRET_KEY` | Backblaze application key secret | Required |
| `EVOLUTION_API_URL` | Evolution API base URL | Defaults to `http://localhost:8081` |
| `EVOLUTION_API_KEY` | Evolution API authentication key | Required |
| `EVOLUTION_WEBHOOK_SECRET` | Secret for validating WhatsApp webhooks | Required |
| `EVOLUTION_WEBHOOK_URL` | Webhook endpoint configured in Evolution API | Defaults to `http://host.docker.internal:8080/api/webhooks/whatsapp` |

**Do not** use someone else's bucket, Google OAuth credentials, encryption key, or Evolution API key. Create your own. The default B2 bucket name and region are developer-specific examples; override them for your own Backblaze account.

## 5. Set up integrations

### Backblaze B2

1. Create a private B2 bucket in your chosen region.
2. Create a restricted application key with read/write permissions for that bucket.
3. Set `B2_ENDPOINT`, `B2_REGION`, `B2_BUCKET`, `B2_ACCESS_KEY`, and `B2_SECRET_KEY` to match your account.
4. Start the app and verify that a newly received attachment can be uploaded and subsequently opened.

### Google / Gmail

1. Create a Google Cloud project and configure its OAuth consent screen.
2. Create an OAuth 2.0 web client and supply `GOOGLE_CLIENT_ID` and `GOOGLE_CLIENT_SECRET`.
3. Configure the authorized redirect URI to match Spring Security's callback for your deployment, typically `{baseUrl}/login/oauth2/code/google`; confirm against your security configuration.
4. Enable the Gmail API and configure the necessary scopes/consent/test users according to Google's current policies.
5. Connect Gmail through the app's integrations interface.

### Telegram

1. Create your own Telegram bot using BotFather.
2. Make the app reachable over HTTPS (e.g., with a development tunnel) and set `APP_PUBLIC_URL`.
3. Use the application's Telegram integration flow to register/connect the bot and configure its webhook.
4. Send a message to the bot and confirm it appears in the inbox.

### WhatsApp / Evolution API

1. Run a compatible Evolution API instance separately, along with its required database and Redis configuration.
2. Set `EVOLUTION_API_URL`, `EVOLUTION_API_KEY`, `EVOLUTION_WEBHOOK_SECRET`, and `EVOLUTION_WEBHOOK_URL`.
3. Make sure Evolution API can reach the webhook URL. `host.docker.internal` is appropriate only for certain local Docker Desktop setups; use a reachable public URL or correct internal hostname elsewhere.
4. Open the app's WhatsApp integration and scan the QR code using your test WhatsApp account.

Evolution API uses a WhatsApp Web-based connection; its behavior, compatibility, and operational risks differ from the official Meta Cloud API. Do not assume this setup is suitable for production or compliant with every provider policy.

## 6. Run the application

```powershell
.\mvnw.cmd spring-boot:run
```

If using the local profile, use the command shown in Option B instead.

Open <http://localhost:8080> (or the configured `PORT`).

## 7. Run tests

```powershell
.\mvnw.cmd test
```

Some tests may depend on real external services or test credentials. Review the test configuration before running integration tests; do not point destructive tests at production resources.

## 8. Troubleshooting

- **`Could not resolve placeholder ...`**: A required environment variable is missing or the `local` profile was not activated.
- **B2 `403 Signature validation failed`**: Check the actual key used by the running process, key permissions, bucket region, endpoint, and any environment-variable overrides.
- **Application starts in terminal but not VS Code**: Restart VS Code or launch from the terminal that contains your environment variables.
- **Webhooks not arriving**: Confirm public HTTPS URL, tunnel status, provider webhook settings, and network reachability.
- **Image message without an attachment**: Check `message_attachments`, webhook processing, and B2 upload errors.

## Security and project limitations

- Never commit `.env`, `application-local.properties`, access keys, tokens, or credentials.
- Previously exposed credentials must be revoked/rotated; removing them from the latest commit does not erase Git history.
- Database schema currently uses `spring.jpa.hibernate.ddl-auto=update`; migrations and production deployment hardening are future work.
- This README describes a manual local setup. A verified one-command Docker Compose setup for all dependencies is not yet included.

## License

No license has been selected yet. Until one is added, do not assume the repository grants permission to redistribute or reuse the code.
