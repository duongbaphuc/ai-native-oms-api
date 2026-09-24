<!--
Role: Principal Security Architect
Task: Application Security Guardrails, RBAC Enforcement, PII Hygiene, and OWASP Defense
Context files: docs/00-coding-rules.md, docs/00-api-rules.md, docs/02-security-auth-spec.md
Constraints: Zero-trust, parameterized queries only, no hardcoded secrets, RBAC per endpoint
Target Files:
- src/main/java/com/gpc/oms/config/SecurityConfig.java
- src/main/java/com/gpc/oms/controller/WorkOrderController.java
-->
# Quy chuẩn Bảo mật (Security Rules)

Các nguyên tắc này thiết lập ranh giới phòng thủ (Guardrails) chặn đứng mọi mã nguồn nguy hiểm do con người hoặc AI tạo ra.

---

## 1. Quản lý Secret & Credentials

1. **Không Hardcode:** Tuyệt đối không lưu trữ khóa bí mật (secrets), mật khẩu, private key hoặc token cấu hình dưới dạng chuỗi ký tự cứng trong mã nguồn hoặc file markdown.
2. **External Configuration:** Toàn bộ thông tin nhạy cảm bắt buộc nạp qua biến môi trường (Environment Variables) hoặc Secret Manager.

---

## 2. Chống SQL Injection & Phòng Vệ Dữ Liệu

1. **Bắt buộc Truy vấn Tham số hóa:** 100% thao tác truy vấn CSDL phải thông qua Spring Data JPA hoặc Parameterized Queries.
2. **CẤM NỐI CHUỖI NATIVE SQL:** Nghiêm cấm tuyệt đối việc sử dụng cộng chuỗi (`+` hoặc `StringBuilder`) để ghép câu lệnh truy vấn SQL.

---

## 3. Kiểm Soát Truy Cập RBAC (Role-Based Access Control)

1. **Khai báo Tường minh:** Không có endpoint nào được "ngầm" công khai. Mọi endpoint đều phải được bảo vệ rõ ràng bằng `@PreAuthorize` hoặc khai báo `permitAll()` trong `SecurityConfig`.
2. **Hệ Thống Roles Chuẩn:**
   - `ROLE_DISPATCHER`: Điều độ viên trung tâm.
   - `ROLE_TECHNICIAN`: Kỹ thuật viên hiện trường.
   - `ROLE_ADMIN`: Quản trị viên hệ thống.
3. **Nguyên tắc Đặc quyền Tối thiểu (Least Privilege):** Mỗi endpoint chỉ cấp quyền cho các role thực sự cần thiết để hoàn thành nghiệp vụ.

---

## 4. Bảo Vệ Dữ Liệu Cá Nhân (PII & Log Hygiene)

1. **Không Log Dữ Liệu Nhạy Cảm:** Nghiêm cấm log email, số điện thoại khách hàng, mật khẩu, JWT token hoặc toàn bộ raw payload vào hệ thống log.
2. **Ẩn Stack Trace:** Không bao giờ để lộ Exception Stack Trace hoặc thông điệp lỗi nội bộ của Database ra ngoài client. Toàn bộ lỗi phải bọc qua `GlobalExceptionHandler` theo chuẩn RFC 7807.

---

## 5. Phòng Vệ Prompt (AI Prompt Hygiene)

1. **Không Dán Dữ Liệu Thật:** Tuyệt đối không copy dữ liệu khách hàng, token thật hoặc mật khẩu vào prompt của GitHub Copilot hoặc bất kỳ mô hình AI nào.
