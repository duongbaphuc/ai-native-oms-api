# Quy tắc Mã hóa (Java Coding Rules)

Quy tắc này cung cấp ngữ cảnh kỹ thuật (context) trực tiếp cho Copilot nhằm duy trì sự đồng nhất trong toàn bộ repo[cite: 5].

## 1. Định chuẩn Kỹ thuật (Technical Standards)
- **Nền tảng:** Sử dụng Java 17+ và Spring Boot 3.3+[cite: 8].
- **Dependency Injection:** Luôn sử dụng tiêm phụ thuộc qua hàm khởi tạo (Constructor Injection)[cite: 8]. KHÔNG sử dụng `@Autowired` trên các trường.

## 2. Tiêu chuẩn Bộ nhớ và Vòng lặp
- **Khai báo biến (Scope Management):** Ưu tiên biến hiệu quả `final` / khai báo ngoài vòng lặp khi tái sử dụng qua nhiều iteration để dễ đọc và tránh cấp phát thừa[cite: 8]. Không áp blanket ban tuyệt đối — mọi cấm cần benchmark + readability check trước khi thành luật.
- **Naming:** Class `PascalCase`, method/var `camelCase`, constant `UPPER_SNAKE`. DTO suffix `Request/Response`.
- **Exception style:** Không trả stack trace ra client. Throw `ResponseStatusException` hoặc `@ControllerAdvice` map sang RFC 7807 problem+json.
- **String building:** Dùng `+` cho nối đơn giản, `StringBuilder` cho loop lớn. Mọi claim O(n²) phải benchmark old vs new trên input thực tế trước khi refactor.
- **Validation location:** Validate ở boundary (Controller `@Valid`) + invariant ở domain/entity, không validate rải rác ở service utils.
- **No hallucinated deps:** Chỉ dùng dependency có trong `pom.xml`. Thêm lib mới phải update `pom.xml` + README lý do.
- **AI-output policy:** Mọi code AI sinh là UNTRUSTED, phải qua human review + `mvn test` green.
- **Secrets ban:** Không hardcode secret/token/password. Chỉ qua env vars / Secret Manager.

## 3. Nhật ký Hệ thống (Logging)
- Bắt buộc sử dụng SLF4J với cú pháp: `private static final Logger log = LoggerFactory.getLogger(ClassName.class);`[cite: 8].

> [!WARNING]
> **Chính sách Dữ liệu Nhạy cảm:** KHÔNG log các dữ liệu định danh cá nhân (PII) như email, số điện thoại, mật khẩu, hoặc toàn bộ payload của hệ thống[cite: 3, 8].
