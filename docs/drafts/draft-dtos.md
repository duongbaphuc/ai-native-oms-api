<!--
Role: Senior Engineer.
Task: Định nghĩa toàn bộ DTOs (Data Transfer Objects) cho Outage Work Order API.
Context files: docs/api-spec.md, docs/domain-model.md, docs/coding-rules.md, docs/api-rules.md, docs/security-rules.md.
Constraints:
  - Java records (immutable), không dùng class thông thường.
  - Strict schema: @JsonIgnoreProperties(ignoreUnknown = false) trên mọi Request DTO.
  - Bean Validation: @NotBlank, @NotNull, @Size — chỉ ở boundary (Controller), không rải vào Service.
  - Fields phải khớp 1:1 với docs/api-spec.md, không được tự ý thêm field.
  - Enum values phải khớp docs/domain-model.md.
  - AI Provenance phải được ghi chú trong header mỗi file.
DRAFT ONLY — scoring target, chưa được wire vào application.
-->

# Draft: DTOs — Outage Work Order API

Tài liệu này định nghĩa toàn bộ Data Transfer Objects (DTOs) sẽ được implement cho dự án.
Mọi field, kiểu dữ liệu và annotation phải khớp với [`docs/api-spec.md`](../api-spec.md) và [`docs/domain-model.md`](../domain-model.md).

---

## 1. Domain Enums (Shared)

Hai enums này thuộc `package com.gpc.oms.domain`, được sử dụng chung bởi tất cả DTOs và Entity.

### 1.1 `Priority`

Khai báo trong `domain-model.md` — 4 mức độ nghiêm trọng của sự cố.

```java
// AI Provenance: generated from docs/domain-model.md §Entities
package com.gpc.oms.domain;

public enum Priority {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}
```

### 1.2 `WorkOrderStatus`

Khai báo trong `domain-model.md` — máy trạng thái đơn hướng (one-way state machine).
`@JsonValue` serialise ra chuỗi khớp api-spec.md (ví dụ: `"Open"` thay vì `"OPEN"`).

```java
// AI Provenance: generated from docs/domain-model.md §Invariants, docs/api-spec.md §4
package com.gpc.oms.domain;

import com.fasterxml.jackson.annotation.JsonValue;

public enum WorkOrderStatus {
    OPEN("Open"),
    IN_PROGRESS("InProgress"),
    DONE("Done");

    private final String value;

    WorkOrderStatus(String value) { this.value = value; }

    @JsonValue
    public String getValue() { return value; }

    /**
     * Kiểm tra tính hợp lệ của chuyển trạng thái.
     * Bắt buộc: Open → InProgress → Done. Lùi trạng thái bị cấm tuyệt đối.
     */
    public boolean canTransitionTo(WorkOrderStatus next) {
        return switch (this) {
            case OPEN        -> next == IN_PROGRESS;
            case IN_PROGRESS -> next == DONE;
            case DONE        -> false; // terminal — không có chuyển tiếp nào hợp lệ
        };
    }
}
```

> [!IMPORTANT]
> `canTransitionTo()` được gọi tại **domain/service layer**, không phải controller. Vi phạm → ném `InvalidStateTransitionException` → handler trả về **422 Unprocessable Entity** (RFC 7807).

---

## 2. Request DTOs

### 2.1 `WorkOrderRequest` — `POST /api/v1/workorders`

Nguồn spec: [`api-spec.md §1`](../api-spec.md).

**Ràng buộc:**
- `@JsonIgnoreProperties(ignoreUnknown = false)` → field lạ bị reject với 400.
- `@NotBlank` / `@Size` trên string fields.
- `@NotNull` trên enum `priority` — Jackson tự reject giá trị enum không hợp lệ với 400.
- KHÔNG có field `status`, `id`, `createdAt` — không được tự ý thêm.

```java
// AI Provenance: generated from docs/api-spec.md §1, docs/domain-model.md, docs/coding-rules.md
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
    @Size(max = 500, message = "description must not exceed 500 characters")
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

Nguồn spec: [`api-spec.md §4`](../api-spec.md).

**Ràng buộc:**
- Chỉ có 1 field duy nhất: `status`.
- `@JsonIgnoreProperties(ignoreUnknown = false)` — field lạ bị reject.
- `@NotNull` — thiếu field → 400.
- Validation logic chuyển trạng thái (one-way) nằm ở **service layer**, không phải DTO.

```java
// AI Provenance: generated from docs/api-spec.md §4, docs/domain-model.md §Invariants
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

Nguồn spec: [`api-spec.md §1–§4`](../api-spec.md).

**Ràng buộc:**
- Immutable record — không có setter.
- Fields khớp 1:1 với api-spec.md response schema.
- `resolvedAt` là `Instant` (nullable) — tự động gán khi `status = Done`.
- Không expose JPA entity trực tiếp ra ngoài — luôn convert sang DTO này.

```java
// AI Provenance: generated from docs/api-spec.md §1–§4, docs/domain-model.md
package com.gpc.oms.dto;

import com.gpc.oms.domain.Priority;
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
    Instant          resolvedAt   // null cho đến khi status = Done
) {}
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

---

## 4. Bảng Tổng Hợp Validation

| DTO | Field | Annotation | Mô tả vi phạm → HTTP |
|---|---|---|---|
| `WorkOrderRequest` | `equipmentId` | `@NotBlank`, `@Size(max=50)` | Trống hoặc vượt 50 ký tự → **400** |
| `WorkOrderRequest` | `description` | `@NotBlank`, `@Size(max=500)` | Trống hoặc vượt 500 ký tự → **400** |
| `WorkOrderRequest` | `priority` | `@NotNull` | Null hoặc giá trị lạ → **400** |
| `WorkOrderRequest` | *(extra field)* | `ignoreUnknown=false` | Field không khai báo → **400** |
| `WorkOrderStatusRequest` | `status` | `@NotNull` | Null hoặc giá trị lạ → **400** |
| `WorkOrderStatusRequest` | *(extra field)* | `ignoreUnknown=false` | Field không khai báo → **400** |
| `WorkOrderStatusRequest` | `status` | *(service layer)* | Chuyển trạng thái ngược → **422** |

---

## 5. Checklist Trước Khi Implement

- [ ] `WorkOrderRequest` dùng Java `record`, KHÔNG dùng `class`.
- [ ] `@JsonIgnoreProperties(ignoreUnknown = false)` có mặt trên tất cả Request DTOs.
- [ ] `WorkOrderResponse` KHÔNG có `@JsonIgnoreProperties` — response chỉ có chiều ra.
- [ ] `WorkOrderStatus` serialize ra `"Open"` / `"InProgress"` / `"Done"` (dùng `@JsonValue`).
- [ ] `resolvedAt` có kiểu `Instant`, KHÔNG phải `String` hay `LocalDateTime`.
- [ ] Không có field nào tự thêm ngoài `api-spec.md`.
- [ ] `canTransitionTo()` được kiểm tra ở service/domain, không ở DTO hay controller.
