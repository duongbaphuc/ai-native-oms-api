package com.company.shared.csvvat;

import com.company.shared.csvvat.config.CsvColumnMapping;
import com.company.shared.csvvat.config.LineTotalDiscrepancyStrategy;
import com.company.shared.csvvat.exception.LineTotalDiscrepancyException;
import com.company.shared.csvvat.model.OrderCalculationResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Kiểm thử tích hợp CsvVatCalculator v2.0 - 4 Trụ cột kiến trúc")
class CsvVatCalculatorV2IntegrationTest {

    private static final Path RESOURCE_DIR = Path.of("src/test/resources/sample_orders");

    @Nested
    @DisplayName("Trụ cột 1: Dynamic Metadata Mapping (Ánh xạ cột động tùy biến)")
    class DynamicMetadataMappingIntegrationTests {

        @Test
        @DisplayName("Đọc file CSV với format cột ERP tùy biến (SKU, QTY, RATE, TAX_RATE, TOTAL)")
        void process_customErpHeaders_shouldParseAndProduceSynchronizedOutput(@TempDir Path tempDir) throws IOException {
            Path inputPath = RESOURCE_DIR.resolve("custom_erp_orders.csv");
            Path outputPath = tempDir.resolve("custom_erp_output.csv");

            CsvColumnMapping erpMapping = CsvColumnMapping.builder()
                    .itemNameColumn("SKU")
                    .quantityColumn("QTY")
                    .unitPriceColumn("RATE")
                    .vatPercentageColumn("TAX_RATE")
                    .lineTotalColumn("TOTAL")
                    .build();

            CsvVatCalculator calculator = CsvVatCalculator.builder()
                    .columnMapping(erpMapping)
                    .currencyScale(0)
                    .roundingMode(RoundingMode.HALF_UP)
                    .build();

            OrderCalculationResult result = calculator.process(inputPath, outputPath);

            // Kiểm tra kết quả tổng thể đơn hàng
            assertThat(result.totalItemsCount()).isEqualTo(3);
            assertThat(result.totalQuantity()).isEqualByComparingTo("7");
            assertThat(result.subtotal()).isEqualByComparingTo("320000");
            assertThat(result.totalVat()).isEqualByComparingTo("18000");
            assertThat(result.grandTotal()).isEqualByComparingTo("338000");

            // Kiểm tra file đầu ra CSV và xác nhận có byte UTF-8 BOM (0xEF, 0xBB, 0xBF) cho Excel
            assertThat(Files.exists(outputPath)).isTrue();
            byte[] fileBytes = Files.readAllBytes(outputPath);
            assertThat(fileBytes[0] & 0xFF).isEqualTo(0xEF);
            assertThat(fileBytes[1] & 0xFF).isEqualTo(0xBB);
            assertThat(fileBytes[2] & 0xFF).isEqualTo(0xBF);

            List<String> outputLines = Files.readAllLines(outputPath, StandardCharsets.UTF_8);

            // Header đầu ra được đồng bộ với tên cột cấu hình
            assertThat(outputLines.get(0).replace("\uFEFF", "")).isEqualTo("SKU,QTY,RATE,TAX_RATE,Line Subtotal,Item VAT,TOTAL");

            // Dòng dữ liệu
            assertThat(outputLines.get(1)).isEqualTo("PROD-001,2,50000,10,100000,10000,110000");
            assertThat(outputLines.get(2)).isEqualTo("PROD-002,4,25000,8,100000,8000,108000");
            assertThat(outputLines.get(3)).isEqualTo("PROD-003,1,120000,0,120000,0,120000");

            // 3 dòng tổng kết kế toán
            assertThat(outputLines.get(4)).isEqualTo("TỔNG TIỀN TRƯỚC THUẾ (SUBTOTAL),,,,320000,,");
            assertThat(outputLines.get(5)).isEqualTo("TỔNG THUẾ VAT (TOTAL VAT),,,,,18000,");
            assertThat(outputLines.get(6)).isEqualTo("TỔNG THANH TOÁN (GRAND TOTAL),,,,,,338000");
        }
    }

    @Nested
    @DisplayName("Trụ cột 2 & 3: Chiến lược kiểm tra chéo thành tiền (Line Total Discrepancy Strategy)")
    class DiscrepancyStrategyIntegrationTests {

