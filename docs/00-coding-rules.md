# Quy Chuẩn Lập Trình Java & Cẩm Nang Design Patterns (Oracle Senior Java Standards)

<!--
Role: Principal Java Software Architect & Senior Java Engineer at Oracle (Oracle Core Platform & OpenJDK Team)
Task: Production-grade Java 17+ coding rules, JVM/GC optimization standards, and GoF design pattern guidelines with Good vs Bad Practice comparisons
Context files: docs/01-domain-model.md, docs/02-api-spec.md, docs/00-security-rules.md, docs/00-internal-coding-standards.md
Constraints: Java 17 LTS, Spring Boot 3.3, Zero-Lombok, 100% Constructor Injection, RFC 7807 compliance, Pure Java Records
-->

Tài liệu này xác lập bộ quy chuẩn viết mã (Coding Standards) và hướng dẫn ứng dụng các Mẫu Thiết Kế (Design Patterns) cho toàn bộ dự án, được biên soạn theo phong cách và tư duy của **Senior Java Engineer tại Oracle (Oracle Core Platform & JDK Team)**.

Mọi dòng mã do con người hay AI Coding Agents (GitHub Copilot / Cursor / Claude) tạo ra đều bắt buộc phải tuân thủ nghiêm ngặt các nguyên tắc dưới đây.

---

## 1. Triết Lý Thiết Kế Cốt Lõi (Oracle Core Engineering Principles)

1. **Nguyên Lý "Effective Java" (Joshua Bloch):**
   - **Immutability by Default:** Luôn ưu tiên tính bất biến. Dữ liệu một khi đã tạo ra không thể bị biến đổi ngầm (side-effects). Sử dụng từ khóa `final` cho mọi field và biến cục bộ khi không có lý do chính đáng để thay đổi giá trị.
   - **Fail-Fast Principle:** Thẩm định tham số đầu vào ngay tại biên giới (Entry Boundary) bằng `@Valid` và ném ngoại lệ rõ ràng (`IllegalArgumentException`, `IllegalStateException`) thay vì để lỗi lan sâu vào bên trong hệ thống.
   - **Defensive Programming:** Trả về bản sao phòng vệ (Defensive Copy) hoặc các cấu trúc dữ liệu không thể chỉnh sửa (`List.copyOf()`, `Collections.unmodifiableList()`) đối với các danh sách mutable bên trong Entity.
   - **Cấm Tuyệt Đối `null` Tự Do:** Không bao giờ trả về `null` cho mảng hoặc Collections (luôn trả về `List.of()`, `Collections.emptyList()`). Sử dụng `Optional<T>` thuần túy làm kiểu trả về của Repository hoặc Service khi đối tượng có thể vắng mặt.

2. **Hiện Đại Hóa Với Java 17 LTS:**
   - **Pure Java Records:** Bắt buộc sử dụng `record` cho toàn bộ tầng DTOs (Request / Response) và Value Objects. Records tự động cung cấp `equals()`, `hashCode()`, `toString()`, và tính bất biến nông (shallow immutability) tối ưu.
   - **Pattern Matching & Switch Expressions:** Sử dụng switch expressions hiện đại để đảm bảo tính bao quát (exhaustiveness) và loại bỏ hoàn toàn các lỗi thiếu `break`.
   - **Text Blocks:** Sử dụng cú pháp Text Blocks (`"""..."""`) cho các chuỗi nhiều dòng như SQL, JSON mẫu, hoặc HTML templates.

3. **Nguyên Tắc Zero-Lombok (No Hidden Bytecode Manipulation):**
   - **Cấm 100% Project Lombok:** Nghiêm cấm các annotation `@Data`, `@Getter`, `@Setter`, `@Builder`, `@AllArgsConstructor`, `@NoArgsConstructor`.
   - **Lý do kỹ thuật từ Oracle Team:**
     * Lombok can thiệp trực tiếp vào AST (Abstract Syntax Tree) trong giai đoạn biên dịch, gây xung đột tiềm ẩn với các bản nâng cấp JDK mới, làm sai lệch báo cáo JaCoCo coverage, và làm giảm tính minh bạch khi debug/đọc stack trace.
     * Mọi getter trên Entity phải được viết thủ công rõ ràng; DTOs sử dụng Java 17 `record` hoàn toàn không cần boilerplate.

