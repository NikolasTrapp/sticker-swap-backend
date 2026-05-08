# AGENTS.md

Instruções para agentes de IA trabalhando neste repositório.

## Contexto Do Projeto

Este backend é uma plataforma de troca de figurinhas da Copa. O produto aproxima usuários para encontrar figurinhas repetidas, visualizar perfis públicos e iniciar conversas individuais com intenção de troca. O sistema não é marketplace: não processa pagamento, entrega, reserva de figurinha nem confirmação de troca física.

O plano inicial está em `../plano-implementacao-figurinhas-copa.md` e os requisitos estão em `../requisitos-figurinhas-copa.md`. O estado atual já avançou além do MVP original em alguns pontos: confirmação de e-mail, recuperação de senha, rate limit, Spring Authorization Server e testes de integração com PostgreSQL/Testcontainers já foram implementados.

## Stack

- Java 25.
- Spring Boot 4.0.4 / Spring Framework 7.x.
- Gradle 9.5, preferencialmente via `./gradlew`.
- Jib plugin para geração de imagem Docker (sem Dockerfile para CI; Dockerfile disponível para builds manuais).
- PostgreSQL 16 para local/testes.
- Flyway em `src/main/resources/db/migration/` (cópia em `src/test/resources/db/migration/` para testes).
- Spring Security + Spring Authorization Server + Resource Server no mesmo deploy.
- Spring Web MVC, Spring WebSocket, Spring Data JPA, Hibernate, Jakarta Validation.
- springdoc-openapi para Swagger/OpenAPI.
- JUnit 5, Mockito, AssertJ e Testcontainers.

## Comandos Úteis

```bash
./gradlew compileJava          # compilar
./gradlew test                 # rodar todos os testes
./gradlew bootRun              # subir localmente
./gradlew bootJar              # gerar fat JAR em build/libs/
./gradlew jibDockerBuild       # gerar imagem Docker local via Jib
./gradlew jib                  # build + push para registry via Jib
docker build -t sticker-swap . # build via Dockerfile (multi-stage)
```

Para validação completa, rode `./gradlew test`. Os testes de integração usam Testcontainers e exigem Docker disponível.

## Arquitetura

O projeto é um monolito modular no pacote raiz `br.com.stickerswap`.

Pacotes principais:

- `identity`: usuários, roles, cadastro, confirmação de e-mail, reset de senha.
- `profile`: perfil próprio e perfil público.
- `album`: álbuns e figurinhas.
- `collection`: figurinhas repetidas e desejadas do usuário.
- `search`: busca de usuários por figurinha.
- `chat`: conversas, mensagens e WebSocket.
- `moderation`: bloqueios e denúncias.
- `shared`: segurança, erros, OpenAPI, filtros e utilitários comuns.

Padrão atual de serviços:

- Controllers e outros módulos devem depender de interfaces, por exemplo `AlbumService`, `AuthService`, `SearchService`.
- Implementações concretas devem usar o sufixo `Impl`, por exemplo `AlbumServiceImpl`, `AuthServiceImpl`, `SearchServiceImpl`.
- Testes unitários podem instanciar/injetar a implementação concreta para verificar comportamento interno.
- Evite acoplar controllers, filtros ou handlers a classes concretas quando houver contrato.
- Cross-module repository access existe em alguns pontos do monolito, mas prefira atravessar módulos por interfaces de serviço quando isso reduzir acoplamento.

## Autenticação E Autorização

Não reintroduza geração manual de access tokens para login.

O fluxo atual usa Spring Authorization Server:

- `/login`: form login do Spring Security usado pelo Authorization Server.
- `/oauth2/authorize`: authorization endpoint.
- `/oauth2/token`: token endpoint.
- `/oauth2/jwks`: JWK Set.
- `/.well-known/oauth-authorization-server`: metadata do authorization server.

O Resource Server valida JWTs emitidos pelo Authorization Server. O claim `role` é convertido para authority `ROLE_<role>`.

Roles:

- `USER`: usuário comum.
- `ADMIN`: administrador.

Regra de acesso:

- `/admin/**` exige `ROLE_ADMIN`.
- Endpoints públicos estão definidos em `SecurityConfig.PUBLIC_PATHS`.
- WebSocket `/ws/**` é permitido no HTTP, mas o STOMP `CONNECT` é validado por `JwtChannelInterceptor`.

Cliente OAuth local:

- `OAuthClientSeeder` cria o client público `sticker-swap-web`.
- O client usa Authorization Code + PKCE.
- Scopes atuais: `openid`, `profile`, `api`.
- Redirect URIs vêm de `app.oauth.web-client.redirect-uris`.

Admin local:

- `AdminSeeder` cria `admin@stickerswap.com` / `changeme`, configurável por `ADMIN_EMAIL` e `ADMIN_PASSWORD`.

## Cadastro, E-mail E Senha

Endpoints próprios de conta ficam em `AuthController`:

- `POST /auth/register`.
- `POST /auth/email-confirmations`.
- `GET /auth/email-confirmations/confirm?token=...`.
- `POST /auth/password-reset-requests`.
- `POST /auth/password-resets`.

Esses endpoints não emitem access token. Eles cuidam somente de criação de conta, confirmação de e-mail e reset de senha.

Tokens de confirmação/reset usam `SecurityTokenService` e a tabela `security_tokens`. Esses tokens são tokens de segurança de conta, não OAuth access tokens.

Ao resetar senha, `AuthServiceImpl` revoga autorizações OAuth existentes removendo registros de `oauth2_authorization` daquele principal.

Em ambientes local e de teste, não envie e-mail real. O profile `test` (`src/test/resources/application-test.yml`) e o profile `local` (`src/main/resources/application-local.yml`) definem:

```yaml
app:
  mail:
    delivery-mode: log
```

Com isso, `AccountEmailServiceImpl` escreve o conteúdo em log DEBUG.

## Rate Limit

O rate limit atual fica em:

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

Para produção horizontal, substitua a implementação in-memory por Redis ou outro backend distribuído mantendo o contrato `RateLimiterService`.

## Flyway

As migrations foram reorganizadas antes de produção e estão separadas por fluxo:

- `V1__identity_accounts_and_profiles.sql`.
- `V2__catalog_albums_and_stickers.sql`.
- `V3__user_collections.sql`.
- `V4__chat.sql`.
- `V5__moderation.sql`.
- `V6__oauth2_authorization_server.sql`.

Como o sistema ainda não foi para produção, é aceitável ajustar essas migrations existentes para manter uma baseline limpa e organizada. Evite criar scripts só com `ALTER TABLE` para corrigir decisões recentes enquanto ainda não houver produção.

Depois que houver produção, trate migrations como imutáveis e crie somente novas versões incrementais.

## Endpoints De Domínio

Catálogo público:

- `GET /albums`.
- `GET /albums/{albumId}`.
- `GET /albums/{albumId}/stickers`.

Admin de catálogo:

- `POST /admin/albums`.
- `PUT /admin/albums/{albumId}`.
- `PATCH /admin/albums/{albumId}/activate`.
- `PATCH /admin/albums/{albumId}/deactivate`.
- `POST /admin/albums/{albumId}/stickers`.
- `PUT /admin/stickers/{stickerId}`.
- `PATCH /admin/stickers/{stickerId}/activate`.
- `PATCH /admin/stickers/{stickerId}/deactivate`.

Não há endpoint administrativo para excluir usuário. Também não há upload de imagem implementado. A entidade `Sticker` possui `imageUrl`, mas os DTOs atuais de criação/edição/resposta não expõem upload nem imagem.

Coleção:

- `GET /me/albums/{albumId}/repeated-stickers`.
- `PUT /me/repeated-stickers/{stickerId}`.
- `DELETE /me/repeated-stickers/{stickerId}`.
- `GET /me/albums/{albumId}/wanted-stickers`.
- `PUT /me/wanted-stickers/{stickerId}`.
- `DELETE /me/wanted-stickers/{stickerId}`.

