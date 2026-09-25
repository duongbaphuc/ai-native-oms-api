<!--
Role: Senior Engineer. Task: Tạo các thành phần tối ưu cú pháp và dùng chung: ProblemTypes, StringToWorkOrderStatusConverter và WorkOrderTestFixtures.
Context files: docs/00-coding-rules.md, docs/02-api-spec.md, docs/01-domain-model.md
Constraints: 
- ProblemTypes: Lớp tiện ích final, private constructor (Effective Java Item 4), 7 hằng số URI RFC 7807 tiền phân bổ bộ nhớ.
- StringToWorkOrderStatusConverter: Converter<String, WorkOrderStatus>, nạp mảng tĩnh VALUES = WorkOrderStatus.values() tránh cấp phát mảng trên heap mỗi request, ném IllegalArgumentException khi giá trị không khớp.
- WorkOrderTestFixtures: Áp dụng Object Mother Pattern, tập trung hóa khởi tạo đối tượng kiểm thử mẫu loại bỏ mã trùng lặp.
- Pure Java 17, Zero Lombok, final modifiers.
DRAFT ONLY — scoring target, never wired into app.
-->
# Draft: Shared Infrastructure, Converters & Test Fixtures

## 1. Target Files

| File | Package | Path | Action | Mục Đích |
|---|---|---|---|---|
| `ProblemTypes.java` | `com.gpc.oms.exception` | `src/main/java/com/gpc/oms/exception/ProblemTypes.java` | NEW | Tập trung hóa 7 hằng số URI định danh loại lỗi RFC 7807, loại bỏ magic strings |
| `StringToWorkOrderStatusConverter.java` | `com.gpc.oms.config` | `src/main/java/com/gpc/oms/config/StringToWorkOrderStatusConverter.java` | NEW | Tự động chuyển đổi String query param (không phân biệt hoa thường) sang enum `WorkOrderStatus` |
| `WorkOrderTestFixtures.java` | `com.gpc.oms.testutil` | `src/test/java/com/gpc/oms/testutil/WorkOrderTestFixtures.java` | NEW | Cung cấp các static factory methods tạo dữ liệu kiểm thử chuẩn (Object Mother Pattern) |
| `ProblemTypesTest.java` | `com.gpc.oms.exception` | `src/test/java/com/gpc/oms/exception/ProblemTypesTest.java` | NEW | Kiểm thử tính bất biến, giá trị URI và tính non-instantiability của lớp ProblemTypes |
| `StringToWorkOrderStatusConverterTest.java` | `com.gpc.oms.config` | `src/test/java/com/gpc/oms/config/StringToWorkOrderStatusConverterTest.java` | NEW | Kiểm thử phân tích query params hoa, thường, khoảng trắng và ngoại lệ giá trị sai |
| `ResourceNotFoundExceptionTest.java` | `com.gpc.oms.exception` | `src/test/java/com/gpc/oms/exception/ResourceNotFoundExceptionTest.java` | NEW | Kiểm thử khởi tạo và thông điệp lỗi của ngoại lệ nghiệp vụ miền |

---

## 2. Architecture & Design Rationale

### 2.1. Tập Trung Hóa URI RFC 7807 (`ProblemTypes`)
Thay vì khởi tạo `URI.create("urn:problem-type:...")` nhiều lần trong runtime, lớp `ProblemTypes` tiền phân bổ sẵn các đối tượng `java.net.URI` tĩnh bất biến trong bộ nhớ. Điều này giúp:
- Tiết kiệm chu kỳ CPU của bộ phân tích URI parser.
- Tránh tạo rác (garbage allocation) trên JVM heap.
- Đảm bảo tính nhất quán tuyệt đối giữa `GlobalExceptionHandler` và `SecurityConfig`.

### 2.2. Tối Ưu GC Với Caching Mảng Enum (`StringToWorkOrderStatusConverter`)
Trong Java chuẩn, mỗi lần gọi `Enum.values()` sẽ tạo ra một bản sao mảng mới (`clone()`). Bằng cách khai báo:
```java
private static final WorkOrderStatus[] VALUES = WorkOrderStatus.values();
```
Bộ chuyển đổi tái sử dụng trực tiếp mảng tham chiếu tĩnh, giúp tối ưu hóa hiệu năng cực cao khi API tiếp nhận hàng nghìn yêu cầu lọc trạng thái mỗi giây.

### 2.3. Tái Sử Dụng Dữ Liệu Kiểm Thử (`WorkOrderTestFixtures`)
Áp dụng **Object Mother Pattern** thay vì khởi tạo lặp đi lặp lại các tham số `UUID.randomUUID()`, `"EQ-100"`, `"Faulty transformer inspection required"`. Mọi bài kiểm thử Unit, Slice, và Integration đều kế thừa dữ liệu mẫu từ một nguồn duy nhất.

---

## 3. Implementation Blueprint: `ProblemTypes.java`

