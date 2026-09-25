<!--
Role: Senior Engineer.
Task: Định nghĩa toàn bộ DTOs (Data Transfer Objects) cho Outage Work Order API.
Context files: docs/02-api-spec.md, docs/01-domain-model.md, docs/00-coding-rules.md, docs/00-api-rules.md, docs/00-security-rules.md.
Constraints:
  - Java records (immutable), không dùng class thông thường.
  - Strict schema: @JsonIgnoreProperties(ignoreUnknown = false) trên mọi Request DTO.
  - Bean Validation: @NotBlank, @NotNull, @Size — chỉ ở boundary (Controller), không rải vào Service.
  - Fields phải khớp 1:1 với docs/02-api-spec.md, không được tự ý thêm field.
  - Enum values phải khớp docs/01-domain-model.md.
  - AI Provenance phải được ghi chú trong header mỗi file.
DRAFT ONLY — scoring target, chưa được wire vào application.
-->

# Draft: DTOs — Outage Work Order API

Tài liệu này định nghĩa toàn bộ Data Transfer Objects (DTOs) sẽ được implement cho dự án.
Mọi field, kiểu dữ liệu và annotation phải khớp với [`docs/02-api-spec.md`](../02-api-spec.md) và [`docs/01-domain-model.md`](../01-domain-model.md).

## Target Files

| # | File | Path | Action |
|---|---|---|---|
| 1 | `WorkOrderRequest.java` | `src/main/java/com/gpc/oms/dto/WorkOrderRequest.java` | NEW |
| 2 | `WorkOrderStatusRequest.java` | `src/main/java/com/gpc/oms/dto/WorkOrderStatusRequest.java` | NEW |
| 3 | `WorkOrderResponse.java` | `src/main/java/com/gpc/oms/dto/WorkOrderResponse.java` | NEW |
| 4 | `PagedResponse.java` | `src/main/java/com/gpc/oms/dto/PagedResponse.java` | NEW |

---

## 1. Domain Enums (Shared References)

Hai Enums `Priority` và `WorkOrderStatus` thuộc `package com.gpc.oms.domain`, được định nghĩa là **Single Source of Truth** tại [`draft-workorder-domain.md`](draft-workorder-domain.md) §5. Tầng DTO import và sử dụng trực tiếp các Enums này:

- **`com.gpc.oms.domain.Priority`:** `LOW`, `MEDIUM`, `HIGH`, `CRITICAL`
- **`com.gpc.oms.domain.WorkOrderStatus`:** `OPEN("Open")`, `IN_PROGRESS("InProgress")`, `DONE("Done")` với Jackson `@JsonValue`.

> [!IMPORTANT]
> `canTransitionTo()` trong Enum `WorkOrderStatus` là **source-of-truth** duy nhất cho state machine logic, được gọi bởi `WorkOrder.advanceStatus()` tại **domain/entity layer**. Vi phạm → ném `IllegalStateException` → Service re-throw → `GlobalExceptionHandler` trả **422 Unprocessable Entity** (RFC 7807 `urn:problem-type:invalid-state-transition`).

---

## 2. Request DTOs

### 2.1 `WorkOrderRequest` — `POST /api/v1/workorders`

**Target file:** `src/main/java/com/gpc/oms/dto/WorkOrderRequest.java`  
Nguồn spec: [`02-api-spec.md §1`](../02-api-spec.md).

**Schema Table:**

| Field | Type | Annotation | Description |
|---|---|---|---|
| `equipmentId` | `String` | `@NotBlank`, `@Size(max=50)` | Mã thiết bị lưới điện |
| `description` | `String` | `@NotBlank`, `@Size(min=10, max=500)` | Mô tả sự cố (tối thiểu 10, tối đa 500 ký tự) |
| `priority` | `Priority` | `@NotNull` | Mức độ: LOW, MEDIUM, HIGH, CRITICAL |

**Ràng buộc:**
- `@JsonIgnoreProperties(ignoreUnknown = false)` → field lạ bị reject với 400.
- `@NotBlank` / `@Size` trên string fields.
- `@NotNull` trên enum `priority` — Jackson tự reject giá trị enum không hợp lệ với 400.
- KHÔNG có field `status`, `id`, `createdAt` — không được tự ý thêm.

