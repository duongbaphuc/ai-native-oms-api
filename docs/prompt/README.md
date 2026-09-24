# Trung Tâm Quản Trị Prompt AI (AI Prompt Repository & Directory Guide)

> **Mục tiêu:** Tổ chức, phân loại và chuẩn hóa toàn bộ các câu lệnh Prompt phục vụ phát triển phần mềm theo phương pháp AI-Native SDLC, giúp các kỹ sư (Developers, QA, Architects) và các Agent AI dễ dàng tra cứu, sử dụng và đóng góp prompt mới.

---

## 1. Cấu Trúc Phân Loại Thư Mục Prompt (Directory Hierarchy)

```
docs/prompt/
├── README.md                              # [Tài liệu này] Mục lục điều hướng và quy chuẩn quản trị prompt
├── 01-sdlc-playbook/                      # Chuỗi 8 prompt quy trình AI-Native SDLC chuẩn mực (End-to-End)
│   ├── 00-MASTER-AI-NATIVE-SDLC-PLAYBOOK.md
│   ├── 01-business-analysis-and-domain.prompt.md
│   ├── 02-technical-architecture-and-api-spec.prompt.md
│   ├── 03-ai-context-auditing-and-remediation.prompt.md
│   ├── 04-spec-driven-implementation.prompt.md
│   ├── 05-automated-testing-and-jacoco-coverage.prompt.md
│   ├── 06-interactive-verification-and-console.prompt.md
│   ├── 07-comprehensive-code-review-audit.prompt.md
│   └── 08-system-handover-documentation.prompt.md
├── 02-copilot-slash-commands/             # Catalog tra cứu lệnh Slash Commands tích hợp trong IDE
│   └── README.md                          # (Tham chiếu trực tiếp đến .github/prompts/*.prompt.md)
├── 03-module-task-prompts/                # Chỉ mục các prompt triển khai theo từng phần kỹ thuật
│   └── README.md                          # (Chỉ mục kết nối với docs/drafts/draft-workorder-*.md)
└── 04-dev-contributions/                  # Khu vực lưu trữ prompt đóng góp từ các Developer khác
    ├── dev-tudtbis92/                     # Các prompt fix bug, bảo mật và audit của dev tudtbis92
    │   ├── fix-status-query-param-400.prompt.md
    │   └── guard-demo-credentials-profile.prompt.md
    └── templates/                         # Mẫu prompt chuẩn mực để dev mới sao chép khi làm việc
        └── standard-dev-task.prompt.template.md
```

---

## 2. Ma Trận Phân Loại & Hướng Dẫn Sử Dụng (Prompt Usage Matrix)

| Nhóm Thư Mục | Đối Tượng Sử Dụng | Ngữ Cảnh Áp Dụng | Định Dạng Cấu Trúc |
|---|---|---|---|
| **`01-sdlc-playbook/`** | Lead Architect, Tech Lead, AI Agents | Quản lý toàn bộ vòng đời dự án từ khi nhận đề bài đến lúc bàn giao (Giai đoạn 1 $\rightarrow$ 8) | Chuẩn **Role - Task - Constraints - Done When** |
| **`02-copilot-slash-commands/`** | Full-Stack Devs trong IDE | Thao tác nhanh trong GitHub Copilot Chat (`/implement-endpoint`, `/review-code`) | Chuẩn GitHub Prompt YAML Frontmatter |
| **`03-module-task-prompts/`** | Backend Engineers | Hiện thực hóa chi tiết từng file mã nguồn (Entity, DTO, Service, Controller, Tests) | Embedded Prompt Header trong Markdown Drafts |
| **`04-dev-contributions/`** | Toàn bộ Team Kỹ sư | Giải quyết các issue cụ thể, fix bug phát sinh, tối ưu hiệu năng hoặc bảo mật | Phân chia theo tên thư mục của từng Developer (`dev-<username>`) |

---

## 3. Quy Chuẩn Đóng Góp Prompt Mới (Prompt Contribution Guidelines)

Khi một lập trình viên trong đội ngũ viết một câu prompt mới để giao việc cho AI:
1. **Sao chép Mẫu Chuẩn:** Lấy mẫu tại [`04-dev-contributions/templates/standard-dev-task.prompt.template.md`](04-dev-contributions/templates/standard-dev-task.prompt.template.md).
2. **Tuân Thủ 4 Thành Tố Bắt Buộc:**
   - `# ROLE:` Vai trò chuyên môn rõ ràng.
   - `# TASK:` Các bước hành động cụ thể, liệt kê rõ tên file cần tác động.
   - `# CONSTRAINTS:` Ràng buộc không được vi phạm (RFC 7807, Java 17, No-Lombok, Zero-Hallucination).
   - `# DONE WHEN:` Tiêu chí đo lường nghiệm thu thành công.
3. **Lưu Tệp Đúng Quy Định:** Đặt tệp tại thư mục `docs/prompt/04-dev-contributions/dev-<github-username>/<tên-chức-năng>.prompt.md`.
4. **Ghi Chú Trong PR:** Trích dẫn đường dẫn file prompt này vào mục `## 2. AI Usage Disclosure & Provenance` trong Pull Request template.
