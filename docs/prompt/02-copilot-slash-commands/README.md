# GitHub Copilot Slash Command Prompts

Thư mục này đóng vai trò mục lục và hướng dẫn tra cứu cho các tệp prompt tái sử dụng trực tiếp trong **GitHub Copilot Chat** (Slash Commands).

> [!NOTE]
> Các tệp thực thi chính của GitHub Copilot được đặt tại `.github/prompts/` theo quy định của GitHub Copilot Workspace.

---

## Danh Mục Lệnh (Slash Commands Inventory)

| Tên Lệnh Copilot | Tệp Cấu Hình Thực Thi | Mục Tiêu Tác Vụ | Cách Kích Hoạt Trong IDE |
|---|---|---|---|
| `/implement-endpoint` | [`.github/prompts/implement-endpoint.prompt.md`](../../../.github/prompts/implement-endpoint.prompt.md) | Sinh code 3 tầng (Controller, Service, Repository, DTOs, Tests) cho một REST endpoint mới tuân thủ Spec-Driven | Nhập `/implement-endpoint` trong khung chat Copilot |
| `/review-code` | [`.github/prompts/review-code.prompt.md`](../../../.github/prompts/review-code.prompt.md) | Kiểm toán mã nguồn hoặc diff của Pull Request dựa trên 12 tiêu chí Scorecard an ninh & chất lượng | Nhập `/review-code` hoặc chọn đoạn mã cần review |

---

## Hướng Dẫn Sử Dụng Trong IDE
1. Mở GitHub Copilot Chat trong Visual Studio Code hoặc Antigravity IDE.
2. Gõ `/` để xem danh sách các lệnh khả dụng.
3. Chọn `/implement-endpoint` hoặc `/review-code` và kèm theo ngữ cảnh file bạn muốn thao tác (ví dụ: `#file:docs/02-api-spec.md`).
