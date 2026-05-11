# AGENTS.md

Instruções para agentes trabalhando no backend Sticker Swap.

## Fonte Da Verdade

Use o código atual como fonte de verdade. O plano e os requisitos na raiz do workspace ajudam a entender o produto, mas o comportamento vigente está nos controllers, serviços, migrations, testes e configurações deste repositório.

## Produto

Sticker Swap é uma plataforma para colecionadores encontrarem pessoas com figurinhas repetidas, compararem interesses e iniciarem conversas. O sistema não processa pagamento, entrega, reserva de figurinha nem confirmação de troca física.

## Stack

- Java 25.
- Spring Boot 4.0.4 / Spring Framework 7.
- Gradle 9.5 via `./gradlew`.
- PostgreSQL.
- Flyway em `src/main/resources/db/migration`.
- Spring Web MVC, Spring Data JPA, Spring Security, OAuth2/OIDC, Resource Server, WebSocket/STOMP, Validation, Thymeleaf e Actuator.
- Micrometer tracing com Brave.
- springdoc-openapi para Swagger UI.
- Jib e Dockerfile para imagem.
- JUnit 5, Mockito, AssertJ e Testcontainers.

## Comandos

```bash
./gradlew compileJava
./gradlew test
./gradlew bootRun
./gradlew bootJar
./gradlew jibDockerBuild
./gradlew jib
docker build -t sticker-swap-backend .
```

Use `./gradlew`, não `gradle` global.

## Configuração Local

O profile padrão é `local`.

Arquivos:

- `src/main/resources/application.yml`: configuração base.
- `src/main/resources/application-local.yml`: defaults locais.
- `src/main/resources/application-prod.yml`: variáveis de produção.
- `src/test/resources/application-test.yml`: profile de testes.

O `application-local.yml` atual aponta para `jdbc:postgresql://localhost:5432/stickerswap` com usuário `postgres` e senha `postgres`. O `docker-compose.yml` do backend cria usuário `stickerswap` e senha `stickerswap`; alinhe credenciais por variável de ambiente ou ajuste o banco local antes de rodar.

## Variáveis Relevantes

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
- `CEP_API_BASE_URL`, `CEP_API_CONNECT_TIMEOUT`, `CEP_API_READ_TIMEOUT`.

## Arquitetura

Pacote raiz: `br.com.stickerswap`.

Áreas principais:

- `api`: controllers e DTOs.
- `domain`: modelos e serviços de domínio.
- `infrastructure`: configuração, repositórios, segurança, e-mail, seed, logging e web.
- `shared`: erros, segurança e contratos compartilhados.

Domínios:

- `identity`: usuários, roles, status, confirmação de e-mail, reset de senha e administração de contas.
- `profile`: perfil próprio, perfil público e CEP.
- `album`: álbuns e figurinhas.
- `collection`: figurinhas repetidas e desejadas.
- `search`: busca de colecionadores por figurinha.
- `chat`: conversas, mensagens e WebSocket.
- `notification`: notificações.
- `moderation`: bloqueios e denúncias.

Controllers devem depender de interfaces de serviço quando houver contrato. Implementações concretas usam sufixo `Impl`.

## Autenticação E Segurança

- OAuth2/OIDC com Authorization Code + PKCE.
- JWT RS256.
- Resource Server valida bearer tokens.
- Claim `role` vira authority `ROLE_<role>`.
- Claim `sub` é o UUID do usuário.
- Claims esperadas no token: `sub`, `email`, `role`.
- `OAuthClientSeeder` registra/atualiza o client público `sticker-swap-web`.
- Scopes atuais: `openid`, `profile`, `api`, `offline_access`.
- Redirect URIs vêm de `app.oauth.web-client.redirect-uris`.
- Post-logout URIs vêm de `app.oauth.web-client.post-logout-redirect-uris`.
- `ActiveUserFilter` bloqueia APIs protegidas para usuário inativo ou e-mail não confirmado.
- `RateLimitingFilter` aplica limites por IP, client ou usuário.

Roles:

- `USER`: usuário comum.
- `ADMIN`: administrador.

Regras:

- `/admin/**` exige `ROLE_ADMIN`.
- `/actuator/**`, exceto health/info públicos, exige `ROLE_ADMIN`.
- `/ws/**` é liberado no HTTP, mas o STOMP `CONNECT` é validado por `JwtChannelInterceptor`.

Não adicione endpoint que emita JWT manualmente.

## Conta, E-mail E Senha

Endpoints de conta em `AuthController`:

- `POST /auth/register`.
- `POST /auth/email-confirmations`.
- `GET /auth/email-confirmations/confirm?token=...`.
- `GET /auth/email-confirmations/confirm?token=...&redirect=false`.
- `POST /auth/password-reset-requests`.
- `POST /auth/password-resets`.

Confirmação de e-mail:

- O link aberto no navegador confirma a conta e redireciona para a tela frontend `email-confirmed`.
- A variante `redirect=false` retorna `UserResponse`.

Tokens de confirmação/reset usam `SecurityTokenService` e tabela `security_tokens`. Eles não são tokens OAuth.

Reset de senha consome tokens abertos de reset e remove autorizações persistidas do usuário em `oauth2_authorization`.

## E-mail

Contrato: `MailProvider`.

Implementações:

- `log`: escreve o e-mail em log.
- `brevo`: usa `https://api.brevo.com/v3/smtp/email`.
- `resend`: usa `https://api.resend.com/emails`.

Seleção por `app.mail.delivery-mode`.

Profiles `local` e `test` usam `log` no estado atual.

## Admin De Usuários

`AdminUserController` expõe:

