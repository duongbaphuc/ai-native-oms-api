# Prompt Giai Đoạn 2: Thiết Kế Kiến Trúc Kỹ Thuật & Hợp Đồng API (API Contracts)

```markdown
# ROLE:
Bạn là một "Principal API Architect" và "Enterprise Security Specialist" am hiểu sâu sắc về RESTful API Design, RFC 7807 (Problem Details for HTTP APIs), Spring Security 6.x, và Bean Validation chuẩn mực.

---

# TASK:
Dựa trên tài liệu miền tại `docs/01-domain-model.md`, hãy thiết kế toàn diện hợp đồng giao tiếp API và kiến trúc bảo mật trong 3 tài liệu:
1. `docs/02-api-spec.md`: Đặc tả chi tiết 4 endpoint:
   - `POST /api/v1/workorders`: Tạo phiếu sự cố (HTTP 201 + Location header).
   - `GET /api/v1/workorders`: Tra cứu danh sách phân trang và lọc theo trạng thái (`status`).
   - `GET /api/v1/workorders/{id}`: Xem chi tiết phiếu theo UUID.
   - `PATCH /api/v1/workorders/{id}/status`: Cập nhật trạng thái phiếu.
2. `docs/02-security-auth-spec.md`: Đặc tả cơ chế xác thực Stateless (HTTP Basic / JWT), ma trận phân quyền RBAC cho 3 vai trò: `ROLE_DISPATCHER`, `ROLE_TECHNICIAN`, `ROLE_ADMIN`.
3. `docs/00-api-rules.md`: Quy chuẩn trả lời lỗi chuẩn hóa theo RFC 7807 (`application/problem+json`) với định danh URN: `urn:problem-type:validation-error`, `urn:problem-type:malformed-json`, `urn:problem-type:unauthorized`, `urn:problem-type:forbidden`, `urn:problem-type:not-found`, `urn:problem-type:invalid-state-transition`, `urn:problem-type:internal-error`.

---

# CONSTRAINTS:
1. **RESTful Plural Naming:** URI bắt buộc sử dụng danh từ số nhiều và tiền tố phiên bản: `/api/v1/workorders`.
2. **Strict RFC 7807:** Mọi response lỗi 4xx/5xx bắt buộc chứa đủ 4 trường cốt lõi: `status` (int), `type` (URI dạng `urn:problem-type:*`), `title` (string ngắn gọn), và `detail` (chi tiết lỗi) hoặc `invalidParams` (danh sách lỗi validation dạng mảng object).
3. **RBAC Boundary:**
   - Tạo phiếu (`POST`): Cho phép `DISPATCHER`, `TECHNICIAN`, `ADMIN`.
   - Xem danh sách / chi tiết (`GET`): Cho phép `DISPATCHER`, `TECHNICIAN`, `ADMIN`.
   - Cập nhật trạng thái (`PATCH`): CHỈ cho phép `TECHNICIAN`, `ADMIN`. Nghiêm cấm `DISPATCHER` cập nhật trạng thái (phải trả về 403 Forbidden).
4. **Pagination Invariants:** Tham số phân trang mặc định `page=0, size=20`. Cấu trúc JSON trả về bắt buộc gồm: `content`, `pageNumber`, `pageSize`, `totalElements`, `totalPages`, `isFirst`, `isLast`.

---

# DONE WHEN:
1. Tạo đầy đủ 3 tài liệu: `docs/02-api-spec.md`, `docs/02-security-auth-spec.md`, `docs/00-api-rules.md`.
2. Mỗi endpoint đều có: Bảng Schema Request Body, Bảng Schema Response Body, Thuật toán xử lý từng bước (Step-by-step Logic), và Ma trận xử lý ngoại lệ (Edge Cases Table).
3. Bảng phân quyền RBAC ánh xạ rõ ràng phương thức HTTP, URI, Role được phép và cú pháp `@PreAuthorize`.
```
