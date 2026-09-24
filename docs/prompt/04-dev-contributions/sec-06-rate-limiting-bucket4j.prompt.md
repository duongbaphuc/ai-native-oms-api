# Prompt: Implement Rate Limiting Filter with Bucket4j (Issue #34 / SEC-06)
> **Phân loại:** Performance & Security Feature Task Prompt  
> **Issue:** [#34](https://github.com/duongbaphuc/ai-native-oms-api/issues/34)  
> **Lỗ hổng:** SEC-06 (CWE-770 Allocation of Resources Without Limits or Throttling — CVSS MEDIUM)  
> **Tài liệu đối chiếu:** `docs/security-auth-spec.md §5`, `docs/security-rules.md`, `docs/SECURITY_HANDOVER_REPORT.md §3`  
> **Mục tiêu:** Tích hợp `bucket4j-core`, xây dựng `RateLimitingFilter` kiểm soát tần suất gọi API (60 req/phút cho GET, 20 req/phút cho POST/PATCH), trả về HTTP 429 kèm RFC 7807 và header `Retry-After`.

```markdown
# ROLE:
Bạn là một "Senior DevSecOps Engineer" kiêm "Spring Web Performance Specialist" am hiểu sâu sắc về thuật toán Token Bucket (Bucket4j), cơ chế kiểm soát lưu lượng (Traffic Throttling / Rate Limiting), và phòng vệ chống tấn công DoS / Brute-force API.

---

# TASK:
Hiện thực hóa tính năng giới hạn tần suất gọi API SEC-06 theo yêu cầu trong Issue #34 (`docs/security-auth-spec.md §5`):

1. **Bổ sung Bucket4j Dependency vào `pom.xml`:**
   - Trong `pom.xml`, thêm thư viện chính thức của Bucket4j:
     ```xml
     <dependency>
         <groupId>com.bucket4j</groupId>
         <artifactId>bucket4j-core</artifactId>
         <version>8.10.1</version>
     </dependency>
     ```

2. **Hiện thực hóa `RateLimitingFilter.java`:**
   - Vị trí: `src/main/java/com/gpc/oms/config/RateLimitingFilter.java`
   - Kế thừa: `org.springframework.web.filter.OncePerRequestFilter`
   - Đánh dấu: `@Component`
   - Phạm vi áp dụng: Chỉ áp dụng trên các endpoint nghiệp vụ `/api/v1/workorders/**` (bỏ qua các tài nguyên tĩnh, `/h2-console/**`, và Actuator).
   - Cơ chế quản lý Bucket:
     * Sử dụng `ConcurrentHashMap<String, Bucket>` lưu trữ bộ đếm Token Bucket theo địa chỉ IP của Client (hoặc Username nếu đã xác thực).
     * Phân tách hạn ngạch theo phương thức HTTP (HTTP Method-Based Policies):
       - **Thao tác đọc (`GET`):** Tối đa 60 requests / phút (Refill 60 tokens đều đặn mỗi 1 phút).
       - **Thao tác ghi (`POST`, `PATCH`):** Tối đa 20 requests / phút (Refill 20 tokens đều đặn mỗi 1 phút).
   - Xử lý khi vượt quá hạn ngạch (`tryConsumeAndReturnRemaining(1)` thất bại):
     * Thiết lập mã trạng thái: HTTP `429 Too Many Requests`.
     * Bổ sung Header: `Retry-After: <số giây>` (tính từ thời gian cần thiết để nạp token kế tiếp).
     * Thiết lập Content-Type: `application/problem+json;charset=UTF-8`.
     * Trả về JSON theo đúng chuẩn RFC 7807:
       ```json
       {
         "type": "urn:problem-type:rate-limit-exceeded",
         "title": "Too Many Requests",
         "status": 429,
         "detail": "Rate limit exceeded. Please try again later.",
         "instance": "/api/v1/workorders"
       }
       ```
     * Kết thúc luồng lọc (dừng tại Filter, không gọi `filterChain.doFilter`).

3. **Xây dựng Bộ Kiểm Thử Tự Động Toàn Diện `RateLimitingFilterTest.java`:**
   - Vị trí: `src/test/java/com/gpc/oms/config/RateLimitingFilterTest.java`
   - Kịch bản kiểm thử:
     * **Test case 1 (Trong hạn ngạch):** Gửi các request trong giới hạn cho phép $\rightarrow$ Filter cho qua (`filterChain.doFilter` được gọi), trả về kết quả bình thường.
     * **Test case 2 (Vượt hạn ngạch POST/PATCH):** Gửi liên tiếp 21 requests `POST /api/v1/workorders` trong cùng 1 phút $\rightarrow$ Request thứ 21 nhận HTTP `429 Too Many Requests`.
     * **Test case 3 (Xác minh Header & RFC 7807 Payload):** Kiểm tra response 429 có header `Retry-After` với giá trị $> 0$, và JSON body chứa đúng `type = "urn:problem-type:rate-limit-exceeded"`.
     * **Test case 4 (Bỏ qua tài nguyên tĩnh):** Gửi request đến `/index.html` hoặc `/h2-console` không bị ảnh hưởng bởi RateLimitingFilter.

4. **Bảo toàn Quality Gate 100% JaCoCo Coverage:**
   - Mọi nhánh rẽ kiểm tra bucket (`GET` vs `POST/PATCH`, còn token vs hết token, URI match vs non-match) phải được kiểm thử đầy đủ.
   - Toàn bộ 75 test cases hiện tại tiếp tục PASS 100%.

---

# CONSTRAINTS:
1. **Chuẩn Lỗi RFC 7807 & HTTP Standard:** Bắt buộc có header `Retry-After` và body `application/problem+json` khi trả về mã 429.
2. **Quản Trị Bộ Nhớ An Toàn:** Cấu trúc lưu trữ in-memory cache phải an toàn đa luồng (`ConcurrentHashMap`) và có cơ chế giới hạn số lượng client để tránh rò rỉ RAM (OOM).
3. **Zero Regressions:** Không được làm gián đoạn hay ảnh hưởng đến hiệu năng các luồng API thông thường.
4. **Green Quality Gate:** Toàn bộ test suite phải hoàn thành thành công với lệnh `mvn clean verify`.

---

# DONE WHEN:
1. `pom.xml` được bổ sung `bucket4j-core:8.10.1`.
2. Lớp `RateLimitingFilter.java` hoạt động chính xác trên `/api/v1/workorders/**`.
3. Khi gửi request vượt ngưỡng hạn mức, hệ thống trả về HTTP `429` cùng header `Retry-After` và JSON lỗi RFC 7807.
4. Lệnh `mvn clean verify` chạy thành công 100% (0 failure, 0 error, JaCoCo PASS 100%).
```