4. **Quản Lý Phụ Thuộc (Dependency Injection Standards):**
   - **100% Constructor Injection:** Mọi Spring Bean (Service, Controller, Component) bắt buộc tiêm phụ thuộc qua hàm khởi tạo công khai.
   - **Nghiêm Cấm Field Injection:** Cấm tuyệt đối `@Autowired` trên field. Field injection phá vỡ tính bao đóng, ngăn cản việc viết Unit Tests bằng POJO thuần, và che giấu hiện tượng "God Class" (quá nhiều dependencies).
   - Mọi dependency fields phải được khai báo dạng `private final`.

5. **Triệt Tiêu 100% Magic Numbers & Literal Strings (Zero Magic Values Principle - Joshua Bloch Item 68):**
   - **Cấm Tuyệt Đối Magic Numbers:** Nghiêm cấm các con số thô (raw numeric literals: 20, 60, 401, 429, 10000, 10m) trực tiếp trong logic điều kiện, vòng lặp, kiểm tra trạng thái hoặc annotations mà không rõ ngữ nghĩa. Bắt buộc: (1) Khai báo hằng số self-explanatory (`private static final int MAX_CACHE_ENTRIES = 10_000`); (2) Dùng hằng số chuẩn từ Spring/JDK (`HttpStatus.TOO_MANY_REQUESTS.value()`); (3) Externalize các tham số dung lượng, giới hạn lưu lượng, thời gian hết hạn (Rate Limiting, Cache, Timeout) vào `application.yml` qua `@ConfigurationProperties`.
   - **Cấm Tuyệt Đối Literal Strings:** Nghiêm cấm các chuỗi ký tự ma thuật (inline strings) phân tán trong mã nguồn:
     * *Tên Metric & Tag Keys (Micrometer):* Cấm viết chuỗi tự do trong `registry.counter(...)`. Bắt buộc gom vào lớp hằng số `WorkOrderMetrics` hoặc Enum chuyên biệt.
     * *Tên Role & Quyền Hạn (Security RBAC):* Cấm viết chuỗi thô (`"ADMIN"`, `"DISPATCHER"`, `"TECHNICIAN"`) trong SecurityConfig và Controller. Bắt buộc gom vào lớp hằng số `RoleConstants` (kèm tiền tố `ROLE_`).
     * *Thuộc tính JSON & Mã lỗi (RFC 7807):* Gom các khóa mở rộng (`invalidParams`, `name`, `reason`, `type`) thành hằng số `public static final` trong `ProblemTypes`.
     * *Tiêu đề HTTP & Media Types:* Bắt buộc dùng `HttpHeaders.RETRY_AFTER`, `MediaType.APPLICATION_PROBLEM_JSON_VALUE`.
     * *Ràng buộc dữ liệu:* Hằng số độ dài tối đa/tối thiểu của trường dữ liệu phải được dùng chung giữa Entity JPA và DTO Bean Validation để chống lệch pha.

---

## 2. Cẩm Nang Ứng Dụng Design Patterns Tối Ưu Hóa Class (Good vs Bad Practice)

Việc áp dụng các Design Pattern phải hướng tới mục tiêu tối ưu hóa tính đóng gói, dễ mở rộng, dễ kiểm thử và tiết kiệm tài nguyên. Dưới đây là đối chiếu chi tiết giữa **Good Practice (Chuẩn Oracle)** và **Bad Practice (Anti-pattern)**:

### 2.1 Static Factory Method Pattern (`from()`, `of()`)
- **Mục đích:** Thay thế constructors thô, tăng tính biểu đạt ngữ nghĩa (expressive naming), cho phép tái sử dụng đối tượng và đóng gói logic chuyển đổi dữ liệu.
- **Áp dụng tại:** Tầng DTOs và Value Objects.

#### ❌ BAD PRACTICE (Constructor thô, lộ chi tiết khởi tạo):
```java
// BAD: Lộ constructor nhiều tham số, client tự gọi getter rời rạc, không kiểm tra null-safety
WorkOrderResponse response = new WorkOrderResponse(
    workOrder.getId(),
    workOrder.getEquipmentId(),
    workOrder.getDescription(),
    workOrder.getPriority(),
    workOrder.getStatus(),
    workOrder.getCreatedAt(),
    workOrder.getResolvedAt()
);
```

