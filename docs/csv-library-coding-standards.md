<!--
Role: Lead Software Architect & Technical Lead
Task: Strict Java 17 Coding Conventions & Style Guide for csv-order-processor
Context files: docs/coding-rules.md, docs/internal-coding-standards.md, docs/csv-order-domain-model.md
Constraints: Google Java Style Guide, Maven Standard Layout, < 50 LOC/method, max 3 nesting levels, mandatory Javadoc
-->
# Bộ Tiêu Chuẩn Lập Trình (Java 17 Coding Standards & Style Guide)

Tài liệu này xác lập các tiêu chuẩn lập trình khắt khe, áp dụng bắt buộc cho toàn bộ đội ngũ Developer khi phát triển, bảo trì và mở rộng thư viện dùng chung **`csv-order-processor`** (nền tảng Java 17).

---

## 1. Cấu Trúc Dự Án Chuẩn Maven (Project Structure)

Mọi mã nguồn và tài nguyên phải tuân thủ nghiêm ngặt mô hình **Maven Standard Directory Layout**, phản chiếu 1-1 giữa mã nguồn chính (`main`) và mã nguồn kiểm thử (`test`):

```text
csv-order-processor/
├── pom.xml                                  <-- Khai báo build JAR, Java 17, Zero runtime dependencies
└── src/
    ├── main/
    │   ├── java/                            <-- Mã nguồn chính của thư viện
    │   │   ├── module-info.java             <-- JPMS: chỉ exports package api, ẩn giấu internal
    │   │   └── com/gpc/order/processor/
    │   │       ├── api/                     <-- [PUBLIC API] Giao diện, Record, Exception công khai
    │   │       │   ├── config/
    │   │       │   ├── exception/
    │   │       │   └── model/
    │   │       └── internal/                <-- [INTERNAL] Logic nội bộ ẩn giấu (parser, calculator, validator)
    │   │           ├── calculator/
    │   │           ├── csv/
    │   │           ├── engine/
    │   │           └── validator/
    │   └── resources/                       <-- Tài nguyên đóng gói vào JAR (META-INF manifest...)
    └── test/
        ├── java/                            <-- Mã nguồn kiểm thử JUnit 5 (phản chiếu gói của main)
        │   └── com/gpc/order/processor/
        │       ├── api/                     <-- Kiểm thử tích hợp từ góc nhìn người dùng Public API
        │       └── internal/                <-- Kiểm thử đơn vị cho từng thuật toán nội bộ
        └── resources/                       <-- Dữ liệu giả lập test (test-data/*.csv)
```

### Quy tắc cấu trúc:
1. **Không tạo thư mục tự do ngoài chuẩn:** Toàn bộ code Java phải nằm trong `src/main/java` và `src/test/java`.
2. **Tách biệt ranh giới Public vs Internal:** Bất kỳ class nào người dùng bên ngoài không cần gọi trực tiếp bắt buộc phải nằm trong package `com.gpc.order.processor.internal.*`.
3. **Phản chiếu Package kiểm thử:** Class `X` trong package `com.gpc.order.processor.internal.csv` phải có test class tương ứng `XTest` trong cùng package ở `src/test/java`.

---

## 2. Quy Tắc Đặt Tên (Naming Conventions)

Tuân thủ triệt để [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html) với tính tường minh cao:

| Thành phần Code | Quy ước Đặt tên | Ví dụ Chuẩn (Compliant) | Ví dụ Vi phạm (Non-compliant) | Ghi chú & Ràng buộc |
|---|---|---|---|---|
| **Class & Interface** | `PascalCase` | `CsvOrderProcessor`, `ExcelCsvParser` | `csvOrderProcessor`, `CSV_Parser` | Danh từ hoặc cụm danh từ. Không dùng tiền tố `I` (như `ICsvProcessor`). |
| **Record (Java 17)** | `PascalCase` | `OrderItem`, `OrderSummary`, `CsvConfig` | `orderItemRecord`, `Order_Summary` | Danh từ đại diện cho thực thể dữ liệu bất biến. Không gắn hậu tố `Record` hay `Dto`. |
| **Enum Type** | `PascalCase` | `OutputMode`, `ColumnKey` | `output_mode`, `EOutputMode` | Danh từ số ít, thể hiện tập giá trị hữu hạn. |
| **Enum Constant** | `UPPER_SNAKE_CASE` | `REPORT_MODE`, `DATA_MODE`, `VAT_RATE` | `reportMode`, `Report_Mode` | Viết hoa toàn bộ, phân cách bằng dấu gạch dưới `_`. |
| **Method (Hàm)** | `camelCase` | `calculateVatAmount()`, `formatRow()` | `CalculateVAT()`, `format_row()` | Bắt đầu bằng động từ thể hiện rõ hành vi hoặc chuyển đổi (vd: `toDomainConfig()`). |
| **Variable & Field** | `camelCase` | `lineTotal`, `vatRate`, `lineNumber` | `LineTotal`, `vat_rate`, `lt` | Có ý nghĩa rõ ràng. **CẤM** viết tắt 1 ký tự (trừ biến lặp `i`, `j` trong vòng for ngắn). |
| **Constant (`static final`)** | `UPPER_SNAKE_CASE` | `MONEY_SCALE`, `DEFAULT_DELIMITER` | `moneyScale`, `default_delimiter` | Chỉ áp dụng cho các hằng số bất biến nguyên thủy hoặc Immutable Object. |
| **Package** | `alllowercase` | `com.gpc.order.processor.api` | `com.gpc.orderProcessor.API` | Viết thường toàn bộ, không chứa dấu gạch dưới hoặc chữ in hoa. |
| **Test Class** | `PascalCase` + `Test` | `CsvOrderProcessorTest`, `CsvWriterTest` | `TestCsvOrderProcessor`, `CsvOrderProcessorTests` | Bắt buộc kết thúc bằng hậu tố `Test` (để Surefire plugin nhận diện). |

---

## 3. Định Dạng Mã Nguồn (Formatting & Layout)

Dựa trên chuẩn Google Java Style Guide:

### 3.1. Thụt lề (Indentation) & Khoảng trắng
- **Quy tắc Thụt lề:** Sử dụng **2 spaces** (hoặc 4 spaces thống nhất dự án) cho mỗi cấp độ thụt lề. **CẤM TUYỆT ĐỐI dùng ký tự Tab (`\t`)**.
- **Continuation Indent:** Khi một câu lệnh bị ngắt xuống dòng, thụt lề dòng tiếp theo thêm ít nhất **+4 spaces**.

### 3.2. Độ dài dòng (Line Length)
- Chiều dài tối đa của một dòng code là **100 ký tự** (hoặc 120 ký tự đối với màn hình hiện đại).
- Nếu dòng dài hơn, bắt buộc phải ngắt dòng tại các toán tử (`+`, `&&`, `,`) hoặc sau dấu chấm gọi hàm builder chùm.

### 3.3. Dấu ngoặc nhọn (Braces `{}`)
- Tuân thủ phong cách **K&R / Kernighan & Ritchie (Egyptian brackets)**: Dấu mở ngoặc `{` nằm ở cuối dòng khai báo, dấu đóng ngoặc `}` nằm ở đầu dòng mới, thẳng hàng với câu lệnh mở đầu:
  ```java
  // Đúng (Compliant)
  if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
    throw new CsvProcessingException(lineNumber, column, "Quantity must be positive");
  } else {
    processLine(quantity);
  }
  ```
- **Không bao giờ bỏ qua ngoặc nhọn:** Mọi câu lệnh `if`, `else`, `for`, `do`, `while` bắt buộc phải có ngoặc nhọn `{}`, ngay cả khi thân lệnh chỉ có 1 dòng duy nhất.
  ```java
  // SAI (Non-compliant)
  if (value == null) return BigDecimal.ZERO;

  // ĐÚNG (Compliant)
  if (value == null) {
    return BigDecimal.ZERO;
  }
  ```

