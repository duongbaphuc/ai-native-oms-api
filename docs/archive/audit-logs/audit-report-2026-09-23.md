# 🔬 AI Context Audit Report — Outage Work Order API

**Ngày rà soát:** 2026-09-23  
**Auditor:** AI Context Auditor  
**Phạm vi:** 7 Draft files + 5 Context files  
**Mục tiêu:** Đánh giá mức độ sẵn sàng cho Copilot/LLM sinh code zero-hallucination

---

## 📊 Tổng Điểm: 62/100

| # | Tiêu chí | Điểm | Trạng thái |
|---|---|---|---|
| 1 | Table-Driven Data | 14/20 | ⚠️ Cần cải thiện |
| 2 | Step-by-step Logic | 8/20 | 🔴 Thiếu nghiêm trọng |
| 3 | Edge Cases Coverage | 16/20 | ✅ Khá tốt |
| 4 | Architectural Constraints | 10/20 | ⚠️ Cần cải thiện |
| 5 | Target File Mapping | 14/20 | ⚠️ Cần cải thiện |

---

## 🗂️ Phân tích Từng File

---

### File 1: [`draft-workorder-domain.md`](file:///c:/ai-native-oms-api/docs/drafts/draft-workorder-domain.md)

**Điểm riêng: 55/100**

#### ✅ Điểm mạnh
- Entity JPA có đầy đủ annotation `@Entity`, `@Table`, `@Column(nullable)`, `@Enumerated`
- State machine logic (`advanceStatus`) được đóng gói trong Entity — đúng Domain-Driven Design
- Enum `Priority` và `WorkOrderStatus` được khai báo rõ ràng

#### 🔴 Lỗ hổng Ngữ cảnh (Context Gaps)

| # | Lỗ hổng | Mức độ | Hậu quả nếu không sửa |
|---|---|---|---|
| D-1 | **Thiếu bảng mô tả Entity** — chỉ có code, không có Markdown Table 4 cột (Field / Type / Constraint / Description). Copilot sẽ không biết `equipmentId` max bao nhiêu ký tự | 🔴 Cao | AI tự đặt `@Size(max=255)` hoặc bỏ qua constraint |
| D-2 | **Xung đột Enum convention** với [`draft-dtos.md`](file:///c:/ai-native-oms-api/docs/drafts/draft-dtos.md) — Domain dùng `Open`, `InProgress`, `Done` (PascalCase); DTO dùng `OPEN`, `IN_PROGRESS`, `DONE` (UPPER_SNAKE) với `@JsonValue`. **Hai file mâu thuẫn nhau** | 🔴 Cao | AI sinh code sai enum value, runtime crash |
| D-3 | **Thiếu `@CreationTimestamp`** hoặc annotation auto-set cho `createdAt`. Constructor gán `Instant.now()` nhưng không rõ JPA callback hay application logic | ⚠️ Trung bình | AI không biết dùng `@PrePersist` hay constructor |
| D-4 | **`// ... getters omitted for brevity in draft ...`** — Copilot sẽ không biết generate Lombok `@Getter` hay manual getters | ⚠️ Trung bình | AI đoán sai convention |
| D-5 | **Thiếu package declaration** — không rõ Entity nằm ở package nào | ⚠️ Trung bình | AI tự đặt package path |
| D-6 | **State machine logic mâu thuẫn** — `advanceStatus` trong file này cho phép `Open → InProgress` HOẶC `Open → Done` (vì chỉ reject khi `newStatus == Open`). Nhưng [`draft-dtos.md`](file:///c:/ai-native-oms-api/docs/drafts/draft-dtos.md) `canTransitionTo()` chỉ cho phép `Open → InProgress` (strict linear). **Hai bản cài đặt khác nhau** | 🔴 Cao | AI chọn 1 trong 2, gây bug logic nghiệp vụ |

#### 📝 Đề xuất Refactor

Thay thế toàn bộ nội dung file bằng:

```markdown
<!--
Role: Senior Engineer. Task: Define WorkOrder entity, Enums, and Repository.
Context files: docs/01-domain-model.md, docs/00-coding-rules.md
Constraints: JPA Entity, UUID, Enum types. NO business logic leaking.
DRAFT ONLY — scoring target, never wired into app.
-->
# Draft: Work Order Domain

## 1. Entity Schema — `WorkOrder`

**Target file:** `src/main/java/com/gpc/oms/domain/WorkOrder.java`  
**Package:** `com.gpc.oms.domain`

| Field | Type | Constraint | Description |
|---|---|---|---|
| `id` | `UUID` | `@Id @GeneratedValue(UUID)`, PK | Định danh hệ thống, auto-gen |
| `equipmentId` | `String` | `@Column(nullable=false, length=50)` | Mã thiết bị lưới điện. Max 50 chars (khớp DTO) |
| `description` | `String` | `@Column(nullable=false, length=500)` | Mô tả sự cố. Max 500 chars (khớp DTO) |
| `priority` | `Priority` | `@Enumerated(STRING)`, `@Column(nullable=false)` | Mức độ: LOW, MEDIUM, HIGH, CRITICAL |
| `status` | `WorkOrderStatus` | `@Enumerated(STRING)`, `@Column(nullable=false)` | Trạng thái vòng đời |
| `createdAt` | `Instant` | `@Column(nullable=false)`, gán trong constructor | Thời điểm ghi nhận |
| `resolvedAt` | `Instant` | nullable, auto-set khi `status=DONE` | Thời điểm khắc phục xong |

## 2. Enums

### `Priority` — `src/main/java/com/gpc/oms/domain/Priority.java`
Giá trị: `LOW`, `MEDIUM`, `HIGH`, `CRITICAL`

### `WorkOrderStatus` — `src/main/java/com/gpc/oms/domain/WorkOrderStatus.java`
Giá trị nội bộ: `OPEN`, `IN_PROGRESS`, `DONE`  
JSON serialize: `"Open"`, `"InProgress"`, `"Done"` (qua `@JsonValue`)

**State Machine (one-way, strict linear):**
1. `OPEN` → `IN_PROGRESS` ✅
2. `IN_PROGRESS` → `DONE` ✅
3. Mọi chuyển đổi khác → `throw IllegalStateException`

## 3. Constructor Logic (Step-by-step)
1. Nhận 3 tham số: `equipmentId`, `description`, `priority`
2. Gán `this.status = WorkOrderStatus.OPEN`
3. Gán `this.createdAt = Instant.now()`
4. `resolvedAt` giữ `null`

## 4. Method `advanceStatus(WorkOrderStatus newStatus)` — Pseudo-code
1. Gọi `this.status.canTransitionTo(newStatus)`
2. Nếu `false` → `throw new IllegalStateException("Invalid state transition from " + this.status + " to " + newStatus)`
3. Nếu `true` → `this.status = newStatus`
4. Nếu `newStatus == DONE` → `this.resolvedAt = Instant.now()`

## 5. Repository — `src/main/java/com/gpc/oms/domain/WorkOrderRepository.java`
```java
public interface WorkOrderRepository extends JpaRepository<WorkOrder, UUID> {}
```

## 6. Getter Convention
- Dùng **manual getters** (KHÔNG dùng Lombok) — khớp `00-coding-rules.md` về implicit dependency.
- Generate getter cho tất cả 7 fields.

## 7. Checklist
- [ ] Entity dùng `@Table(name = "work_orders")`
- [ ] Enum convention khớp `draft-dtos.md` (UPPER_SNAKE nội bộ + @JsonValue)
- [ ] `canTransitionTo()` là source-of-truth cho state machine
- [ ] Protected no-arg constructor cho JPA
```

---

### File 2: [`draft-workorder-create.md`](file:///c:/ai-native-oms-api/docs/drafts/draft-workorder-create.md)

**Điểm riêng: 45/100**

#### 🔴 Lỗ hổng Ngữ cảnh (Context Gaps)

| # | Lỗ hổng | Mức độ | Hậu quả nếu không sửa |
|---|---|---|---|
| C-1 | **Controller gọi Repository trực tiếp** — vi phạm separation of concerns. `00-coding-rules.md` và `00-api-rules.md` không cấm nhưng `01-br-analysis-wo.md` mô tả kiến trúc 3 tầng. Copilot sẽ không biết có Service layer hay không | 🔴 Cao | AI sinh Controller fat, không có Service để unit test |
| C-2 | **Thiếu step-by-step logic** — chỉ có code block, không có pseudo-code mô tả luồng 1→2→3 | 🔴 Cao | Copilot phải reverse-engineer logic từ code |
| C-3 | **Thiếu error mapping table** — không liệt kê đầy đủ các lỗi có thể xảy ra khi POST (400 validation, 400 malformed JSON, 403 no auth, 500 unexpected) | ⚠️ Trung bình | AI bỏ sót edge case |
| C-4 | **`ResponseEntity.status(201)`** thay vì `HttpStatus.CREATED` — inconsistent với draft khác dùng `HttpStatus` constant | ⚠️ Nhỏ | Code style không đồng nhất |
| C-5 | **Không chỉ rõ file path đích** | ⚠️ Trung bình | AI đặt file sai thư mục |

#### 📝 Đề xuất Refactor

```markdown
<!--
Role: Senior Engineer. Task: POST /api/v1/workorders tạo WorkOrder.
Context files: docs/00-coding-rules.md, docs/00-api-rules.md, docs/00-security-rules.md.
Constraints: schema đúng docs/02-api-spec.md, lỗi RFC 7807, @Valid, JPA only.
DRAFT ONLY — scoring target, never wired into app.
-->
# Draft: POST /api/v1/workorders

## Target Files
| File | Path | Action |
|---|---|---|
| `WorkOrderController` | `src/main/java/com/gpc/oms/controller/WorkOrderController.java` | MODIFY (thêm method) |
| `WorkOrderService` | `src/main/java/com/gpc/oms/service/WorkOrderService.java` | NEW |

## Step-by-step Logic

### Controller Layer (`WorkOrderController.create`)
1. Nhận `@Valid @RequestBody WorkOrderRequest req`
2. Log: `log.info("create workorder equipmentIdHash={}", req.equipmentId().hashCode())` — KHÔNG log raw equipmentId (PII policy)
3. Gọi `workOrderService.createWorkOrder(req)` — delegate toàn bộ logic sang Service
4. Trả về `ResponseEntity.status(HttpStatus.CREATED).body(response)`

### Service Layer (`WorkOrderService.createWorkOrder`)
1. Tạo entity: `new WorkOrder(req.equipmentId(), req.description(), req.priority())`
2. Persist: `repo.save(entity)`
3. Convert sang DTO: `WorkOrderResponse.from(savedEntity)`
4. Return DTO

## Error Mapping

| Điều kiện vi phạm | HTTP Status | RFC 7807 type | Message |
|---|---|---|---|
| Body thiếu field bắt buộc (`@NotBlank`, `@NotNull`) | 400 | `.../errors/validation` | `invalidParams` chi tiết |
| Body có field lạ (`ignoreUnknown=false`) | 400 | `.../errors/validation` | `Malformed Request Body` |
| Enum value không hợp lệ (vd: `"URGENT"`) | 400 | `.../errors/validation` | `invalidParams[].name=body` |
| Không có auth hoặc role không đủ | 403 | `.../errors/forbidden` | `Access Denied` |
| Lỗi hệ thống không mong đợi | 500 | `.../errors/internal` | `An unexpected error occurred` |

## Architectural Constraint
- Controller **KHÔNG chứa business logic**. Chỉ: validate → delegate → return.
- Service chứa orchestration logic. Repository chỉ persistence.
```

---

### File 3: [`draft-workorder-get.md`](file:///c:/ai-native-oms-api/docs/drafts/draft-workorder-get.md)

**Điểm riêng: 42/100**

#### 🔴 Lỗ hổng Ngữ cảnh (Context Gaps)

| # | Lỗ hổng | Mức độ |
|---|---|---|
| G-1 | **Controller gọi `repo` trực tiếp** — cùng vấn đề C-1, không qua Service layer | 🔴 Cao |
| G-2 | **Thiếu step-by-step logic** — chỉ có code, không có pseudo-code | 🔴 Cao |
| G-3 | **GET list không có pagination** — `repo.findAll()` sẽ gây OOM nếu table lớn. `02-api-spec.md` cũng không đề cập pagination. **Đây là context gap ở cấp spec, cần quyết định từ PO** | ⚠️ Trung bình |
| G-4 | **Thiếu error mapping table** cho cả 2 endpoint | ⚠️ Trung bình |
| G-5 | **Không chỉ rõ file path đích** — GET methods nằm trong class nào? | ⚠️ Trung bình |
| G-6 | **`WorkOrderResponse.from(wo)`** — static factory method này chưa được define trong domain draft. Chỉ được define ở [`draft-dtos.md`](file:///c:/ai-native-oms-api/docs/drafts/draft-dtos.md) nhưng thiếu implementation code | ⚠️ Trung bình |

#### 📝 Đề xuất Refactor

```markdown
<!--
Role: Senior Engineer. Task: GET /api/v1/workorders và GET /api/v1/workorders/{id}.
Context files: docs/02-api-spec.md, docs/00-security-rules.md
Constraints: Phân quyền RBAC, trả WorkOrderResponse, 404 RFC 7807.
DRAFT ONLY — scoring target, never wired into app.
-->
# Draft: GET /api/v1/workorders

## Target Files
| File | Path | Action |
|---|---|---|
| `WorkOrderController` | `src/main/java/com/gpc/oms/controller/WorkOrderController.java` | MODIFY (thêm 2 methods) |
| `WorkOrderService` | `src/main/java/com/gpc/oms/service/WorkOrderService.java` | MODIFY (thêm 2 methods) |

## Endpoint 1: GET / (List All)

### Step-by-step Logic
**Controller (`getAll`):**
1. Annotation: `@GetMapping` + `@PreAuthorize("hasRole('DISPATCHER')")`
2. Log: `log.info("get all workorders")`
3. Gọi `workOrderService.getAllWorkOrders()`
4. Return `ResponseEntity.ok(list)`

**Service (`getAllWorkOrders`):**
1. Gọi `repo.findAll()`
2. Map mỗi entity sang `WorkOrderResponse.from(entity)`
3. Return `List<WorkOrderResponse>`

> [!WARNING]
> **Open Question:** `findAll()` không có pagination. Nếu table > 10K rows, cần thêm `Pageable` param. Cần xác nhận với PO về tầm scale.

### Error Mapping
| Điều kiện | HTTP | type |
|---|---|---|
| Role không phải DISPATCHER | 403 | `.../errors/forbidden` |

---

## Endpoint 2: GET /{id} (Get By ID)

### Step-by-step Logic
**Controller (`getById`):**
1. Annotation: `@GetMapping("/{id}")` + `@PreAuthorize("hasRole('TECHNICIAN') or hasRole('DISPATCHER')")`
2. Nhận `@PathVariable UUID id`
3. Log: `log.info("get workorder by id={}", id)`
4. Gọi `workOrderService.getWorkOrderById(id)`
5. Return `ResponseEntity.ok(response)`

**Service (`getWorkOrderById`):**
1. Gọi `repo.findById(id)`
2. Nếu `Optional.empty()` → `throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Work Order Not Found")`
3. Convert sang `WorkOrderResponse.from(entity)`
4. Return DTO

### Error Mapping
| Điều kiện | HTTP | type | Message |
|---|---|---|---|
| ID không tồn tại | 404 | `.../errors/not-found` | `Work Order Not Found` |
| Role không đủ quyền | 403 | `.../errors/forbidden` | `Access Denied` |
| ID format không phải UUID | 400 | `.../errors/validation` | `Invalid UUID format` |
```

---

### File 4: [`draft-workorder-patch.md`](file:///c:/ai-native-oms-api/docs/drafts/draft-workorder-patch.md)

**Điểm riêng: 60/100**

#### ✅ Điểm mạnh
- Đóng gói state machine rule trong Entity (`wo.advanceStatus`)
- Map `IllegalStateException` → 422 RFC 7807 rõ ràng
- `StatusUpdateRequest` record có `@JsonIgnoreProperties(ignoreUnknown = false)`

#### 🔴 Lỗ hổng Ngữ cảnh

| # | Lỗ hổng | Mức độ |
|---|---|---|
| P-1 | **Controller gọi repo trực tiếp** — vẫn cùng pattern vi phạm | 🔴 Cao |
| P-2 | **`StatusUpdateRequest` khai báo trong file này** — mâu thuẫn với [`draft-dtos.md`](file:///c:/ai-native-oms-api/docs/drafts/draft-dtos.md) đã khai báo `WorkOrderStatusRequest` (tên khác, cùng schema). Copilot sẽ tạo duplicate class | 🔴 Cao |
| P-3 | **Thiếu step-by-step pseudo-code** | ⚠️ Trung bình |
| P-4 | **Không chỉ rõ file path đích** | ⚠️ Trung bình |

---

### File 5: [`draft-global-exception-handler.md`](file:///c:/ai-native-oms-api/docs/drafts/draft-global-exception-handler.md)

**Điểm riêng: 75/100**

#### ✅ Điểm mạnh
- Phủ đầy đủ 5 loại exception: Validation(400), ResponseStatus(404/422), AccessDenied(403), MalformedJSON(400), Unexpected(500)
- RFC 7807 `ProblemDetail` chuẩn Spring Boot 3
- Không leak stack trace ra client
- Mỗi handler có comment tiếng Việt giải thích context

#### ⚠️ Lỗ hổng Ngữ cảnh

| # | Lỗ hổng | Mức độ |
|---|---|---|
| E-1 | **Thiếu error mapping summary table** — liệt kê Exception → Status → Type URI → Response body trong 1 table để Copilot cross-reference nhanh | ⚠️ Trung bình |
| E-2 | **Thiếu `@ResponseStatus` annotation** trên mỗi handler hoặc ghi chú rằng `ProblemDetail` tự set status | ⚠️ Nhỏ |
| E-3 | **Thiếu import list** — `AccessDeniedException` là từ `org.springframework.security.access` hay `java.nio.file`? Copilot có thể import sai | ⚠️ Trung bình |
| E-4 | **Chồng chéo handler** — `ResponseStatusException` handler xử lý cả `FORBIDDEN`, nhưng đã có `AccessDeniedException` handler riêng. Cần ghi rõ thứ tự ưu tiên | ⚠️ Nhỏ |

---

### File 6: [`draft-dtos.md`](file:///c:/ai-native-oms-api/docs/drafts/draft-dtos.md)

**Điểm riêng: 88/100** ⭐ File tốt nhất trong bộ draft

#### ✅ Điểm mạnh
- Table-driven: Bảng Validation tổng hợp (Section 4) rất chuẩn
- Có example payload hợp lệ VÀ bị reject
- Checklist trước khi implement (Section 5)
- `@JsonIgnoreProperties(ignoreUnknown = false)` trên mọi Request DTO
- AI Provenance comment header — best practice
- Liên kết cross-reference đến `02-api-spec.md` và `01-domain-model.md`

#### ⚠️ Lỗ hổng Ngữ cảnh

| # | Lỗ hổng | Mức độ |
|---|---|---|
| DTO-1 | **`WorkOrderResponse` thiếu static factory method `from(WorkOrder)`** — các draft API đều gọi `WorkOrderResponse.from(wo)` nhưng method này chưa được define | 🔴 Cao |
| DTO-2 | **Thiếu target file path** cho từng DTO — chỉ có package, chưa có full path | ⚠️ Trung bình |
| DTO-3 | **`canTransitionTo()` nằm trong Enum `WorkOrderStatus`** (DTO file) nhưng domain draft lại đặt logic state machine trong Entity method `advanceStatus()`. Cần ghi rõ ai là source-of-truth | ⚠️ Trung bình |

#### 📝 Đề xuất bổ sung — `WorkOrderResponse.from()`:

Thêm vào Section 3.1 sau record declaration:

```java
    /**
     * Factory method: convert JPA Entity → Response DTO.
     * Được gọi bởi Controller/Service sau mỗi thao tác CRUD.
     */
    public static WorkOrderResponse from(WorkOrder entity) {
        return new WorkOrderResponse(
            entity.getId(),
            entity.getEquipmentId(),
            entity.getDescription(),
            entity.getPriority(),
            entity.getStatus(),
            entity.getCreatedAt(),
            entity.getResolvedAt()
        );
    }
```

---

### File 7: [`draft-workorder-tests.md`](file:///c:/ai-native-oms-api/docs/drafts/draft-workorder-tests.md)

**Điểm riêng: 82/100**

#### ✅ Điểm mạnh
- Acceptance matrix Given/When/Then — 15 test cases phủ đầy đủ happy path + edge cases
- Unit test state machine (không cần Spring context)
- `@WebMvcTest` slice test đúng pattern
- Ghi chú Boot 3.3.4 → `@MockBean` vs Boot 3.4+ → `@MockitoBean` — AI-aware

#### ⚠️ Lỗ hổng Ngữ cảnh

| # | Lỗ hổng | Mức độ |
|---|---|---|
| T-1 | **Matrix row 4** — `priority = URGENT` sẽ gây `HttpMessageNotReadableException` (malformed enum), KHÔNG phải `MethodArgumentNotValidException`. Test sketch assert `invalidParams[0].name=priority` nhưng handler `handleMalformedJson` trả `invalidParams[0].name=body`. **Mâu thuẫn giữa matrix và code** | 🔴 Cao |
| T-2 | **Thiếu test case cho `ignoreUnknown=false`** — matrix ghi nhận ở row "Ghi chú review" nhưng không có test sketch | ⚠️ Trung bình |
| T-3 | **Test sketch dùng `Status.InProgress`** (PascalCase) nhưng domain có thể là `WorkOrderStatus.IN_PROGRESS` (UPPER_SNAKE). Cần thống nhất | ⚠️ Trung bình |
| T-4 | **Target file path thiếu** — test class nên ở `src/test/java/com/gpc/oms/...` | ⚠️ Nhỏ |

---

## 🏗️ Phân tích Chéo — Các Vấn đề Toàn Cục

### 🔴 Vấn đề Nghiêm Trọng #1: Không có Service Layer — Mâu thuẫn Kiến trúc

[`draft-workorder-create.md`](file:///c:/ai-native-oms-api/docs/drafts/draft-workorder-create.md), [`draft-workorder-get.md`](file:///c:/ai-native-oms-api/docs/drafts/draft-workorder-get.md), [`draft-workorder-patch.md`](file:///c:/ai-native-oms-api/docs/drafts/draft-workorder-patch.md) đều cho Controller gọi `repo` trực tiếp. Nhưng [`01-br-analysis-wo.md`](file:///c:/ai-native-oms-api/docs/01-br-analysis-wo.md) mô tả kiến trúc 3 tầng (UI/API/Data). 

**Quyết định cần từ bạn:** Controller → Repository (2-tier) hay Controller → Service → Repository (3-tier)?

> [!IMPORTANT]
> Nếu chọn 3-tier, cần tạo file mới: `src/main/java/com/gpc/oms/service/WorkOrderService.java` và update tất cả 3 draft API.

### 🔴 Vấn đề Nghiêm Trọng #2: Enum Convention Mâu Thuẫn

| File | Enum Value `Status` | Convention |
|---|---|---|
| [`draft-workorder-domain.md`](file:///c:/ai-native-oms-api/docs/drafts/draft-workorder-domain.md) L:42,62 | `Open`, `InProgress`, `Done` | PascalCase (Java enum name = JSON value) |
| [`draft-dtos.md`](file:///c:/ai-native-oms-api/docs/drafts/draft-dtos.md) L:53-76 | `OPEN`, `IN_PROGRESS`, `DONE` + `@JsonValue("Open")` | UPPER_SNAKE + custom serialization |
| [`draft-workorder-tests.md`](file:///c:/ai-native-oms-api/docs/drafts/draft-workorder-tests.md) L:39 | `Status.InProgress` | PascalCase (khớp domain draft) |

**Quyết định cần từ bạn:** Chuẩn hóa theo bản nào? Khuyến nghị: **`draft-dtos.md`** (UPPER_SNAKE + `@JsonValue`) vì tuân thủ Java Enum naming convention.

### 🔴 Vấn đề Nghiêm Trọng #3: State Machine Logic Duplicate & Mâu Thuẫn

| File | Method | Logic |
|---|---|---|
| [`draft-workorder-domain.md`](file:///c:/ai-native-oms-api/docs/drafts/draft-workorder-domain.md) L:49-58 | `advanceStatus()` | Reject khi: `status==Done`, `newStatus==Open`, hoặc `(status==InProgress && newStatus!=Done)`. **Cho phép `Open → Done` skip** |
| [`draft-dtos.md`](file:///c:/ai-native-oms-api/docs/drafts/draft-dtos.md) L:69-75 | `canTransitionTo()` | Strict: `OPEN→IN_PROGRESS` only, `IN_PROGRESS→DONE` only. **Cấm skip** |

> [!CAUTION]
> Hai file có logic mâu thuẫn nhau. `01-domain-model.md` nói "tuyến tính" → `draft-dtos.md` là đúng. File `draft-workorder-domain.md` cần sửa `advanceStatus()` để dùng `canTransitionTo()`.

### ⚠️ Vấn đề #4: Tên DTO Request Không Thống Nhất

| File | Tên class |
|---|---|
| [`draft-workorder-patch.md`](file:///c:/ai-native-oms-api/docs/drafts/draft-workorder-patch.md) L:33 | `StatusUpdateRequest` |
| [`draft-dtos.md`](file:///c:/ai-native-oms-api/docs/drafts/draft-dtos.md) L:163 | `WorkOrderStatusRequest` |

**Khuyến nghị:** Dùng `WorkOrderStatusRequest` (từ `draft-dtos.md`) làm source-of-truth.

### ⚠️ Vấn đề #5: `02-api-spec.md` dùng `"id": "WO-10432"` (String prefix) nhưng Domain dùng `UUID`

[`02-api-spec.md`](file:///c:/ai-native-oms-api/docs/02-api-spec.md) L:19 response example có `"id": "WO-10432"` nhưng entity dùng `UUID`. Đây là lỗi trong example JSON của spec hay đặc tả format hiển thị?

---

## 📋 Target File Mapping — Bảng Tổng Hợp (Thiếu trong các Draft)

Đây là bảng mà **tất cả các draft đều thiếu** — cần bổ sung vào mỗi file hoặc tạo 1 file `draft-file-mapping.md` chung:

| # | File Java | Package | Action | Draft nguồn |
|---|---|---|---|---|
| 1 | `WorkOrder.java` | `com.gpc.oms.domain` | NEW | `draft-workorder-domain.md` |
| 2 | `Priority.java` | `com.gpc.oms.domain` | NEW | `draft-dtos.md` |
| 3 | `WorkOrderStatus.java` | `com.gpc.oms.domain` | NEW | `draft-dtos.md` |
| 4 | `WorkOrderRepository.java` | `com.gpc.oms.domain` | NEW | `draft-workorder-domain.md` |
| 5 | `WorkOrderRequest.java` | `com.gpc.oms.dto` | NEW | `draft-dtos.md` |
| 6 | `WorkOrderStatusRequest.java` | `com.gpc.oms.dto` | NEW | `draft-dtos.md` |
| 7 | `WorkOrderResponse.java` | `com.gpc.oms.dto` | NEW | `draft-dtos.md` |
| 8 | `WorkOrderController.java` | `com.gpc.oms.controller` | NEW | `draft-workorder-create/get/patch.md` |
| 9 | `WorkOrderService.java` | `com.gpc.oms.service` | NEW | *(chưa có draft — cần tạo)* |
| 10 | `GlobalExceptionHandler.java` | `com.gpc.oms.exception` | NEW | `draft-global-exception-handler.md` |
| 11 | `WorkOrderTest.java` | `com.gpc.oms.domain` (test) | NEW | `draft-workorder-tests.md` |
| 12 | `WorkOrderControllerTest.java` | `com.gpc.oms.controller` (test) | NEW | `draft-workorder-tests.md` |

---

## 🎯 Danh sách Hành động Ưu tiên

| Ưu tiên | Hành động | File cần sửa |
|---|---|---|
| 🔴 P0 | **Thống nhất Enum convention** → chọn UPPER_SNAKE + `@JsonValue` | `draft-workorder-domain.md`, `draft-workorder-tests.md` |
| 🔴 P0 | **Sửa state machine logic** → dùng `canTransitionTo()` strict linear | `draft-workorder-domain.md` |
| 🔴 P0 | **Quyết định 2-tier vs 3-tier** → nếu 3-tier, tạo `draft-workorder-service.md` | Tất cả API drafts |
| 🔴 P0 | **Thống nhất tên DTO** → `WorkOrderStatusRequest` | `draft-workorder-patch.md` |
| ⚠️ P1 | **Thêm Entity schema table** (4 cột) vào domain draft | `draft-workorder-domain.md` |
| ⚠️ P1 | **Thêm step-by-step pseudo-code** vào 3 API drafts | `draft-workorder-create/get/patch.md` |
| ⚠️ P1 | **Thêm error mapping table** vào 3 API drafts | `draft-workorder-create/get/patch.md` |
| ⚠️ P1 | **Thêm `WorkOrderResponse.from()` implementation** | `draft-dtos.md` |
| ⚠️ P1 | **Sửa `02-api-spec.md` example** — `id` nên là UUID, không phải `"WO-10432"` | `02-api-spec.md` |
| ⚠️ P2 | **Thêm target file path** vào tất cả drafts | Tất cả |
| ⚠️ P2 | **Thêm import list** cho exception handler | `draft-global-exception-handler.md` |
| ⚠️ P2 | **Sửa test row 4** — assert đúng handler cho invalid enum | `draft-workorder-tests.md` |
