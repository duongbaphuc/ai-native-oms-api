# Prompt Giai Đoạn 4: Hiện Thực Hóa Mã Nguồn Theo Đặc Tả (Spec-Driven Implementation)

```markdown
# ROLE:
Bạn là một "Principal Software Engineer" và "Senior Spring Boot 3.3 Architect" với chuyên môn sâu về Domain-Driven Design (DDD), Clean Architecture, và Spring Security. Nhiệm vụ của bạn là hiện thực hóa mã nguồn từ ngữ cảnh tài liệu đã được kiểm toán.

---

# TASK:
Đọc bản đồ điều hướng `docs/03-CONTEXT_INDEX.md` và sinh toàn bộ mã nguồn Java 17 + Spring Boot 3.3 theo cấu trúc Clean Architecture 3 tầng (Layered Architecture):

1. **Database Migration Script:**
   - `src/main/resources/db/migration/V1__init_work_orders_schema.sql`: Khởi tạo bảng `work_orders`, UUID PK, Check constraints (`chk_work_orders_priority`, `chk_work_orders_status`), và indexes tối ưu truy vấn.
2. **Domain Layer:**
   - `Priority.java`: Enum mức ưu tiên.
   - `WorkOrderStatus.java`: Enum trạng thái kèm phương thức bất biến `canTransitionTo()`.
   - `WorkOrder.java`: JPA Entity Aggregate Root, đóng gói quy tắc `advanceStatus()` và gán `resolvedAt`.
   - `WorkOrderRepository.java`: Kế thừa `JpaRepository` kèm `findByStatus(WorkOrderStatus, Pageable)`.
3. **DTO Layer:**
   - `WorkOrderRequest.java`: Java Record với Jakarta Validation (`@NotBlank`, `@Size(min=10, max=500)`).
   - `WorkOrderStatusRequest.java`: Java Record cho yêu cầu cập nhật trạng thái.
   - `WorkOrderResponse.java`: Java Record kết quả kèm static factory method `from(WorkOrder)`.
   - `PagedResponse.java`: Java Record phân trang chuẩn hóa kèm static factory method `from(Page<T>)`.
4. **Exception Handling:**
   - `ResourceNotFoundException.java`: Domain Exception kế thừa `RuntimeException`.
   - `GlobalExceptionHandler.java`: `@RestControllerAdvice` xử lý 6 nhóm ngoại lệ trả về RFC 7807 `ProblemDetail`.
5. **Service & Controller:**
   - `WorkOrderService.java`: 3-tier Service xử lý logic, transaction và chuyển đổi DTO.
   - `WorkOrderController.java`: `@RestController` với tiền tố `/api/v1/workorders`, Bean Validation `@Valid`, và RBAC annotations `@PreAuthorize`.
6. **Security & Configuration:**
   - `SecurityConfig.java`: Cấu hình Stateless, HTTP Basic, Method Security, và `AuthenticationEntryPoint` RFC 7807 (`urn:problem-type:unauthorized`).
   - `StringToWorkOrderStatusConverter.java`: Converter query param hỗ trợ cả `Open` lẫn `OPEN`.
   - `application.yml`: H2 in-memory DB, `fail-on-unknown-properties: true`.

---

# CONSTRAINTS:
1. **Tuân thủ Coding Rules:** CẤM dùng Lombok (viết getters/constructors thủ công hoặc dùng Java Record). Dùng Java 17 Switch Expressions.
2. **Zero-Hallucination:** Tuyệt đối không tự ý thêm các trường hoặc endpoint không có trong `docs/02-api-spec.md`.
3. **Comment AI Provenance:** Dòng đầu tiên của mỗi file bắt buộc có comment ghi rõ nguồn gốc tài liệu đặc tả sinh ra code (VD: `// AI Provenance: generated from docs/02-api-spec.md §1`).

---

# DONE WHEN:
1. Đầy đủ các file mã nguồn tại các package: `com.gpc.oms.domain`, `com.gpc.oms.dto`, `com.gpc.oms.service`, `com.gpc.oms.controller`, `com.gpc.oms.exception`, `com.gpc.oms.config`.
2. Chạy lệnh `mvn compile` hoàn thành thành công (BUILD SUCCESS), 0 warning, 0 error.
```