```java
// AI Provenance: generated from docs/02-api-spec.md §1, docs/01-domain-model.md, docs/00-coding-rules.md
package com.gpc.oms.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.gpc.oms.domain.Priority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@JsonIgnoreProperties(ignoreUnknown = false)
public record WorkOrderRequest(

    @NotBlank(message = "equipmentId must not be blank")
    @Size(max = 50, message = "equipmentId must not exceed 50 characters")
    String equipmentId,

    @NotBlank(message = "description must not be blank")
    @Size(min = 10, max = 500, message = "description must be between 10 and 500 characters")
    String description,

    @NotNull(message = "priority must not be null; valid values: LOW, MEDIUM, HIGH, CRITICAL")
    Priority priority

) {}
```

**Ví dụ payload hợp lệ:**
```json
{
  "equipmentId": "EQ-77",
  "description": "Máy biến áp T3 khu vực Bình Thạnh bị quá tải, mất điện toàn bộ tuyến",
  "priority": "HIGH"
}
```

**Ví dụ payload bị reject (field lạ):**
```json
{
  "equipmentId": "EQ-77",
  "description": "...",
  "priority": "HIGH",
  "status": "Open"   ← BỊ REJECT — 400 Bad Request
}
```

---

### 2.2 `WorkOrderStatusRequest` — `PATCH /api/v1/workorders/{id}/status`

**Target file:** `src/main/java/com/gpc/oms/dto/WorkOrderStatusRequest.java`  
Nguồn spec: [`02-api-spec.md §4`](../02-api-spec.md).

**Schema Table:**

| Field | Type | Annotation | Description |
|---|---|---|---|
| `status` | `WorkOrderStatus` | `@NotNull` | Trạng thái mới: Open, InProgress, Done |

**Ràng buộc:**
- Chỉ có 1 field duy nhất: `status`.
- `@JsonIgnoreProperties(ignoreUnknown = false)` — field lạ bị reject.
- `@NotNull` — thiếu field → 400.
- Validation logic chuyển trạng thái (one-way) nằm ở **Entity layer** (`advanceStatus()`), không phải DTO.

```java
// AI Provenance: generated from docs/02-api-spec.md §4, docs/01-domain-model.md §Invariants
package com.gpc.oms.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.gpc.oms.domain.WorkOrderStatus;
import jakarta.validation.constraints.NotNull;

@JsonIgnoreProperties(ignoreUnknown = false)
public record WorkOrderStatusRequest(

    @NotNull(message = "status must not be null; valid values: Open, InProgress, Done")
    WorkOrderStatus status

) {}
```

**Ví dụ payload hợp lệ:**
```json
{
  "status": "InProgress"
}
```

---

## 3. Response DTO

### 3.1 `WorkOrderResponse` — Dùng chung cho tất cả endpoints

**Target file:** `src/main/java/com/gpc/oms/dto/WorkOrderResponse.java`  
Nguồn spec: [`02-api-spec.md §1–§4`](../02-api-spec.md).

**Schema Table:**

| Field | Type | Nullable | Description |
|---|---|---|---|
| `id` | `UUID` | No | Định danh hệ thống |
| `equipmentId` | `String` | No | Mã thiết bị |
| `description` | `String` | No | Mô tả sự cố |
| `priority` | `Priority` | No | Mức độ ưu tiên |
| `status` | `WorkOrderStatus` | No | Trạng thái vòng đời (serialize: "Open"/"InProgress"/"Done") |
| `createdAt` | `Instant` | No | Thời điểm tạo |
| `resolvedAt` | `Instant` | Yes | Thời điểm giải quyết (null nếu chưa Done) |

**Ràng buộc:**
- Immutable record — không có setter.
- Fields khớp 1:1 với 02-api-spec.md response schema.
- `resolvedAt` là `Instant` (nullable) — tự động gán khi `status = DONE`.
- Không expose JPA entity trực tiếp ra ngoài — luôn convert sang DTO này.

```java
// AI Provenance: generated from docs/02-api-spec.md §1–§4, docs/01-domain-model.md
package com.gpc.oms.dto;

import com.gpc.oms.domain.Priority;
import com.gpc.oms.domain.WorkOrder;
import com.gpc.oms.domain.WorkOrderStatus;

import java.time.Instant;
import java.util.UUID;

public record WorkOrderResponse(
    UUID             id,
    String           equipmentId,
    String           description,
    Priority         priority,
    WorkOrderStatus  status,
    Instant          createdAt,
    Instant          resolvedAt   // null cho đến khi status = DONE
) {

    /**
     * Factory method: convert JPA Entity → Response DTO.
     * Được gọi bởi WorkOrderService sau mỗi thao tác CRUD.
     * 
     * Step-by-step:
     * 1. Nhận WorkOrder entity
     * 2. Extract tất cả 7 fields qua getter
     * 3. Return new WorkOrderResponse record
     */
    public static WorkOrderResponse from(final WorkOrder entity) {
        java.util.Objects.requireNonNull(entity, "workOrder must not be null");
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
}
```

