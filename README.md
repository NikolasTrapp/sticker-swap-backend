# Sticker Swap Backend

Backend da plataforma Sticker Swap, uma aplicação para colecionadores gerenciarem figurinhas repetidas e desejadas, encontrarem outros usuários, conversarem e receberem notificações.

## Stack

- Java 25.
- Spring Boot 4.0.4.
- Gradle 9.5.
- PostgreSQL e Flyway.
- Spring Web MVC, Spring Data JPA, Spring Security, OAuth2/OIDC, Resource Server, WebSocket/STOMP, Mail, Validation e Actuator.
- springdoc-openapi.
- Micrometer tracing com Brave.
- Jib e Dockerfile para imagem.
- JUnit 5, Mockito, AssertJ e Testcontainers.

## Executando Localmente

```bash
./gradlew bootRun
```

O profile padrão é `local`. Ele espera PostgreSQL em `localhost:5432/stickerswap`.

Config local atual:

- usuário: `postgres`
- senha: `postgres`
- issuer/base pública: `http://localhost:8080`
- frontend login: `http://localhost:4200/login`
- reset de senha: `http://localhost:4200/password-reset`
- e-mail: `delivery-mode: log`

O `docker-compose.yml` deste repositório cria banco com usuário/senha `stickerswap/stickerswap`. Se usar esse compose, alinhe as credenciais via configuração local ou variáveis de ambiente.

## Comandos

```bash
./gradlew compileJava
./gradlew compileTestJava
./gradlew test
./gradlew bootRun
./gradlew bootJar
./gradlew jibDockerBuild
docker build -t sticker-swap-backend .
```

## Configuração De Produção

`application-prod.yml` lê variáveis:

- `DB_URL`, `DB_USER`, `DB_PASSWORD`.
- `ADMIN_EMAIL`, `ADMIN_PASSWORD`.
- `APP_SECURITY_ISSUER`.
- `APP_PUBLIC_BASE_URL`.
- `APP_PASSWORD_RESET_URL`.
- `APP_SECURITY_FRONTEND_LOGIN_URL`.
- `APP_SECURITY_JWK_SET_JSON`.
- `APP_SECURITY_CORS_ALLOWED_ORIGINS`.
- `OAUTH_WEB_REDIRECT_URIS`.
- `OAUTH_WEB_POST_LOGOUT_REDIRECT_URIS`.
- `APP_MAIL_FROM`, `APP_MAIL_DELIVERY_MODE`, `APP_MAIL_API_KEY`.

Providers de e-mail aceitos:

- `log`
- `brevo`
- `resend`

## Autenticação E Autorização

- OAuth2/OIDC com Authorization Code + PKCE.
- Access tokens JWT RS256.
- Claims no JWT: `sub` com UUID do usuário, `email`, `role`.
- Roles: `USER` e `ADMIN`.
- `/admin/**` exige `ROLE_ADMIN`.
- APIs protegidas exigem usuário ativo e e-mail confirmado.
- WebSocket valida bearer token no `STOMP CONNECT`.

O client público `sticker-swap-web` é registrado/atualizado no startup com redirect URIs configurados.

## Conta

Endpoints públicos:

- `POST /auth/register`.
- `POST /auth/email-confirmations`.
- `GET /auth/email-confirmations/confirm?token=...`.
- `GET /auth/email-confirmations/confirm?token=...&redirect=false`.
- `POST /auth/password-reset-requests`.
- `POST /auth/password-resets`.

Confirmação de e-mail por link redireciona para a tela frontend `email-confirmed`; a variante `redirect=false` retorna JSON.

Reset de senha revoga autorizações persistidas do usuário.

## APIs De Produto

Principais áreas:

- Catálogo: álbuns e figurinhas.
- Perfil: perfil próprio, CEP e perfil público.
- Coleção: repetidas, desejadas e visão consolidada.
- Busca: colecionadores que possuem uma figurinha.
- Chat: conversas e mensagens.
- Notificações.
- Moderação: bloqueio e denúncia.
- Admin: usuários, catálogo e denúncias.

Swagger local:

- `GET /swagger-ui.html`
- `GET /v3/api-docs`

Health:

- `GET /actuator/health`
- `GET /actuator/info`

## Tempo Real

- STOMP endpoint: `/ws`.
- Envio de mensagem: `/app/chat/{conversationId}/send`.
- Chat: `/topic/chat/{conversationId}`.
- Notificações: `/user/queue/notifications`.
- Eventos de segurança: `/user/queue/security`.

Quando um admin bloqueia um usuário, o backend revoga autorizações persistidas e publica evento de segurança para a sessão ativa.

## Migrations

Flyway usa `src/main/resources/db/migration`.

Migrations atuais:

- `V1`: identidade, tokens e perfis.
- `V2`: álbuns e figurinhas.
- `V3`: coleção do usuário.
- `V4`: chat.
- `V5`: moderação.
- `V6`: tabelas OAuth2 JDBC.
- `V7`: figurinhas 2026.
- `V8`: notificações.
- `V9`: último IP do usuário.

## Testes

```bash
./gradlew test
```

Os testes de integração usam Testcontainers e profile `test`.

Áreas cobertas:

- Segurança de conta.
- OAuth/JWK/Flyway.
- Admin de usuários.
- Busca com PostgreSQL.
- Chat, coleção, moderação, perfil, rate limiter e JWT.

## Deploy

Jib:

```bash
./gradlew jibDockerBuild
./gradlew jib
```

Dockerfile:

```bash
docker build -t sticker-swap-backend .
```

O container expõe `8080`. A configuração de produção deve vir por variáveis de ambiente.
