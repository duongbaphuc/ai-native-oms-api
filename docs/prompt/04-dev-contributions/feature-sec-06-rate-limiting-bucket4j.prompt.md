# Prompt: Triển Khai SEC-06 - Bộ Lọc Giới Hạn Tần Suất Gọi API (Rate Limiting) với Bucket4j Đạt Chuẩn Oracle Java (Issue #34)

> **Mã tính năng / Issue:** `[FEATURE] SEC-06: Triển khai bộ lọc giới hạn tần suất gọi API (Rate Limiting) với Bucket4j` ([#34](https://github.com/duongbaphuc/ai-native-oms-api/issues/34))  
> **Phân loại:** Security Hardening, DoS Mitigation & Performance Engineering Task Prompt  
> **Nền tảng công nghệ:** OpenJDK / Oracle Java 17 LTS | Spring Boot 3.3.4 | Spring Security 6.x | Bucket4j 8.10.1  
> **Quy chuẩn đối chiếu:**  
> - `docs/SECURITY_HANDOVER_REPORT.md §3` (Lỗ hổng SEC-06, CWE-770 Allocation of Resources Without Limits or Throttling)  
> - `docs/security-auth-spec.md §4 & §5` (Chính sách Rate Limiting & Chuẩn lỗi RFC 7807)  
> - `docs/security-rules.md` (Quy chuẩn An ninh Tầng Biên giới Mạng)  
> - `docs/ORACLE_JAVA_DOCUMENTATION.md` & `docs/internal-coding-standards.md` (Oracle Java Coding Conventions, JEP 106 Javadoc Tags)

---

```markdown
# ROLE:
Bạn là một "Oracle Certified Master Java Enterprise Architect" kiêm "Principal DevSecOps & Application Security Specialist". Bạn sở hữu chuyên môn sâu sắc về chuẩn lập trình Oracle Java 17 LTS, đặc tả Javadoc (JEP 106), cấu trúc an toàn bộ nhớ và đồng thời (Thread-Safety & Non-blocking Concurrency), thuật toán Token Bucket (Bucket4j), cùng cơ chế kiểm soát lưu lượng biên giới mạng phòng chống tấn công DoS, Brute-force và API Flooding (CWE-770).

---

# CONTEXT:
Theo Hồ sơ Bàn giao An ninh (`docs/SECURITY_HANDOVER_REPORT.md §3`, mã phát hiện SEC-06 / CWE-770) và Tài liệu Đặc tả An ninh (`docs/security-auth-spec.md §4 & §5`), microservice Outage Work Order API (`oms-api-demo`) hiện chưa có cơ chế kiểm soát tốc độ gọi API (Rate Limiting / Throttling). Điều này dẫn đến nguy cơ hệ thống bị tê liệt hoặc suy giảm tài nguyên khi gặp các cuộc tấn công DoS hoặc quét tự động.

Các quy chuẩn và ràng buộc cốt lõi đạt chuẩn Oracle Java của dự án:
1. **Oracle Coding Standards & Zero-Lombok:** 100% mã nguồn Java tường minh (Explicit Code), cấm tuyệt đối Lombok, bắt buộc tiêm phụ thuộc qua Constructor (Constructor Injection), tận dụng Java 17 Records bất biến.
2. **Oracle Javadoc JEP 106 Specification:** Mọi lớp, interface, hằng số và phương thức công khai phải được tài liệu hóa bằng Javadoc tiêu chuẩn với các tag chuyên biệt: `@apiNote` (hướng dẫn sử dụng API), `@implSpec` (ràng buộc cài đặt kỹ thuật), `@implNote` (ghi chú hiệu năng/bộ nhớ), `@param`, `@return`, `@throws`, `@see`.
3. **Mô Hình Đồng Thời & An Toàn Bộ Nhớ (Thread-Safety & Memory Bounds):**
   - Bộ lưu trữ Token Bucket in-memory phải tuyệt đối an toàn đa luồng (`ConcurrentHashMap`).
   - Có cơ chế chặn rò rỉ bộ nhớ (OOM / CWE-400) thông qua việc giới hạn dung lượng cache hoặc cơ chế dọn dẹp bucket quá hạn.
4. **Chuẩn Lỗi RFC 7807 & HTTP Standard:**
   - Khi vượt quá hạn mức gọi, hệ thống bắt buộc phản hồi HTTP Status `429 Too Many Requests` với `Content-Type: application/problem+json;charset=UTF-8`.
   - Bắt buộc trả về HTTP Header `Retry-After: <số giây>` chỉ định thời gian client cần chờ trước khi thử lại.
5. **Bảo Toàn Chất Lượng JaCoCo Quality Gate:** Toàn bộ test suite phải đạt 100% Green, không làm suy giảm ngưỡng 100% Line & Branch Coverage.

---

# TASK:
Hiện thực hóa trọn vẹn giải pháp Rate Limiting SEC-06 theo chuẩn Oracle Java qua 5 giai đoạn chi tiết sau:

### Giai Đoạn 1: Bổ Sung Dependency Bucket4j vào `pom.xml`
Bổ sung thư viện chính thức của Bucket4j vào `pom.xml`:
```xml
<!-- Bucket4j Token Bucket Rate Limiting (SEC-06, CWE-770) -->
<dependency>
    <groupId>com.bucket4j</groupId>
    <artifactId>bucket4j-core</artifactId>
    <version>8.10.1</version>
</dependency>
```

### Giai Đoạn 2: Hiện Thực Hóa Bộ Lọc `RateLimitingFilter.java` Đạt Chuẩn Oracle
Tạo lớp bộ lọc tại `src/main/java/com/gpc/oms/config/RateLimitingFilter.java`:
1. **Kế thừa & Nhận diện Component:** Kế thừa `org.springframework.web.filter.OncePerRequestFilter`, đánh dấu `@Component`.
2. **Oracle Javadoc:** Bổ sung JavaDoc chi tiết ở cấp độ Class và Method với các tag `@apiNote`, `@implSpec`, `@implNote`, `@see`.
3. **Đặc Tả Chính Sách Phân Tách Lưu Lượng (Rate Limit Policies):**
   - **Thao tác đọc (`GET`):** Hạn mức 60 requests / phút (Refill 60 tokens mỗi 1 phút theo chu kỳ đều đặn `Bandwidth.builder().capacity(60).refillGreedy(60, Duration.ofMinutes(1))` hoặc `refillIntervally`).
   - **Thao tác ghi (`POST`, `PATCH`):** Hạn mức 20 requests / phút (Refill 20 tokens mỗi 1 phút).
4. **Phạm Vi Áp Dụng (URI Scope Resolution):**
   - Chỉ áp dụng lọc trên các endpoint nghiệp vụ: `/api/v1/workorders/**`.
   - Bỏ qua (bypass) toàn bộ các tài nguyên tĩnh (`/`, `/index.html`, `/favicon.ico`), console phát triển (`/h2-console/**`), và các endpoint giám sát vận hành (`/actuator/**`).
5. **Quản Lý Định Danh Khách Hàng (Client Identification):**
   - Trích xuất Client IP qua header `X-Forwarded-For` (lấy địa chỉ IP đầu tiên nếu qua proxy/load balancer) hoặc `request.getRemoteAddr()`.
   - Khóa phân biệt: Kết hợp `<ClientIP>:<HTTP_METHOD_CATEGORY>` (ví dụ: `192.168.1.10:READ` và `192.168.1.10:WRITE`).
6. **Xử Lý Vi Phạm Hạn Mức (Rate Limit Exceeded - HTTP 429):**
   - Khi `bucket.tryConsumeAndReturnRemaining(1)` thất bại (`consumptionProbe.isConsumed() == false`):
     - Tính toán thời gian chờ: `long waitForRefillNanos = consumptionProbe.getNanosToWaitForRefill();`
     - Chuyển đổi thành giây: `long retryAfterSeconds = Math.max(1, TimeUnit.NANOSECONDS.toSeconds(waitForRefillNanos));`
     - Thiết lập HTTP Header: `response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));`
     - Thiết lập Status: `response.setStatus(429);`
     - Thiết lập Content-Type: `response.setContentType("application/problem+json;charset=UTF-8");`
     - Xuất JSON RFC 7807 Problem Details:
       ```json
       {
         "type": "urn:problem-type:rate-limit-exceeded",
         "title": "Too Many Requests",
         "status": 429,
         "detail": "Bạn đã vượt quá giới hạn tần suất gọi API. Vui lòng thử lại sau %d giây.".formatted(retryAfterSeconds),
         "instance": request.getRequestURI()
       }
       ```
     - Kết thúc luồng lọc (dừng tại Filter, không gọi `filterChain.doFilter`).

### Giai Đoạn 3: Tích Hợp Bảo Mật & Thứ Tự Bộ Lọc tại `SecurityConfig.java`
1. Đăng ký `RateLimitingFilter` vào chuỗi `SecurityFilterChain`:
   - Đặt `RateLimitingFilter` trước `UsernamePasswordAuthenticationFilter` hoặc tại vị trí thích hợp nhằm bảo vệ hệ thống trước khi tài nguyên backend bị chiếm dụng:
     `http.addFilterBefore(rateLimitingFilter, org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class);`
2. Bảo đảm tương thích 100% với các endpoint `permitAll()` và RBAC đã cấu hình sẵn.

### Giai Đoạn 4: Xây Dựng Bộ Kiểm Thử Tự Động Toàn Diện `RateLimitingFilterTest.java`
Tạo tệp kiểm thử tại `src/test/java/com/gpc/oms/config/RateLimitingFilterTest.java`:
1. **Kiểm thử Luồng Thao Tác Đọc (`GET`):**
   - Gửi 60 requests `GET /api/v1/workorders` hợp lệ $\rightarrow$ Toàn bộ nhận HTTP 200 OK.
   - Request thứ 61 $\rightarrow$ Nhận HTTP 429 Too Many Requests, header `Retry-After` tồn tại với giá trị $> 0$, body đúng chuẩn RFC 7807.
2. **Kiểm thử Luồng Thao Tác Ghi (`POST`, `PATCH`):**
   - Gửi 20 requests `POST /api/v1/workorders` hợp lệ $\rightarrow$ Toàn bộ được cho qua (HTTP 201 Created).
   - Request thứ 21 $\rightarrow$ Bị chặn ngay lập tức với HTTP 429 Too Many Requests và header `Retry-After`.
3. **Kiểm thử Cách Ly Client (Multi-tenant / Multi-IP Isolation):**
   - Client A (IP: `10.0.0.1`) bị cạn token $\rightarrow$ Client B (IP: `10.0.0.2`) vẫn gửi request thành công bình thường.
4. **Kiểm thử Bỏ Qua Endpoint Ngoại Lệ (Bypass Scenarios):**
   - Các request tới `/actuator/health`, `/h2-console/**`, `/index.html` không bị giới hạn bởi Token Bucket.
5. **Kiểm thử Phục Hồi Token (Token Refill Recovery):**
   - Xác minh sau khi bucket được refill, request mới lại được chấp thuận bình thường.

### Giai Đoạn 5: Kiểm Toán Mã Nguồn, Javadoc & JaCoCo Quality Gate
1. Chạy công cụ kiểm tra Javadoc của Maven: `mvn javadoc:javadoc` để bảo đảm không có warning/lỗi cú pháp Javadoc.
2. Thực thi toàn bộ test suite và nghiệm thu JaCoCo: `mvn clean verify`.
3. Đảm bảo toàn bộ 78 test cases hiện có và các test case mới đều PASS 100% Green.

---

# CONSTRAINTS:
1. **Chuẩn Oracle Java 17 LTS:**
   - Không sử dụng Lombok.
   - Sử dụng immutable constructs, explicit types, static constants cho header và error types.
   - Viết Javadoc đầy đủ với các tag JEP 106 (`@apiNote`, `@implSpec`, `@implNote`).
2. **Phòng Vệ Tràn Bộ Nhớ (Memory Leak Prevention):**
   - Cấu trúc lưu trữ in-memory token bucket không được phép phình to vô hạn khi gặp hàng triệu IP ngẫu nhiên. Cần có cơ chế kiểm soát số lượng bucket hoặc giới hạn kích thước map an toàn.
3. **Định Dạng Lỗi Bắt Buộc RFC 7807:**
   - Trường `type` bắt buộc là `urn:problem-type:rate-limit-exceeded`.
   - Header `Retry-After` bắt buộc có mặt trong mọi phản hồi 429.
4. **Không Suy Giảm Độ Phủ JaCoCo:**
   - Duy trì tỷ lệ bao phủ Line Coverage và Branch Coverage đạt 100% trên toàn bộ các package được giám sát.

---

# DONE WHEN:
1. `pom.xml` tích hợp thành công dependency `bucket4j-core:8.10.1`.
2. Lớp `RateLimitingFilter.java` được cài đặt hoàn chỉnh với JavaDoc chuẩn Oracle JEP 106.
3. Requests `GET` bị chặn tại request thứ 61 trong 1 phút; requests `POST`/`PATCH` bị chặn tại request thứ 21 trong 1 phút với cùng Client IP.
4. Mọi phản hồi bị chặn đều trả về HTTP 429, header `Retry-After: <giây>`, và JSON body chuẩn RFC 7807.
5. Bộ kiểm thử `RateLimitingFilterTest.java` bao phủ 100% các nhánh rẽ và kịch bản nghiệp vụ.
6. Lệnh `mvn clean verify` chạy thành công 100% Green (0 failure, 0 error, 100% JaCoCo Line & Branch Coverage).
```
