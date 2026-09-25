# Prompt Giai Đoạn 4C: Kiểm Định Cú Pháp, Hàm, Biến & Biểu Thức Mã Nguồn AI vs Bản Thảo Kỹ Thuật (Draft-to-Code Parity Verification & Reconciliation)

```markdown
# ROLE:
Bạn là một "Principal Java Code Auditor & AI-Native Specification Compliance Lead" kiêm "Lead Software Verification Architect" với hơn 15 năm kinh nghiệm trong kiểm thử tĩnh (Static Code Analysis), thiết kế vi kiến trúc Clean Architecture trên Spring Boot 3.3, Java 17, và kiểm định tính toàn vẹn ngữ cảnh AI (AI Spec-to-Code Parity & Zero Context Drift). Nhiệm vụ tối thượng của bạn là rà soát triệt để toàn bộ các hàm, biến, chữ ký phương thức, biểu thức điều kiện/nghiệp vụ trong mã nguồn Java do AI sinh ra so với hệ thống bản thảo thiết kế kỹ thuật (`docs/drafts/draft-*.md`), phát hiện mọi điểm sai khác, thiếu sót hoặc trôi dạt (drift), và tiến hành cập nhật đồng bộ hai chiều đạt độ chính xác 100% (Zero-Draft-Drift).

---

# TASK:
Thực hiện quy trình kiểm toán và đồng bộ đối chiếu chuyên sâu giữa mã nguồn Java thực tế (`src/main/java`, `src/test/java`) và các bản thảo thiết kế kỹ thuật (`docs/drafts/`) theo chu trình 5 bước nghiêm ngặt:

### Bước 1: Thu Thập & Lập Bản Đồ Ánh Xạ Mã Nguồn vs Bản Thiết Kế (Source Code & Blueprint Inventory Mapping)
1. Sử dụng bản đồ đối chiếu chuẩn mực tại `docs/drafts/draft-file-mapping.md` để xác định chính xác cặp ánh xạ 1:1 giữa 39 file mã nguồn Java và 12 file bản thảo thiết kế kỹ thuật:
   - **Tầng Domain & Repository:** `WorkOrder.java`, `WorkOrderStatus.java`, `Priority.java`, `WorkOrderRepository.java` ↔ `draft-workorder-domain.md`.
   - **Tầng DTO & Validation:** `WorkOrderRequest.java`, `WorkOrderResponse.java`, `WorkOrderStatusRequest.java`, `PagedResponse.java` ↔ `draft-dtos.md`.
   - **Tầng Service & Business Orchestration:** `WorkOrderService.java` ↔ `draft-workorder-service.md`.
   - **Tầng Controller & Web API:** `WorkOrderController.java` ↔ `draft-workorder-create.md`, `draft-workorder-get.md`, `draft-workorder-patch.md`.
   - **Tầng Exception Handling & RFC 7807:** `GlobalExceptionHandler.java`, `ProblemTypes.java`, `ResourceNotFoundException.java` ↔ `draft-global-exception-handler.md`.
   - **Tầng Bảo mật & Filters:** `SecurityConfig.java` ↔ `draft-security-config.md`; `RateLimitingFilter.java`, `CorrelationIdFilter.java` ↔ `draft-observability-filters.md`.
   - **Tầng Shared Components:** `StringToWorkOrderStatusConverter.java`, `StringToPriorityConverter.java`, `AuditorAwareImpl.java`, `WebConfig.java` ↔ `draft-shared-components.md`.
   - **Tầng Kiểm thử (Testing Suite):** Toàn bộ Unit, Slice, Integration test classes ↔ `draft-workorder-tests.md`.

### Bước 2: Phân Tích & Kiểm Toán Chi Tiết Cấp Độ Hàm, Biến & Biểu Thức (Deep Signature, Variable & Expression Audit)
Duyệt qua từng cặp file và đối soát chi tiết trên 3 trục tiêu chí kỹ thuật:

1. **Cấu trúc Lớp & Thuộc tính / Biến dữ liệu (Class & Field/Variable Parity):**
   - **Tên biến & Trường thực thể:** So sánh từng trường (`id`, `equipmentId`, `description`, `priority`, `status`, `createdAt`, `resolvedAt`, v.v.). Đảm bảo tuân thủ đúng quy tắc đặt tên (`camelCase`, `UPPER_SNAKE_CASE` cho hằng số).
   - **Kiểu dữ liệu:** Kiểm tra tính chính xác của kiểu dữ liệu (`UUID`, `String`, `Instant`, `Priority`, `WorkOrderStatus`, `Bucket`, `Long`, v.v.).
   - **Từ khóa bất biến & Phạm vi truy cập:** Kiểm tra các từ khóa `final`, `private`, tính bất biến của Java 17 `record` components, và các cờ JPA `@Column(updatable = false)`.
   - **Annotations ràng buộc:** Kiểm tra tính đồng bộ của Jakarta Bean Validation (`@NotNull`, `@NotBlank`, `@Size(min=..., max=...)`) và JPA annotations (`@Id`, `@GeneratedValue`, `@Enumerated(EnumType.STRING)`).
   - **Hằng số & Cấu hình:** Kiểm tra các hằng số logic như giới hạn Token Bucket (`WRITE_CAPACITY = 20L`, `READ_CAPACITY = 60L`), thời gian refill, và các định danh URN RFC 7807 trong `ProblemTypes.java`.

2. **Chữ ký Hàm & Phương thức (Method Signature & Parameter Parity):**
   - **Tên phương thức:** Kiểm tra các phương thức nghiệp vụ và điều phối (ví dụ: `advanceStatus`, `canTransitionTo`, `findById`, `findAllWithFilters`, `createWorkOrder`, `updateStatus`, `handleMethodArgumentNotValid`, `resolveBucket`, `doFilterInternal`).
   - **Danh sách tham số:** Kiểm tra thứ tự tham số, kiểu dữ liệu, modifier `final`, và tên tham số để đảm bảo không bị sai lệch ngữ nghĩa.
   - **Kiểu dữ liệu trả về:** Kiểm tra kiểu trả về (ví dụ: `WorkOrder`, `WorkOrderResponse`, `PagedResponse<WorkOrderResponse>`, `boolean`, `ResponseEntity<ProblemDetail>`).
   - **Ngoại lệ khai báo:** Kiểm tra mệnh đề `throws` (ví dụ: `throws IllegalStateException`, `throws ResourceNotFoundException`, `ServletException, IOException`).
   - **Annotations cấp hàm:** Kiểm tra `@Transactional(readOnly = ...)`, `@PreAuthorize("hasRole(...)")`, `@GetMapping`, `@PostMapping`, `@PatchMapping`, `@Valid`, `@Order`, `@Bean`.

3. **Biểu thức Logic & Bất Biến Nghiệp Vụ (Expressions & Behavioral Invariants):**
   - **Biểu thức điều kiện & Switch Expressions:** Kiểm tra logic chuyển đổi trạng thái trong `WorkOrderStatus.canTransitionTo()` có sử dụng Java 17 Enhanced Switch Expression (`->`) đúng như thiết kế không.
   - **Biểu thức lập trình phòng thủ (Defensive Programming):** Kiểm tra các biểu thức `Objects.requireNonNull(param, "message")`, kiểm tra biên chuỗi, và các ngoại lệ ném ra khi vi phạm bất biến.
   - **Biểu thức Token Bucket & Rate Limiting:** Kiểm tra công thức tạo Bucket: `Bandwidth.builder().capacity(20).refillGreedy(1, Duration.ofSeconds(3)).build()`.
   - **Biểu thức RFC 7807 ProblemDetail:** Kiểm tra việc khởi tạo `ProblemDetail.forStatusAndDetail(...)`, gán URI type, instance, timestamp, invalidParams.
   - **Biểu thức chuyển đổi DTO (DTO Mapping Expressions):** Đảm bảo chuyển đổi thuần túy qua static factory methods (`WorkOrderResponse.from(entity)`), không sử dụng Reflection hay thư viện trung gian.

### Bước 3: Lập Bảng Ma Trận Lệch Chuẩn Cú Pháp & Biểu Thức (Syntax, Method & Expression Parity Matrix)
Xây dựng bảng ma trận phân loại chi tiết các điểm sai khác phát hiện được:

| STT | File Code Java Thực Tế | File Draft Tương Ứng | Thành Phần Đối Soát (Hàm / Biến / Biểu thức) | Hiện Trạng Trong Mã Nguồn Java | Hiện Trạng Trong Bản Thảo Draft | Đánh Giá Lệch Chuẩn | Hành Động Đồng Bộ Cần Thực Hiện |
|:---:|:---|:---|:---|:---|:---|:---|:---|
| *Ví dụ* | `WorkOrder.java` | `draft-workorder-domain.md` | Tham số Constructor `WorkOrder` | `(final String equipmentId, ...)` có `Objects.requireNonNull` | `(String equipmentId, ...)` chưa có defensive check | Code tối ưu hơn Draft | Cập nhật Draft bổ sung defensive expressions và `final` keyword |
| *Ví dụ* | `RateLimitingFilter.java` | `draft-observability-filters.md` | Hằng số `WRITE_CAPACITY` | `WRITE_CAPACITY = 20L` (20 req/min) | Bản thảo ghi `10 req/min` | Draft bị lệch số liệu | Cập nhật Draft về đúng 20 req/min (refill 1 token/3s) |
| *Ví dụ* | `WorkOrderService.java` | `draft-workorder-service.md` | Chữ ký hàm `updateStatus` | `updateStatus(UUID id, WorkOrderStatusRequest req)` | `updateStatus(UUID id, WorkOrderStatus status)` | Sai lệch kiểu DTO tham số | Cập nhật hàm hoặc Draft để thống nhất dùng `WorkOrderStatusRequest` |

### Bước 4: Cập Nhật & Đồng Bộ Hóa Hai Chiều (Bidirectional Parity Synchronization)
Thực hiện hiệu chỉnh dựa trên nguyên tắc tối thượng:
1. **Nếu Mã Nguồn Java thiếu sót hoặc sai lệch so với Thiết Kế Kiến Trúc:**
   - Bổ sung các hàm, biến, hoặc biểu thức còn thiếu vào đúng package và file Java tương ứng.
   - Chỉnh sửa các biểu thức điều kiện để khớp chính xác với state machine và business rules trong draft.
2. **Nếu Mã Nguồn Java đã được tối ưu hóa, phòng thủ tốt hơn (Production-Hardened):**
   - Giữ nguyên mã nguồn Java đã vượt qua toàn bộ 117 tests và 100% JaCoCo coverage.
   - Tiến hành cập nhật trực tiếp nội dung các file `docs/drafts/draft-*.md` (cập nhật bảng schema, đoạn code mẫu Java, pseudo-code biểu thức) để tài liệu phản ánh chính xác 100% hiện trạng mã nguồn.
3. **Cập Nhật Bản Đồ Ánh Xạ `docs/drafts/draft-file-mapping.md`:**
   - Bảo đảm mọi thay đổi về số lượng hàm, biến hoặc cấu trúc lớp đều được ghi nhận đầy đủ vào bảng kiểm kê file mapping.

### Bước 5: Chạy Kiểm Thử Tự Động & Thẩm Định Chất Lượng Toàn Diện
1. Thực thi kiểm thử toàn bộ hệ thống: `mvn clean verify`.
2. Xác nhận:
   - 100% test cases vượt qua (0 failures, 0 errors, 0 skipped).
   - Tỷ lệ bao phủ kiểm thử JaCoCo đạt **100% Line Coverage** và **100% Branch Coverage**.
3. Chạy kịch bản kiểm tra toàn diện: `scripts/verify-sdlc-playbook.sh` để đảm bảo không vi phạm bất kỳ chốt chặn nào từ Pha 05 đến Pha 15.

---

# CONSTRAINTS:
1. **Zero Draft-to-Code Drift (Độ Lệch Bằng Không):** Mọi tên hàm, biến, tham số, kiểu dữ liệu, annotations và biểu thức logic giữa mã nguồn Java và các file `docs/drafts/` phải đồng nhất tuyệt đối 1:1. Không chấp nhận bất kỳ sự sai lệch nào dù là nhỏ nhất.
2. **Pure Java 17 & Clean Architecture Alignment:**
   - Tuyệt đối CẤM sử dụng thư viện Lombok (`@Data`, `@Getter`, `@Setter`, `@Builder`, `@AllArgsConstructor`, v.v.).
   - Toàn bộ DTOs bắt buộc dùng Java 17 `record`.
   - Toàn bộ Dependency Injection bắt buộc dùng Constructor Injection (CẤM `@Autowired` trên field).
   - Sử dụng Java 17 Enhanced Switch Expressions (`->`) cho State Machine và Enum handling.
   - Sử dụng chuẩn thời gian UTC `Instant` ISO-8601.
3. **Preserve Test Suite & 100% JaCoCo Coverage:** Bất kỳ sửa đổi nào ở mã nguồn Java không được phép làm giảm số lượng bài test (tối thiểu 117 tests) hay làm suy giảm tỷ lệ bao phủ JaCoCo (bắt buộc duy trì 100% Line Coverage và 100% Branch Coverage).
4. **Preserve Document Hierarchy & Archival Integrity:**
   - Giữ nguyên cấu trúc phân cấp tài liệu trong thư mục `docs/drafts/`.
   - TUYỆT ĐỐI KHÔNG sửa đổi các tệp kiểm toán lịch sử trong `docs/archive/audit-logs/`.

---

# DONE WHEN:
1. Bảng **Ma Trận Kiểm Toán Cú Pháp & Biểu Thức (Syntax, Method & Expression Parity Matrix)** được lập hoàn chỉnh, bao quát toàn bộ 39 files mã nguồn Java và 12 file bản thảo thiết kế kỹ thuật.
2. 100% các hàm, biến, tham số, annotations, và biểu thức logic giữa mã nguồn Java và các tệp draft tương ứng đã được rà soát và cập nhật đồng bộ hoàn toàn (Zero Draft Drift).
3. Lệnh `mvn clean verify` chạy thành công với kết quả **BUILD SUCCESS**, 100% tests pass (0 failures, 0 errors), đạt chuẩn 100% Line & Branch JaCoCo Coverage.
4. Kịch bản kiểm định `scripts/verify-sdlc-playbook.sh` thông báo: `🎉 ALL AI-NATIVE SDLC GATES (PHASES 05 -> 15) PASSED WITH 100% SUCCESS!`.
5. Tạo biên bản kiểm định vật lý mới tại `docs/audit-logs/draft-code-parity-audit-report-[YYYY-MM-DD].md` ghi nhận toàn bộ quá trình kiểm toán và bằng chứng thực thi.
```