```java
// AI Provenance: generated from docs/02-api-spec.md §5, docs/00-api-rules.md §2
package com.gpc.oms.exception;

import java.net.URI;

/**
 * Centralized RFC 7807 Problem Detail Type URIs.
 * Pre-allocated immutable URI constants to optimize JVM performance and eliminate magic strings.
 */
public final class ProblemTypes {

    private ProblemTypes() {
        // Enforce non-instantiability (Effective Java Item 4)
    }

    public static final URI VALIDATION_ERROR = URI.create("urn:problem-type:validation-error");
    public static final URI MALFORMED_JSON = URI.create("urn:problem-type:malformed-json");
    public static final URI UNAUTHORIZED = URI.create("urn:problem-type:unauthorized");
    public static final URI FORBIDDEN = URI.create("urn:problem-type:forbidden");
    public static final URI NOT_FOUND = URI.create("urn:problem-type:not-found");
    public static final URI INVALID_STATE_TRANSITION = URI.create("urn:problem-type:invalid-state-transition");
    public static final URI INTERNAL_ERROR = URI.create("urn:problem-type:internal-error");
}
```

---

## 4. Implementation Blueprint: `StringToWorkOrderStatusConverter.java`

```java
// AI Provenance: generated from docs/02-api-spec.md §2, docs/01-domain-model.md §Invariants
package com.gpc.oms.config;

import com.gpc.oms.domain.WorkOrderStatus;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class StringToWorkOrderStatusConverter implements Converter<String, WorkOrderStatus> {

    private static final WorkOrderStatus[] VALUES = WorkOrderStatus.values();

    @Override
    public WorkOrderStatus convert(final String source) {
        if (source == null || source.isBlank()) {
            return null;
        }
        for (final WorkOrderStatus status : VALUES) {
            if (status.name().equalsIgnoreCase(source) || status.getValue().equalsIgnoreCase(source)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown WorkOrderStatus: " + source);
    }
}
```

---

## 5. Implementation Blueprint: `WorkOrderTestFixtures.java`

```java
// AI Provenance: test fixture for reusable object creation across test classes
package com.gpc.oms.testutil;

import com.gpc.oms.domain.Priority;
import com.gpc.oms.domain.WorkOrder;
import com.gpc.oms.domain.WorkOrderStatus;
import com.gpc.oms.dto.WorkOrderRequest;
import com.gpc.oms.dto.WorkOrderResponse;
import com.gpc.oms.dto.WorkOrderStatusRequest;

import java.time.Instant;
import java.util.UUID;

public final class WorkOrderTestFixtures {

    private WorkOrderTestFixtures() {
        // Enforce non-instantiability
    }

    public static final String DEFAULT_EQUIPMENT_ID = "EQ-100";
    public static final String DEFAULT_DESCRIPTION = "Faulty transformer inspection required";
    public static final Priority DEFAULT_PRIORITY = Priority.HIGH;

    public static WorkOrder createDefaultEntity() {
        return new WorkOrder(DEFAULT_EQUIPMENT_ID, DEFAULT_DESCRIPTION, DEFAULT_PRIORITY);
    }

    public static WorkOrder createEntity(final String equipmentId, final String description, final Priority priority) {
        return new WorkOrder(equipmentId, description, priority);
    }

    public static WorkOrder createDoneEntity(final String equipmentId, final String description, final Priority priority) {
        final WorkOrder wo = new WorkOrder(equipmentId, description, priority);
        wo.advanceStatus(WorkOrderStatus.IN_PROGRESS);
        wo.advanceStatus(WorkOrderStatus.DONE);
        return wo;
    }

    public static WorkOrderRequest createDefaultRequest() {
        return new WorkOrderRequest(DEFAULT_EQUIPMENT_ID, DEFAULT_DESCRIPTION, DEFAULT_PRIORITY);
    }

    public static WorkOrderRequest createRequest(final String equipmentId, final String description, final Priority priority) {
        return new WorkOrderRequest(equipmentId, description, priority);
    }

    public static WorkOrderStatusRequest createStatusRequest(final WorkOrderStatus status) {
        return new WorkOrderStatusRequest(status);
    }

    public static WorkOrderResponse createResponse(final UUID id, final String equipmentId, final String description,
                                                  final Priority priority, final WorkOrderStatus status,
                                                  final Instant createdAt, final Instant resolvedAt) {
        return new WorkOrderResponse(id, equipmentId, description, priority, status, createdAt, resolvedAt);
    }
}
```

---

## 6. Shared Component Test Sketches

### Sketch 1: `ProblemTypesTest.java`
- **Mục tiêu:**
  - Kiểm tra tính bất khả khởi tạo (private constructor phản chiếu ném `InvocationTargetException` hoặc kiểm tra modifier private).
  - Khẳng định 7 hằng số URI mang giá trị chính xác theo chuẩn RFC 7807.

### Sketch 2: `StringToWorkOrderStatusConverterTest.java`
- **Mục tiêu:**
  - Chuyển đổi `"Open"` hoặc `"OPEN"` thành `WorkOrderStatus.OPEN`.
  - Chuyển đổi `"InProgress"` hoặc `"IN_PROGRESS"` thành `WorkOrderStatus.IN_PROGRESS`.
  - Chuyển đổi `"Done"` hoặc `"DONE"` thành `WorkOrderStatus.DONE`.
  - Trả về `null` khi chuỗi rỗng hoặc chỉ chứa khoảng trắng.
  - Ném `IllegalArgumentException` khi truyền giá trị không hợp lệ như `"INVALID"`.

### Sketch 3: `ResourceNotFoundExceptionTest.java`
- **Mục tiêu:**
  - Kiểm tra khởi tạo với ID chuỗi và ID UUID, xác nhận message định dạng chuẩn `"WorkOrder not found with ID: " + id`.