#### ✅ GOOD PRACTICE (Oracle Standard - Static Factory Method):
```java
public record WorkOrderResponse(
    UUID id,
    String equipmentId,
    String description,
    Priority priority,
    WorkOrderStatus status,
    Instant createdAt,
    Instant resolvedAt
) {
    // Static Factory Method đóng gói logic mapping, null-safety và tăng tính biểu đạt
    public static WorkOrderResponse from(WorkOrder workOrder) {
        Objects.requireNonNull(workOrder, "workOrder must not be null");
        return new WorkOrderResponse(
            workOrder.getId(),
            workOrder.getEquipmentId(),
            workOrder.getDescription(),
            workOrder.getPriority(),
            workOrder.getStatus(),
            workOrder.getCreatedAt(),
            workOrder.getResolvedAt()
        );
    }
}
```

---

### 2.2 State Pattern & Strategy Pattern
- **Mục đích:** Đóng gói toàn bộ máy trạng thái (State Machine) và các quy tắc rẽ nhánh vào chính Enum/State class, triệt tiêu các khối lệnh `if-else` lồng nhau rải rác ở tầng Service/Controller.
- **Áp dụng tại:** `WorkOrderStatus.java` và logic chuyển đổi trạng thái của Entity.

#### ❌ BAD PRACTICE (Chuỗi `if-else` phân tán, dễ sót nhánh):
```java
// BAD: Service tự kiểm tra trạng thái bằng if-else phức tạp, vi phạm Open-Closed Principle
if (currentStatus == WorkOrderStatus.OPEN && targetStatus == WorkOrderStatus.IN_PROGRESS) {
    workOrder.setStatus(targetStatus);
} else if (currentStatus == WorkOrderStatus.IN_PROGRESS && targetStatus == WorkOrderStatus.DONE) {
    workOrder.setStatus(targetStatus);
} else {
    throw new IllegalStateException("Invalid transition");
}
```

#### ✅ GOOD PRACTICE (Oracle Standard - State Transition Encapsulation):
```java
public enum WorkOrderStatus {
    OPEN,
    IN_PROGRESS,
    DONE;

    // Đóng gói trọn vẹn quy tắc chuyển đổi trong Enum, sử dụng Switch Expression hiện đại
    public boolean canTransitionTo(WorkOrderStatus target) {
        if (target == null) {
            return false;
        }
        return switch (this) {
            case OPEN -> target == IN_PROGRESS;
            case IN_PROGRESS -> target == DONE;
            case DONE -> false; // Terminal state - không cho phép chuyển tiếp
        };
    }
}

// Trong Domain Aggregate Root:
public WorkOrder advanceStatus(WorkOrderStatus newStatus) {
    if (!this.status.canTransitionTo(newStatus)) {
        throw new IllegalStateException("Invalid state transition from " + this.status + " to " + newStatus);
    }
    this.status = newStatus;
    if (newStatus == WorkOrderStatus.DONE) {
        this.resolvedAt = Instant.now();
    }
    return this;
}
```

---

### 2.3 Builder Pattern & Immutable Record Pattern
- **Mục đích:** Tối ưu hóa việc tạo lập đối tượng nhiều thuộc tính mà vẫn giữ trọn vẹn tính bất biến của dữ liệu.
- **Áp dụng tại:** Tầng DTOs và cấu hình phân trang.

#### ❌ BAD PRACTICE (Lombok `@Builder` hoặc Mutable JavaBean với setters):
```java
// BAD: Class có setters cho phép biến đổi trạng thái sau khi tạo, khó kiểm soát luồng đa luồng
@Data // LOMBOK BANNED
public class WorkOrderRequestDTO {
    private String equipmentId;
    private String description;
    private String priority;
}
```

#### ✅ GOOD PRACTICE (Oracle Standard - Pure Java 17 Record):
```java
// GOOD: Java 17 Record bất biến, ngắn gọn, an toàn đa luồng, Bean Validation chặt chẽ
public record WorkOrderRequest(
    @NotBlank(message = "equipmentId must not be blank")
    @Size(max = 50, message = "equipmentId must not exceed 50 characters")
    String equipmentId,

    @NotBlank(message = "description must not be blank")
    @Size(min = 10, max = 500, message = "description must be between 10 and 500 characters")
    String description,

    @NotNull(message = "priority must not be null")
    Priority priority
) {}
```

---

### 2.4 Explicit Adapter / Mapper Pattern
- **Mục đích:** Chuyển đổi dữ liệu 1-1 giữa Domain Entity và DTOs mà không dùng reflection.
- **Nguyên tắc:** Cấm sử dụng các thư viện như `ModelMapper`, `Dozer`, `BeanUtils.copyProperties`.

