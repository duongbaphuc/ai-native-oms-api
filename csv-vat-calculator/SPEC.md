# Technical Specification: csv-vat-calculator (v2.0)

**Version:** 2.0.0  
**Status:** Approved / Architecture Design  
**Target Platform:** Java 17 LTS+  
**Target Artifact:** `com.company.shared:csv-vat-calculator:2.0.0-SNAPSHOT`  
**Author:** Senior Java Architect & AI-Native SDLC Lead  

---

## 1. Tổng quan & Triết lý Kiến trúc v2.0 (Overview & Architecture Philosophy)

### 1.1. Bối cảnh
Thư viện `csv-vat-calculator` là thư viện Java dùng chung (Enterprise Shared Library) phục vụ toàn bộ các dự án dịch vụ, thương mại điện tử, thanh toán và kế toán trong hệ sinh thái của công ty mẹ. Phiên bản **v2.0** được thiết kế lại nhằm loại bỏ hoàn toàn các giả định cứng (hardcoded assumptions), mang lại khả năng cấu hình tối đa, độ an toàn tính toán tuyệt đối và trải nghiệm lập trình viên (Developer Experience - DX) đỉnh cao.

### 1.2. Bốn trụ cột kiến trúc cốt lõi (The 4 Architectural Pillars)
```
                       ┌────────────────────────────────────────────────────────┐
                       │               CsvVatCalculator (Facade)                │
                       │   .builder()                                           │
                       │     .columnMapping(...)       // Pillar 1: Dynamic     │
                       │     .currencyScale(0)         // Pillar 2: BigDecimal  │
                       │     .roundingMode(HALF_UP)    // Pillar 2: BigDecimal  │
                       │     .discrepancyStrategy(...) // Pillar 3: Validation  │
                       └───────────────────┬────────────────────────────────────┘
                                           │
        ┌──────────────────────────────────┼──────────────────────────────────┐
        ▼                                  ▼                                  ▼
[1. Dynamic Metadata]             [2. Core Calculation]             [3. Flexible Output]
- CsvColumnMapping                - Scale & RoundingMode             - process(Path, Path) -> File CSV
  (Custom headers per project)    - DiscrepancyStrategy:             - processToString(...) -> String CSV
- Fallback alias auto-detect        + FAIL_ON_MISMATCH               - processToResult(...) -> Pure POJO
                                    + WARN_AND_RECALCULATE           - process(Stream, Stream)
                                    + ACCEPT_INPUT_TOTAL
```

1. **Dynamic Metadata Mapping:** Không hardcode tên cột CSV. Cung cấp cơ chế định nghĩa tên cột tùy biến theo từng dự án (`CsvColumnMapping`) kèm cơ chế dự phòng (fallback) tự động nhận diện alias đa ngôn ngữ (Tiếng Anh & Tiếng Việt).
2. **Chuẩn hóa tính toán tiền tệ:** 100% phép toán số học sử dụng `BigDecimal`. Người dùng được quyền tùy biến `currencyScale` và `RoundingMode` tự do thông qua `CalculatorConfig`.
3. **Chiến lược kiểm tra chéo thành tiền (Line Total Cross-Validation Strategy):**
   - Chặn nghiêm ngặt số lượng âm (`quantity < 0`), ném ngoại lệ rõ ràng.
   - So sánh đối chiếu giữa tích số $(\text{Quantity} \times \text{Unit Price})$ và cột "Số tổng / Line Total" đầu vào (nếu có sẵn trong CSV) với 3 chiến lược linh hoạt: `FAIL_ON_MISMATCH`, `WARN_AND_RECALCULATE`, và `ACCEPT_INPUT_TOTAL`.
4. **Fluent API & Đa dạng hóa đầu ra (Diverse Outputs):**
   - Thiết kế Fluent Builder trực quan.
   - Hỗ trợ xuất dữ liệu ra nhiều định dạng đích: Ghi ra File CSV (`Path`/`File`), ghi ra luồng nhị phân (`OutputStream`), xuất ra chuỗi in-memory String CSV (`processToString`), hoặc chỉ trả về đối tượng Java POJO/Record thuần túy (`processToResult`).

---

## 2. Dynamic Metadata Mapping (Ánh xạ Cột Linh Hoạt)

