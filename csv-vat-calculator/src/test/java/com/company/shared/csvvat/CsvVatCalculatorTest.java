package com.company.shared.csvvat;

import com.company.shared.csvvat.config.CalculatorConfig;
import com.company.shared.csvvat.config.CsvColumnMapping;
import com.company.shared.csvvat.config.LineTotalDiscrepancyStrategy;
import com.company.shared.csvvat.exception.CsvVatException;
import com.company.shared.csvvat.model.OrderCalculationResult;
import com.company.shared.csvvat.service.CsvReaderService;
import com.company.shared.csvvat.service.CsvWriterService;
import com.company.shared.csvvat.service.VatCalculator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Kiểm thử CsvVatCalculator - Facade và Fluent Builder v2.0")
class CsvVatCalculatorTest {

    private static final String SAMPLE_CSV = """
            Item Name,Quantity,Unit Price,VAT (%)
            Bánh mì xíu mại,2,35000,8
            Cà phê muối,3,28000,10
            """;

    private CsvVatCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = CsvVatCalculator.createDefault();
    }

    @Nested
    @DisplayName("1. Fluent Builder & Cấu hình Facade")
    class BuilderAndConfigTests {

        @Test
        @DisplayName("Khởi tạo mặc định với createDefault()")
        void createDefault_shouldHaveDefaultConfig() {
            CsvVatCalculator defaultCalc = CsvVatCalculator.createDefault();
            assertThat(defaultCalc.getConfig()).isEqualTo(CalculatorConfig.defaultConfig());
            assertThat(defaultCalc.getReaderService()).isNotNull();
            assertThat(defaultCalc.getVatCalculator()).isNotNull();
            assertThat(defaultCalc.getWriterService()).isNotNull();
        }

        @Test
        @DisplayName("Khởi tạo với create(config)")
        void create_withConfig_shouldApplyConfig() {
            CalculatorConfig config = CalculatorConfig.builder()
                    .currencyScale(0)
                    .roundingMode(RoundingMode.HALF_UP)
                    .build();

            CsvVatCalculator calc = CsvVatCalculator.create(config);
            assertThat(calc.getConfig().currencyScale()).isEqualTo(0);
        }

        @Test
        @DisplayName("Fluent Builder tùy biến toàn diện tất cả các thuộc tính")
        void builder_fullCustomization_shouldApplyAllSettings() {
            CsvColumnMapping mapping = CsvColumnMapping.builder()
                    .itemNameColumn("Ten SP")
                    .quantityColumn("SL")
                    .unitPriceColumn("Gia")
                    .vatPercentageColumn("Thue")
                    .lineTotalColumn("Tong")
                    .build();

            CsvVatCalculator customCalc = CsvVatCalculator.builder()
                    .columnMapping(mapping)
                    .discrepancyStrategy(LineTotalDiscrepancyStrategy.FAIL_ON_MISMATCH)
                    .discrepancyTolerance(new BigDecimal("0.05"))
                    .currencyScale(0)
                    .roundingMode(RoundingMode.HALF_EVEN)
                    .build();

            assertThat(customCalc.getConfig().columnMapping()).isEqualTo(mapping);
            assertThat(customCalc.getConfig().discrepancyStrategy()).isEqualTo(LineTotalDiscrepancyStrategy.FAIL_ON_MISMATCH);
            assertThat(customCalc.getConfig().discrepancyTolerance()).isEqualByComparingTo("0.05");
            assertThat(customCalc.getConfig().currencyScale()).isEqualTo(0);
            assertThat(customCalc.getConfig().roundingMode()).isEqualTo(RoundingMode.HALF_EVEN);
        }

        @Test
        @DisplayName("Fluent Builder hỗ trợ inject custom services")
        void builder_withCustomServices_shouldUseInjectedServices() {
            CsvReaderService mockReader = Mockito.mock(CsvReaderService.class);
            VatCalculator mockCalculator = Mockito.mock(VatCalculator.class);
            CsvWriterService mockWriter = Mockito.mock(CsvWriterService.class);

            CsvVatCalculator customCalc = CsvVatCalculator.builder()
                    .readerService(mockReader)
                    .vatCalculator(mockCalculator)
                    .writerService(mockWriter)
                    .build();

            assertThat(customCalc.getReaderService()).isSameAs(mockReader);
            assertThat(customCalc.getVatCalculator()).isSameAs(mockCalculator);
            assertThat(customCalc.getWriterService()).isSameAs(mockWriter);
        }
    }

    @Nested
    @DisplayName("2. Các chế độ xuất dữ liệu linh hoạt (Diverse Output Capabilities)")
    class DiverseOutputTests {

        @Test
        @DisplayName("Chế độ 1: Xuất ra file CSV vật lý (process Path, Path)")
        void process_fromPathToPath_shouldProduceEnrichedCsvFile(@TempDir Path tempDir) throws IOException {
            Path inputPath = tempDir.resolve("input.csv");
            Path outputPath = tempDir.resolve("output_enriched.csv");

            Files.writeString(inputPath, SAMPLE_CSV, StandardCharsets.UTF_8);

            OrderCalculationResult result = calculator.process(inputPath, outputPath);

            assertThat(result).isNotNull();
            assertThat(result.subtotal()).isEqualByComparingTo("154000.00");
            assertThat(result.totalVat()).isEqualByComparingTo("14000.00");
            assertThat(result.grandTotal()).isEqualByComparingTo("168000.00");

            assertThat(Files.exists(outputPath)).isTrue();
            String outputContent = Files.readString(outputPath, StandardCharsets.UTF_8);
            assertThat(outputContent).contains("TỔNG THANH TOÁN (GRAND TOTAL)");
        }

        @Test
        @DisplayName("Chế độ 2: Stream I/O (process InputStream, OutputStream và Reader, Writer)")
        void process_streamAndReaderWriter_shouldWriteEnrichedOutput() {
            // Stream I/O
            ByteArrayInputStream inStream = new ByteArrayInputStream(SAMPLE_CSV.getBytes(StandardCharsets.UTF_8));
            ByteArrayOutputStream outStream = new ByteArrayOutputStream();

            OrderCalculationResult result1 = calculator.process(inStream, outStream);
            assertThat(result1.grandTotal()).isEqualByComparingTo("168000.00");
            assertThat(outStream.toString(StandardCharsets.UTF_8)).contains("Line Subtotal");

            // Reader & Writer I/O
            StringReader reader = new StringReader(SAMPLE_CSV);
            StringWriter writer = new StringWriter();

            OrderCalculationResult result2 = calculator.process(reader, writer);
            assertThat(result2.grandTotal()).isEqualByComparingTo("168000.00");
            assertThat(writer.toString()).contains("Line Subtotal");
        }

        @Test
        @DisplayName("Chế độ 3: Xuất ra chuỗi In-Memory String CSV (processToString)")
        void processToString_fromPathInputStreamAndReader_shouldReturnEnrichedString(@TempDir Path tempDir) throws IOException {
            Path inputPath = tempDir.resolve("input_string.csv");
            Files.writeString(inputPath, SAMPLE_CSV, StandardCharsets.UTF_8);

            // From Path
            String csvFromPath = calculator.processToString(inputPath);
            assertThat(csvFromPath).contains("Bánh mì xíu mại").contains("168000.00");

            // From InputStream
            ByteArrayInputStream inStream = new ByteArrayInputStream(SAMPLE_CSV.getBytes(StandardCharsets.UTF_8));
            String csvFromStream = calculator.processToString(inStream);
            assertThat(csvFromStream).isEqualTo(csvFromPath);

            // From Reader
            String csvFromReader = calculator.processToString(new StringReader(SAMPLE_CSV));
            assertThat(csvFromReader).isEqualTo(csvFromPath);
        }

        @Test
        @DisplayName("Chế độ 4: Chỉ trả về POJO/Record kết quả (processToResult)")
        void processToResult_fromPathInputStreamAndReader_shouldReturnPureRecord(@TempDir Path tempDir) throws IOException {
            Path inputPath = tempDir.resolve("input_pojo.csv");
            Files.writeString(inputPath, SAMPLE_CSV, StandardCharsets.UTF_8);

            // From Path
            OrderCalculationResult resultFromPath = calculator.processToResult(inputPath);
            assertThat(resultFromPath.totalItemsCount()).isEqualTo(2);
            assertThat(resultFromPath.grandTotal()).isEqualByComparingTo("168000.00");

            // From InputStream
            ByteArrayInputStream inStream = new ByteArrayInputStream(SAMPLE_CSV.getBytes(StandardCharsets.UTF_8));
            OrderCalculationResult resultFromStream = calculator.processToResult(inStream);
            assertThat(resultFromStream.grandTotal()).isEqualByComparingTo("168000.00");

            // From Reader
            OrderCalculationResult resultFromReader = calculator.processToResult(new StringReader(SAMPLE_CSV));
            assertThat(resultFromReader.grandTotal()).isEqualByComparingTo("168000.00");
        }
    }

    @Nested
    @DisplayName("3. Quản lý an toàn tài nguyên I/O và kiểm tra ngoại lệ")
    class ExceptionAndResourceSafetyTests {

        @Test
        @DisplayName("Tham số null: Ném NullPointerException cho toàn bộ các hàm Facade")
        void nullArguments_shouldThrowNullPointerException() {
            assertThatThrownBy(() -> calculator.process((Path) null, Path.of("out.csv")))
                    .isInstanceOf(NullPointerException.class);

            assertThatThrownBy(() -> calculator.process(Path.of("in.csv"), (Path) null))
                    .isInstanceOf(NullPointerException.class);

            assertThatThrownBy(() -> calculator.process((InputStream) null, new ByteArrayOutputStream()))
                    .isInstanceOf(NullPointerException.class);

            assertThatThrownBy(() -> calculator.process(new ByteArrayInputStream(new byte[0]), (OutputStream) null))
                    .isInstanceOf(NullPointerException.class);

            assertThatThrownBy(() -> calculator.process((java.io.Reader) null, new StringWriter()))
                    .isInstanceOf(NullPointerException.class);

            assertThatThrownBy(() -> calculator.process(new StringReader(""), (java.io.Writer) null))
                    .isInstanceOf(NullPointerException.class);

            assertThatThrownBy(() -> calculator.processToString((Path) null))
                    .isInstanceOf(NullPointerException.class);

            assertThatThrownBy(() -> calculator.processToString((InputStream) null))
                    .isInstanceOf(NullPointerException.class);

            assertThatThrownBy(() -> calculator.processToString((java.io.Reader) null))
                    .isInstanceOf(NullPointerException.class);

            assertThatThrownBy(() -> calculator.processToResult((Path) null))
                    .isInstanceOf(NullPointerException.class);

            assertThatThrownBy(() -> calculator.processToResult((InputStream) null))
                    .isInstanceOf(NullPointerException.class);

            assertThatThrownBy(() -> calculator.processToResult((java.io.Reader) null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("File không tồn tại: Ném CsvVatException cho process, processToString và processToResult")
        void nonExistentFile_shouldThrowCsvVatException() {
            Path nonExistent = Path.of("missing_input_file.csv");

            assertThatThrownBy(() -> calculator.process(nonExistent, Path.of("out.csv")))
                    .isInstanceOf(CsvVatException.class)
                    .hasMessageContaining("Failed to process order CSV files");

            assertThatThrownBy(() -> calculator.processToString(nonExistent))
                    .isInstanceOf(CsvVatException.class)
                    .hasMessageContaining("Failed to read CSV from path");

            assertThatThrownBy(() -> calculator.processToResult(nonExistent))
                    .isInstanceOf(CsvVatException.class)
                    .hasMessageContaining("Failed to read CSV from path");
        }

        @Test
        @DisplayName("process(Path, Path): Bắt lỗi khi cleanup file thất bại (IOException ignored)")
        void process_cleanupFailure_shouldIgnoreCleanupErrorAndThrowException(@TempDir Path tempDir) throws IOException {
            Path inputPath = tempDir.resolve("valid_input.csv");
            Files.writeString(inputPath, SAMPLE_CSV, StandardCharsets.UTF_8);

            // Tạo thư mục không rỗng làm outputPath để Files.deleteIfExists(outputPath) ném IOException trong khối catch
            Path nonDeletableDir = tempDir.resolve("non_deletable_dir");
            Files.createDirectory(nonDeletableDir);
            Files.writeString(nonDeletableDir.resolve("child.txt"), "content");

            assertThatThrownBy(() -> calculator.process(inputPath, nonDeletableDir))
                    .isInstanceOf(CsvVatException.class)
                    .hasMessageContaining("Failed to process order CSV files");
        }

        @Test
        @DisplayName("Builder: Cấu hình writeUtf8Bom tùy biến")
        void builder_writeUtf8Bom() {
            CsvVatCalculator calcWithoutBom = CsvVatCalculator.builder()
                    .writeUtf8Bom(false)
                    .build();
            assertThat(calcWithoutBom.getConfig().writeUtf8Bom()).isFalse();

            CsvVatCalculator calcWithBom = CsvVatCalculator.builder()
                    .writeUtf8Bom(true)
                    .build();
            assertThat(calcWithBom.getConfig().writeUtf8Bom()).isTrue();
        }
    }
}
