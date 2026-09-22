# Quy tắc Mã hóa (Java Coding Rules)

Quy tắc này cung cấp ngữ cảnh kỹ thuật (context) trực tiếp cho Copilot nhằm duy trì sự đồng nhất trong toàn bộ repo[cite: 5].

## 1. Định chuẩn Kỹ thuật (Technical Standards)
- **Nền tảng:** Sử dụng Java 17+ và Spring Boot 3.3+[cite: 8].
- **Dependency Injection:** Luôn sử dụng tiêm phụ thuộc qua hàm khởi tạo (Constructor Injection)[cite: 8]. KHÔNG sử dụng `@Autowired` trên các trường.

## 2. Tiêu chuẩn Bộ nhớ và Vòng lặp
- **Khai báo biến (Scope Management):** Tuyệt đối KHÔNG khai báo biến bên trong thân vòng lặp (`for`, `while`)[cite: 8]. Mọi biến tạm phục vụ vòng lặp phải được khai báo bên ngoài để tránh chi phí cấp phát lại bộ nhớ và tăng cường khả năng đọc hiểu[cite: 8].

## 3. Nhật ký Hệ thống (Logging)
- Bắt buộc sử dụng SLF4J với cú pháp: `private static final Logger log = LoggerFactory.getLogger(ClassName.class);`[cite: 8].

> [!WARNING]
> **Chính sách Dữ liệu Nhạy cảm:** KHÔNG log các dữ liệu định danh cá nhân (PII) như email, số điện thoại, mật khẩu, hoặc toàn bộ payload của hệ thống[cite: 3, 8].
