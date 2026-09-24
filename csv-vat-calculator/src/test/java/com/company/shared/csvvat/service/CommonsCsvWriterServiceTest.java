package com.company.shared.csvvat.service;

import com.company.shared.csvvat.config.CsvColumnMapping;
import com.company.shared.csvvat.model.CalculatedItem;
import com.company.shared.csvvat.model.OrderCalculationResult;
import com.company.shared.csvvat.model.OrderItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Kiểm thử CommonsCsvWriterService - Bộ xuất file CSV đơn hàng v2.0")
class CommonsCsvWriterServiceTest {

    private CommonsCsvWriterService writerService;

    @BeforeEach
    void setUp() {
        writerService = new CommonsCsvWriterService();
    }

    private OrderCalculationResult createSampleResult() {
        OrderItem item1 = new OrderItem(2, "Bánh mì xíu mại trứng muối", new BigDecimal("2"), new BigDecimal("35000.00"), new BigDecimal("8.00"), new BigDecimal("70000.00"));
        CalculatedItem calc1 = new CalculatedItem(item1, new BigDecimal("70000.00"), new BigDecimal("5600.00"), new BigDecimal("75600.00"));

        OrderItem item2 = new OrderItem(3, "Cà phê, sữa đá đặc biệt", new BigDecimal("3"), new BigDecimal("28000.00"), new BigDecimal("10.00"), new BigDecimal("84000.00"));
        CalculatedItem calc2 = new CalculatedItem(item2, new BigDecimal("84000.00"), new BigDecimal("8400.00"), new BigDecimal("92400.00"));

        return new OrderCalculationResult(
                "ORD-2026-001",
                List.of(calc1, calc2),
                new BigDecimal("154000.00"),
                new BigDecimal("14000.00"),
                new BigDecimal("168000.00"),
                new BigDecimal("5"),
                2
        );
    }

    @Nested
    @DisplayName("1. Xuất file chuẩn UTF-8 và tuần tự hóa chuỗi (writeToString)")
    class DefaultOutputTests {

        @Test
        @DisplayName("writeToString tuần tự hóa thành chuỗi CSV đúng format và chứa 3 dòng tổng kết")
        void writeToString_standardOrder_shouldGenerateEnrichedCsvString() {
            OrderCalculationResult result = createSampleResult();

            String csv = writerService.writeToString(result);

            assertThat(csv).isNotNull();
            String[] lines = csv.split("\\R");
            // 1 header + 2 data rows + 3 summary rows = 6 rows
            assertThat(lines).hasSize(6);

            // Row 0: Header
            assertThat(lines[0]).isEqualTo("Item Name,Quantity,Unit Price,VAT (%),Line Subtotal,Item VAT,Line Total");

            // Row 1: Data item 1
            assertThat(lines[1]).isEqualTo("Bánh mì xíu mại trứng muối,2,35000.00,8.00,70000.00,5600.00,75600.00");

            // Row 2: Data item 2 with comma (auto-quoted)
            assertThat(lines[2]).isEqualTo("\"Cà phê, sữa đá đặc biệt\",3,28000.00,10.00,84000.00,8400.00,92400.00");

            // Summary rows
            assertThat(lines[3]).isEqualTo("TỔNG TIỀN TRƯỚC THUẾ (SUBTOTAL),,,,154000.00,,");
            assertThat(lines[4]).isEqualTo("TỔNG THUẾ VAT (TOTAL VAT),,,,,14000.00,");
            assertThat(lines[5]).isEqualTo("TỔNG THANH TOÁN (GRAND TOTAL),,,,,,168000.00");
        }

        @Test
        @DisplayName("Xuất ra OutputStream hỗ trợ UTF-8 chuẩn có BOM mặc định cho Excel")
        void write_toOutputStream_shouldProduceUtf8BytesWithBom() {
            OrderCalculationResult result = createSampleResult();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            writerService.write(result, baos);

            byte[] bytes = baos.toByteArray();
            assertThat(bytes[0] & 0xFF).isEqualTo(0xEF);
            assertThat(bytes[1] & 0xFF).isEqualTo(0xBB);
            assertThat(bytes[2] & 0xFF).isEqualTo(0xBF);

            String csv = baos.toString(StandardCharsets.UTF_8);
            assertThat(csv).contains("Bánh mì xíu mại trứng muối");
            assertThat(csv).contains("TỔNG THANH TOÁN (GRAND TOTAL)");
        }

        @Test
        @DisplayName("Xuất ra OutputStream khi tắt UTF-8 BOM")
        void write_toOutputStream_disabledBom_shouldNotHaveBomPrefix() {
            CommonsCsvWriterService noBomWriter = new CommonsCsvWriterService(CsvColumnMapping.defaultMapping(), false);
            assertThat(noBomWriter.isWriteUtf8Bom()).isFalse();

            OrderCalculationResult result = createSampleResult();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            noBomWriter.write(result, baos);

            byte[] bytes = baos.toByteArray();
            assertThat(bytes[0] & 0xFF).isNotEqualTo(0xEF);
        }

