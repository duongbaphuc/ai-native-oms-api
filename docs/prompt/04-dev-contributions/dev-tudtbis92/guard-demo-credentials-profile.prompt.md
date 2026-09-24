# Prompt: Guard Demo Credentials Behind Non-Production Profile (Issue #21 / Finding F-02)
> **Tác giả / Contributor:** `tudtbis92`  
> **Pull Request:** [#25](https://github.com/duongbaphuc/ai-native-oms-api/pull/25)  
> **Tài liệu đối chiếu:** `docs/00-security-rules.md`, `docs/10-devops-pipeline-spec.md §4`  

```markdown
# ROLE:
Bạn là một "Application Security Specialist" kiêm "Spring Security Architect".

---

# TASK:
Khắc phục nguy cơ an ninh Finding F-02 trong `docs/archive/audit-logs/review-code-vs-spec-WO-REVIEW-01.md`:
1. Bảo vệ bean `UserDetailsService` (chứa các tài khoản demo InMemory `admin`, `dispatcher`, `technician`) trong `SecurityConfig.java` bằng Spring Profile.
2. Đảm bảo các tài khoản này chỉ được kích hoạt trong môi trường phát triển / kiểm thử cục bộ (`@Profile("!prod")` hoặc `@Profile({"dev", "test", "default"})`).
3. Ngăn chặn tuyệt đối việc bean demo credentials này được nạp vào ứng dụng khi chạy trên môi trường sản xuất (`prod`).

---

# CONSTRAINTS:
1. Không làm hỏng các bài kiểm thử tự động hiện tại (`@SpringBootTest`, `@WebMvcTest`).
2. Môi trường local/dev vẫn chạy bình thường với tài khoản demo mà không cần cấu hình thêm cờ dòng lệnh phức tạp.

---

# DONE WHEN:
1. Chạy với profile `default` / `dev` thì `userDetailsService` được nạp thành công.
2. Chạy với profile `prod` thì bean không được khởi tạo tự động, yêu cầu cấu hình OAuth2 / IdP bên ngoài.
3. Toàn bộ security tests vượt qua `mvn test`.
```
