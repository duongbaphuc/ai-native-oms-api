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
- **Oracle Senior Java Guidelines:** Pure Java 17, Effective Java (Joshua Bloch), Clean Architecture.
- **Design Patterns:**
  * *Static Factory Method Pattern:* DTOs (`WorkOrderResponse.from()`, `PagedResponse.from()`) with defensive `Objects.requireNonNull()`.
  * *State Pattern / State Machine:* Enforce valid transitions (`canTransitionTo()`) in Enum; Domain Entities (`WorkOrder.advanceStatus()`) protect domain invariants with `IllegalStateException`.
  * *Pure Records & Immutability:* 100% Request/Response DTOs are Java 17 `record`. Local variables and parameters marked `final` for JIT escape analysis.
  * *Pre-sizing Collections & Array Caching:* Always supply `initialCapacity` when collection size is known (`new ArrayList<>(size)`). Cache `Enum.values()` static array clone in hot paths (e.g. converters).
  * *Centralized Constants (DRY):* Reuse `ProblemTypes` for RFC 7807 problem type URIs across exception handlers, controllers, and security configs to eliminate magic literals and duplicate `URI.create()` allocations.
- **Dependency Injection:** Constructor injection only with `private final` fields. CẤM TUYỆT ĐỐI `@Autowired` trên field.
- **Zero-Lombok & Zero-Reflection:** CẤM TUYỆT ĐỐI Lombok annotations và Reflection mappers (`ModelMapper`). Ánh xạ tường minh bằng Static Factory hoặc MapStruct.
- **SLF4J Logging:** Parametric logging only (`log.info("...", var)`). Cấm string concatenation `+` trong log. Cấm log PII hoặc raw payload.
- **REST & Security:** Plural resources under `/api/v1`, `@Valid` on inputs, RFC 7807 `problem+json`, RBAC via `@PreAuthorize`.

## Testing
- JUnit 5 + Spring Boot Test. `@WebMvcTest` for controller tests, `@DataJpaTest` for repository tests.
- **Object Mother / Test Fixtures Pattern:** Use centralized `WorkOrderTestFixtures` (`src/test/java/com/gpc/oms/testutil/`) to generate mock entities and DTOs; eliminate boilerplate object instantiations across test suites.
- Every endpoint must have tests for: success case, validation failure (400), unauthorized (401), forbidden (403), not found (404), invalid state transition (422).
- Quality Gate: 100% Line & Branch JaCoCo coverage required across domain, service, controller, dto, exception packages.
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