#### ❌ BAD PRACTICE (Reflection-heavy Mapper):
```java
// BAD: Reflection ngầm, tốn CPU, không bắt được lỗi khi đổi tên field tại thời điểm compile
BeanUtils.copyProperties(workOrder, responseDTO);
ModelMapper mapper = new ModelMapper();
WorkOrderResponse res = mapper.map(workOrder, WorkOrderResponse.class);
```

#### ✅ GOOD PRACTICE (Oracle Standard - Type-Safe Explicit Mapping):
```java
// GOOD: Trình biên dịch kiểm tra kiểu tĩnh (Static Type Checking), tốc độ microsecond, zero-allocation overhead
public static WorkOrderResponse from(WorkOrder entity) {
    Objects.requireNonNull(entity, "Entity cannot be null");
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

### 2.5 Chain of Responsibility Pattern
- **Mục đích:** Phân tách rạch ròi các khâu xử lý an ninh, xác thực, truy vết và bắt lỗi thành các mắt xích độc lập.
- **Áp dụng tại:**
  - `SecurityFilterChain` (Dual FilterChain: `@Order(1)` cho H2 Console non-prod, `@Order(2)` cho API chính).
  - `GlobalExceptionHandler` (`@RestControllerAdvice`): Bắt các nhóm ngoại lệ chuyên biệt từ cụ thể nhất (`MethodArgumentNotValidException`) đến tổng quát nhất (`Exception.class`).

#### ✅ GOOD PRACTICE (Oracle Standard - Clean Exception Handling Advice):
```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // Mắt xích #1: Bắt lỗi Validation đầu vào (HTTP 400)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationErrors(MethodArgumentNotValidException ex) {
        log.warn("Validation failed: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Validation Failed");
        problem.setType(URI.create("urn:problem-type:validation-error"));
        // Trích xuất invalidParams[] tường minh
        return problem;
    }

    // Mắt xích #2: Bắt lỗi vi phạm State Machine (HTTP 422)
    @ExceptionHandler(IllegalStateException.class)
    public ProblemDetail handleIllegalState(IllegalStateException ex) {
        log.warn("Illegal state transition: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
        problem.setType(URI.create("urn:problem-type:invalid-state-transition"));
        return problem;
    }

    // Mắt xích #3: Chốt chặn an toàn cuối cùng (HTTP 500 - Che giấu stack trace)
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex) {
        log.error("Unexpected system error", ex); // Log full stacktrace CHỈ ở server
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
        problem.setType(URI.create("urn:problem-type:internal-error"));
        return problem;
    }
}
```

---

### 2.6 Template Method & Orchestration Pattern
- **Mục đích:** Phân tách rõ trách nhiệm giữa tầng điều phối giao dịch (Service) và tầng nghiệp vụ cốt lõi (Domain Aggregate Root).
- **Nguyên tắc:**
  - Service chỉ đóng vai trò **Orchestrator** (Lấy entity từ DB $\rightarrow$ Kích hoạt phương thức nghiệp vụ của entity $\rightarrow$ Lưu lại $\rightarrow$ Chuyển đổi sang Response).
  - Không bao giờ chuyển đổi trạng thái trực tiếp trong Service bằng setter thô. Entity phải tự bảo vệ tính bất biến qua `advanceStatus()`.

#### ✅ GOOD PRACTICE (Oracle Standard - Orchestration in Service):
```java
@Service
@Transactional
public class WorkOrderService {
    private final WorkOrderRepository repository;

    // Constructor Injection tường minh
    public WorkOrderService(WorkOrderRepository repository) {
        this.repository = repository;
    }

    public WorkOrderResponse updateStatus(UUID id, WorkOrderStatus targetStatus) {
        // 1. Orchestrate: Tìm kiếm entity
        WorkOrder workOrder = repository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("WorkOrder not found with id: " + id));

        // 2. Delegate: Ủy quyền kiểm tra invariant và chuyển đổi cho Domain Model
        workOrder.advanceStatus(targetStatus);

