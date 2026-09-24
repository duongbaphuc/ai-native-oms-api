---
name: Review OMS Code
description: Audit code against the project's 12 Scorecard quality and security criteria.
---

Audit the selected code or PR changes against the project standards in `#file:docs/01-br-analysis-wo.md`, `#file:docs/00-coding-rules.md`, `#file:docs/00-api-rules.md`, `#file:docs/00-security-rules.md`, and `#file:docs/00-internal-coding-standards.md`.

Verify each of the following 12 criteria:

1. **Constructor Injection:** No field-level `@Autowired`.
2. **SLF4J Logging:** No PII or raw payloads logged. Correlation ID and masked IDs only.
3. **No Hardcoded Secrets:** Credentials and tokens exclusively via environment variables or Secret Manager.
4. **Plural Resource & Versioned Paths:** Endpoints must use `/api/v1/<plural-resource>`.
5. **Strict Schema:** Unknown fields must be rejected (`ignoreUnknown = false` or `fail-on-unknown-properties: true`).
6. **RFC 7807 Errors:** `application/problem+json` returned for all error statuses, with `invalidParams` details. No stack traces leaked.
7. **Boundary Validation:** `@Valid` on controller request bodies.
8. **RBAC:** Explicit `@PreAuthorize("hasAnyRole(...)")` on every endpoint.
9. **JPA Parameterized:** No string concatenation in native SQL queries.
10. **No Hallucinated Deps:** Only libraries already declared in `#file:pom.xml`.
11. **State Machine Invariant:** Enforce linear one-way transitions (`OPEN` -> `IN_PROGRESS` -> `DONE`) at the domain level.
12. **AI Provenance:** Document tools, models, and prompts used.

**Output Format:**
Present the audit results as a Markdown table with columns: `#`, `Criterion`, `Status (Pass/Partial/Fail)`, `Evidence / Remediation`. End with an overall tally and clear Verdict.
