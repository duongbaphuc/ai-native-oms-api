# Outage Work Order API ⚡

[![Build Status](https://img.shields.io/badge/build-passing-brightgreen)](#)
[![Java Version](https://img.shields.io/badge/Java-17%2B-blue)](#)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3-success)](#)

Dịch vụ Outage Work Order là một microservice cốt lõi thuộc phân hệ Outage Management System (OMS). API này cung cấp các giao thức RESTful để tạo, quản lý và theo dõi vòng đời của các sự kiện mất điện trên lưới điện.

Dự án áp dụng phương pháp luận **AI-Native SDLC**, sử dụng GitHub Copilot dưới sự ràng buộc chặt chẽ của kỹ thuật thiết kế ngữ cảnh (Context Engineering).

## Kiến trúc Tổng quan (Architecture Overview)

- **Framework:** Java 17, Spring Boot 3.3.
- **Data Store:** H2 Database (In-memory) - Tối ưu cho môi trường Lab/Demo.
- **Thiết kế API:** RESTful tuân thủ chuẩn RFC 7807 (Problem Details).

> [!IMPORTANT]
> **Chính sách Phát triển (Spec-Driven):** Dự án tuân thủ nguyên tắc không sinh mã nguồn tính năng nếu chưa phê duyệt tài liệu đặc tả (Domain Model & API Spec).

## Yêu cầu Hệ thống (Prerequisites)

- [Java Development Kit (JDK) 17+](https://adoptium.net/)
- [Maven 3.8+](https://maven.apache.org/)

## Hướng dẫn Khởi chạy (Getting Started)

**Bước 1: Tải dependencies và biên dịch**
```bash
./mvnw clean install -DskipTests
```

**Bước 2: Khởi động dịch vụ**
```bash
./mvnw spring-boot:run
```

> [!TIP]
> Ứng dụng khởi chạy tại http://localhost:8080. Bạn có thể truy cập http://localhost:8080/h2-console (JDBC URL: jdbc:h2:mem:workorderdb) để trực tiếp xem các thay đổi dữ liệu trong bộ nhớ.

## CSV lib (tudt)

Thư viện tính tổng tiền đơn hàng từ file CSV, dùng pure JDK (không thêm dependency), namespace `com.gpc.oms.csv.tudt`. Input là CSV UTF-8, phân cách phẩy, dòng đầu là header. Output chính là `record Totals(goods, vat, payable)` kiểu `BigDecimal` scale 2 `HALF_UP`; file CSV kết quả là output phụ tùy chọn (qua overload có `csvOut`).

### Ví dụ mapping header (Header mapping example)

Hệ thống quét tất cả column trong file trước, sau đó user mapping tên logic sang header thực tế. 4 field logic bắt buộc: `product`, `quantity`, `unit_price`, `vat_rate`. Column thừa được bỏ qua khi tính nhưng giữ nguyên trong file kết quả.

```java
List<String> headers = CsvTotals.scanHeaders(csv);
// headers = [product, quantity, unit_price, vat_rate, note]

// File dùng tên tiếng Việt: mapping tường minh
Map<String, String> mapping = Map.of(
        "product", "TenHang",
        "quantity", "SoLuong",
        "unit_price", "DonGia",
        "vat_rate", "ThueSuat");
CsvTotals.Totals t = CsvTotals.calculate(csv, mapping);
```

Lưu ý default identity: khi caller truyền `null` hoặc map rỗng thì lib dùng mapping mặc định `product -> product`, `quantity -> quantity`, `unit_price -> unit_price`, `vat_rate -> vat_rate`. Map thiếu key logic (ví dụ thiếu `unit_price`) sẽ throw thay vì fallback ngầm.

### Danh sách overload (Overload list)

```java
List<String> scanHeaders(String csv);
List<String> scanHeaders(Path csv);
Totals calculate(String csv, Map<String, String> mapping);
Totals calculate(Path csv, Map<String, String> mapping);
Totals calculate(String csv, Map<String, String> mapping, Path csvOut);
Totals calculate(Path csv, Map<String, String> mapping, Path csvOut);
record Totals(BigDecimal goods, BigDecimal vat, BigDecimal payable) {}
```

Overload `String` và `Path` hành xử giống hệt nhau. Overload có `csvOut` ghi đè file đích, tự tạo parent dirs, ghi UTF-8 với trailing newline; `csvOut` null nghĩa là không ghi file.

### Ví dụ lỗi (Error samples)

Mọi lỗi validation đều throw `IllegalArgumentException`, fail fast ở dòng lỗi đầu tiên. `Row N` là số dòng vật lý (header là dòng 1).

```text
Row 4: column 'quantity' value '' blank numeric cell
Row 2: column 'unit_price' value '-5' must be >= 0
Row 3: column 'vat_rate' value '150' exceeds 100%
Missing mapping for logical field 'unit_price'
Missing column for logical field 'product' (header 'nope')
Duplicate header 'product' at line 1
```

### Quy tắc VAT tự động (VAT auto-percent rule)

Giá trị `vat_rate <= 1` hiểu là fraction (ví dụ `0.10` = 10%, `0.08` = 8%). Giá trị `> 1` hiểu là percent và chia cho 100. Biên: `1` và `1.0` và `100` đều nghĩa là 100%; tối đa `100`; vượt `100` throw; âm throw. Ô số trống throw, không mặc định 0.

```text
0.10 -> 10%   | 0.08 -> 8%   | 1 -> 100% | 1.0 -> 100% | 10 -> 10% | 100 -> 100%
```

### Ví dụ dòng TOTAL (TOTAL row example)

File kết quả giữ nguyên thứ tự column gốc rồi append `line_total,vat_amount,payable` ở cuối, mỗi giá trị money format scale 2 plain string. Dòng cuối là TOTAL: literal `TOTAL` ở column đã map với `product`, 3 tổng ở 3 column cuối, các ô còn lại để trống.

```text
product,quantity,unit_price,vat_rate,note,line_total,vat_amount,payable
Apple,2,10.00,0.10,fresh,20.00,2.00,22.00
Banana,1,5.00,10,sweet,5.00,0.50,5.50
TOTAL,,,,,25.00,2.50,27.50
```

### Hạn chế đã biết v1.1 (Known limitations)

- Ô product có literal `TOTAL` sẽ không phân biệt được với dòng tổng trong file kết quả.
- Formula injection khi mở lại bằng Excel: ô product bắt đầu bằng `=`, `+`, `-`, `@` (ví dụ `=CMD(...)`) sẽ bị Excel diễn giải thành công thức khi mở file. v1.1 chỉ document, không thêm logic defuse; caller cần cảnh giác khi mở file kết quả bằng spreadsheet.