        @Test
        @DisplayName("Chiến lược FAIL_ON_MISMATCH: Ném LineTotalDiscrepancyException khi phát hiện lệch tiền")
        void process_discrepancyFailFast_shouldThrowLineTotalDiscrepancyException(@TempDir Path tempDir) {
            Path inputPath = RESOURCE_DIR.resolve("discrepancy_orders.csv");
            Path outputPath = tempDir.resolve("discrepancy_out.csv");

            CsvColumnMapping erpMapping = CsvColumnMapping.builder()
                    .itemNameColumn("SKU")
                    .quantityColumn("QTY")
                    .unitPriceColumn("RATE")
                    .vatPercentageColumn("TAX_RATE")
                    .lineTotalColumn("TOTAL")
                    .build();

            CsvVatCalculator calculator = CsvVatCalculator.builder()
                    .columnMapping(erpMapping)
                    .discrepancyStrategy(LineTotalDiscrepancyStrategy.FAIL_ON_MISMATCH)
                    .discrepancyTolerance(new BigDecimal("0.01"))
                    .currencyScale(0)
                    .build();

            assertThatThrownBy(() -> calculator.process(inputPath, outputPath))
                    .isInstanceOf(LineTotalDiscrepancyException.class)
                    .satisfies(e -> {
                        LineTotalDiscrepancyException ex = (LineTotalDiscrepancyException) e;
                        assertThat(ex.getRowNumber()).isEqualTo(2);
                        assertThat(ex.getColumnName()).isEqualTo("TOTAL");
                        assertThat(ex.getCalculatedTotal()).isEqualByComparingTo("100000");
                        assertThat(ex.getInputTotal()).isEqualByComparingTo("200000");
                        assertThat(ex.getDifference()).isEqualByComparingTo("100000");
                    });

            // Đảm bảo không tạo file đầu ra khi bị dừng khẩn cấp (Fail-fast)
            assertThat(Files.exists(outputPath)).isFalse();
        }

        @Test
        @DisplayName("Chiến lược WARN_AND_RECALCULATE: Tự động dùng giá trị tự tính (100.000) và hoàn thành đơn hàng")
        void process_discrepancyWarnAndRecalculate_shouldAutoRecoverAndProduceCorrectTotals(@TempDir Path tempDir) throws IOException {
            Path inputPath = RESOURCE_DIR.resolve("discrepancy_orders.csv");
            Path outputPath = tempDir.resolve("recalculated_out.csv");

            CsvColumnMapping erpMapping = CsvColumnMapping.builder()
                    .itemNameColumn("SKU")
                    .quantityColumn("QTY")
                    .unitPriceColumn("RATE")
                    .vatPercentageColumn("TAX_RATE")
                    .lineTotalColumn("TOTAL")
                    .build();

            CsvVatCalculator calculator = CsvVatCalculator.builder()
                    .columnMapping(erpMapping)
                    .discrepancyStrategy(LineTotalDiscrepancyStrategy.WARN_AND_RECALCULATE)
                    .currencyScale(0)
                    .build();

            OrderCalculationResult result = calculator.process(inputPath, outputPath);

            // Item 1 tự tính lại: 2 * 50.000 = 100.000, VAT 10% = 10.000
            // Item 2: 3 * 30.000 = 90.000, VAT 8% = 7.200
            // Subtotal: 190.000, VAT: 17.200, Grand Total: 207.200
            assertThat(result.subtotal()).isEqualByComparingTo("190000");
            assertThat(result.totalVat()).isEqualByComparingTo("17200");
            assertThat(result.grandTotal()).isEqualByComparingTo("207200");

            assertThat(Files.exists(outputPath)).isTrue();
            List<String> outputLines = Files.readAllLines(outputPath, StandardCharsets.UTF_8);
            assertThat(outputLines.get(1)).isEqualTo("PROD-001,2,50000,10,100000,10000,110000");
        }
    }

    @Nested
    @DisplayName("Trụ cột 4: Đa dạng hóa đầu ra (Diverse Outputs)")
    class DiverseOutputsIntegrationTests {

        @Test
        @DisplayName("processToString: Xuất dữ liệu enriched trực tiếp ra chuỗi String In-Memory")
        void processToString_shouldReturnEnrichedCsvString() {
            Path inputPath = RESOURCE_DIR.resolve("custom_erp_orders.csv");

            CsvColumnMapping erpMapping = CsvColumnMapping.builder()
                    .itemNameColumn("SKU")
                    .quantityColumn("QTY")
                    .unitPriceColumn("RATE")
                    .vatPercentageColumn("TAX_RATE")
                    .lineTotalColumn("TOTAL")
                    .build();

            CsvVatCalculator calculator = CsvVatCalculator.builder()
                    .columnMapping(erpMapping)
                    .currencyScale(0)
                    .build();

            String enrichedString = calculator.processToString(inputPath);

            assertThat(enrichedString).isNotNull();
            assertThat(enrichedString).contains("SKU,QTY,RATE,TAX_RATE,Line Subtotal,Item VAT,TOTAL");
            assertThat(enrichedString).contains("PROD-001,2,50000,10,100000,10000,110000");
            assertThat(enrichedString).contains("TỔNG THANH TOÁN (GRAND TOTAL),,,,,,338000");
        }

