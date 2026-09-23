---
name: Implement OMS Endpoint
description: Generate a new REST endpoint adhering to AI-Native OMS architecture, domain model, and security rules.
---

Follow the project's Spec-Driven Development workflow:

1. **Context & Schemas:**
   - Read the endpoint contract from `#file:docs/api-spec.md`.
   - Read the entity rules and invariants from `#file:docs/domain-model.md`.
   - Strictly follow `#file:docs/coding-rules.md`, `#file:docs/api-rules.md`, and `#file:docs/security-rules.md`.

2. **Implementation Sequence:**
   - **DTOs:** Create Java records with `@JsonIgnoreProperties(ignoreUnknown = false)` and Bean Validation annotations (`@NotBlank`, `@NotNull`).
   - **Domain / Entity:** Enforce linear one-way state transitions (`Open` -> `InProgress` -> `Done`) inside entity methods; throw `IllegalStateException` on backward transitions.
   - **Repository:** Extend Spring Data JPA `JpaRepository`. Never use string-concatenated native SQL.
   - **Controller:** Use Constructor Injection, declare explicit authorization (`@PreAuthorize("hasRole(...)")`), use SLF4J with correlation ID and masked equipment ID (no PII), and return `ResponseEntity` with location headers on creation.
   - **Exception Handling:** Ensure all errors return RFC 7807 `application/problem+json` with `invalidParams` details.

3. **Automated Testing:**
   - Write tests in `src/test/java` covering:
     - 200/201 Success cases
     - 400 Validation & malformed input (RFC 7807)
     - 403 Forbidden (RBAC violation)
     - 404 Not Found (RFC 7807)