        // 3. Persist & Map: Lưu DB và trả về DTO
        WorkOrder updated = repository.save(workOrder);
        return WorkOrderResponse.from(updated);
    }
}
```

---

### 2.7 Centralized Constants & Configuration Properties Pattern
- **Mục đích:** Tập trung hóa định nghĩa các giá trị không đổi, loại bỏ trùng lặp chuỗi, đảm bảo tính nhất quán giữa các tầng kiến trúc và cho phép điều chỉnh cấu hình môi trường mà không cần sửa code.
- **Áp dụng tại:** Tầng Metrics, Security RBAC, RFC 7807 Problem Details, Validation Constraints, Rate Limiting & Caching.

#### ❌ BAD PRACTICE (Magic Numbers, Inline Literal Strings rải rác):
```java
// BAD: Magic numbers và literal strings phân tán khắp nơi, dễ sai chính tả, khó bảo trì
registry.counter("oms_workorders_created_total", "priority", request.priority().name()).increment();

if (count > 60) {
    response.setStatus(429);
    response.setHeader("Retry-After", "60");
    response.setContentType("application/problem+json");
}

@PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')") // Chuỗi thô không thể tái cấu trúc an toàn
```

#### ✅ GOOD PRACTICE (Oracle Standard - Centralized Constants & Type-Safe Config):
```java
// GOOD: Gom metrics và tags vào lớp hằng số tập trung
public final class WorkOrderMetrics {
    public static final String COUNTER_CREATED = "oms_workorders_created_total";
    public static final String TAG_PRIORITY = "priority";
    private WorkOrderMetrics() {}
}

// GOOD: Gom roles vào RoleConstants
public final class RoleConstants {
    public static final String ROLE_ADMIN = "ROLE_ADMIN";
    public static final String ROLE_DISPATCHER = "ROLE_DISPATCHER";
    public static final String ROLE_TECHNICIAN = "ROLE_TECHNICIAN";
    public static final String HAS_ROLE_ADMIN_OR_DISPATCHER = 
        "hasAnyRole('" + ROLE_ADMIN + "', '" + ROLE_DISPATCHER + "')";
    private RoleConstants() {}
}

// GOOD: Externalize cấu hình qua @ConfigurationProperties
@ConfigurationProperties(prefix = "oms.rate-limit")
public record RateLimitProperties(
    int maxRequestsPerMinute,
    Duration windowDuration,
    int cacheMaxSize
) {}
```

---

## 3. Tối Ưu Hóa Bộ Nhớ, Vòng Lặp & Hiệu Năng JVM (GC Pressure)

1. **Quản Lý Phạm Vi Biến (Variable Scoping & Escape Analysis):**
   - Khai báo biến trong phạm vi hẹp nhất có thể. Đặt biến sát thời điểm sử dụng đầu tiên.
   - Thêm từ khóa `final` cho các biến cục bộ không thay đổi giá trị. Việc này giúp JIT Compiler thực hiện **Escape Analysis** hiệu quả hơn để phân bổ đối tượng trên Stack thay vì Heap (Scalar Replacement), giảm áp lực cho bộ thu gom rác (Garbage Collector).

2. **Chiến Lược Thao Tác Chuỗi (String Concatenation Strategy):**
   - **Nối chuỗi đơn giản:** Sử dụng toán tử `+`. Kể từ Java 9, trình biên dịch sử dụng `invokedynamic` với `StringConcatFactory` tự động tối ưu hóa việc ghép chuỗi mà không tạo ra các đối tượng trung gian lãng phí.
   - **Vòng lặp lớn:** Bắt buộc sử dụng `StringBuilder` khi ghép chuỗi bên trong vòng lặp (`for`, `while`) có số lần lặp không xác định hoặc lớn hơn 10 phần tử. Định trước dung lượng ban đầu (initial capacity) nếu ước tính được: `new StringBuilder(128)`.

3. **Khởi Tạo Kích Thước Collections (Pre-Sizing Collections):**
   - Khi biết trước số lượng phần tử cần thêm vào `ArrayList`, `HashMap`, hoặc `HashSet`, luôn cung cấp `initialCapacity` để triệt tiêu chi phí cấp phát lại mảng ngầm (Array resizing & copying):
     ```java
     // Tránh cấp phát thừa và resize nhiều lần trong bộ nhớ
     List<WorkOrderResponse> result = new ArrayList<>(workOrders.size());
     for (final WorkOrder wo : workOrders) {
         result.add(WorkOrderResponse.from(wo));
     }
     ```

4. **Sử Dụng Stream API Đúng Lúc, Đúng Chỗ:**
   - **Nên dùng Stream:** Khi thực hiện các phép biến đổi dữ liệu khai báo (map, filter, collect, flatMap) trên tập dữ liệu vừa và lớn.
   - **Tránh dùng Stream:** Không lạm dụng Stream chỉ để chạy một vòng lặp `forEach` đơn giản trên mảng 2-3 phần tử. Việc tạo Stream pipeline, lambda instances và iterator sẽ tạo ra rác (Garbage) không cần thiết trong hot path.

---

## 4. Giao Ước Xử Lý Ngoại Lệ & An Ninh Biên Giới (Defensive Contracts)

1. **Phân Định Rõ Ràng Các Loại Ngoại Lệ:**
   - **Domain Invariant Violations:** Sử dụng `IllegalStateException` khi trạng thái thực thể không cho phép thao tác (vd: vi phạm State Machine).
   - **Resource Absences:** Sử dụng custom runtime exception `ResourceNotFoundException`.
   - **Validation Failures:** Kích hoạt Bean Validation (`@Valid`) tại Controller.

2. **Quy Tắc Vàng: "Log-Once or Throw-Once":**
   - Tuyệt đối không vừa log `ERROR` vừa ném tiếp ngoại lệ lên tầng trên. Hành vi này làm ô nhiễm log file với nhiều stack trace trùng lặp cho cùng một sự cố.
   - Bắt và log stack trace duy nhất tại chốt chặn cuối cùng: `GlobalExceptionHandler`.

3. **Tuân Thủ Chuẩn RFC 7807 Problem Details:**
   - Mọi response lỗi 4xx/5xx bắt buộc trả về content-type `application/problem+json`.
   - Tuyệt đối không để lộ cấu trúc bảng CSDL, tên lớp nội bộ, hay stack trace ra client trong payload lỗi HTTP 500 (chỉ trả về URN `urn:problem-type:internal-error` kèm title chung).

---

## 5. Quy Chuẩn Đặt Tên & Bố Cục Mã Nguồn (Naming & Clean Code Layout)

| Đối Tượng | Quy Chuẩn Đặt Tên | Ví Dụ Chuẩn | Anti-Pattern Cần Tránh |
|---|---|---|---|
| **Class / Record / Interface** | `PascalCase`, danh từ mang tính biểu đạt cao | `WorkOrderController`, `WorkOrderStatus` | `Workorder_manager`, `IWorkOrderService` |
| **Method** | `camelCase`, động từ hoặc cụm động từ | `advanceStatus()`, `getWorkOrderById()` | `workOrderProcess()`, `doUpdate()` |
| **Field / Variable** | `camelCase`, tên có nghĩa, tránh viết tắt | `equipmentId`, `resolvedAt` | `eqId`, `dt`, `temp` |
| **Constant / Enum Value** | `UPPER_SNAKE_CASE` | `IN_PROGRESS`, `CRITICAL` | `InProgress`, `critical_prio` |
| **DTO Record Suffix** | Hậu tố `Request` hoặc `Response` | `WorkOrderRequest`, `WorkOrderResponse` | `WorkOrderDTO`, `WorkOrderData` |

### Bố Cục Thống Nhất Trong Một Java Class:
1. Static Constants & Static Loggers
2. Instance Fields (`private final`)
3. Constructor (Constructor Injection)
4. Public Business Methods (API bề mặt)
5. Static Factory Methods / Helpers
6. Private Internal Helper Methods

---

## 6. Chính Sách Nhật Ký Hệ Thống & Bảo Mật Dữ Liệu (Secure Logging)

1. **Cú Pháp Chuẩn SLF4J:**
   ```java
   private static final Logger log = LoggerFactory.getLogger(ClassName.class);
   ```

2. **Parametric Logging (Tránh lãng phí CPU):**
   - Luôn sử dụng cú pháp tham số hóa `{}` của SLF4J:
     ```java
     // ĐÚNG: Không tốn chi phí cộng chuỗi nếu log level INFO bị tắt
     log.info("Creating work order for equipment: {}", request.equipmentId());

     // SAI: Luôn tốn CPU cộng chuỗi trước khi gọi method log
     log.info("Creating work order for equipment: " + request.equipmentId());
     ```

3. **Chính Sách Cấm Dữ Liệu Nhạy Cảm (PII Ban):**
   > [!WARNING]
   > **Nghiêm Cấm:** Ghi lại mật khẩu, access token, cookies, thông tin định danh cá nhân (PII), hoặc toàn bộ chuỗi JSON request body thô vào file log.