        @Test
        @DisplayName("processToResult: Chỉ tính toán và trả về đối tượng POJO/Record thuần túy")
        void processToResult_shouldComputePurePojoWithoutWritingCsv() {
            Path inputPath = RESOURCE_DIR.resolve("custom_erp_orders.csv");

            CsvColumnMapping erpMapping = CsvColumnMapping.builder()
                    .itemNameColumn("SKU")
                    .quantityColumn("QTY")
                    .unitPriceColumn("RATE")
                    .vatPercentageColumn("TAX_RATE")
                    .lineTotalColumn("TOTAL")
                    .build();

            CsvVatCalculator calculator = CsvVatCalculator.builder()
                    .columnMapping(erpMapping)
                    .currencyScale(2)
                    .build();

            OrderCalculationResult result = calculator.processToResult(inputPath);

            assertThat(result).isNotNull();
            assertThat(result.totalItemsCount()).isEqualTo(3);
            assertThat(result.totalQuantity()).isEqualByComparingTo("7");
            assertThat(result.subtotal()).isEqualByComparingTo("320000.00");
            assertThat(result.totalVat()).isEqualByComparingTo("18000.00");
            assertThat(result.grandTotal()).isEqualByComparingTo("338000.00");
        }
    }

    @Nested
    @DisplayName("Kịch bản E2E thực tế: Tiếng Việt có dấu với format từ SPEC §7.1 & §7.2")
    class SpecRealisticVietnameseE2ETests {

        @Test
        @DisplayName("Xử lý file vietnamese_custom_orders.csv khớp 100% với tài liệu đặc tả SPEC")
        void process_vietnameseSpecOrders_shouldMatchExactSpecRequirements(@TempDir Path tempDir) throws IOException {
            Path inputPath = RESOURCE_DIR.resolve("vietnamese_custom_orders.csv");
            Path outputPath = tempDir.resolve("spec_output_enriched.csv");

            CsvColumnMapping mapping = CsvColumnMapping.builder()
                    .itemNameColumn("Tên hàng hóa")
                    .quantityColumn("SL")
                    .unitPriceColumn("Giá bán lẻ")
                    .vatPercentageColumn("% VAT")
                    .lineTotalColumn("Thành tiền đầu vào")
                    .build();

            CsvVatCalculator calculator = CsvVatCalculator.builder()
                    .columnMapping(mapping)
                    .currencyScale(2)
                    .roundingMode(RoundingMode.HALF_UP)
                    .build();

            OrderCalculationResult result = calculator.process(inputPath, outputPath);

            // Khớp các con số đặc tả tại SPEC §7.2:
            // Subtotal: 234000.00, VAT: 18000.00, Grand Total: 252000.00
            assertThat(result.subtotal()).isEqualByComparingTo("234000.00");
            assertThat(result.totalVat()).isEqualByComparingTo("18000.00");
            assertThat(result.grandTotal()).isEqualByComparingTo("252000.00");
            assertThat(result.totalQuantity()).isEqualByComparingTo("11");
            assertThat(result.totalItemsCount()).isEqualTo(4);

            // Kiểm tra file có UTF-8 BOM
            byte[] fileBytes = Files.readAllBytes(outputPath);
            assertThat(fileBytes[0] & 0xFF).isEqualTo(0xEF);
            assertThat(fileBytes[1] & 0xFF).isEqualTo(0xBB);
            assertThat(fileBytes[2] & 0xFF).isEqualTo(0xBF);

            List<String> lines = Files.readAllLines(outputPath, StandardCharsets.UTF_8);
            assertThat(lines.get(0).replace("\uFEFF", "")).isEqualTo("Tên hàng hóa,SL,Giá bán lẻ,% VAT,Line Subtotal,Item VAT,Thành tiền đầu vào");
            assertThat(lines.get(1)).isEqualTo("Bánh mì xíu mại trứng muối,2,35000,8,70000.00,5600.00,75600.00");
            assertThat(lines.get(2)).isEqualTo("Cà phê muối xứ Huế,3,28000,10,84000.00,8400.00,92400.00");
            assertThat(lines.get(3)).isEqualTo("Trà đào cam sả,1,40000,10,40000.00,4000.00,44000.00");
            assertThat(lines.get(4)).isEqualTo("Nước suối đóng chai,5,8000,0,40000.00,0.00,40000.00");
            assertThat(lines.get(5)).isEqualTo("TỔNG TIỀN TRƯỚC THUẾ (SUBTOTAL),,,,234000.00,,");
            assertThat(lines.get(6)).isEqualTo("TỔNG THUẾ VAT (TOTAL VAT),,,,,18000.00,");
            assertThat(lines.get(7)).isEqualTo("TỔNG THANH TOÁN (GRAND TOTAL),,,,,,252000.00");
        }
    }
}
