## 1. Traceability (Issue link, Spec path)
- Issue: `closes #` / `WO-` (nhánh `feature/WO-<id>`)
- Spec: `docs/domain-model.md`, `docs/api-spec.md` (mục nào thay đổi?)
- Loại thay đổi: [ ] `feat` [ ] `fix` [ ] `refactor` [ ] `docs` [ ] `test`

## 2. AI Usage Disclosure & Provenance (tools, prompts/context, generated vs hand-written)
- Tool: (Copilot / khác, phiên bản nếu có)
- Context files: `docs/coding-rules.md`, `docs/api-rules.md`, `docs/security-rules.md`
- Phần nào AI sinh / phần nào viết tay / đã review hallucination (không package/API ảo):

## 3. Verification & Evidence (tests, linter, spec review)
- Lệnh chạy: `mvn test` (paste log green)
- Tuân thủ coding-rules (Java 17+, constructor injection, SLF4J không PII):
- Tuân thủ api-rules (resource số nhiều, RFC 7807, strict schema, `@Valid`):
- Human reviewer: @

## 4. Risk & Security Checklist (secrets scan, OWASP, rollback plan)
- [ ] Không secrets/credentials/PII trong code/prompt/log
- [ ] JPA/parameterized queries, không nối chuỗi native SQL
- [ ] RBAC `@PreAuthorize` đúng role, boundary validation đầy đủ
- [ ] Không lộ stack trace cho client, lỗi theo RFC 7807
- [ ] Rollback plan:
