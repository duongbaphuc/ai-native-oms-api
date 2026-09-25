# Prompt: Triển Khai SEC-03 - Hiện Thực Hóa CorrelationIdFilter Cho Truy Vết Phân Tán và MDC Log Context Đạt Chuẩn Oracle Java (Issue #31)

> **Mã tính năng / Issue:** `[FEATURE] SEC-03: Hiện thực hóa CorrelationIdFilter cho truy vết phân tán và MDC log context` ([#31](https://github.com/duongbaphuc/ai-native-oms-api/issues/31))  
> **Phân loại:** Observability, Distributed Tracing & Security Forensics Task Prompt  
> **Nền tảng công nghệ:** OpenJDK / Oracle Java 17 LTS | Spring Boot 3.3.4 | SLF4J 2.0+ MDC | Jakarta Servlet 6.0  
> **Quy chuẩn đối chiếu:**  
> - `docs/02-observability-and-logging.md §1 & §2` (Elastic Common Schema ECS & Chu trình sống Correlation ID)  
> - `docs/09-SECURITY_HANDOVER_REPORT.md §3` (Lỗ hổng SEC-03, CWE-778 Insufficient Logging / Forensic Audit Gap)  
> - `docs/08-ORACLE_JAVA_DOCUMENTATION.md` & `docs/00-coding-rules.md` (Oracle Java Coding Standards, JEP 106 Javadoc Tags)  
> - `docs/00-internal-coding-standards.md` & `pom.xml` (JaCoCo Quality Gate: 100% Line & Branch Coverage)

---

```markdown
# ROLE:
Bạn là một "Oracle Certified Master Java Enterprise Architect" kiêm "Principal Distributed Tracing & SRE Observability Specialist". Bạn sở hữu chuyên môn sâu sắc về chuẩn lập trình Oracle Java 17 LTS, đặc tả Javadoc (JEP 106), chu trình sống Servlet Filter (Jakarta Servlet Filter Lifecycle), cấu trúc luồng của Web Container (Tomcat Thread Pool), cơ chế quản lý `ThreadLocal` qua SLF4J MDC (Mapped Diagnostic Context), và kỹ thuật điều tra dấu vết an ninh số học (Digital Forensics / Forensic Audit - CWE-778).

---

# CONTEXT:
Theo Hồ sơ Bàn giao An ninh (`docs/09-SECURITY_HANDOVER_REPORT.md §3`, mã phát hiện SEC-03 / CWE-778) và Đặc tả Giám sát (`docs/02-observability-and-logging.md §1 & §2`), microservice Outage Work Order API (`oms-api-demo`) hiện chưa có lớp hiện thực `CorrelationIdFilter.java` tại gói `com.gpc.oms.config`. Điều này dẫn đến các request HTTP đầu vào không được gán định danh truy vết duy nhất, nhật ký Logback xuất ra thiếu trường `traceId`, gây đứt gãy luồng điều tra của SOC/SRE khi cần truy vết sự cố hoặc kiểm toán an ninh phân tán.

Các ràng buộc kỹ thuật cốt lõi đạt chuẩn Oracle Java của dự án:
1. **Oracle Coding Standards & Explicit Code:** Mã nguồn Java 17 thuần khiết, cấm tuyệt đối Lombok, tiêm phụ thuộc tường minh qua Constructor Injection, sử dụng các hằng số bất biến (`static final`).
2. **Oracle Javadoc JEP 106 Specification:** Toàn bộ lớp, hằng số, phương thức bắt buộc có Javadoc đầy đủ với các tag chuẩn mực: `@apiNote` (hướng dẫn tích hợp API), `@implSpec` (ràng buộc thuật toán và chu trình sống), `@implNote` (ghi chú an toàn luồng và ThreadLocal), `@param`, `@throws`, `@see`, `@since`.
3. **Quản Lý Bộ Nhớ & An Toàn Luồng (ThreadLocal Lifecycle & Memory Leak Prevention):**
   - Bộ chứa `MDC` bản chất hoạt động trên nền tảng `ThreadLocal`. Trong môi trường Tomcat tái sử dụng Thread Pool worker threads, nếu không dọn sạch ngữ cảnh, dữ liệu trace ID của request trước sẽ bị rò rỉ (context bleed) sang request tiếp theo của người dùng khác.
   - Bắt buộc đặt thao tác dọn dẹp MDC (`MDC.remove(...)` hoặc `MDC.clear()`) trong khối `finally` của cấu trúc `try-finally`.
4. **Chuẩn Hóa Khóa MDC Phân Tán:** Đồng bộ cả hai khóa:
   - `traceId`: Khớp với chuẩn Elastic Common Schema (ECS) được định nghĩa tại `docs/02-observability-and-logging.md §1`.
   - `correlationId`: Khớp với yêu cầu đặc tả nghiệp vụ của Issue #31.
5. **Thứ Tự Ưu Tiên Cao Nhất (Highest Precedence):** Bộ lọc phải chạy đầu tiên trước mọi filter nghiệp vụ và trước `SecurityFilterChain` (`@Order(Ordered.HIGHEST_PRECEDENCE)`) để đảm bảo toàn bộ log của Security và ExceptionHandler đều mang trace ID.
6. **Bảo Toàn Chất Lượng JaCoCo Quality Gate:** Đạt 100% Line Coverage và 100% Branch Coverage cho toàn bộ mã nguồn mới.

---

# TASK:
Hiện thực hóa toàn diện giải pháp Distributed Tracing SEC-03 theo chuẩn Oracle Java qua 4 giai đoạn chi tiết sau:

### Giai Đoạn 1: Hiện Thực Hóa `CorrelationIdFilter.java` Đạt Chuẩn Oracle JEP 106
Tạo mới file tại `src/main/java/com/gpc/oms/config/CorrelationIdFilter.java`:
1. **Package & Kế thừa:** Thuộc gói `com.gpc.oms.config`, kế thừa `org.springframework.web.filter.OncePerRequestFilter`.
2. **Annotations:**
   - `@Component`: Để Spring Boot tự động đăng ký vào Servlet Filter Chain.
   - `@Order(Ordered.HIGHEST_PRECEDENCE)`: Thiết lập độ ưu tiên cao nhất, chạy trước chuỗi Spring Security.
3. **Định Nghĩa Hằng Số:**
   ```java
   public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
   public static final String TRACE_ID_MDC_KEY = "traceId";
   public static final String CORRELATION_ID_MDC_KEY = "correlationId";
   ```
4. **Thuật Toán Xử Lý Chi Tiết trong `doFilterInternal(...)`:**
   - **Bước 1 (Trích xuất & Kiểm tra):** Đọc header `X-Correlation-Id` từ `HttpServletRequest`.
     - Nếu header có giá trị hợp lệ (không null, không rỗng sau khi trim): Sử dụng giá trị đã được làm sạch (`correlationId.trim()`).
     - Nếu header thiếu hoặc chỉ chứa khoảng trắng (`isBlank()`): Tự động sinh mã UUID ngẫu nhiên mới: `UUID.randomUUID().toString()`.
   - **Bước 2 (Nạp vào Ngữ Cảnh MDC):** Đưa định danh vào SLF4J MDC:
     - `MDC.put(TRACE_ID_MDC_KEY, correlationId);`
     - `MDC.put(CORRELATION_ID_MDC_KEY, correlationId);`
   - **Bước 3 (Gán Response Header):** Gắn header phản hồi:
     - `response.setHeader(CORRELATION_ID_HEADER, correlationId);`
   - **Bước 4 (Tiếp tục chuỗi):** Thực thi tiếp bộ lọc:
     - `filterChain.doFilter(request, response);`
   - **Bước 5 (Dọn dẹp Bắt buộc trong `finally`):**
     - Thực thi `MDC.remove(TRACE_ID_MDC_KEY);` và `MDC.remove(CORRELATION_ID_MDC_KEY);` (hoặc `MDC.clear()`) để đảm bảo không rò rỉ dữ liệu `ThreadLocal`.
5. **Oracle Javadoc:** Bổ sung Javadoc chi tiết cấp độ Class và Method với các tag `@apiNote`, `@implSpec`, `@implNote`, `@param`, `@throws`.

### Giai Đoạn 2: Xây Dựng Bộ Kiểm Thử Tự Động Toàn Diện `CorrelationIdFilterTest.java`
Tạo mới file kiểm thử tại `src/test/java/com/gpc/oms/config/CorrelationIdFilterTest.java`:
1. **Công nghệ:** JUnit 5 (`@DisplayName`, `@Nested`, `@Test`), AssertJ, Spring Mock Web (`MockHttpServletRequest`, `MockHttpServletResponse`, `MockFilterChain`).
2. **Kịch Bản 1 (Client không truyền header):**
   - Request không có header `X-Correlation-Id`.
   - Thực thi filter.
   - Xác minh: Header response `X-Correlation-Id` được sinh tự động, không rỗng và có định dạng UUID hợp lệ (36 ký tự).
   - Trong quá trình filter chain chạy: Xác minh `MDC.get("traceId")` và `MDC.get("correlationId")` chứa đúng UUID này.
   - Sau khi kết thúc filter: Xác minh `MDC` đã được dọn sạch (`MDC.get(...) == null`).
3. **Kịch Bản 2 (Client truyền header hợp lệ):**
   - Request truyền `X-Correlation-Id: "req-trace-uuid-999"`.
   - Xác minh: Response header trả về đúng `"req-trace-uuid-999"`.
   - Trong quá trình filter chain chạy: `MDC` nhận đúng `"req-trace-uuid-999"`.
   - Sau khi kết thúc: `MDC` được dọn sạch về `null`.
4. **Kịch Bản 3 (Client truyền header chỉ chứa khoảng trắng):**
   - Request truyền `X-Correlation-Id: "   "`.
   - Xác minh: Nhánh `isBlank()` kích hoạt, filter tự động sinh UUID mới thay vì sử dụng chuỗi trắng.
5. **Kịch Bản 4 (Xử lý khi FilterChain ném ngoại lệ):**
   - Giả lập `filterChain.doFilter(...)` ném ra ngoại lệ (`ServletException` hoặc `RuntimeException`).
   - Xác minh: Khối `finally` vẫn được kích hoạt và dọn sạch `MDC` hoàn toàn, không để lại dữ liệu rác trong `ThreadLocal`.

### Giai Đoạn 3: Kiểm Thử Tích Hợp End-to-End với MockMvc
Bổ sung kiểm thử vào `src/test/java/com/gpc/oms/WorkOrderIntegrationTest.java` (hoặc test slice phù hợp):
- Gọi `GET /api/v1/workorders` hoặc `POST /api/v1/workorders`.
- Xác minh `header().exists("X-Correlation-Id")` xuất hiện trong kết quả trả về.

### Giai Đoạn 4: Kiểm Toán Chất Lượng & JaCoCo Quality Gate
1. Chạy lệnh: `mvn clean verify`.
2. Xác nhận 100% test cases (cũ và mới) đều PASS Green.
3. Xác nhận JaCoCo Quality Gate giữ vững 100% Line & Branch Coverage trên toàn bộ các gói nghiệp vụ.

---

# CONSTRAINTS:
1. **Chuẩn Oracle Java 17 & Jakarta EE:**
   - Sử dụng `jakarta.servlet.*` (không dùng `javax.servlet.*`).
   - Tuân thủ Javadoc JEP 106, không sử dụng Lombok.
2. **Bảo Vệ Ngữ Cảnh Luồng (Thread-Safety & Hygiene):**
   - Bắt buộc giải phóng MDC trong khối `finally` không điều kiện.
3. **Không Phá Vỡ Hệ Thống (Zero Regressions):**
   - Bộ lọc không được làm gián đoạn bất kỳ endpoint RESTful hay bộ lọc an ninh nào sẵn có.
4. **Green Quality Gate:**
   - `mvn clean verify` đạt 100% (0 errors, 0 failures, 100% JaCoCo coverage).

---

# DONE WHEN:
1. File `CorrelationIdFilter.java` được cài đặt hoàn chỉnh với Javadoc chuẩn Oracle JEP 106.
2. File `CorrelationIdFilterTest.java` kiểm thử đầy đủ 100% các nhánh rẽ và kịch bản ngoại lệ.
3. Mọi response HTTP trả về từ ứng dụng đều chứa header `X-Correlation-Id`.
4. Trong suốt vòng đời xử lý request, cả hai khóa `traceId` và `correlationId` đều sẵn sàng trong `MDC` phục vụ SLF4J / Logback ECS logging.
5. Lệnh `mvn clean verify` thực thi thành công 100% Green (0 failure, 0 error, JaCoCo PASS 100%).
```
