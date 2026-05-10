# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Stack

- Java 25, Spring Boot 4.0.4, Spring Framework 7.x
- Gradle (use `./gradlew`, not system `gradle`)
- PostgreSQL 16 (local via existing Docker container named `postgres`)
- Flyway migrations in `src/main/resources/db/migration/`
- springdoc-openapi 2.8.6 for Swagger UI

## Common commands

```bash
# Build (compile only)
./gradlew compileJava

# Run tests
./gradlew test

# Run application (requires Postgres running)
./gradlew bootRun

# Package JAR
./gradlew bootJar
```

## Local database setup

The project uses the existing `postgres` Docker container (already running on port 5432). Create the database once:

```bash
docker exec postgres psql -U postgres -c "CREATE USER stickerswap WITH PASSWORD 'stickerswap';"
docker exec postgres psql -U postgres -c "CREATE DATABASE stickerswap OWNER stickerswap;"
```

Default `application.yml` connects to `localhost:5432/stickerswap` with user/pass `stickerswap`. Override via env vars `DB_URL`, `DB_USER`, `DB_PASSWORD`.

## Verification endpoints

- `GET /actuator/health` — health check (public)
- `GET /swagger-ui.html` — Swagger UI (public)
- `GET /v3/api-docs` — OpenAPI JSON (public)

## Modular package structure

Root package: `br.com.stickerswap`

Modules (one package per domain, following the architecture in `../plano-implementacao-figurinhas-copa.md` §4.1):

| Package | Responsibility |
|---|---|
| `identity` | Authentication, authorization, users, roles |
| `profile` | Nickname, city/state, private CEP, public flags |
| `album` | Album and sticker catalog |
| `collection` | User's repeated and wanted stickers |
| `search` | Search and ranking of users by sticker |
| `chat` | Conversations, messages, WebSocket |
| `moderation` | Blocks and reports |
| `admin` | Admin-only APIs |
| `shared` | Error handling, security config, audit, utilities |

Modules communicate via service interfaces. Cross-module repository access is accepted within the monolith (e.g., `chat` accesses `StickerRepository` from `album`).

## Error format

All errors follow `ApiError` in `shared/error/`:

```json
{
  "timestamp": "...",
  "status": 400,
  "error": "Bad Request",
  "message": "...",
  "path": "/some/path",
  "fieldErrors": [{"field": "email", "message": "must not be blank"}]
}
```

## Auth flow

- `POST /auth/register` — `{"email","password"}` → 201 UserResponse
- Browser SPA login uses Authorization Code + PKCE:
  - `GET /oauth2/csrf` — returns CSRF token and sets `XSRF-TOKEN` cookie (handled by `OAuthBrowserAuthController`)
  - `POST /oauth2/login` — `{"email","password"}` with `X-XSRF-TOKEN` header and `withCredentials`; creates the browser session only (204 No Content)
  - `GET /oauth2/authorize` — browser redirect triggered by the SPA; requires the session established above
  - Backend redirects to frontend `/oauth/callback?code=…`
  - `POST /oauth2/token` exchanges the authorization code for tokens
- Use `Authorization: Bearer <token>` in subsequent API requests.
- Tokens are RS256 JWTs. For production, provide `APP_SECURITY_JWK_SET_JSON`; otherwise startup generates an ephemeral RSA key (key is lost on restart, invalidating all tokens).
- Default admin: `admin@stickerswap.com` / `changeme` (override via `ADMIN_EMAIL`/`ADMIN_PASSWORD`)
- JWT claims customized by `OAuthTokenCustomizerConfig`: `sub` = user UUID, `email`, `role`.

### OAuth2 client (seeder)

`OAuthClientSeeder` runs on startup and registers `sticker-swap-web` in the `oauth2_registered_client` JDBC table. **It skips seeding if the client already exists.** If you need to change redirect URIs in an existing environment, delete the row first:

```sql
DELETE FROM oauth2_registered_client WHERE client_id = 'sticker-swap-web';
```

Then restart the app to re-seed with current config values.

### Security filter chains (in order)

| Order | Name | Matcher | Purpose |
|---|---|---|---|
| 1 | `authorizationServerFilterChain` | OAuth2 endpoints | Spring Authorization Server |
| 2 | `loginFilterChain` | `/login`, `/logout`, `/oauth2/login`, `/oauth2/csrf` | Session-based credential exchange |
| 3 | `apiFilterChain` | `/**` | Stateless JWT resource server |

### PKCE / origin consistency

The SPA's `redirect_uri` must match exactly a URI registered in `oauth2_registered_client`. The OIDC library stores the PKCE verifier keyed to the SPA's browser origin (`window.location.origin`). **If the SPA origin and the `redirect_uri` differ** (e.g. user accesses `127.0.0.1:4200` but `redirectUrl` is `localhost:4200`), the code-exchange fails silently at the callback. Ensure all origins you use during development are registered as redirect URIs.

### Database: OAuth2 JDBC tables

Migration `V6` creates the three Spring Authorization Server JDBC tables:

- `oauth2_registered_client` — OAuth clients (populated by seeder)
- `oauth2_authorization` — per-request authorization state and issued tokens
- `oauth2_authorization_consent` — consent records

### Important backend env vars

- `APP_SECURITY_CORS_ALLOWED_ORIGINS` — comma-separated allowed origins (default: `localhost:4200,127.0.0.1:4200`)
- `OAUTH_WEB_REDIRECT_URIS` — comma-separated redirect URIs for `sticker-swap-web` client
- `OAUTH_WEB_POST_LOGOUT_REDIRECT_URIS` — post-logout redirect URIs
- `APP_SECURITY_FRONTEND_LOGIN_URL` — URL of the SPA login page (used as auth entry point; default: `http://localhost:4200/login`)
- `APP_PASSWORD_RESET_URL` — full URL of the SPA password-reset page
- `APP_SECURITY_ISSUER` — OAuth2 issuer URL (must match what the SPA uses as `authority`)

## Role-based access

- `ROLE_USER` — standard authenticated user
- `ROLE_ADMIN` — admin user; required for `/admin/**` endpoints
- JWT claim `role` → Spring Security `ROLE_<role>` authority

## WebSocket (Chat)

- STOMP endpoint: `/ws` (permitted in SecurityConfig, validated by `JwtChannelInterceptor`)
- Send: `STOMP CONNECT` with `Authorization: Bearer <token>` header, then publish to `/app/chat/{conversationId}/send`
- Subscribe: `/topic/chat/{conversationId}` for real-time messages

## Implementation phases

See `../plano-implementacao-figurinhas-copa.md` §7 for the full 9-phase roadmap. **Fases 0-8 complete.** Fase 9 (AWS deploy) is next.

## Known warnings

- Lombok `sun.misc.Unsafe` deprecation on Java 25 — benign, does not affect compilation.
- Mockito dynamic agent warning in tests — benign, tests pass normally.
