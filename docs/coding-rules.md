# Quy Chuẩn Lập Trình Java & Cẩm Nang Design Patterns (Oracle Senior Java Standards)

<!--
Role: Principal Java Software Architect & Senior Java Engineer at Oracle
Task: Define production-grade Java 17+ coding rules, JVM/GC optimization standards, and GoF design pattern guidelines
Context files: docs/domain-model.md, docs/api-spec.md, docs/security-rules.md, docs/internal-coding-standards.md
Constraints: Java 17 LTS, Spring Boot 3.3, Zero-Lombok, 100% Constructor Injection, RFC 7807 compliance, Pure Java Records
-->

Tài liệu này xác lập bộ quy chuẩn viết mã (Coding Standards) và hướng dẫn ứng dụng các Mẫu Thiết Kế (Design Patterns) cho toàn bộ dự án, được biên soạn theo phong cách và tư duy của **Senior Java Engineer tại Oracle (Oracle Core Platform & JDK Team)**.

Mọi dòng mã do con người hay AI Coding Agents (GitHub Copilot / Cursor) tạo ra đều bắt buộc phải tuân thủ nghiêm ngặt các nguyên tắc dưới đây.

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

---

## 2. Cẩm Nang Ứng Dụng Design Patterns Tối Ưu Hóa Class

Việc áp dụng các Design Pattern phải hướng tới mục tiêu tối ưu hóa tính đóng gói, dễ mở rộng, dễ kiểm thử và tiết kiệm tài nguyên:

### 2.1 Static Factory Method Pattern (`of()`, `from()`)
- **Mục đích:** Thay thế constructors thô, cung cấp tên gọi mang ý nghĩa nghiệp vụ rõ ràng, và đóng gói logic chuyển đổi dữ liệu.
- **Áp dụng tại:** Tầng DTOs và Value Objects.
- **Code mẫu chuẩn:**
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
      // Static Factory Method đóng gói logic mapping từ Domain Entity
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

### 2.2 State Pattern & Strategy Pattern
- **Mục đích:** Đóng gói toàn bộ máy trạng thái (State Machine) và các quy tắc rẽ nhánh vào chính Enum/State class, triệt tiêu các khối lệnh `if-else` hoặc `switch-case` lộn xộn nằm rải rác ở tầng Service/Controller.
- **Áp dụng tại:** `WorkOrderStatus.java`.
- **Code mẫu chuẩn:**
  ```java
  public enum WorkOrderStatus {
      OPEN,
      IN_PROGRESS,
      DONE;

      // State Transition Validator đóng gói trọn vẹn quy tắc chuyển đổi
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
  ```

### 2.3 Explicit Adapter / Mapper Pattern
- **Mục đích:** Chuyển đổi dữ liệu 1-1 giữa Domain Entity và DTOs mà không dùng reflection.
- **Nguyên tắc:** 
  - Cấm sử dụng các thư viện như `ModelMapper`, `Dozer`, `BeanUtils.copyProperties`. Các thư viện này gây overhead lớn về CPU do liên tục inspect metadata qua reflection và dễ sinh lỗi runtime khi tên trường bị lệch mà trình biên dịch không phát hiện được.
  - Sử dụng phương thức thuần Java (Explicit Mapping qua static methods hoặc dedicated Mapper classes).

### 2.4 Chain of Responsibility Pattern
- **Mục đích:** Phân tách rạch ròi các khâu xử lý an ninh, xác thực, truy vết và bắt lỗi thành các mắt xích độc lập.
- **Áp dụng tại:**
  - `SecurityFilterChain` (Dual FilterChain: `@Order(1)` cho H2 Console non-prod, `@Order(2)` cho API chính).
  - `GlobalExceptionHandler` (`@RestControllerAdvice`): Bắt các nhóm ngoại lệ chuyên biệt từ cụ thể nhất (`MethodArgumentNotValidException`) đến tổng quát nhất (`Exception.class`).

### 2.5 Template Method & Orchestration Pattern
- **Mục đích:** Phân tách rõ trách nhiệm giữa tầng điều phối giao dịch (Service) và tầng nghiệp vụ cốt lõi (Domain Aggregate Root).
- **Nguyên tắc:**
  - Service chỉ đóng vai trò **Orchestrator** (Lấy entity từ DB $\rightarrow$ Kích hoạt phương thức nghiệp vụ của entity $\rightarrow$ Lưu lại $\rightarrow$ Chuyển đổi sang Response).
  - Không bao giờ chuyển đổi trạng thái trực tiếp trong Service bằng setter thô. Entity phải tự bảo vệ tính bất biến (Domain Invariant Encapsulation) qua phương thức `advanceStatus()`.

---

## 3. Tối Ưu Hóa Bộ Nhớ, Vòng Lặp & Hiệu Năng JVM (GC Optimization)

1. **Quản Lý Phạm Vi Biến (Variable Scoping & Escape Analysis):**
   - Khai báo biến trong phạm vi hẹp nhất có thể. Đặt biến sát thời điểm sử dụng đầu tiên.
   - Thêm từ khóa `final` cho các biến cục bộ không thay đổi giá trị. Việc này giúp JIT Compiler thực hiện **Escape Analysis** hiệu quả hơn để phân bổ đối tượng trên Stack thay vì Heap (Scalar Replacement), giảm áp lực cho bộ thu gom rác (Garbage Collector).

2. **Chiến Lược Thao Tác Chuỗi (String Concatenation Strategy):**
   - **Nối chuỗi đơn giản:** Sử dụng toán tử `+`. Kể từ Java 9, trình biên dịch sử dụng `invokedynamic` với `StringConcatFactory` tự động tối ưu hóa việc ghép chuỗi mà không tạo ra các đối tượng trung gian lãng phí.
   - **Vòng lặp lớn:** Bắt buộc sử dụng `StringBuilder` khi ghép chuỗi bên trong vòng lặp (`for`, `while`) có số lần lặp không xác định hoặc lớn hơn 10 phần tử. Định trước dung lượng ban đầu (initial capacity) nếu ước tính được: `new StringBuilder(128)`.

3. **Khởi Tạo Kích Thước Collections (Pre-Sizing Collections):**
   - Khi biết trước số lượng phần tử cần thêm vào `ArrayList`, `HashMap`, hoặc `HashSet`, luôn cung cấp `initialCapacity` để triệt tiêu chi phí cấp phát lại mảng ngầm (Array resizing & copying):
     ```java
     // Tránh cấp phát thừa và resize nhiều lần
     List<WorkOrderResponse> result = new ArrayList<>(workOrders.size());
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
