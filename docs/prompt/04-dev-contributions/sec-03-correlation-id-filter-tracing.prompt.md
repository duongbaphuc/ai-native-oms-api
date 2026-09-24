# Prompt: Implement CorrelationIdFilter for Distributed Tracing and MDC Context (Issue #31 / SEC-03)
> **Phân loại:** Observability & Security Feature Task Prompt  
> **Issue:** [#31](https://github.com/duongbaphuc/ai-native-oms-api/issues/31)  
> **Lỗ hổng:** SEC-03 (CWE-778 Insufficient Logging / Forensic Audit Gap — CVSS MEDIUM)  
> **Tài liệu đối chiếu:** `docs/02-observability-and-logging.md §2`, `docs/09-SECURITY_HANDOVER_REPORT.md §3`, `docs/08-SYSTEM_HANDOVER.md §2.1`  
> **Mục tiêu:** Hiện thực hóa `CorrelationIdFilter` kế thừa `OncePerRequestFilter`, gắn mã truy vết liên chuỗi `X-Correlation-Id` vào MDC log context và HTTP Response, hỗ trợ điều tra dấu vết an ninh (Forensic Audit).

```markdown
# ROLE:
Bạn là một "Senior Observability Engineer" kiêm "Spring Web Security Specialist" am hiểu sâu sắc về chuẩn hóa Distributed Tracing, SLF4J MDC (Mapped Diagnostic Context), và kiến trúc Servlet Filter trong Spring Boot 3.3+.

---

# TASK:
Hiện thực hóa tính năng truy vết phân tán SEC-03 bằng Java cho dự án hiện tại theo yêu cầu trong Issue #31 (`docs/02-observability-and-logging.md §2`):

1. **Hiện thực hóa lớp `CorrelationIdFilter.java`:**
   - **Đường dẫn tệp:** `src/main/java/com/gpc/oms/config/CorrelationIdFilter.java`
   - **Package:** `com.gpc.oms.config`
   - **Kế thừa:** `org.springframework.web.filter.OncePerRequestFilter`
   - **Annotations:**
     * `@Component` (để Spring tự động nạp vào Filter Chain)
     * `@Order(Ordered.HIGHEST_PRECEDENCE)` (chạy đầu tiên trong chuỗi filter để gắn trace ID vào MDC trước khi SecurityFilterChain hoặc Controller xử lý)
   - **Hằng số cốt lõi:**
     ```java
     public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
     public static final String MDC_KEY = "correlationId";
     ```
   - **Thuật toán xử lý trong phương thức `doFilterInternal(...)`:**
     1. Lấy giá trị header từ request: `String correlationId = request.getHeader(CORRELATION_ID_HEADER);`
     2. Kiểm tra tính hợp lệ:
        * Nếu `correlationId == null || correlationId.isBlank()`: Sinh UUID ngẫu nhiên mới qua `UUID.randomUUID().toString()`.
        * Ngược lại: Làm sạch chuỗi bằng `correlationId.trim()`.
     3. Đưa vào MDC Context để các logger Logback tự động in ra log:
        `MDC.put(MDC_KEY, correlationId);`
     4. Gắn header vào phản hồi HTTP để client/browser nhận diện được trace ID:
        `response.setHeader(CORRELATION_ID_HEADER, correlationId);`
     5. Tiếp tục chuỗi filter:
        `filterChain.doFilter(request, response);`
     6. **Khối `finally` (Bắt buộc):**
        `MDC.remove(MDC_KEY);` (hoặc `MDC.clear();`) nhằm giải phóng `ThreadLocal`, triệt tiêu hoàn toàn nguy cơ rò rỉ context giữa các request khi tái sử dụng luồng trong Tomcat Thread Pool.

---

2. **Xây dựng Bộ Kiểm Thử Tự Động `CorrelationIdFilterTest.java`:**
   - **Đường dẫn tệp:** `src/test/java/com/gpc/oms/config/CorrelationIdFilterTest.java`
   - **Package:** `com.gpc.oms.config`
   - **Công nghệ kiểm thử:** JUnit 5, AssertJ, Spring Mock Web (`MockHttpServletRequest`, `MockHttpServletResponse`, `MockFilterChain`).
   - **Kịch bản kiểm thử bắt buộc (100% Branch Coverage):**
     * **Test Case 1 — Client KHÔNG truyền header `X-Correlation-Id`:**
       - Gửi request không có header.
       - Thực thi `filter.doFilter(request, response, filterChain)`.
       - Xác minh `response.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER)` không null và có định dạng UUID hợp lệ (36 ký tự).
       - Trong lúc filter chain chạy, xác minh `MDC.get(CorrelationIdFilter.MDC_KEY)` chứa đúng UUID đó.
       - Sau khi kết thúc, xác minh `MDC.get(CorrelationIdFilter.MDC_KEY)` đã được xóa về `null`.
     * **Test Case 2 — Client CÓ truyền header `X-Correlation-Id`:**
       - Gửi request kèm header `X-Correlation-Id: "custom-trace-id-12345"`.
       - Xác minh `response.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER)` nhận đúng `"custom-trace-id-12345"`.
       - Xác minh trong lúc filter chain chạy, `MDC.get(CorrelationIdFilter.MDC_KEY)` nhận đúng `"custom-trace-id-12345"`.
       - Sau khi kết thúc, `MDC` được dọn sạch về `null`.
     * **Test Case 3 — Client truyền header khoảng trắng (`"   "`):**
       - Xác minh nhánh `isBlank()` được kích hoạt, hệ thống tự động sinh UUID mới thay vì dùng chuỗi rỗng.

---

3. **Bảo toàn Tiêu Chuẩn Chất Lượng (JaCoCo Quality Gate):**
   - Đạt **100% Line Coverage & 100% Branch Coverage** cho lớp `CorrelationIdFilter`.
   - Toàn bộ 75 test cases hiện tại của dự án phải tiếp tục PASS 100%.

---

# CONSTRAINTS:
1. **Phòng Chống Rò Rỉ ThreadLocal (CWE-778 Defense):** Lệnh dọn dẹp MDC (`MDC.clear()` hoặc `MDC.remove(...)`) bắt buộc đặt trong khối `finally` của `try-finally`.
2. **Tuân thủ Chuẩn Java 17 & Spring Boot 3.3:** Sử dụng API chuẩn `jakarta.servlet.*` (thay vì `javax.servlet.*`).
3. **Zero Regressions:** Không làm thay đổi hay gián đoạn bất kỳ endpoint hay cấu hình bảo mật hiện hành nào.
4. **Green Quality Gate:** Lệnh `mvn clean verify` phải vượt qua 100% kiểm thử.

---

# DONE WHEN:
1. File `src/main/java/com/gpc/oms/config/CorrelationIdFilter.java` được tạo hoàn chỉnh.
2. File `src/test/java/com/gpc/oms/config/CorrelationIdFilterTest.java` kiểm chứng thành công toàn bộ các kịch bản.
3. Khi gọi bất kỳ API nào, Header `X-Correlation-Id` đều xuất hiện trong HTTP response.
4. Lệnh `mvn clean verify` chạy thành công 100% (0 failure, 0 error, JaCoCo Quality Gate PASS).
```
