package com.company.shared.csvvat;

import com.company.shared.csvvat.config.CsvColumnMapping;
import com.company.shared.csvvat.config.LineTotalDiscrepancyStrategy;
import com.company.shared.csvvat.model.OrderCalculationResult;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Demo runner for csv-vat-calculator v2.0.
 * <p>
 * Demonstrates:
 * <ul>
 *     <li>Dynamic column metadata mapping for unconventional CSV headers.</li>
 *     <li>Direct in-memory String output via {@link CsvVatCalculator#processToString(Path)}.</li>
 * </ul>
 */
public class MainV2 {

    public static void main(String[] args) {
        System.out.println("================================================================================");
        System.out.println("  CSV VAT CALCULATOR v2.0 - DYNAMIC METADATA & IN-MEMORY STRING DEMO");
        System.out.println("================================================================================\n");

        Path inputPath = Path.of("input_custom.csv");
        Path outputPath = Path.of("output_custom.csv");

        // 1. Ánh xạ các cột dị: Ma_SP, SL, Gia_Ban, Thue, Tong_Cong
        CsvColumnMapping customMapping = CsvColumnMapping.builder()
                .itemNameColumn("Ma_SP")
                .quantityColumn("SL")
                .unitPriceColumn("Gia_Ban")
                .vatPercentageColumn("Thue")
                .lineTotalColumn("Tong_Cong")
                .build();

        // 2. Khởi tạo Facade với CsvVatCalculator.builder()
        CsvVatCalculator calculator = CsvVatCalculator.builder()
                .columnMapping(customMapping)
                .currencyScale(0) // Tiền Đồng (VND) không lấy số lẻ
                .roundingMode(RoundingMode.HALF_UP)
                .discrepancyStrategy(LineTotalDiscrepancyStrategy.WARN_AND_RECALCULATE)
                .discrepancyTolerance(new BigDecimal("0.01"))
                .build();

        System.out.printf(">> Đang đọc file đầu vào : %s%n", inputPath.toAbsolutePath());
        System.out.println(">> Cấu hình ánh xạ cột   : " + customMapping);

        // 3. Xử lý và xuất trực tiếp ra file CSV output_custom.csv
        OrderCalculationResult result = calculator.process(inputPath, outputPath);
        System.out.printf(">> Đã xuất file CSV thành công tại: %s%n%n", outputPath.toAbsolutePath());

        // 4. In nội dung file CSV output ra Console để dễ kiểm tra
        System.out.println("--- [ NỘI DUNG FILE CSV ĐẦU RA (output_custom.csv) ] ---");
        try {
            String content = java.nio.file.Files.readString(outputPath, java.nio.charset.StandardCharsets.UTF_8);
            System.out.println(content.replace("\uFEFF", ""));
        } catch (java.io.IOException e) {
            System.err.println("Không thể đọc file output: " + e.getMessage());
        }

        // 5. In tổng hợp số liệu
        System.out.println("--- [ TỔNG HỢP KẾ TOÁN ] ---");
        System.out.printf("• Tổng tiền trước thuế (Subtotal) : %,d VND%n", result.subtotal().longValue());
        System.out.printf("• Tổng tiền thuế VAT (Total VAT)  : %,d VND%n", result.totalVat().longValue());
        System.out.printf("• Tổng thanh toán (Grand Total)   : %,d VND%n", result.grandTotal().longValue());
        System.out.printf("• Tổng số mặt hàng                : %d dòng%n", result.totalItemsCount());
        System.out.printf("• Tổng số lượng sản phẩm          : %s%n", result.totalQuantity());

        System.out.println("\n================================================================================");
        System.out.println("  DEMO THÀNH CÔNG RỰC RỠ! FILE ĐÃ ĐƯỢC LƯU VÀO ĐĨA");
        System.out.println("================================================================================");
    }
}
