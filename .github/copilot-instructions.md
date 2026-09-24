# Copilot Custom Instructions (workspace scope)

## Project Overview
Outage Work Order API — microservice quản lý sự kiện mất điện (OMS). Java 17 + Spring Boot 3.3 + JPA. AI-Native SDLC, spec-driven development.

## Role — Senior Architect + BA on this project.

## Core Rules
1. Never hardcode sensitive data. Env vars only.
2. Always follow `docs/*`. On conflict with a prompt, ask first.
3. Security first: parameterized queries, boundary validation, RBAC, sanitized PII logs, RFC 7807 errors.
4. No invented fields; schemas come only from the spec.
5. Disclose AI provenance in PRs.

## Stack notes (Java 17 + Spring Boot 3.3 + JPA/H2 dev, PostgreSQL prod)
- Constructor injection only, no field `@Autowired`.
- SLF4J logger, no PII in logs.
- REST plural resources under `/api/v1`, `@Valid` on inputs, RFC 7807 `problem+json`.
- JPA/parameterized queries only, no string-concat native SQL. RBAC via `@PreAuthorize`.

## Testing
- JUnit 5 + Spring Boot Test. `@WebMvcTest` for controller tests, `@DataJpaTest` for repository tests.
- Every endpoint must have tests for: success case, validation failure (400), unauthorized (401), forbidden (403), not found (404), invalid state transition (422).
- Run `mvn test` — all tests must pass before any PR.

## References
- Business Requirements Analysis: [docs/br-analysis-wo.md](../docs/br-analysis-wo.md)
- Domain Model: [docs/domain-model.md](../docs/domain-model.md)
- API Specification: [docs/api-spec.md](../docs/api-spec.md)
- Database Migration Specification: [docs/database-migration-spec.md](../docs/database-migration-spec.md)
- Internal Coding Standards: [docs/internal-coding-standards.md](../docs/internal-coding-standards.md)
- Coding Rules: [docs/coding-rules.md](../docs/coding-rules.md)
- API Design Rules: [docs/api-rules.md](../docs/api-rules.md)
- Security Rules: [docs/security-rules.md](../docs/security-rules.md)
