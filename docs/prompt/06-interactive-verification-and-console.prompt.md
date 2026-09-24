# Prompt Giai Đoạn 6: Nghiệm Thu Môi Trường Thực Tế & Giao Diện Test Console (Interactive Verification)

```markdown
# ROLE:
Bạn là một "Senior Full-Stack QA Engineer" và "Front-End UI Specialist" am hiểu trải nghiệm người dùng (UX) hiện đại, REST Client Testing, và tích hợp Spring Boot Web.

---

# TASK:
Tạo môi trường nghiệm thu trực quan trên trình duyệt (Browser Testing Environment) cho dự án:
1. **Tạo Giao diện Test Console Hiện đại:**
   - Tạo file `src/main/resources/static/index.html` độc lập (HTML5, Vanilla CSS, JavaScript).
   - Thiết kế giao diện Dark Slate / Glassmorphism cao cấp, trực quan.
   - Tích hợp thanh công cụ chuyển đổi vai trò (Role Switcher): `ADMIN` (`admin/admin123`), `DISPATCHER` (`dispatcher/dispatcher123`), `TECHNICIAN` (`technician/technician123`), và `NO AUTH`.
   - Bảng điều khiển Form tạo Work Order kèm các tag điền nhanh mã thiết bị.
   - Bảng hiển thị danh sách phiếu sự cố thời gian thực với bộ lọc trạng thái và phân trang.
   - Sơ đồ trực quan hóa State Machine kèm các nút thao tác chuyển trạng thái (kèm nút thử nghiệm hành vi vi phạm như Skip hay Rollback).
   - Cửa sổ **API Live Inspector** hiển thị thời gian phản hồi (latency), HTTP Status Badge, và JSON RFC 7807 Problem Details có định dạng màu.
2. **Cấu hình Spring Security cho Demo:**
   - Cập nhật `SecurityConfig.java`: khai báo `UserDetailsService` InMemory cho 3 tài khoản demo.
   - Mở quyền truy cập public cho static files (`/`, `/index.html`) và H2 Database Web Console (`/h2-console/**`).
   - Cấu hình `headers.frameOptions.sameOrigin()` cho phép H2 console hoạt động trên trình duyệt.
3. **Khởi chạy Server:**
   - Chạy ứng dụng local trên cổng 8080 và cung cấp hướng dẫn truy cập cho người dùng.

---

# CONSTRAINTS:
1. Giao diện phải hoàn toàn tương thích với các API RESTful backend hiện có, không can thiệp hay làm giảm độ bao phủ test (100% JaCoCo) đã đạt được ở Giai đoạn 5.
2. Sử dụng Vanilla CSS và JavaScript thuần, không phụ thuộc vào CDN bên ngoài để đảm bảo chạy mượt mà ngay cả khi offline.

---

# DONE WHEN:
1. File `src/main/resources/static/index.html` được tạo và render hoàn hảo tại `http://localhost:8080/`.
2. Người dùng có thể thực hiện toàn bộ vòng đời phiếu sự cố trên trình duyệt: Tạo phiếu `Open` → Chuyển `InProgress` → Hoàn thành `Done` → Xem log lỗi 422 khi nhảy cóc.
3. Chạy lại `mvn clean verify` vẫn duy trì 100% test pass và JaCoCo Quality Gate đạt 100%.
```
