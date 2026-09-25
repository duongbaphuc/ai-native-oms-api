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
- **Modern Java 17 Idioms:**
  * *Compact Constructors:* Dùng compact constructor `public RecordName { ... }` để validate invariants trong Records.
  * *Enhanced Switch Expressions:* 100% switch dùng arrow syntax `case X -> ...` không có `break`, bao quát exhaustive.
  * *Text Blocks & Stream Pipelines:* Dùng `"""` cho multi-line strings; dùng `Stream.toList()` tối ưu thay vì `Collectors.toList()`.
- **Design Patterns & Performance:**
  * *Static Factory Method Pattern:* DTOs (`WorkOrderResponse.from()`, `PagedResponse.from()`) với defensive `Objects.requireNonNull()`.
  * *State Pattern / State Machine:* Enforce valid transitions (`canTransitionTo()`) in Enum; Domain Entities (`WorkOrder.advanceStatus()`) protect domain invariants with `IllegalStateException`.
  * *Pure Records & Immutability:* 100% Request/Response DTOs are Java 17 `record`. Local variables and parameters marked `final` for JIT escape analysis.
  * *Pre-sizing Collections & Array Caching:* Always supply `initialCapacity` when collection size is known (`new ArrayList<>(size)`). Cache `Enum.values()` static array clone in hot paths (e.g. converters).
  * *Zero Magic Numbers & Literal Strings (Centralized Constants & Config Properties):* CẤM TUYỆT ĐỐI magic numbers (status codes, timeouts, cache size, rate limits) và literal strings (metric names, tag keys, role names, JSON error keys, media types). Bắt buộc: (1) Gom metric names và tag keys vào `WorkOrderMetrics`; (2) Gom role names vào `RoleConstants` (`ROLE_ADMIN`, `ROLE_DISPATCHER`, `ROLE_TECHNICIAN`); (3) Gom RFC 7807 problem types, extension keys (`invalidParams`, `name`, `reason`), và HTTP media types vào `ProblemTypes`; (4) Dùng chung hằng số độ dài (`MAX_EQUIPMENT_ID_LENGTH = 50`, `MIN_DESCRIPTION_LENGTH = 10`, `MAX_DESCRIPTION_LENGTH = 500`) giữa Entity và DTO; (5) Externalize các tham số rate limiting, cache, timeout ra `application.yml` qua `@ConfigurationProperties`.
- **Dependency Injection:** Constructor injection only with `private final` fields. CẤM TUYỆT ĐỐI `@Autowired` trên field.
- **Zero-Lombok & Zero-Reflection:** CẤM TUYỆT ĐỐI Lombok annotations và Reflection mappers (`ModelMapper`). Ánh xạ tường minh bằng Static Factory hoặc MapStruct.
- **SLF4J Logging:** Parametric logging only (`log.info("...", var)`). Cấm string concatenation `+` trong log. Cấm log PII hoặc raw payload.
- **REST & Security:** Plural resources under `/api/v1`, `@Valid` on inputs, RFC 7807 `problem+json`, RBAC via `@PreAuthorize`.

## Testing
- JUnit 5 + Spring Boot Test. `@WebMvcTest` for controller tests, `@DataJpaTest` for repository tests.
- **Static Imports:** Đồng bộ 100% static imports cho assertions (`assertThat`, `assertEquals`, `assertThrows`) và mocks (`when`, `verify`, `times`, `never`).
- **Object Mother / Test Fixtures Pattern:** Use centralized `WorkOrderTestFixtures` (`src/test/java/com/gpc/oms/testutil/`) to generate mock entities, DTOs, and JSON payloads; eliminate boilerplate object instantiations across test suites.
- Every endpoint must have tests for: success case, validation failure (400), unauthorized (401), forbidden (403), not found (404), invalid state transition (422).
- Quality Gate: 100% Line & Branch JaCoCo coverage required across domain, service, controller, dto, exception packages.
- Run `mvn clean verify` — all tests must pass before any PR.

## References (AI-Native SDLC Order)
- Phase 00 (Governance & Rules): [docs/00-coding-rules.md](../docs/00-coding-rules.md), [docs/00-internal-coding-standards.md](../docs/00-internal-coding-standards.md), [docs/00-api-rules.md](../docs/00-api-rules.md), [docs/00-security-rules.md](../docs/00-security-rules.md)
- Phase 01 (Business & Domain): [docs/01-br-analysis-wo.md](../docs/01-br-analysis-wo.md), [docs/01-domain-model.md](../docs/01-domain-model.md)
- Phase 02 (Architecture & Specs): [docs/02-api-spec.md](../docs/02-api-spec.md), [docs/02-database-migration-spec.md](../docs/02-database-migration-spec.md), [docs/02-security-auth-spec.md](../docs/02-security-auth-spec.md), [docs/02-observability-and-logging.md](../docs/02-observability-and-logging.md), [docs/02-ADR-001-use-h2-database.md](../docs/02-ADR-001-use-h2-database.md)
- Phase 03 (Context Index): [docs/03-CONTEXT_INDEX.md](../docs/03-CONTEXT_INDEX.md)
- Phase 08 (Handover & Dossier): [docs/08-SYSTEM_HANDOVER.md](../docs/08-SYSTEM_HANDOVER.md), [docs/08-ORACLE_JAVA_DOCUMENTATION.md](../docs/08-ORACLE_JAVA_DOCUMENTATION.md)
- Phase 09 (Security Handover): [docs/09-SECURITY_HANDOVER_REPORT.md](../docs/09-SECURITY_HANDOVER_REPORT.md)
- Phase 10 (DevOps & CI/CD): [docs/10-devops-pipeline-spec.md](../docs/10-devops-pipeline-spec.md)
