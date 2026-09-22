## 1. Phân loại Thay đổi (Type of Change)
- [ ] `feat`: Tính năng mới (New feature)
- [ ] `fix`: Sửa lỗi (Bug fix)
- [ ] `refactor`: Tái cấu trúc mã nguồn (Code refactoring)
- [ ] `docs`: Cập nhật tài liệu (Documentation)
- [ ] `test`: Bổ sung kiểm thử (Tests)

## 2. Liên kết Issue / Ticket
Liên kết Issue: `WO-<issue-id>` (Ví dụ: `closes #12` hoặc `WO-10432`)
Nhánh thực hiện: `feature/WO-<issue-id>`

## 3. Tóm tắt Thay đổi (Summary of Changes)
<!-- Mô tả thay đổi logic duy nhất - Nguyên tử hóa (Atomic PR) -->

## 4. Danh mục Kiểm tra Chính sách AI (AI-Generated Code Policy Checklist)
> [!WARNING]
> Toàn bộ mã nguồn hoặc gợi ý từ GitHub Copilot được phân loại là **Không đáng tin cậy (UNTRUSTED)** cho đến khi được kỹ sư xác minh.

- [ ] **Kiểm soát Ảo giác (Hallucination Control):** Đã kiểm tra không có package, thư viện hoặc API ảo/không tồn tại.
- [ ] **Bảo mật Prompt & Secrets:** Không đưa credentials, API key, mật khẩu, hoặc dữ liệu nhạy cảm (PII) vào code/prompt.
- [ ] **Tuân thủ Coding Rules:** Tuân thủ `docs/coding-rules.md` (Java 17+, Constructor Injection, biến tạm ngoài vòng lặp, log SLF4J không chứa PII).
- [ ] **Tuân thủ API Rules:** Tuân thủ `docs/api-rules.md` (REST resource số nhiều, RFC 7807 Problem Details, strict schema, `@Valid`).
- [ ] **Tuân thủ Security Rules:** Tuân thủ `docs/security-rules.md` (JPA/Parameterized queries chống SQL injection, RBAC `@PreAuthorize`).
- [ ] **Phê duyệt con người (Human Approval):** Đã được ít nhất 01 human reviewer kiểm tra và phê duyệt.