### 2.1. Thiết kế `CsvColumnMapping`
Lớp cấu hình ánh xạ cột cho phép từng dự án khách hàng (client project) tùy biến cấu trúc file CSV đầu vào:

```java
package com.company.shared.csvvat.config;

import java.util.Optional;

/**
 * Cấu hình ánh xạ tên cột CSV động cho từng dự án.
 */
public record CsvColumnMapping(
    String itemNameColumn,
    String quantityColumn,
    String unitPriceColumn,
    String vatPercentageColumn,
    String lineTotalColumn // Tùy chọn (Optional)
) {
    public static CsvColumnMapping defaultMapping() {
        return new CsvColumnMapping("Item Name", "Quantity", "Unit Price", "VAT (%)", null);
    }

    public boolean isDynamic() {
        return itemNameColumn != null || quantityColumn != null 
            || unitPriceColumn != null || vatPercentageColumn != null;
    }

    public Optional<String> getLineTotalColumn() {
        return Optional.ofNullable(lineTotalColumn);
    }
}
```

### 2.2. Cơ chế Fallback Alias Auto-Detection
Nếu người dùng không chỉ định `CsvColumnMapping` (hoặc truyền null), hệ thống tự động kích hoạt cơ chế nhận diện tự động (Case-insensitive, bỏ khoảng trắng và chuẩn hóa không dấu tiếng Việt NFD):

| Dữ liệu nghiệp vụ | Header chuẩn | Danh sách Alias được nhận diện tự động |
| :--- | :--- | :--- |
| **Item Name** | `Item Name` | `item`, `product`, `product_name`, `tên mặt hàng`, `tên sản phẩm`, `mặt hàng`, `hàng hóa`, `description` |
| **Quantity** | `Quantity` | `qty`, `số lượng`, `so_luong`, `sl`, `amount` |
| **Unit Price** | `Unit Price` | `price`, `unit_price`, `đơn giá`, `don_gia`, `giá`, `unit_cost`, `cost` |
| **VAT Percentage** | `VAT (%)` | `vat`, `vat_percentage`, `vat_rate`, `thuế vat`, `thuế (%)`, `phần trăm vat`, `tax`, `tax_rate` |
| **Line Total (Optional)** | `Line Total` | `thành tiền`, `tổng tiền`, `total`, `line_total`, `subtotal`, `thanh_tien` |

---

## 3. Chuẩn hóa Tính toán & Tiền tệ (Calculation & Monetary Standards)

### 3.1. Đối tượng cấu hình `CalculatorConfig`
```java
package com.company.shared.csvvat.config;

import java.math.BigDecimal;
import java.math.RoundingMode;

public record CalculatorConfig(
    int currencyScale,
    RoundingMode roundingMode,
    LineTotalDiscrepancyStrategy discrepancyStrategy,
    BigDecimal discrepancyTolerance,
    CsvColumnMapping columnMapping,
    boolean writeUtf8Bom
) {
    public static final int DEFAULT_SCALE = 2;
    public static final RoundingMode DEFAULT_ROUNDING_MODE = RoundingMode.HALF_UP;
    public static final LineTotalDiscrepancyStrategy DEFAULT_STRATEGY = LineTotalDiscrepancyStrategy.WARN_AND_RECALCULATE;
    public static final BigDecimal DEFAULT_TOLERANCE = new BigDecimal("0.01");
    public static final boolean DEFAULT_WRITE_UTF8_BOM = true;

    public static Builder builder() {
        return new Builder();
    }
}
```

### 3.2. Công thức toán học
Đối với mỗi dòng sản phẩm $i$:
1. **Thành tiền tính toán (Calculated Subtotal):**
   $$\text{CalculatedSubtotal}_i = (\text{Quantity}_i \times \text{UnitPrice}_i).\text{setScale}(\text{currencyScale}, \text{roundingMode})$$

2. **Tiền thuế VAT (Calculated Item VAT):**
   $$\text{ItemVat}_i = \left(\text{EffectiveSubtotal}_i \times \frac{\text{VatPercentage}_i}{100}\right).\text{setScale}(\text{currencyScale}, \text{roundingMode})$$

