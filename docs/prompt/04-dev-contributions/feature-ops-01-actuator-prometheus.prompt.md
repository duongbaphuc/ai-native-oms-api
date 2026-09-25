# Prompt: Triển Khai OPS-01 - Tích Hợp Spring Boot Actuator & Prometheus Micrometer Metrics (Issue #44)

> **Mã tính năng / Issue:** `[FEATURE] OPS-01: Tích hợp Spring Boot Actuator và Prometheus Micrometer Metrics` ([#44](https://github.com/duongbaphuc/ai-native-oms-api/issues/44))  
> **Phân loại:** Observability, Production Readiness & Security Enforcement Task Prompt  
> **Nền tảng công nghệ:** Java 17 LTS | Spring Boot 3.3.4 | Spring Security 6.x | Micrometer 1.13+ | Prometheus  
> **Tài liệu tham chiếu:**  
> - `docs/observability-and-logging.md §4` (Chỉ số đo lường & Giám sát)  
> - `docs/security-auth-spec.md §3` (Ma trận phân quyền RBAC Actuator)  
> - `docs/archive/audit-logs/code-vs-spec-audit-report-2026-09-24-2.md` (Finding F-05)  
> - `docs/internal-coding-standards.md` & `pom.xml` (JaCoCo Quality Gate: 100% Line & Branch Coverage)

---

```markdown
# ROLE:
Bạn là một "Principal Site Reliability Engineer (SRE)" kiêm "Senior Spring Boot Security Architect". Bạn có chuyên môn sâu về Spring Boot 3.3.4 Actuator internals, Micrometer Metrics Registry, Prometheus exposition format, và kiến trúc phòng vệ an ninh phân quyền nhiều lớp (Spring Security 6 Stateless RBAC).

---

# CONTEXT:
Microservice Outage Work Order API (`oms-api-demo`) đang chuẩn bị bàn giao cho đội ngũ SRE vận hành trên hạ tầng Kubernetes. Theo báo cáo kiểm toán chất lượng phần mềm (`docs/archive/audit-logs/code-vs-spec-audit-report-2026-09-24-2.md`, Finding F-05) và đặc tả giám sát (`docs/observability-and-logging.md §4`), hệ thống hiện tại chưa tích hợp Actuator và Prometheus Metrics.

Các ràng buộc kiến trúc bất di bất dịch của dự án:
1. **Kiến trúc Zero-Lombok & Explicit Constructor Injection:** Tuyệt đối không dùng Lombok; khởi tạo dependency qua explicit constructor injection.
2. **JaCoCo Quality Gate:** Dự án áp dụng ngưỡng bắt buộc 100% Line Coverage và 100% Branch Coverage cho toàn bộ các packages nghiệp vụ (`com.gpc.oms.domain`, `service`, `controller`, `dto`, `exception`). Mọi dòng code mới hoặc thay đổi đều phải được kiểm thử đầy đủ 100%.
3. **Phân Quyền RBAC Nghiêm Ngặt (Zero-Trust):**
   - `/actuator/health` và `/actuator/info`: Cho phép truy cập công khai (Public / `permitAll()`) phục vụ Kubernetes Liveness/Readiness Probes.
   - `/actuator/prometheus`: Chỉ mở cho Quản trị viên (`ROLE_ADMIN`). Mọi truy cập không có token/credentials phải trả về HTTP 401; có credentials nhưng không có role ADMIN phải trả về HTTP 403 với định dạng RFC 7807 Problem Details.

---

# TASK:
Hiện thực hóa toàn diện tính năng OPS-01 qua 5 giai đoạn chi tiết sau:

### Giai Đoạn 1: Bổ Sung Dependencies vào `pom.xml`
Khai báo 2 dependencies chính thức của Spring Boot ecosystem (không chỉ định version do kế thừa từ `spring-boot-starter-parent` 3.3.4):
1. `org.springframework.boot:spring-boot-starter-actuator`
2. `io.micrometer:micrometer-registry-prometheus`

### Giai Đoạn 2: Cấu Hình Giám Sát trong `src/main/resources/application.yml`
Bổ sung khối cấu hình `management` chuẩn Kubernetes SRE:
1. Mở các web endpoints qua thuộc tính: `management.endpoints.web.exposure.include: health,info,prometheus`.
2. Kích hoạt probes Kubernetes: `management.endpoint.health.probes.enabled: true`.
3. Cấu hình chi tiết hiển thị health: `management.endpoint.health.show-details: always`.
4. Đính kèm common metric tags: `management.metrics.tags.application: ${spring.application.name}`.

### Giai Đoạn 3: Cấu Hình Bảo Mật Endpoint tại `com.gpc.oms.config.SecurityConfig`
Cập nhật `filterChain(HttpSecurity http)`:
1. Bổ sung matcher công khai: `.requestMatchers("/actuator/health", "/actuator/info").permitAll()`.
2. Bổ sung matcher phân quyền quản trị: `.requestMatchers("/actuator/prometheus").hasRole("ADMIN")`.
3. Đảm bảo xử lý `accessDeniedHandler` trả về mã lỗi HTTP 403 định dạng RFC 7807 Problem Details (`application/problem+json`) tương thích với `authenticationEntryPoint` 401 sẵn có:
   - status: 403
   - type: `urn:problem-type:forbidden`
   - title: `Forbidden`
   - detail: `Access Denied: You do not have permission to access this resource`

### Giai Đoạn 4: Tích Hợp Custom Business Metrics với Micrometer
Tại `com.gpc.oms.service.WorkOrderService`:
1. Inject `io.micrometer.core.instrument.MeterRegistry` thông qua constructor injection tường minh (`public WorkOrderService(WorkOrderRepository repo, MeterRegistry meterRegistry)`).
2. Hiện thực hóa 2 Counters nghiệp vụ theo đúng đặc tả `docs/observability-and-logging.md §4`:
   - **`oms_workorders_created_total`**: Ghi nhận khi một WorkOrder được tạo mới thành công (`createWorkOrder`), với các tags:
     - `priority`: Giá trị enum priority (ví dụ: `CRITICAL`, `HIGH`, `MEDIUM`, `LOW`).
     - `status`: Giá trị enum status khởi tạo (`OPEN`).
   - **`oms_workorder_status_transitions_total`**: Ghi nhận khi chuyển đổi trạng thái WorkOrder thành công (`updateStatus`), với các tags:
     - `from_status`: Trạng thái nguồn trước khi cập nhật (ví dụ: `OPEN`).
     - `to_status`: Trạng thái đích sau khi cập nhật thành công (ví dụ: `IN_PROGRESS`, `DONE`).
3. Cập nhật Javadoc `@implSpec`, `@apiNote` và duy trì cấu trúc code sạch, không phá vỡ logic cũ.

### Giai Đoạn 5: Kiểm Thử Tự Động (Testing) & Bảo Đảm JaCoCo Quality Gate
1. **Cập nhật Unit Tests:**
   - Cập nhật `src/test/java/com/gpc/oms/service/WorkOrderServiceTest.java`: Mock hoặc sử dụng `SimpleMeterRegistry` để kiểm thử chính xác việc increment các Counters trong cả kịch bản `createWorkOrder` và `updateStatus`.
2. **Bổ Sung Actuator Security & Endpoint Integration Tests:**
   - Tạo mới `src/test/java/com/gpc/oms/controller/ActuatorEndpointTest.java` (hoặc tích hợp vào `WorkOrderIntegrationTest`):
     - `GET /actuator/health` không cần credentials trả về HTTP 200 OK với body chứa `"status":"UP"`, kiểm tra liveness và readiness probes.
     - `GET /actuator/info` trả về HTTP 200 OK.
     - `GET /actuator/prometheus` không credentials trả về HTTP 401 Unauthorized (RFC 7807).
     - `GET /actuator/prometheus` với user có role `DISPATCHER` hoặc `TECHNICIAN` trả về HTTP 403 Forbidden (RFC 7807).
     - `GET /actuator/prometheus` với user có role `ADMIN` trả về HTTP 200 OK với text/plain chứa Prometheus scrape metrics.
3. **Kiểm thử Custom Metrics Scraped:**
   - Tạo WorkOrder và chuyển trạng thái, sau đó scrape `/actuator/prometheus` bằng role `ADMIN` để xác thực metric `oms_workorders_created_total` và `oms_workorder_status_transitions_total` xuất hiện trong payload output.

---

# CONSTRAINTS:
1. **Tuân thủ Documentation Standards:** Mọi class và method mới/sửa đổi phải có Javadoc chuẩn Oracle với các tag `@apiNote`, `@implSpec`.
2. **Không làm hỏng 78 unit/integration tests hiện tại:** Toàn bộ test suite cũ phải giữ vững kết quả Green 100%.
3. **JaCoCo Quality Gate 100%:** Ngưỡng 1.00 (100%) cho cả Line và Branch Coverage trên BUNDLE và CLASS không được phép bị suy giảm.
4. **Không dùng Lombok:** Duy trì nguyên tắc Java 17 chuẩn mực của dự án.
5. **Định dạng lỗi:** Luôn tuân thủ chuẩn RFC 7807 Problem Details (`application/problem+json`).

---

# DONE WHEN:
1. `pom.xml` nạp đủ `spring-boot-starter-actuator` và `micrometer-registry-prometheus`.
2. `application.yml` cấu hình đầy đủ `health`, `info`, `prometheus` và Kubernetes health probes.
3. Endpoint `/actuator/health` phản hồi HTTP 200 `{"status":"UP"}` cho unauthenticated request.
4. Endpoint `/actuator/prometheus` bảo vệ nghiêm ngặt: 401 khi ẩn danh, 403 khi thiếu quyền ADMIN, 200 khi có quyền ADMIN.
5. Hai metrics `oms_workorders_created_total` và `oms_workorder_status_transitions_total` xuất hiện và tăng giá trị chính xác khi có hoạt động tạo và chuyển trạng thái phiếu.
6. Lệnh `mvn clean verify` chạy thành công 100% Green, vượt qua toàn bộ JaCoCo rule checks (100% Line & Branch Coverage).
```
