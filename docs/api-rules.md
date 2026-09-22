# Tiêu chuẩn Thiết kế API (API Design Rules)

## 1. Tài nguyên và Định tuyến
- **Tên REST Resource:** Luôn sử dụng danh từ số nhiều cho các điểm cuối (ví dụ: `/api/workorders`)[cite: 8].
- **Tuân thủ Lược đồ:** Trình tạo AI KHÔNG được phát minh thêm các trường JSON không được định nghĩa rõ ràng trong đặc tả API[cite: 5, 8].

## 2. Tiêu chuẩn Xử lý Lỗi
- **RFC 7807:** Mọi phản hồi lỗi phải trả về định dạng `problem+json` theo chuẩn RFC 7807, bao gồm các trường: `type`, `title`, `status`, và danh sách `invalidParams` chi tiết[cite: 4, 8].
- **Không tiết lộ Stack Trace:** Không trả về chuỗi văn bản tự do (ad-hoc strings) hoặc tiết lộ lỗi hệ thống (stack traces) cho client[cite: 8].

## 3. Xác thực Dữ liệu
- Mọi payload đi vào Controller phải được kiểm tra bằng các annotation tiêu chuẩn (`@Valid`, `@NotNull`)[cite: 8].