3. **Thành tiền sau thuế (Line Total With VAT):**
   $$\text{LineTotal}_i = \text{EffectiveSubtotal}_i + \text{ItemVat}_i$$

4. **Tổng kết đơn hàng (Order Level):**
   $$\text{OrderSubtotal} = \sum_{i=1}^n \text{EffectiveSubtotal}_i, \quad \text{OrderTotalVat} = \sum_{i=1}^n \text{ItemVat}_i$$
   $$\text{GrandTotal} = \text{OrderSubtotal} + \text{OrderTotalVat} = \sum_{i=1}^n \text{LineTotal}_i$$

---

## 4. Chiến lược Kiểm tra chéo Thành tiền (Line Total Cross-Validation Strategy)

### 4.1. Quy tắc ràng buộc số lượng (Negative Quantity Blocking)
- Bất kỳ giá trị nào có $\text{Quantity} < 0$ đều bị chặn tuyệt đối và lập tức ném ra ngoại lệ `IllegalArgumentException` hoặc `CsvRowValidationException` mang thông báo rõ ràng: `"Quantity cannot be negative: <value>"`. Không chấp nhận số âm dưới mọi hình thức.
- Giá trị $\text{Quantity} = 0$ cũng bị từ chối với thông báo: `"Quantity must be greater than 0"`.

### 4.2. So khớp chéo Thành tiền (Cross-Check Validation)
Khi file CSV đầu vào có cung cấp cột tổng thành tiền trước thuế $\text{InputLineTotal}_i$, hệ thống tính toán sai lệch tuyệt đối:
$$\Delta_i = |\text{CalculatedSubtotal}_i - \text{InputLineTotal}_i|$$

Nếu $\Delta_i > \text{discrepancyTolerance}$, hệ thống hành xử theo cấu hình enum `LineTotalDiscrepancyStrategy`:

| Giá trị Enum | Hành vi thực thi | Trường hợp sử dụng |
| :--- | :--- | :--- |
| **`FAIL_ON_MISMATCH`** | Lập tức ném `LineTotalDiscrepancyException` chỉ rõ dòng, giá trị tính toán, giá trị nhập vào và độ lệch. | Chế độ kế toán khắt khe, kiểm toán dữ liệu. |
| **`WARN_AND_RECALCULATE`** *(Mặc định)* | Ghi log cảnh báo mức `WARN` qua SLF4J, bỏ qua giá trị đầu vào và sử dụng $\text{CalculatedSubtotal}_i$ làm chuẩn. | Chế độ phục hồi dữ liệu tự động, ưu tiên tính nhất quán. |
| **`ACCEPT_INPUT_TOTAL`** | Chấp nhận $\text{InputLineTotal}_i$ làm giá trị tính thuế VAT (chỉ log `INFO` nếu có sai lệch nhỏ). | Tôn trọng giá trị từ hệ thống POS/ERP cũ cung cấp. |

---

## 5. Fluent API & Đa dạng hóa Đầu ra (Diverse Output Capabilities)

Lớp Facade `com.company.shared.csvvat.CsvVatCalculator` cung cấp 4 chế độ xuất dữ liệu đáp ứng mọi tình huống sử dụng:

### 5.1. Chế độ 1: Xuất ra File CSV (File-based Enriched Output)
```java
OrderCalculationResult result = calculator.process(
    Path.of("orders/input.csv"), 
    Path.of("orders/output_enriched.csv")
);
```

### 5.2. Chế độ 2: Xuất ra Chuỗi In-Memory String CSV (`processToString`)
Rất hữu ích khi cần gửi dữ liệu CSV đã làm giàu qua Kafka, MQ, hoặc kiểm thử nhanh:
```java
String enrichedCsvString = calculator.processToString(Path.of("orders/input.csv"));
// hoặc từ InputStream:
String csvString = calculator.processToString(inputStream);
```

### 5.3. Chế độ 3: Chỉ trả về POJO Kết quả (`processToResult`)
Dành cho trường hợp chỉ cần tính toán để lưu Database hoặc xử lý nghiệp vụ, không cần chi phí ghi ra file CSV:
```java
OrderCalculationResult pojoResult = calculator.processToResult(Path.of("orders/input.csv"));
BigDecimal totalVat = pojoResult.totalVat();
BigDecimal grandTotal = pojoResult.grandTotal();
```

### 5.4. Chế độ 4: Stream I/O (Dành cho Spring Boot Web API)
```java
OrderCalculationResult result = calculator.process(multipartFile.getInputStream(), responseOutputStream);
```

---

## 6. Thiết kế Hợp đồng & Cấu trúc Package (Architecture & Packages)

```
com.company.shared.csvvat
├── CsvVatCalculator.java                     // Entry point Facade
├── config
│   ├── CalculatorConfig.java                 // Cấu hình tổng hợp (Record)
│   ├── CsvColumnMapping.java                 // Ánh xạ cột động (Record)
│   └── LineTotalDiscrepancyStrategy.java     // Enum chiến lược kiểm tra chéo
├── exception
│   ├── CsvVatException.java                  // Base Runtime Exception
│   ├── InvalidCsvFormatException.java        // Sai cấu trúc header/format
│   ├── CsvRowValidationException.java        // Lỗi dữ liệu từng dòng
│   ├── LineTotalDiscrepancyException.java    // Lỗi lệch tiền giữa tính toán & đầu vào
│   └── EmptyOrderException.java              // File CSV không có mặt hàng
├── model
│   ├── OrderItem.java                        // Dữ liệu đọc từ CSV
│   ├── CalculatedItem.java                   // Dữ liệu từng dòng sau tính toán
│   └── OrderCalculationResult.java          // Kết quả tổng thể đơn hàng
└── service
    ├── VatCalculator.java                    // Interface tính toán VAT & tiền tệ
    ├── DefaultVatCalculator.java             // Core engine thực thi tính toán
    ├── CsvReaderService.java                 // Interface đọc CSV
    ├── CommonsCsvReaderService.java          // Đọc CSV với Commons CSV + BOM UTF-8
    ├── CsvWriterService.java                 // Interface ghi CSV
    └── CommonsCsvWriterService.java          // Ghi CSV Enriched + String serializer
```

---

## 7. Mẫu File CSV Đầu vào & Đầu ra (v2.0 Samples)

### 7.1. CSV Đầu vào với Header Tùy biến (`custom_headers_input.csv`)
Giả sử dự án ERP sử dụng bộ header riêng:
```csv
Mã SP,Tên hàng hóa,SL,Giá bán lẻ,% VAT,Thành tiền đầu vào
SP01,Bánh mì xíu mại trứng muối,2,35000,8,70000
SP02,Cà phê muối xứ Huế,3,28000,10,84000
SP03,Trà đào cam sả,1,40000,10,40000
SP04,Nước suối đóng chai,5,8000,0,40000
```

### 7.2. CSV Đầu ra Enriched (`custom_headers_output_enriched.csv`)
```csv
Item Name,Quantity,Unit Price,VAT (%),Line Subtotal,Item VAT,Line Total
Bánh mì xíu mại trứng muối,2,35000,8,70000.00,5600.00,75600.00
Cà phê muối xứ Huế,3,28000,10,84000.00,8400.00,92400.00
Trà đào cam sả,1,40000,10,40000.00,4000.00,44000.00
Nước suối đóng chai,5,8000,0,40000.00,0.00,40000.00
TỔNG TIỀN TRƯỚC THUẾ (SUBTOTAL),,,,234000.00,,
TỔNG THUẾ VAT (TOTAL VAT),,,,,18000.00,
TỔNG THANH TOÁN (GRAND TOTAL),,,,,,25200.00
```

---

## 8. Tiêu chuẩn Kiểm thử & Định nghĩa Hoàn thành (DoD v2.0)

1. **Kiểm thử đơn vị & Kiểm thử tích hợp:** Đầy đủ test cases cho cả 4 trụ cột mới (Dynamic mapping, custom scale/rounding, discrepancy strategies, và diverse outputs).
2. **Code Coverage:** Đảm bảo **Instruction Coverage $\ge 90\%$** và **Branch Coverage $\ge 80\%$** qua JaCoCo.
3. **Immutability & Thread-Safety:** 100% Stateless Services và Immutable Models/Configs.
4. **Backward Compatibility:** Các API v1 cũ (`createDefault()`, `process(Path, Path)`) vẫn hoạt động trơn tru không gây breaking change cho code đã viết.
