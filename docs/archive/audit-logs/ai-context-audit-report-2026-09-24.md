# 🔬 AI Context Audit Report — Outage Work Order API (Full Workspace)

**Ngày rà soát:** 2026-09-24  
**Auditor:** Senior AI Context Auditor (Claude Opus 4.6)  
**Phạm vi:** Toàn bộ 28 tệp Markdown trong dự án `ai-native-oms-api`  
**Mục tiêu:** Đánh giá mức độ sẵn sàng cho Copilot/LLM sinh code zero-hallucination  
**Audit trước đó:** [audit-report-2026-09-23.md](file:///c:/ai-native-oms-api/docs/ai-context-auditor/audit-report-2026-09-23.md) (62/100 → 91/100 sau refactor)

---

## 📊 1. Điểm Chuẩn Hóa AI: **93 / 100**

| # | Tiêu chí | Điểm | Trước (23/09) | Trạng thái | Nhận xét |
|---|---|:---:|:---:|---|---|
| 1 | **Table-Driven Data** | 19/20 | 19/20 | ✅ Xuất sắc | Entity, DTO, JWT Claims, DDL, RBAC Matrix đều có bảng 4-5 cột chuẩn |
| 2 | **Step-by-step Logic** | 18/20 | 18/20 | ✅ Tốt | Pseudo-code tuần tự ở mọi method Controller/Service/Entity |
| 3 | **Edge Cases Coverage** | 19/20 | 19/20 | ✅ Xuất sắc | Ma trận ngoại lệ RFC 7807 đầy đủ ở từng endpoint |
| 4 | **Architectural Constraints** | 18/20 | 18/20 | ✅ Tốt | 3-tier enforced, flow diagrams, cấm leaking |
| 5 | **Target File Mapping** | 19/20 | 17/20 | ✅ Xuất sắc | `draft-file-mapping.md` tổng hợp 12 files + implementation order |

---

## 📁 2. Phân Tích Từng Nhóm File

### Nhóm A: Tài Liệu Nền Tảng (Foundation Specs) — ⭐ 96/100

Các file cấp kiến trúc, quy tắc, và đặc tả gốc.

#### [domain-model.md](file:///c:/ai-native-oms-api/docs/domain-model.md) — ⭐ 98/100

**Điểm mạnh:**
- Bảng schema 4 cột đầy đủ (Thuộc tính / Kiểu / Ràng buộc & Annotation / Mô tả)
- Sơ đồ Mermaid `stateDiagram-v2` cho State Machine — trực quan
- Bảng ma trận chuyển đổi hợp lệ 5 dòng, có cột "Mã Ngoại lệ khi Vi phạm"
- Pseudo-code `advanceStatus()` 5 bước tuần tự
- Ràng buộc kiến trúc rõ ràng (cấm `setStatus()`, Controller → Service → Entity)
- HTML comment đầu file có Target Files checklist

**Lỗ hổng nhỏ:**

| # | Lỗ hổng | Mức độ | Hậu quả |
|---|---|---|---|
| DM-1 | **`@Column(length)` thiếu trên `priority` và `status`** — bảng schema ghi `@Enumerated(EnumType.STRING), @Column(nullable = false, length = 20)` nhưng `database-migration-spec.md` khai báo `VARCHAR(20)`. Khớp nhau, nhưng nên thêm `length = 20` vào annotation để Copilot không sinh `VARCHAR(255)` | ⚠️ Nhỏ | AI có thể bỏ qua length trên enum column |
| DM-2 | **Thiếu `@Table(name = "work_orders")`** trong pseudo-code `advanceStatus()` — tuy phần 1 đã ghi "Bảng CSDL: `work_orders`" nhưng khác section, Copilot có thể miss | ⚠️ Nhỏ | Redundancy giúp AI nhất quán hơn |

---

#### [api-spec.md](file:///c:/ai-native-oms-api/docs/api-spec.md) — ⭐ 97/100

**Điểm mạnh:**
- 4 endpoints đều có: Schema Table (Request/Response), Step-by-step Logic, Ma trận Ngoại lệ
- RBAC annotation chính xác (`@PreAuthorize`)
- RFC 7807 example JSON ở cuối file
- `Location` header trả về trong POST 201
- HTML comment đầu file liệt kê Target Files

**Lỗ hổng:**

| # | Lỗ hổng | Mức độ | Hậu quả |
|---|---|---|---|
| AS-1 | **GET list endpoint:** Response mô tả "trả về `List<WorkOrderResponse>` **hoặc** `PagedResponse<WorkOrderResponse>`" — từ "hoặc" tạo mơ hồ. Copilot phải đoán dùng cái nào. `internal-coding-standards.md` đã define `PagedResponse<T>` nhưng api-spec không chốt. | ⚠️ Trung bình | AI tự chọn 1 trong 2, có thể không khớp service/test |
| AS-2 | **GET list thiếu step-by-step logic** cho Service layer — có Query Parameters table nhưng không có pseudo-code xử lý filter `status` param | ⚠️ Trung bình | AI bỏ qua filter logic, trả `findAll()` luôn |
| AS-3 | **`description` min length** — Request schema ghi `@Size(min = 10, max = 500)` nhưng `domain-model.md` Entity schema **không ghi min length**. Validation ở boundary (Controller) có min=10, nhưng Entity `@Column` không enforce → có thể inconsistent nếu data vào từ nguồn khác | ⚠️ Nhỏ | Chỉ là semantic mismatch, functional vẫn đúng |

---

#### [database-migration-spec.md](file:///c:/ai-native-oms-api/docs/database-migration-spec.md) — ⭐ 98/100

**Điểm mạnh:**
- Bảng Data Types Mapping 6 dòng (Java → PostgreSQL → H2) — cực kỳ hữu ích
- Bảng naming conventions chi tiết (Table/Column/PK/FK/Index/Check)
- DDL script mẫu hoàn chỉnh, tương thích PostgreSQL + H2
- Flyway migration rules + zero-downtime 4-phase pattern
- Indexing strategy giải thích purpose từng index

**Không có lỗ hổng đáng kể.** File này đạt chuẩn gần hoàn hảo.

---

#### [security-auth-spec.md](file:///c:/ai-native-oms-api/docs/security-auth-spec.md) — ⭐ 96/100

**Điểm mạnh:**
- JWT Claims Schema bảng 6 dòng với ví dụ cụ thể
- RBAC Matrix 6 endpoints, có annotation chính xác
- Code snippet `JwtAuthenticationConverter`
- CORS policy, HTTP Security Headers, CSRF policy chi tiết
- Rate Limiting: Token Bucket config + RFC 7807 429 response
- RFC 7807 payloads cho 401/403

**Lỗ hổng:**

| # | Lỗ hổng | Mức độ | Hậu quả |
|---|---|---|---|
| SA-1 | **Target Files thiếu trong HTML comment header** — không liệt kê `SecurityConfig.java`, `JwtAuthenticationFilter.java`, `CorrelationIdFilter.java` sẽ được sinh ra từ spec này | ⚠️ Trung bình | AI không biết chính xác file đích |
| SA-2 | **Rate Limiting** — spec ghi dùng `Bucket4j` nhưng dependency này không nằm trong `pom.xml` và coding-rules ghi "No hallucinated deps". Cần quyết định: implement Rate Limiting hay defer? | ⚠️ Trung bình | AI import Bucket4j → build fail |

---

#### [observability-and-logging.md](file:///c:/ai-native-oms-api/docs/observability-and-logging.md) — ⭐ 95/100

**Điểm mạnh:**
- Log Event Schema table 11 dòng (JSON key → type → source)
- `CorrelationIdFilter` step-by-step 6 bước + code snippet
- Log Level Decision Tree + PII Masking Regex table
- Custom Business Metrics bảng 4 dòng (Prometheus name → Type → Tags)
- Mẫu log chuẩn trong Service layer

**Lỗ hổng:**

| # | Lỗ hổng | Mức độ | Hậu quả |
|---|---|---|---|
| OL-1 | **Target Files thiếu** — không liệt kê file đích: `CorrelationIdFilter.java`, `logback-spring.xml`, `application.yml` metrics config | ⚠️ Trung bình | AI không biết tạo file ở đâu |
| OL-2 | **PII Regex Masking** — ghi auto-masking bằng Regex nhưng không chỉ rõ implement ở đâu (Logback Converter? Custom Appender? Filter?) | ⚠️ Nhỏ | AI đoán implementation pattern |

---

#### [internal-coding-standards.md](file:///c:/ai-native-oms-api/docs/internal-coding-standards.md) — ⭐ 97/100

**Điểm mạnh:**
- Date/Time Policy table 4 dòng (Database/Entity/DTO Response/DTO Request)
- Object Mapping Standard: Static Factory → MapStruct → **CẤM ModelMapper**
- `PagedResponse<T>` record hoàn chỉnh với `from(Page<T>)` factory
- Pagination Query Parameters table có boundary constraints (max 100)
- Bean Validation bảng 5 dòng với thông điệp lỗi RFC 7807
- Resilience retry rules: Transient vs Business errors

**Không có lỗ hổng đáng kể.**

---

#### [devops-pipeline-spec.md](file:///c:/ai-native-oms-api/docs/devops-pipeline-spec.md) — ⭐ 96/100

**Điểm mạnh:**
- Multi-stage Dockerfile hoàn chỉnh, non-root user (UID 10001)
- `.dockerignore` đầy đủ
- GitHub Actions YAML workflow
- Quality Gates table (lint → test → coverage ≥80% → Trivy scan)
- Environment Variables matrix 7 biến × 3 môi trường

**Lỗ hổng:**

| # | Lỗ hổng | Mức độ | Hậu quả |
|---|---|---|---|
| DP-1 | **JaCoCo threshold script** dùng `awk` parse `jacoco.csv` — logic chỉ tính `instructions` coverage, không tính `branch` coverage riêng mặc dù spec yêu cầu "Branch coverage ≥ 75%" | ⚠️ Nhỏ | Branch gate bị bỏ qua trong CI |

---

#### Các File Rules & Guidelines

| File | Điểm | Nhận xét |
|---|:---:|---|
| [coding-rules.md](file:///c:/ai-native-oms-api/docs/coding-rules.md) | 94/100 | Đầy đủ quy tắc coding, SLF4J, PII ban. Thiếu Target Files (file này là rules, acceptable). |
| [security-rules.md](file:///c:/ai-native-oms-api/docs/security-rules.md) | 95/100 | 5 sections: Secrets, SQL Injection, RBAC, PII, Prompt Hygiene. Có Target Files header. |
| [api-rules.md](file:///c:/ai-native-oms-api/docs/api-rules.md) | 97/100 | RFC 7807 Error Catalog bảng 7 dòng, Strict Schema validation. |
| [ADR-001-use-h2-database.md](file:///c:/ai-native-oms-api/docs/ADR-001-use-h2-database.md) | 90/100 | ADR chuẩn format (Context → Decision → Consequences). Đúng mục đích. |
| [br-analysis-wo.md](file:///c:/ai-native-oms-api/docs/br-analysis-wo.md) | 95/100 | Raw BR → Table-driven entity → RBAC alignment → 3-tier decomposition. Rõ ràng. |

---

### Nhóm B: Draft Files (Implementation Specs) — ⭐ 92/100

Các file cấp triển khai chi tiết, chứa code sketch và pseudo-code.

#### [draft-file-mapping.md](file:///c:/ai-native-oms-api/docs/drafts/draft-file-mapping.md) — ⭐ 98/100

**Điểm mạnh siêu việt:**
- Bảng 12 files (10 production + 2 test): File → Package → Full Path → Action → Draft Nguồn
- Package Structure Visualization (tree diagram)
- Implementation Order (dependency-first)
- **Đây là file tốt nhất trong toàn bộ draft suite** — zero ambiguity

---

#### [draft-dtos.md](file:///c:/ai-native-oms-api/docs/drafts/draft-dtos.md) — ⭐ 96/100

**Điểm mạnh:**
- Schema Table cho từng DTO (Field/Type/Annotation/Description)
- `@JsonIgnoreProperties(ignoreUnknown = false)` trên mọi Request DTO
- `WorkOrderResponse.from(WorkOrder)` factory method hoàn chỉnh
- Validation Summary table 7 dòng
- Example payloads (valid + rejected)
- Checklist 10 items

**Lỗ hổng nhỏ:**

| # | Lỗ hổng | Mức độ | Hậu quả |
|---|---|---|---|
| DTO-1 | **`description` validation mismatch** — DTO ghi `@Size(max=500)` nhưng `api-spec.md` ghi `@Size(min=10, max=500)`. Draft thiếu `min=10`. | ⚠️ Trung bình | AI sinh DTO thiếu min-length validation |

---

#### [draft-workorder-domain.md](file:///c:/ai-native-oms-api/docs/drafts/draft-workorder-domain.md) — ⭐ 95/100

**Điểm mạnh:**
- Entity Schema Table 7 dòng đầy đủ
- Constructor Logic 7 bước tuần tự
- `advanceStatus()` pseudo-code 4 bước
- `canTransitionTo()` strict linear switch expression
- Enum code hoàn chỉnh (Priority + WorkOrderStatus)
- Repository interface
- Checklist 8 items

**Lỗ hổng:**

| # | Lỗ hổng | Mức độ | Hậu quả |
|---|---|---|---|
| DD-1 | **`@JsonValue` serialization** — `OPEN("Open")`, `IN_PROGRESS("InProgress")`, `DONE("Done")`. Giá trị `"InProgress"` (PascalCase ghép) hơi bất thường. Nên cân nhắc `"IN_PROGRESS"` hoặc `"In Progress"` cho consistency. Tuy nhiên, nếu đây là quyết định thiết kế thì cần **note rõ lý do** trong spec | ⚠️ Nhỏ | Không ảnh hưởng code, chỉ API contract |

---

#### [draft-workorder-service.md](file:///c:/ai-native-oms-api/docs/drafts/draft-workorder-service.md) — ⭐ 94/100

**Điểm mạnh:**
- 4 methods đầy đủ: create, getAll, getById, updateStatus
- Step-by-step Logic cho mỗi method
- Architectural Constraint diagram (ASCII art)
- Error Mapping per method
- Code hoàn chỉnh, production-ready
- Checklist 6 items

**Lỗ hổng:**

| # | Lỗ hổng | Mức độ | Hậu quả |
|---|---|---|---|
| DS-1 | **`getAllWorkOrders()` thiếu `Pageable` param** — ghi warning `findAll()` không pagination, nhưng `internal-coding-standards.md` đã define `PagedResponse<T>` chuẩn. Draft nên chốt: dùng `Pageable` hay không? | ⚠️ Trung bình | AI có thể sinh `findAll()` không có pagination |
| DS-2 | **`updateStatus()` thiếu log** cho case NOT_FOUND — chỉ log success, không log when entity not found (trước khi throw) | ⚠️ Nhỏ | Thiếu trace khi debug 404 |
| DS-3 | **`ResponseStatusException` vs Custom Exception** — Service throw `ResponseStatusException` (Spring Web dependency) vi phạm nguyên tắc "Service không phụ thuộc tầng Controller". Nên dùng custom `ResourceNotFoundException` rồi `GlobalExceptionHandler` map sang 404 | ⚠️ Trung bình | Coupling Service → Spring Web |

---

#### [draft-workorder-create.md](file:///c:/ai-native-oms-api/docs/drafts/draft-workorder-create.md) — ⭐ 92/100

**Điểm mạnh:**
- Step-by-step Controller + Service layers
- Error Mapping table 5 dòng
- Architectural Constraint callout
- Code hoàn chỉnh
- Checklist 6 items

**Lỗ hổng:**

| # | Lỗ hổng | Mức độ | Hậu quả |
|---|---|---|---|
| DC-1 | **RBAC mismatch** — Draft ghi `@PreAuthorize("hasRole('TECHNICIAN') or hasRole('DISPATCHER')")` nhưng `api-spec.md` ghi quyền là `DISPATCHER, TECHNICIAN, **ADMIN**`. Draft **thiếu ADMIN** | ⚠️ Trung bình | AI sinh code thiếu quyền cho ADMIN |
| DC-2 | **Thiếu `Location` header** trong code — api-spec ghi "trả về header `Location: /api/v1/workorders/{id}`" nhưng code chỉ return body, không set header | ⚠️ Trung bình | AI bỏ sót header chuẩn RESTful |
| DC-3 | **RFC 7807 `type` URI khác nhau** — Draft dùng `https://api.oms.gpc.com/errors/validation`, nhưng api-spec dùng `urn:problem-type:validation-error`. **Hai convention mâu thuẫn** | 🔴 Cao | AI chọn sai URI, response không nhất quán |

---

#### [draft-workorder-get.md](file:///c:/ai-native-oms-api/docs/drafts/draft-workorder-get.md) — ⭐ 90/100

**Điểm mạnh:**
- 2 endpoints rõ ràng, step-by-step logic cho cả Controller + Service
- Error Mapping per endpoint
- Pagination warning Open Question
- Checklist 5 items

**Lỗ hổng:**

| # | Lỗ hổng | Mức độ | Hậu quả |
|---|---|---|---|
| DG-1 | **RBAC sai** — `getAll()` ghi `@PreAuthorize("hasRole('DISPATCHER')")` chỉ cho DISPATCHER. Nhưng `api-spec.md` và `security-auth-spec.md` cho phép cả `DISPATCHER, TECHNICIAN, ADMIN` | 🔴 Cao | AI sinh code chặn TECHNICIAN/ADMIN xem danh sách |
| DG-2 | **`getById()` RBAC thiếu ADMIN** — giống DC-1 | ⚠️ Trung bình | Thiếu quyền ADMIN |
| DG-3 | **Thiếu filter logic** — `api-spec.md` ghi GET list có `@RequestParam status`, nhưng draft chỉ `findAll()` không filter | ⚠️ Trung bình | AI sinh endpoint không filter được |
| DG-4 | **RFC 7807 `type` URI** — cùng vấn đề DC-3, dùng `https://api.oms.gpc.com/errors/...` thay vì `urn:problem-type:...` | 🔴 Cao | Inconsistent với api-spec |

---

#### [draft-workorder-patch.md](file:///c:/ai-native-oms-api/docs/drafts/draft-workorder-patch.md) — ⭐ 91/100

**Điểm mạnh:**
- Error Mapping 6 dòng chi tiết
- Architectural Constraint flow diagram (ASCII)
- Cross-reference `draft-dtos.md` cho DTO
- Checklist 6 items

**Lỗ hổng:**

| # | Lỗ hổng | Mức độ | Hậu quả |
|---|---|---|---|
| DPT-1 | **RBAC thiếu ADMIN** — `api-spec.md` cho phép `TECHNICIAN, ADMIN` nhưng draft ghi `TECHNICIAN or DISPATCHER`. Vừa thiếu ADMIN vừa thêm sai DISPATCHER | 🔴 Cao | AI sinh authorization sai |
| DPT-2 | **RFC 7807 URI** — cùng vấn đề toàn cục DC-3 | 🔴 Cao | Inconsistent |

---

#### [draft-global-exception-handler.md](file:///c:/ai-native-oms-api/docs/drafts/draft-global-exception-handler.md) — ⭐ 94/100

**Điểm mạnh:**
- Error Mapping Summary Table 5 dòng (Exception → HTTP → Type → Trigger → Ưu tiên)
- 5 handlers đầy đủ: Validation, MalformedJSON, AccessDenied, ResponseStatus, Fallback
- `ProblemDetail` Spring Boot 3 native
- Không leak stack trace
- Import `AccessDeniedException` ghi rõ package
- Checklist 8 items

**Lỗ hổng:**

| # | Lỗ hổng | Mức độ | Hậu quả |
|---|---|---|---|
| DE-1 | **RFC 7807 `type` URI** — dùng `https://api.oms.gpc.com/errors/...` nhưng `api-rules.md` catalog dùng `urn:problem-type:...` | 🔴 Cao | Handler trả URI khác spec |
| DE-2 | **Handler #4 thiếu `title` field** — RFC 7807 yêu cầu `title` nhưng `handleResponseStatusException` chỉ set `type`, `status`, `detail`. Thiếu `problem.setTitle(...)` | ⚠️ Trung bình | Response RFC 7807 không đầy đủ |
| DE-3 | **Handler #1 set `detail` = "Validation Failed"** — nhưng RFC 7807 `detail` nên là "Validation failed for field: {fieldName}". Hiện tại `detail` giống `title` | ⚠️ Nhỏ | Minor, `invalidParams` bổ sung chi tiết |

---

#### [draft-workorder-tests.md](file:///c:/ai-native-oms-api/docs/drafts/draft-workorder-tests.md) — ⭐ 93/100

**Điểm mạnh:**
- Acceptance Matrix 16 rows (Given/When/Then)
- Unit test state machine (5 cases, không cần Spring)
- `@WebMvcTest` slice test đúng pattern
- Important callouts: Row 4 vs Row 5, Row 6 distinct handlers
- Boot 3.3 → `@MockBean` migration note
- Checklist 7 items

**Lỗ hổng:**

| # | Lỗ hổng | Mức độ | Hậu quả |
|---|---|---|---|
| DT-1 | **Row 8: TECHNICIAN cannot list → 403** — mâu thuẫn với `api-spec.md` cho phép TECHNICIAN xem list. Test case này sẽ fail nếu code đúng spec | 🔴 Cao | Test sai requirement |
| DT-2 | **Thiếu test case 401 Unauthorized** — Row 3 ghi "không auth" → 403, nhưng thực tế không có token → 401 (Spring Security default). Test nên phân biệt 401 (no auth) vs 403 (wrong role) | ⚠️ Trung bình | Test bỏ sót 401 case |
| DT-3 | **`@WithMockUser(roles = "DISPATCHER")` cho PATCH** — nhưng `api-spec.md` chỉ cho `TECHNICIAN, ADMIN`. DISPATCHER không được PATCH | ⚠️ Trung bình | Test dùng sai role |

---

### Nhóm C: Tài Liệu Hỗ Trợ (Support Docs) — ⭐ 90/100

| File | Điểm | Vai trò |
|---|:---:|---|
| [README.md](file:///c:/ai-native-oms-api/README.md) | 92/100 | Entry point. Rõ ràng, có badges, getting started, H2 console tip |
| [CONTRIBUTING.md](file:///c:/ai-native-oms-api/CONTRIBUTING.md) | 90/100 | PR process, AI-Generated Code Policy, links |
| [scorecard-workorder-create.md](file:///c:/ai-native-oms-api/docs/scorecard-workorder-create.md) | 88/100 | 12-criterion scorecard, honest Fail/Partial tracking |
| [review-workorder-create.md](file:///c:/ai-native-oms-api/docs/review-workorder-create.md) | 85/100 | 9-point review, actionable fixes |
| [audit-report-2026-09-23.md](file:///c:/ai-native-oms-api/docs/ai-context-auditor/audit-report-2026-09-23.md) | 95/100 | Comprehensive previous audit |
| [post-refactor-walkthrough](file:///c:/ai-native-oms-api/docs/ai-context-auditor/post-refactor-walkthrough-2026-09-23.md) | 93/100 | Change tracking |
| [final-workspace-audit](file:///c:/ai-native-oms-api/docs/ai-context-auditor/final-workspace-audit-report-2026-09-23.md) | 90/100 | Final validation |

---

## 🚨 3. Lỗ Hổng Ngữ Cảnh Toàn Cục (Cross-Cutting Context Gaps)

### 🔴 GAP #1 — RFC 7807 `type` URI Xung Đột (Critical — Ảnh hưởng 5+ files)

**Hai convention song song, không có quyết định chốt:**

| Nguồn | Convention | Ví dụ |
|---|---|---|
| `api-rules.md` (Error Catalog) | `urn:problem-type:{name}` | `urn:problem-type:validation-error` |
| `api-spec.md` (Endpoint spec) | `urn:problem-type:{name}` | `urn:problem-type:validation-error` |
| Tất cả Draft files | `https://api.oms.gpc.com/errors/{name}` | `https://api.oms.gpc.com/errors/validation` |
| `draft-global-exception-handler.md` | `https://api.oms.gpc.com/errors/{name}` | `https://api.oms.gpc.com/errors/validation` |

> [!CAUTION]
> Spec nền tảng dùng `urn:problem-type:*` nhưng toàn bộ draft implementation dùng `https://api.oms.gpc.com/errors/*`. Copilot sẽ sinh code dùng URL format (từ drafts) nhưng test assert URN format (từ spec), gây **test fail hàng loạt**.

### 🔴 GAP #2 — RBAC Permission Mâu Thuẫn Giữa Spec Và Draft

| Endpoint | `api-spec.md` | `security-auth-spec.md` | Draft file | Sai lệch |
|---|---|---|---|---|
| `POST /workorders` | `DISPATCHER, TECHNICIAN, ADMIN` | `DISPATCHER, TECHNICIAN, ADMIN` | `TECHNICIAN, DISPATCHER` | **Thiếu ADMIN** |
| `GET /workorders` (list) | `DISPATCHER, TECHNICIAN, ADMIN` | `DISPATCHER, TECHNICIAN, ADMIN` | `DISPATCHER` only | **Thiếu TECHNICIAN + ADMIN** |
| `GET /workorders/{id}` | `DISPATCHER, TECHNICIAN, ADMIN` | `DISPATCHER, TECHNICIAN, ADMIN` | `TECHNICIAN, DISPATCHER` | **Thiếu ADMIN** |
| `PATCH /workorders/{id}/status` | `TECHNICIAN, ADMIN` | `TECHNICIAN, ADMIN` | `TECHNICIAN, DISPATCHER` | **Thêm sai DISPATCHER, thiếu ADMIN** |

### ⚠️ GAP #3 — Pagination Chưa Được Chốt

- `api-spec.md` (§2) define Query Parameters cho pagination (`page`, `size`, `status`)
- `internal-coding-standards.md` define `PagedResponse<T>` hoàn chỉnh
- `draft-workorder-service.md` vẫn dùng `repo.findAll()` + open warning
- `draft-workorder-get.md` vẫn trả `List<WorkOrderResponse>` không pagination

**Cần chốt:** Service method dùng `Pageable` param hay không?

### ⚠️ GAP #4 — `ResponseStatusException` trong Service Layer

`draft-workorder-service.md` throw `ResponseStatusException` (thuộc `org.springframework.web.server`) trong Service layer. Điều này vi phạm nguyên tắc separation of concerns: Service phụ thuộc Spring Web layer. 

`api-spec.md` ghi throw `ResourceNotFoundException` nhưng class này chưa được define trong bất kỳ draft nào.

### ⚠️ GAP #5 — Thiếu Target Files Cho Security & Observability Specs

| File spec | Thiếu Target Files |
|---|---|
| `security-auth-spec.md` | `SecurityConfig.java`, `JwtAuthenticationFilter.java`, `CustomAccessDeniedHandler.java`, `CustomAuthenticationEntryPoint.java` |
| `observability-and-logging.md` | `CorrelationIdFilter.java`, `logback-spring.xml`, `application.yml` (metrics) |

---

## 📝 4. Đề Xuất Refactor (Actionable Fixes)

### Fix #1 — Thống nhất RFC 7807 `type` URI

Chọn **một** convention và apply toàn bộ. Khuyến nghị: giữ `urn:problem-type:*` (từ `api-rules.md`) vì đúng chuẩn RFC 7807.

**Sửa trong [draft-global-exception-handler.md](file:///c:/ai-native-oms-api/docs/drafts/draft-global-exception-handler.md):**

```diff
-        problem.setType(URI.create("https://api.oms.gpc.com/errors/validation"));
+        problem.setType(URI.create("urn:problem-type:validation-error"));

-        problem.setType(URI.create("https://api.oms.gpc.com/errors/validation"));
+        problem.setType(URI.create("urn:problem-type:malformed-json"));

-        problem.setType(URI.create("https://api.oms.gpc.com/errors/forbidden"));
+        problem.setType(URI.create("urn:problem-type:forbidden"));

-            problem.setType(URI.create("https://api.oms.gpc.com/errors/not-found"));
+            problem.setType(URI.create("urn:problem-type:not-found"));

-            problem.setType(URI.create("https://api.oms.gpc.com/errors/invalid-state-transition"));
+            problem.setType(URI.create("urn:problem-type:invalid-state-transition"));

-        problem.setType(URI.create("https://api.oms.gpc.com/errors/internal"));
+        problem.setType(URI.create("urn:problem-type:internal-error"));
```

Apply tương tự cho: `draft-workorder-create.md`, `draft-workorder-get.md`, `draft-workorder-patch.md`, `draft-workorder-tests.md`.

---

### Fix #2 — Đồng bộ RBAC trong Drafts

**Sửa trong [draft-workorder-create.md](file:///c:/ai-native-oms-api/docs/drafts/draft-workorder-create.md):**

```diff
-    @PreAuthorize("hasRole('TECHNICIAN') or hasRole('DISPATCHER')")
+    @PreAuthorize("hasAnyRole('DISPATCHER', 'TECHNICIAN', 'ADMIN')")
```

**Sửa trong [draft-workorder-get.md](file:///c:/ai-native-oms-api/docs/drafts/draft-workorder-get.md) — `getAll()`:**

```diff
-    @PreAuthorize("hasRole('DISPATCHER')")
+    @PreAuthorize("hasAnyRole('DISPATCHER', 'TECHNICIAN', 'ADMIN')")
```

**Sửa trong [draft-workorder-get.md](file:///c:/ai-native-oms-api/docs/drafts/draft-workorder-get.md) — `getById()`:**

```diff
-    @PreAuthorize("hasRole('TECHNICIAN') or hasRole('DISPATCHER')")
+    @PreAuthorize("hasAnyRole('DISPATCHER', 'TECHNICIAN', 'ADMIN')")
```

**Sửa trong [draft-workorder-patch.md](file:///c:/ai-native-oms-api/docs/drafts/draft-workorder-patch.md):**

```diff
-    @PreAuthorize("hasRole('TECHNICIAN') or hasRole('DISPATCHER')")
+    @PreAuthorize("hasAnyRole('TECHNICIAN', 'ADMIN')")
```

---

### Fix #3 — Bổ sung `@Size(min=10)` cho description trong draft-dtos.md

```diff
     @NotBlank(message = "description must not be blank")
-    @Size(max = 500, message = "description must not exceed 500 characters")
+    @Size(min = 10, max = 500, message = "description must be between 10 and 500 characters")
     String description,
```

---

### Fix #4 — Thêm `ResourceNotFoundException` vào draft-file-mapping.md

Thêm file mới vào bảng Production Code:

```markdown
| 11 | `ResourceNotFoundException.java` | `com.gpc.oms.exception` | `src/main/java/com/gpc/oms/exception/ResourceNotFoundException.java` | NEW | `api-spec.md` §3 |
```

Và cập nhật `draft-workorder-service.md` để dùng `ResourceNotFoundException` thay vì `ResponseStatusException`:

```diff
-import org.springframework.web.server.ResponseStatusException;
+import com.gpc.oms.exception.ResourceNotFoundException;

-    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Work Order Not Found"));
+    .orElseThrow(() -> new ResourceNotFoundException("WorkOrder not found with id: " + id));
```

---

### Fix #5 — Sửa test case RBAC trong draft-workorder-tests.md

```diff
-    // Row 8: TECHNICIAN cannot list → 403
-    @Test
-    @WithMockUser(roles = "TECHNICIAN")
-    void list_technicianRole_returns403() throws Exception {
-        mockMvc.perform(get("/api/v1/workorders")).andExpect(status().isForbidden());
-    }
+    // Row 8: TECHNICIAN can list → 200
+    @Test
+    @WithMockUser(roles = "TECHNICIAN")
+    void list_technicianRole_returns200() throws Exception {
+        when(workOrderService.getAllWorkOrders()).thenReturn(List.of());
+        mockMvc.perform(get("/api/v1/workorders"))
+            .andExpect(status().isOk())
+            .andExpect(jsonPath("$").isArray());
+    }
```

```diff
-    // Row 3: No auth → 403
+    // Row 3: No auth → 401 (Spring Security returns 401 when no token provided)
     @Test
     void create_noAuth_returns403() throws Exception {
         mockMvc.perform(post("/api/v1/workorders")
                 .contentType(MediaType.APPLICATION_JSON).content("{}"))
-            .andExpect(status().isForbidden());
+            .andExpect(status().isUnauthorized());
     }
```

---

## 📋 5. Danh Sách Hành Động Ưu Tiên

| Ưu tiên | Hành động | Files cần sửa | Effort |
|---|---|---|---|
| 🔴 P0 | **Thống nhất RFC 7807 `type` URI** → chốt `urn:problem-type:*` hoặc `https://` | 5 draft files + exception handler | 30 phút |
| 🔴 P0 | **Đồng bộ RBAC** giữa spec và draft | 4 draft files + test file | 20 phút |
| ⚠️ P1 | **Chốt pagination** → dùng `Pageable` + `PagedResponse` | service draft + get draft | 15 phút |
| ⚠️ P1 | **Tạo `ResourceNotFoundException`** thay `ResponseStatusException` trong Service | service draft + file mapping | 15 phút |
| ⚠️ P1 | **Fix `description` validation** — thêm `min=10` | draft-dtos.md | 5 phút |
| ⚠️ P1 | **Sửa test cases** — Row 3 (401), Row 8 (200) | draft-workorder-tests.md | 10 phút |
| ⚠️ P2 | **Thêm Target Files** cho security-auth-spec và observability | 2 spec files | 10 phút |
| ⚠️ P2 | **Bổ sung `Location` header** trong POST create draft | draft-workorder-create.md | 5 phút |

---

## 🎯 6. Kết Luận

Workspace `ai-native-oms-api` đạt **93/100** — mức **rất tốt** cho AI-Native SDLC. So với audit trước (62 → 91 → 93), tiến bộ rõ rệt.

**Điểm mạnh vượt trội:**
- Table-Driven Data chuẩn mực (Entity, DTO, DDL, RBAC Matrix, JWT Claims)
- Step-by-step pseudo-code ở mọi method
- Edge Cases Coverage với ma trận RFC 7807
- `draft-file-mapping.md` là best practice

**Rủi ro lớn nhất hiện tại:**
1. **RFC 7807 URI xung đột** (spec vs draft) → test sẽ fail
2. **RBAC mâu thuẫn** → code chặn sai role

> [!IMPORTANT]
> Sau khi sửa 2 vấn đề P0, workspace sẽ đạt **96-97/100** — mức zero-hallucination thực sự, an toàn để Copilot sinh toàn bộ codebase.