**Ví dụ response (201 Created):**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "equipmentId": "EQ-77",
  "description": "Máy biến áp T3 khu vực Bình Thạnh bị quá tải, mất điện toàn bộ tuyến",
  "priority": "HIGH",
  "status": "Open",
  "createdAt": "2026-08-30T09:15:00Z",
  "resolvedAt": null
}
```

### 3.2 `PagedResponse<T>` — Generic Pagination Wrapper

**Target file:** `src/main/java/com/gpc/oms/dto/PagedResponse.java`  
Nguồn spec: [`00-internal-coding-standards.md §3`](../00-internal-coding-standards.md), [`02-api-spec.md §2`](../02-api-spec.md).

**Schema Table:**

| Field | Type | Nullable | Description |
|---|---|---|---|
| `content` | `List<T>` | No | Danh sách đối tượng dữ liệu trong trang hiện tại |
| `pageNumber` | `int` | No | Chỉ số trang hiện tại (0-indexed) |
| `pageSize` | `int` | No | Số lượng bản ghi tối đa mỗi trang |
| `totalElements` | `long` | No | Tổng số bản ghi thỏa điều kiện lọc trong CSDL |
| `totalPages` | `int` | No | Tổng số trang |
| `isFirst` | `boolean` | No | `true` nếu là trang đầu tiên |
| `isLast` | `boolean` | No | `true` nếu là trang cuối cùng |

```java
// AI Provenance: generated from docs/00-internal-coding-standards.md §3, docs/02-api-spec.md §2
package com.gpc.oms.dto;

import org.springframework.data.domain.Page;
import java.util.List;

public record PagedResponse<T>(
    List<T> content,
    int pageNumber,
    int pageSize,
    long totalElements,
    int totalPages,
    boolean isFirst,
    boolean isLast
) {
    public static <T> PagedResponse<T> from(final Page<T> page) {
        java.util.Objects.requireNonNull(page, "page must not be null");
        return new PagedResponse<>(
            page.getContent(),
            page.getNumber(),
            page.getSize(),
            page.getTotalElements(),
            page.getTotalPages(),
            page.isFirst(),
            page.isLast()
        );
    }
}
```

---

## 4. Bảng Tổng Hợp Validation

| DTO | Field | Annotation | Mô tả vi phạm → HTTP |
|---|---|---|---|
| `WorkOrderRequest` | `equipmentId` | `@NotBlank`, `@Size(max=50)` | Trống hoặc vượt 50 ký tự → **400** |
| `WorkOrderRequest` | `description` | `@NotBlank`, `@Size(min=10, max=500)` | Trống, dưới 10, hoặc vượt 500 ký tự → **400** |
| `WorkOrderRequest` | `priority` | `@NotNull` | Null hoặc giá trị lạ → **400** |
| `WorkOrderRequest` | *(extra field)* | `ignoreUnknown=false` | Field không khai báo → **400** |
| `WorkOrderStatusRequest` | `status` | `@NotNull` | Null hoặc giá trị lạ → **400** |
| `WorkOrderStatusRequest` | *(extra field)* | `ignoreUnknown=false` | Field không khai báo → **400** |
| `WorkOrderStatusRequest` | `status` | *(Entity layer)* | Chuyển trạng thái ngược/nhảy cóc → **422** |

---

## 5. Checklist Trước Khi Implement

- [ ] `WorkOrderRequest` dùng Java `record`, KHÔNG dùng `class`.
- [ ] `@JsonIgnoreProperties(ignoreUnknown = false)` có mặt trên tất cả Request DTOs.
- [ ] `WorkOrderResponse` KHÔNG có `@JsonIgnoreProperties` — response chỉ có chiều ra.
- [ ] `WorkOrderResponse.from(WorkOrder)` static factory method đã được implement.
- [ ] `WorkOrderStatus` serialize ra `"Open"` / `"InProgress"` / `"Done"` (dùng `@JsonValue`).
- [ ] Enum convention: UPPER_SNAKE nội bộ (`OPEN`, `IN_PROGRESS`, `DONE`).
- [ ] `resolvedAt` có kiểu `Instant`, KHÔNG phải `String` hay `LocalDateTime`.
- [ ] Không có field nào tự thêm ngoài `02-api-spec.md`.
- [ ] `canTransitionTo()` là source-of-truth, được kiểm tra ở Entity `advanceStatus()`, không ở DTO hay controller.
- [ ] DTO class name thống nhất: `WorkOrderStatusRequest` (KHÔNG `StatusUpdateRequest`).