- `GET /admin/users?q=&page=&size=&sort=`.
- `PATCH /admin/users/{userId}/block`.
- `PATCH /admin/users/{userId}/unblock`.

Bloqueio:

- impede auto-bloqueio administrativo;
- muda status para `INACTIVE`;
- remove autorizações persistidas;
- publica `UserAccessRevokedEvent`;
- notifica o usuário por `/user/queue/security`.

Desbloqueio muda status para `ACTIVE`; o usuário precisa entrar novamente.

## Endpoints De Domínio

Catálogo:

- `GET /albums`.
- `GET /albums/{albumId}`.
- `GET /albums/{albumId}/stickers`.

Admin de catálogo:

- `POST /admin/albums`.
- `PUT /admin/albums/{albumId}`.
- `PATCH /admin/albums/{albumId}/activate`.
- `PATCH /admin/albums/{albumId}/deactivate`.
- `POST /admin/albums/{albumId}/stickers`.
- `GET /admin/albums/{albumId}/stickers`.
- `PUT /admin/stickers/{stickerId}`.
- `PATCH /admin/stickers/{stickerId}/activate`.
- `PATCH /admin/stickers/{stickerId}/deactivate`.

Perfil:

- `GET /me/profile`.
- `PUT /me/profile`.
- `GET /users/{userId}/profile`.
- `GET /ceps/{cep}`.

Coleção:

- `GET /me/albums/{albumId}/collection`.
- `GET /me/albums/{albumId}/repeated-stickers`.
- `PUT /me/repeated-stickers/{stickerId}`.
- `DELETE /me/repeated-stickers/{stickerId}`.
- `GET /me/albums/{albumId}/wanted-stickers`.
- `PUT /me/wanted-stickers/{stickerId}`.
- `DELETE /me/wanted-stickers/{stickerId}`.

Busca:

- `GET /albums/{albumId}/stickers/{stickerId}/holders`.

Chat:

- `POST /stickers/{stickerId}/interest`.
- `GET /chats`.
- `GET /chats/{conversationId}/messages`.
- STOMP `/app/chat/{conversationId}/send`.
- Tópico `/topic/chat/{conversationId}`.

Notificações:

- `GET /notifications`.
- `GET /notifications/unread-count`.
- `PUT /notifications/{notificationId}/read`.
- `PUT /notifications/read-all`.
- `PUT /chats/{conversationId}/notifications/read`.
- Fila `/user/queue/notifications`.

Moderação:

- `PUT /users/{userId}/block`.
- `DELETE /users/{userId}/block`.
- `GET /me/blocked-users`.
- `POST /users/{userId}/report`.
- `GET /admin/moderation/reports`.

## Rate Limit

Implementação atual:

- `RateLimitingFilter`.
- `RateLimiterService`.
- `InMemoryRateLimiterService`.

Limites atuais:

- `POST /auth/register`: 5 por IP por hora.
- `POST /auth/email-confirmations`: 10 por IP por hora.
- `POST /auth/password-reset-requests`: 10 por IP por hora.
- `POST /login`: 10 por IP por minuto.
- `POST /oauth2/token`: 30 por IP/client por minuto.
- APIs autenticadas: 300 por usuário por minuto.

Para produção horizontal, substitua a implementação in-memory mantendo o contrato `RateLimiterService`.

## Flyway

Migrations atuais:

- `V1__identity_accounts_and_profiles.sql`.
- `V2__catalog_albums_and_stickers.sql`.
- `V3__user_collections.sql`.
- `V4__chat.sql`.
- `V5__moderation.sql`.
- `V6__oauth2_authorization_server.sql`.
- `V7__create_2026_stickers.sql`.
- `V8__notifications.sql`.
- `V9__user_last_ip_address.sql`.

Não há cópia de migrations em `src/test/resources/db/migration` no estado atual; os testes usam `classpath:db/migration`.

Antes de produção, a baseline ainda pode ser ajustada com cuidado. Depois de produção, trate migrations como imutáveis.

## Erros

Formato padrão: `ApiError`.

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

Use exceções existentes quando aplicável:

- `BusinessRuleException`.
- `ResourceNotFoundException`.
- `EmailAlreadyExistsException`.
- `RateLimitExceededException`.

## Testes

Testes existentes cobrem:

- Segurança de conta em `AccountSecurityIntegrationTest`.
- OAuth/Flyway/JWK em `OAuthAndSchemaIntegrationTest`.
- Busca com PostgreSQL real em `SearchFlowIntegrationTest`.
- Serviços de chat, coleção, moderação, perfil, busca, auth e admin.
- Rate limiter e JWT.

Base Testcontainers: `PostgresIntegrationTest`.

Ao alterar segurança, auth, migrations, busca, chat, notificações ou contratos principais:

```bash
./gradlew test
```

Para validações menores:

```bash
./gradlew compileJava
./gradlew compileTestJava
```

## Cuidados

- Não emitir JWT manualmente.
- Não permitir `/admin/**` sem `ROLE_ADMIN`.
- Não expor CEP em perfil público.
- Não transformar token de confirmação/reset em token OAuth.
- Não enviar e-mail real nos profiles `local` e `test`.
- Não usar `@ActiveProfiles("local")` em teste.
- Não remover a validação de usuário ativo/e-mail confirmado nas APIs protegidas.
- Não remover a validação JWT do STOMP `CONNECT`.
- Não fazer delete físico de álbuns/figurinhas com vínculo de usuário; use ativação/desativação.
- Não assumir que upload de imagem existe.

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

A imagem expõe porta `8080` e usa profile `prod` no container Jib.
