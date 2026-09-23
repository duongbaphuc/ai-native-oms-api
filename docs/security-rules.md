# Quy chuẩn Bảo mật (Security Rules)

Các nguyên tắc này thiết lập ranh giới phòng thủ (Guardrails) chặn đứng mọi mã nguồn nguy hiểm do con người hoặc AI tạo ra[cite: 1, 5].

## 1. Quản lý Secret
- **Không Hardcode:** Tuyệt đối không lưu trữ khóa bí mật (secrets), mật khẩu, hoặc token cấu hình dưới dạng chuỗi ký tự cứng trong mã nguồn[cite: 5, 8]. 

> [!IMPORTANT]
> Toàn bộ thông tin cấu hình nhạy cảm phải được truyền qua biến môi trường (Environment variables) hoặc Secret Manager.

## 2. Chống SQL Injection
- Bắt buộc sử dụng Spring Data JPA, Hibernate, hoặc Parameterized Queries (Truy vấn tham số hóa)[cite: 5, 8].
- **Cấm Tuyệt Đối:** Sử dụng nối chuỗi (String concatenation) để xây dựng lệnh native SQL[cite: 5, 8].

## 3. Kiểm soát Truy cập (RBAC)
- Xác minh tính toàn vẹn của mọi payload từ client[cite: 8].
- Định nghĩa rõ các role truy cập bằng các annotation bảo mật (ví dụ: `@PreAuthorize("hasRole('TECHNICIAN')")`)[cite: 8].

## 4. PII & Log hygiene
- Không log email/phone/password/toàn bộ payload. Log chỉ id + status + correlation id.
- Lỗi trả client theo RFC 7807, không lộ stack trace / SQL message.

## 5. Prompt hygiene
- Không paste credentials, PII khách hàng, secret vào prompt Copilot. Secret chỉ sống trong env / Secret Manager, exclusion file chỉ là convenience client-side.
