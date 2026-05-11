# CLAUDE.md

Instruções para trabalhar neste backend.

## Stack

- Java 25.
- Spring Boot 4.0.4.
- Gradle via `./gradlew`.
- PostgreSQL com Flyway.
- Spring Web MVC, JPA, Security, OAuth2/OIDC, Resource Server, WebSocket/STOMP, Validation, Mail, Thymeleaf e Actuator.
- JUnit 5, Mockito, AssertJ e Testcontainers.

## Comandos

```bash
./gradlew compileJava
./gradlew compileTestJava
./gradlew test
./gradlew bootRun
./gradlew bootJar
./gradlew jibDockerBuild
```

## Configuração

Profile padrão: `local`.

Arquivos:

- `src/main/resources/application.yml`
- `src/main/resources/application-local.yml`
- `src/main/resources/application-prod.yml`
- `src/test/resources/application-test.yml`

O local atual usa banco `stickerswap` em `localhost:5432` com usuário/senha `postgres/postgres`. O `docker-compose.yml` do backend cria `stickerswap/stickerswap`, então alinhe as credenciais se for usar esse compose.

## Arquitetura

Pacote raiz: `br.com.stickerswap`.

- `api`: controllers e DTOs.
- `domain`: modelos e serviços.
- `infrastructure`: segurança, repositórios, e-mail, config, seed e web.
- `shared`: erros e contratos compartilhados.

Controllers devem depender de interfaces de serviço quando houver contrato. Implementações usam sufixo `Impl`.

## Segurança

- OAuth2/OIDC com Authorization Code + PKCE.
- JWT RS256.
- Resource Server em APIs protegidas.
- Claims esperadas: `sub` como UUID do usuário, `email`, `role`.
- `role` vira authority `ROLE_<role>`.
- `USER` e `ADMIN` são as roles atuais.
- `/admin/**` exige `ROLE_ADMIN`.
- `ActiveUserFilter` exige usuário ativo e e-mail confirmado nas APIs protegidas.
- `JwtChannelInterceptor` valida JWT em `STOMP CONNECT`.
- `RateLimitingFilter` aplica limites de cadastro, confirmação, reset, token e APIs autenticadas.

Não adicionar endpoint que gere access token manualmente.

## Conta

Endpoints em `AuthController`:

- `POST /auth/register`.
- `POST /auth/email-confirmations`.
- `GET /auth/email-confirmations/confirm?token=...`.
- `GET /auth/email-confirmations/confirm?token=...&redirect=false`.
- `POST /auth/password-reset-requests`.
- `POST /auth/password-resets`.

O link de confirmação aberto no navegador redireciona para a tela frontend `email-confirmed`. A variante `redirect=false` retorna `UserResponse`.

Reset de senha revoga autorizações persistidas do usuário.

## Admin

Admin de usuários:

- `GET /admin/users`.
- `PATCH /admin/users/{userId}/block`.
- `PATCH /admin/users/{userId}/unblock`.

Bloqueio muda o usuário para `INACTIVE`, revoga autorizações persistidas e envia evento em `/user/queue/security`.

Admin de catálogo e moderação ficam em `AdminAlbumController` e `AdminModerationController`.

## E-mail

`MailProviderConfig` seleciona provider por `app.mail.delivery-mode`:

- `log`.
- `brevo`.
- `resend`.

Profiles local e test usam `log`.

## Tempo Real

- Endpoint STOMP: `/ws`.
- Chat recebe mensagens em `/app/chat/{conversationId}/send`.
- Chat publica em `/topic/chat/{conversationId}`.
- Notificações usam `/user/queue/notifications`.
- Eventos de segurança usam `/user/queue/security`.

## Migrations

Flyway usa `src/main/resources/db/migration`.

Versões atuais: `V1` a `V9`, incluindo OAuth2 JDBC, figurinhas 2026, notificações e `last_ip_address`.

## Testes

Use `./gradlew test` para mudanças em segurança, auth, migrations, busca, chat, notificações ou contratos.

Testes de integração usam Testcontainers e profile `test`.

## Erros

Erros HTTP devem seguir `ApiError` e passar pelo `GlobalExceptionHandler`.

Use exceções existentes:

- `BusinessRuleException`.
- `ResourceNotFoundException`.
- `EmailAlreadyExistsException`.
- `RateLimitExceededException`.

## Cuidados

- Não expor CEP em perfil público.
- Não permitir admin apenas por UI; backend deve exigir `ROLE_ADMIN`.
- Não remover validação de conta ativa.
- Não remover validação JWT no WebSocket.
- Não enviar e-mail real em local/test.
- Não assumir upload de imagem.