### 3.4. Tổ chức Import
- **CẤM TUYỆT ĐỐI Wildcard Imports:** Không dùng `import java.util.*;` hay `import static org.junit.jupiter.api.Assertions.*;`. Mọi class phải được import tường minh từng class một.
- Nhóm import theo thứ tự:
  1. `static` imports (nếu có, đặt trên cùng).
  2. Các package chuẩn của Java SDK (`java.*`, `javax.*`).
  3. Các package của bên thứ ba hoặc thư viện ngoài.
  4. Các package nội bộ của dự án (`com.gpc.*`).

---

## 4. Giới Hạn Độ Phức Tạp (Complexity & Quality Limits)

Nhằm giữ cho mã nguồn luôn dễ đọc, dễ kiểm thử và có tính bảo trì cao:

### 4.1. Giới hạn độ dài hàm (Method Length) $\le$ 50 Dòng Code
- **Quy định:** Mỗi method/hàm **không được vượt quá 50 dòng code thực tế** (không tính dòng trống và comment).
- **Giải pháp:** Nếu hàm dài hơn 50 dòng, lập tức tách nhỏ thành các hàm phụ trợ riêng biệt (Extract Method) theo từng bước logic độc lập.

### 4.2. Giới hạn mức độ lồng nhau (Nesting Depth) $\le$ 3 Mức
- **Quy định:** Trong thân hàm, độ sâu lồng nhau của các khối điều khiển (`if`, `else`, `for`, `while`, `switch`) **tối đa không quá 3 cấp**.
- **Giải pháp: Áp dụng Guard Clauses (Return Early / Fail Fast)**
  - Thay vì bọc toàn bộ code trong nhiều lớp `if`, hãy đảo ngược điều kiện và return/throw ngay từ đầu:

```java
// SAI: Lồng nhau quá sâu (Nesting level = 4)
public void process(Row row) {
  if (row != null) {
    if (!row.isEmpty()) {
      for (Cell cell : row) {
        if (cell.isValid()) {
          // Xử lý sâu...
        }
      }
    }
  }
}

// ĐÚNG: Áp dụng Guard Clauses (Làm phẳng mã nguồn, Nesting level <= 2)
public void process(Row row) {
  if (row == null || row.isEmpty()) {
    return;
  }
  for (Cell cell : row) {
    if (!cell.isValid()) {
      continue;
    }
    // Xử lý trực tiếp, rõ ràng và dễ đọc
  }
}
```

---

## 5. Tiêu Chuẩn JavaDoc & Tài Liệu Hóa (Documentation)

Mọi thành phần công khai (Public Interface) phải được ghi chép đầy đủ để lập trình viên khác có thể gọi hàm mà không cần đọc mã nguồn triển khai.

### 5.1. Phạm vi bắt buộc có JavaDoc
- **Bắt buộc 100%:** Mọi `public` class, `public` interface, `public` record, `public` enum.
- **Bắt buộc 100%:** Mọi `public` và `protected` method.
- **Khuyến khích:** Các class và method phức tạp trong tầng `internal`.

### 5.2. Cấu trúc JavaDoc Chuẩn
```java
/**
 * Tính toán số tiền thuế VAT cho một dòng chi tiết đơn hàng dựa trên thành tiền trước thuế và thuế suất.
 * Áp dụng quy tắc làm tròn HALF_UP đến 2 chữ số thập phân chuẩn siêu thị.
 *
 * @param lineTotal Thành tiền trước thuế của dòng (bắt buộc khác null, scale = 2).
 * @param vatRate   Thuế suất phần trăm VAT (ví dụ: 10 đại diện cho 10%).
 * @return Số tiền VAT đã làm tròn HALF_UP 2 chữ số thập phân (không bao giờ null).
 * @throws NullPointerException nếu lineTotal hoặc vatRate là null.
 * @throws IllegalArgumentException nếu vatRate có giá trị âm.
 */
public static BigDecimal calculateVatAmount(BigDecimal lineTotal, BigDecimal vatRate) {
  // ...
}
```

### 5.3. Quy tắc viết mô tả JavaDoc
1. Câu đầu tiên là một tóm tắt ngắn gọn, kết thúc bằng dấu chấm. Bắt đầu bằng động từ hành động (ví dụ: *"Tính toán số tiền thuế..."*, *"Phân tích cú pháp chuỗi CSV..."*).
2. Phải khai báo đầy đủ tất cả các thẻ `@param`, `@return` và `@throws` (chỉ rõ điều kiện kích hoạt ngoại lệ).

---

## 6. Quy Chuẩn Tài Chính, Làm Tròn & Bất Biến (Domain & Financial Rules)

Thư viện xử lý hóa đơn và tính tiền, vì vậy tính chính xác về số học là tối thượng:

1. **Tuyệt đối cấm kiểu số thực trôi nổi:** **CẤM DÙNG** `float` và `double` cho bất kỳ phép tính tiền tệ, thuế hay số lượng nào. Luôn luôn sử dụng `java.math.BigDecimal`.
2. **Quy tắc làm tròn siêu thị:**
   - Mọi kết quả tiền tệ đều có `scale = 2` và `RoundingMode.HALF_UP`.
   - Các số tổng (`subtotal`, `totalVat`) là **tổng của các giá trị đã được làm tròn ở từng dòng chi tiết** để tránh lỗi tích lũy sai số tiền lẻ.
3. **Tính bất biến (Immutability):**
   - Sử dụng Java 17 `record` cho tất cả các đối tượng truyền tải dữ liệu (`OrderItem`, `OrderSummary`, `CsvConfig`).
   - Mọi Collection trong Record phải được bọc bằng `Collections.unmodifiableList()` hoặc `List.copyOf()`.

---

## 7. Quy Chuẩn Kiểm Thử Tự Động (Unit Testing with JUnit 5)

Mọi tính năng mới hoặc bản vá lỗi bắt buộc phải có Unit Test đi kèm:

1. **Đặt tên Test Method mang tính ngữ nghĩa cao:**
   - Sử dụng `@DisplayName` bằng tiếng Việt hoặc tiếng Anh mô tả rõ ràng kịch bản kiểm thử:
   ```java
   @Test
   @DisplayName("Làm tròn HALF_UP 2 số lẻ: 1234.565 làm tròn thành 1234.57")
   void roundMoney_givenHalfUpValue_shouldRoundUpCorrectly() { ... }
   ```
2. **Cấu trúc Test 3A (Arrange - Act - Assert):** Phân tách rõ ràng 3 giai đoạn trong mỗi hàm test.
3. **Thư viện Assert:** Sử dụng **AssertJ** (`assertThat(...)`) để câu lệnh kiểm tra dễ đọc và có thông báo lỗi trực quan.
4. **Kiểm tra ngoại lệ:** Dùng `assertThatThrownBy(() -> ...).isInstanceOf(...)` để kiểm tra cả kiểu ngoại lệ, số dòng lỗi (`lineNumber`) và cột lỗi (`columnIdentifier`).

---

## 8. Bảng Kiểm Tra Nhanh Khi Review Code (Pull Request Checklist)

Trước khi submit PR, mỗi Developer tự kiểm tra theo checklist sau:

- [ ] Dự án build xanh với lệnh: `mvn clean test` (100% tests pass).
- [ ] Tuân thủ cấu trúc thư mục chuẩn Maven, không có file rác bị commit.
- [ ] Không có class/interface `public` nào thiếu JavaDoc.
- [ ] Không có method nào vượt quá **50 dòng code**.
- [ ] Không có khối lệnh nào bị lồng sâu quá **3 mức** (đã dùng Guard Clauses).
- [ ] Không dùng `double`/`float` để tính tiền; tiền tệ dùng `BigDecimal` với `HALF_UP` 2 số lẻ.
- [ ] Toàn bộ logic nội bộ đều nằm trong package `internal` và không bị lộ ra Public API.
- [ ] Không thêm dependency runtime bên ngoài vào `pom.xml` (Zero Runtime Dependency).
