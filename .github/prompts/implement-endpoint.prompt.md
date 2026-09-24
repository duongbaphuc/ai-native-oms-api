---
name: Implement OMS Endpoint
description: Generate a new REST endpoint adhering to AI-Native OMS architecture, domain model, and security rules.
---

Follow the project's Spec-Driven Development workflow:

1. **Context & Schemas:**
   - Read the business requirements from `#file:docs/br-analysis-wo.md`.
   - Read the endpoint contract from `#file:docs/api-spec.md`.
   - Read the entity rules and invariants from `#file:docs/domain-model.md`.
   - Strictly follow `#file:docs/coding-rules.md`, `#file:docs/api-rules.md`, `#file:docs/security-rules.md`, and `#file:docs/internal-coding-standards.md`.

2. **Implementation Sequence (3-Tier Architecture):**
   - **DTOs:** Create Java records with `@JsonIgnoreProperties(ignoreUnknown = false)` on requests, Bean Validation annotations (`@NotBlank`, `@NotNull`, `@Size`), and static factory mapping methods (`WorkOrderResponse.from()`, `PagedResponse.from()`).
   - **Domain / Entity:** Enforce linear one-way state transitions (`OPEN` -> `IN_PROGRESS` -> `DONE`) inside entity methods; delegate to Enum `canTransitionTo()`; throw `IllegalStateException` on backward/skipped transitions.
   - **Repository:** Extend Spring Data JPA `JpaRepository`. Never use string-concatenated native SQL. Support pagination with `Pageable`.
   - **Service:** Encapsulate business logic in `@Service` class. Delegate state machine checks to entity, re-throw `IllegalStateException`, throw `ResourceNotFoundException` when ID is not found, convert entities to DTOs.
   - **Controller:** Use Constructor Injection, delegate to Service (never call Repository directly), declare explicit authorization (`@PreAuthorize("hasAnyRole(...)")`), use SLF4J (no PII in logs), and return `ResponseEntity` with location headers on creation.
   - **Exception Handling:** Ensure all errors return RFC 7807 `application/problem+json` via `GlobalExceptionHandler`.

3. **Automated Testing:**
   - Write tests in `src/test/java` covering:
     - 200/201 Success cases (including pagination)
     - 400 Validation & malformed input (RFC 7807 `validation-error`, `malformed-json`)
     - 401 Unauthorized & 403 Forbidden (RBAC security violations)
     - 404 Not Found (RFC 7807 `not-found`)
     - 422 Unprocessable Entity (RFC 7807 `invalid-state-transition`)
