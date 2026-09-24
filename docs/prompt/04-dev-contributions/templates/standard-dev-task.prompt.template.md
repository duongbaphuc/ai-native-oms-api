# Mẫu Chuẩn Cho Developer Đóng Góp Prompt Mới (Dev Task Prompt Template)

> **Hướng dẫn:** Sao chép mẫu này khi bạn cần viết một câu prompt để yêu cầu AI sinh code cho tính năng, sửa lỗi (bugfix) hoặc viết kiểm thử mới.  
> Lưu tệp mới theo quy ước: `docs/prompt/04-dev-contributions/dev-<github-username>/<task-name>.prompt.md`.

---

```markdown
# ROLE:
[Nêu rõ vai trò chuyên môn kỹ thuật mà bạn yêu cầu AI đảm nhận, ví dụ: Senior Backend Engineer / QA Automation Engineer / Security Architect]

---

# TASK:
[Mô tả chi tiết, tuần tự từng bước công việc mà AI cần thực hiện]
1. [Bước 1: Tệp cần chỉnh sửa hoặc tạo mới]
2. [Bước 2: Logic cần hiện thực hóa]
3. [Bước 3: Tích hợp kiểm thử tự động]

---

# CONSTRAINTS:
[Các ràng buộc kỹ thuật bất di bất dịch mà AI không được phép vi phạm]
1. Tuân thủ 100% tài liệu đặc tả: [Liệt kê các tệp trong docs/ liên quan]
2. Chuẩn mã nguồn: Java 17, immutable records, không dùng Lombok, constructor injection.
3. Chuẩn lỗi: RFC 7807 Problem Details (`application/problem+json`).
4. Không xóa hoặc làm hỏng các test cases hiện có.

---

# DONE WHEN:
[Tiêu chí đo lường chính xác để xác nhận công việc đã hoàn thành thành công]
1. [Tệp A và B được tạo / chỉnh sửa chính xác]
2. [Lệnh `mvn test` chạy thành công 100%]
3. [Hành vi chức năng được kiểm chứng thành công]
```
