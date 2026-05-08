# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Stack

- Java 25, Spring Boot 4.0.4, Spring Framework 7.x
- Maven (use `./mvnw`, not system `mvn`)
- PostgreSQL 16 (local via existing Docker container named `postgres`)
- Flyway migrations in `src/main/resources/db/migration/`
- springdoc-openapi 2.8.6 for Swagger UI

## Common commands

```bash
# Build (compile only)
./mvnw compile

# Run tests
./mvnw test

# Run application (requires Postgres running)
./mvnw spring-boot:run

# Package JAR
./mvnw package -DskipTests
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
  - `GET /oauth2/csrf` — returns CSRF token and sets `XSRF-TOKEN`
  - `POST /oauth2/login` — `{"email","password"}` with `X-XSRF-TOKEN` and credentials; creates the browser session only
  - `GET /oauth2/authorize` → frontend `/oauth/callback`
  - `POST /oauth2/token` exchanges the authorization code for tokens
- Use `Authorization: Bearer <token>` in subsequent API requests.
- Tokens are RS256 JWTs. For production, provide `APP_SECURITY_JWK_SET_JSON`; otherwise startup generates an ephemeral RSA key.
- Default admin: `admin@stickerswap.com` / `changeme` (override via `ADMIN_EMAIL`/`ADMIN_PASSWORD`)

Important frontend env vars:

- `APP_SECURITY_CORS_ALLOWED_ORIGINS`
- `OAUTH_WEB_REDIRECT_URIS`
- `OAUTH_WEB_POST_LOGOUT_REDIRECT_URIS`
- `APP_PASSWORD_RESET_URL`

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