        @Test
        @DisplayName("Xuất ra File Path vật lý")
        void write_toPath_shouldCreateFileOnDisk(@TempDir Path tempDir) throws IOException {
            Path outputPath = tempDir.resolve("output_enriched.csv");
            OrderCalculationResult result = createSampleResult();

            writerService.write(result, outputPath);

            assertThat(Files.exists(outputPath)).isTrue();
            List<String> lines = Files.readAllLines(outputPath, StandardCharsets.UTF_8);
            assertThat(lines).hasSize(6);
            assertThat(lines.get(0)).contains("Line Subtotal");
        }
    }

    @Nested
    @DisplayName("2. Tùy biến Header qua CsvColumnMapping")
    class CustomMappingOutputTests {

        @Test
        @DisplayName("Đồng bộ tên cột xuất ra theo CsvColumnMapping đầu vào")
        void writeToString_customColumnMapping_shouldUseCustomHeaderNames() {
            CsvColumnMapping mapping = CsvColumnMapping.builder()
                    .itemNameColumn("Tên mặt hàng")
                    .quantityColumn("SL")
                    .unitPriceColumn("Đơn giá niêm yết")
                    .vatPercentageColumn("Thuế (%)")
                    .lineTotalColumn("Tổng thành tiền")
                    .build();

            CommonsCsvWriterService customWriter = new CommonsCsvWriterService(mapping);
            assertThat(customWriter.getColumnMapping()).isSameAs(mapping);

            OrderCalculationResult result = createSampleResult();
            String csv = customWriter.writeToString(result);

            String headerLine = csv.split("\\R")[0];
            assertThat(headerLine).isEqualTo("Tên mặt hàng,SL,Đơn giá niêm yết,Thuế (%),Line Subtotal,Item VAT,Tổng thành tiền");
        }
    }

    @Nested
    @DisplayName("3. Kiểm tra các ngoại lệ và ràng buộc tham số")
    class ParameterValidationTests {

        @Test
        @DisplayName("Truyền null result hoặc writer/stream: Ném NullPointerException")
        void write_nullArguments_shouldThrowNullPointerException() {
            OrderCalculationResult result = createSampleResult();

            assertThatThrownBy(() -> writerService.write(null, new StringWriter()))
                    .isInstanceOf(NullPointerException.class);

            assertThatThrownBy(() -> writerService.write(result, (StringWriter) null))
                    .isInstanceOf(NullPointerException.class);

            assertThatThrownBy(() -> writerService.write(result, (ByteArrayOutputStream) null))
                    .isInstanceOf(NullPointerException.class);

            assertThatThrownBy(() -> writerService.writeToString(null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("Khởi tạo với null columnMapping: fallback về defaultMapping")
        void constructor_nullMapping_shouldDefault() {
            CommonsCsvWriterService service = new CommonsCsvWriterService(null);
            assertThat(service.getColumnMapping()).isNotNull();
            assertThat(service.getColumnMapping()).isEqualTo(CsvColumnMapping.defaultMapping());
        }

        @Test
        @DisplayName("Tùy biến với các cột rỗng (blank) trong mapping: fallback về default header tương ứng")
        void write_blankColumnsInMapping_shouldFallbackToDefaultHeaders() {
            CsvColumnMapping blankMapping = CsvColumnMapping.builder()
                    .itemNameColumn("   ")
                    .quantityColumn("")
                    .unitPriceColumn(null)
                    .vatPercentageColumn("  ")
                    .lineTotalColumn("   ")
                    .build();

            CommonsCsvWriterService customWriter = new CommonsCsvWriterService(blankMapping);
            String csv = customWriter.writeToString(createSampleResult());
            String header = csv.split("\\R")[0];
            assertThat(header).isEqualTo("Item Name,Quantity,Unit Price,VAT (%),Line Subtotal,Item VAT,Line Total");
        }

        @Test
        @DisplayName("Xử lý lỗi IOException khi ghi ra Writer hoặc OutputStream: Ném CsvVatException")
        void write_ioException_shouldThrowCsvVatException() {
            OrderCalculationResult result = createSampleResult();

            java.io.Writer brokenWriter = new java.io.Writer() {
                @Override public void write(char[] cbuf, int off, int len) throws IOException { throw new IOException("Disk failure"); }
                @Override public void flush() throws IOException { throw new IOException("Disk failure"); }
                @Override public void close() throws IOException {}
            };

            assertThatThrownBy(() -> writerService.write(result, brokenWriter))
                    .isInstanceOf(com.company.shared.csvvat.exception.CsvVatException.class)
                    .hasMessageContaining("Failed to serialize CSV records");

            java.io.OutputStream brokenOs = new java.io.OutputStream() {
                @Override public void write(int b) throws IOException { throw new IOException("Network broken"); }
            };

            assertThatThrownBy(() -> writerService.write(result, brokenOs))
                    .isInstanceOf(com.company.shared.csvvat.exception.CsvVatException.class)
                    .hasMessageContaining("Network broken");
        }
    }
}
