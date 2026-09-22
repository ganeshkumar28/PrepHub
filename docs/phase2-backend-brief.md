# Phase 2 — Backend scaffolding brief (for Antigravity)

## Project setup
- Java 18, Spring Boot 3.3.x, Maven (matches your existing local setup)
- Package base: `com.prephub.api`
- Dependencies: `spring-boot-starter-web`, `spring-boot-starter-data-jpa`, `spring-boot-starter-security`, `spring-boot-starter-oauth2-resource-server`, `flyway-core`, `flyway-database-postgresql`, `org.postgresql:postgresql`, `springdoc-openapi-starter-webmvc-ui` (optional, for a live Swagger UI from `docs/openapi.yaml`)

## Folder structure
```
backend/
  src/main/java/com/prephub/api/
    config/         SecurityConfig, CorsConfig
    entity/         JPA entities matching V1__init.sql
    repository/      Spring Data JPA repositories
    controller/      REST controllers matching openapi.yaml operationIds
    dto/            Request/response records matching openapi.yaml schemas
    service/        Business logic (extraction orchestration, publish flow)
  src/main/resources/
    application.yml
    db/migration/V1__init.sql   (already exists)
```

## Supabase JWT validation (the part to get right)
Spring Boot must **validate** Supabase's JWT, never issue its own. Configure it as an OAuth2 resource server pointed at Supabase's JWKS endpoint:

```yaml
# application.yml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          jwk-set-uri: https://<your-project-ref>.supabase.co/auth/v1/.well-known/jwks.json
  datasource:
    url: jdbc:postgresql://<your-project-ref>.supabase.co:5432/postgres
    username: postgres
    password: ${DB_PASSWORD}
```

Any endpoint marked `security: []` in `openapi.yaml` (the public browse/search ones) should be `permitAll()` in `SecurityConfig`; everything else requires a valid bearer token. The JWT's `sub` claim is the Supabase `auth.users.id` — that's what you match against `profiles.id`.

## What "done" looks like for this phase
- `GET /api/v1/companies?q=test` returns `[]` with **no** Authorization header (public endpoint)
- `GET /api/v1/me` returns `401` with no header, and a valid `User` object with a real Supabase JWT
- JPA entities compile and match `V1__init.sql` field-for-field — no extra columns Antigravity might be tempted to add (avatar_url, role, etc. were deliberately cut in v1)
