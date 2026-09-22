# Hướng dẫn Đóng góp (Contributing Guidelines)

Để duy trì chất lượng mã nguồn và vận hành an toàn trong môi trường AI-Native, mọi kỹ sư cần tuân thủ các quy định dưới đây.

## 1. Quy trình Pull Request (PR Process)

1. **Phân nhánh (Branching):** Không đẩy mã nguồn trực tiếp lên nhánh `main`[cite: 1, 10]. Luôn sử dụng nhánh tính năng `feature/WO-<issue-id>`.
2. **Nguyên tử hóa (Atomic PRs):** Mỗi PR chỉ chứa một thay đổi logic duy nhất để tránh việc Copilot sinh mã thừa[cite: 1].
3. **Phê duyệt (Approval):** Con người là chốt chặn cuối cùng. Yêu cầu ít nhất 01 human reviewer phê duyệt[cite: 1, 10].

## 2. Chính sách Sinh mã AI (AI-Generated Code Policy)

> [!WARNING]
> Toàn bộ mã nguồn hoặc gợi ý từ GitHub Copilot được phân loại là **Không đáng tin cậy (UNTRUSTED)** cho đến khi được kỹ sư xác minh[cite: 10].

- **Kiểm soát Ảo giác (Hallucination Control):** Kỹ sư review phải kiểm tra xác nhận Copilot không sử dụng các packages, thư viện, hoặc API không tồn tại[cite: 1, 10].
- **Bảo mật Prompt:** Tuyệt đối không đưa credentials, khóa bí mật, hoặc dữ liệu nhạy cảm của khách hàng vào prompt của Copilot[cite: 1, 10].

## 3. Liên kết Tài liệu (References)
- [Quy tắc Lập trình (Coding Rules)](docs/coding-rules.md)
- [Quy tắc API (API Rules)](docs/api-rules.md)
- [Quy tắc Bảo mật (Security Rules)](docs/security-rules.md)
