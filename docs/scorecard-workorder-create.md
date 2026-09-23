# Scorecard: draft-workorder-create

| # | Criterion (source rule) | Pass/Partial/Fail | Evidence |
|---|---|---|---|
| 1 | Constructor injection (coding) | Pass | `public WorkOrderController(repo)` |
| 2 | SLF4J, no PII (coding) | Pass | log id hash only |
| 3 | No secret hardcode (coding/security) | Pass | không secret |
| 4 | Plural resource + versioned path (api) | Pass | `/api/v1/workorders` |
| 5 | Strict schema, no invented fields (api) | Partial | dùng đúng equipmentId/priority, thiếu `additionalProperties:false` |
| 6 | RFC 7807 errors (api) | Fail | chưa có `@ControllerAdvice` problem+json |
| 7 | `@Valid` boundary validation (api) | Pass | có `@Valid` |
| 8 | Auth per endpoint (api/security) | Pass | `@PreAuthorize TECHNICIAN` |
| 9 | JPA/parameterized, no concat SQL (security) | Pass | `repo.save` |
| 10 | No hallucinated deps (coding) | Pass | chỉ spring-boot-starter có trong pom |
| 11 | State machine one-way (domain) | Partial | chưa enforce Open->InProgress->Done ở entity |
| 12 | AI provenance disclosed | Fail | draft chưa ghi tool/prompt version |

Tally: 8 Pass / 2 Partial / 2 Fail.
Follow-ups: thêm `@ControllerAdvice` RFC 7807; enforce state machine ở entity; bổ sung provenance.
Verdict: must NOT be used as-is.