Busca:

- `GET /albums/{albumId}/stickers/{stickerId}/holders`.

Perfil:

- `GET /me/profile`.
- `PUT /me/profile`.
- `GET /users/{userId}/profile`.

Chat:

- `POST /stickers/{stickerId}/interest`.
- `GET /chats`.
- `GET /chats/{conversationId}/messages`.
- WebSocket STOMP em `/ws`.

Moderação:

- `PUT /users/{userId}/block`.
- `DELETE /users/{userId}/block`.
- `POST /users/{userId}/report`.
- `GET /admin/moderation/reports`.

## Erros

Erros devem seguir `ApiError` em `shared/error`:

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

Use exceções de domínio existentes quando fizer sentido:

- `BusinessRuleException`.
- `ResourceNotFoundException`.
- `EmailAlreadyExistsException`.
- `RateLimitExceededException`.

## Testes

Testes existentes:

- Unitários de serviços com Mockito.
- Integração de segurança de conta em `AccountSecurityIntegrationTest`.
- Integração OAuth/Flyway em `OAuthAndSchemaIntegrationTest`.
- Integração de busca com PostgreSQL real em `SearchFlowIntegrationTest`.
- Base Testcontainers em `PostgresIntegrationTest` (usa `@ActiveProfiles("test")`).
- Configurações de teste em `src/test/resources/application-test.yml` (Testcontainers reuse, mail log, logging, etc.).
- Migrations de teste em `src/test/resources/db/migration/` (cópia sincronizada das migrations principais).

Ao alterar queries, migrations, segurança, auth, busca ou fluxos principais, rode a suíte completa com Docker ativo:

```bash
./gradlew test
```

Para mudanças pequenas de contrato/compilação:

```bash
./gradlew compileTestJava
```

## Cuidados Importantes

- Não adicionar endpoint `/auth/login` que gere JWT manualmente.
- Não bypassar Spring Authorization Server para emitir access tokens.
- Não expor CEP ou e-mail em perfil público.
- Não permitir `/admin/**` sem `ROLE_ADMIN`.
- Não transformar reset/confirm token em token OAuth.
- Não enviar e-mail real nos profiles `local` e `test`.
- Não usar `@ActiveProfiles("local")` em testes; testes devem usar o profile `test`.
- Ao criar novas migrations, copiar o arquivo para `src/test/resources/db/migration/` também.
- Não remover Testcontainers dos testes de integração.
- Não acoplar novos controllers diretamente a implementações `*ServiceImpl`.
- Não fazer delete físico de álbuns/figurinhas se houver vínculo de usuário; use ativação/desativação.
- Não assumir que upload de imagem já existe.

## Docker E Deploy

Duas opções para gerar a imagem Docker:

1. **Jib (recomendado para CI)**: `./gradlew jibDockerBuild` gera imagem sem Docker daemon. `./gradlew jib` faz build + push para registry.
2. **Dockerfile (multi-stage)**: `docker build -t sticker-swap-backend .` usa JDK para build e JRE-alpine para runtime.

Flags JVM nos dois caminhos:

- `-XX:+UseContainerSupport`: respeita limites de CPU/memória do container.
- `-XX:MaxRAMPercentage=75.0`: usa até 75% da RAM do container para heap.
- `-Djava.security.egd=file:/dev/./urandom`: startup mais rápido.

Para EC2 spot: a aplicação é stateless (exceto WebSocket), então basta um healthcheck em `/actuator/health` e um balanceador que drene conexões antes de terminar a instância.

## Warnings Conhecidos

- Lombok em Java 25 emite warning de `sun.misc.Unsafe`; atualmente é benigno.
- Mockito pode emitir warning de dynamic agent em Java recente; atualmente os testes passam.
- Testcontainers pode avisar que reuse não está habilitado em `~/.testcontainers.properties`; isso não impede a suíte.
