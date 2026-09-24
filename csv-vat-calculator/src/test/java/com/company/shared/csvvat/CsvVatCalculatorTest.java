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
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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

    @Test
    @DisplayName("Khởi tạo bộ test CsvVatCalculatorTest")
    void contextLoads() {
        assertThat(new CsvVatCalculatorTest()).isNotNull();
        assertThat(new CsvVatCalculatorTest().new BuilderAndConfigTests()).isNotNull();
        assertThat(new CsvVatCalculatorTest().new DiverseOutputTests()).isNotNull();
        assertThat(new CsvVatCalculatorTest().new ExceptionAndResourceSafetyTests()).isNotNull();
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
            CsvReaderService mockReader = mock(CsvReaderService.class);
            VatCalculator mockCalculator = mock(VatCalculator.class);
            CsvWriterService mockWriter = mock(CsvWriterService.class);

            CsvVatCalculator customCalc = CsvVatCalculator.builder()
                    .readerService(mockReader)
                    .vatCalculator(mockCalculator)
                    .writerService(mockWriter)
                    .build();

            assertThat(customCalc.getReaderService()).isSameAs(mockReader);
            assertThat(customCalc.getVatCalculator()).isSameAs(mockCalculator);
            assertThat(customCalc.getWriterService()).isSameAs(mockWriter);
        }

        @Test
        @DisplayName("Fluent Builder inject riêng lẻ từng custom service để đảm bảo 100% branch fallback")
        void builder_withIndividualCustomServices_shouldFallbackToDefaults() {
            CsvReaderService mockReader = mock(CsvReaderService.class);
            VatCalculator mockCalculator = mock(VatCalculator.class);
            CsvWriterService mockWriter = mock(CsvWriterService.class);

            // Chỉ inject readerService
            CsvVatCalculator calcReaderOnly = CsvVatCalculator.builder()
                    .readerService(mockReader)
                    .build();
            assertThat(calcReaderOnly.getReaderService()).isSameAs(mockReader);
            assertThat(calcReaderOnly.getVatCalculator()).isNotNull();
            assertThat(calcReaderOnly.getWriterService()).isNotNull();

            // Chỉ inject vatCalculator
            CsvVatCalculator calcCalculatorOnly = CsvVatCalculator.builder()
                    .vatCalculator(mockCalculator)
                    .build();
            assertThat(calcCalculatorOnly.getReaderService()).isNotNull();
            assertThat(calcCalculatorOnly.getVatCalculator()).isSameAs(mockCalculator);
            assertThat(calcCalculatorOnly.getWriterService()).isNotNull();

            // Chỉ inject writerService
            CsvVatCalculator calcWriterOnly = CsvVatCalculator.builder()
                    .writerService(mockWriter)
                    .build();
            assertThat(calcWriterOnly.getReaderService()).isNotNull();
            assertThat(calcWriterOnly.getVatCalculator()).isNotNull();
            assertThat(calcWriterOnly.getWriterService()).isSameAs(mockWriter);
        }

        @Test
        @DisplayName("Constructor trực tiếp với các dependencies và kiểm tra getters")
        void constructor_withCustomDependencies_shouldExposeGetters() {
            CalculatorConfig config = CalculatorConfig.defaultConfig();
            CsvReaderService mockReader = mock(CsvReaderService.class);
            VatCalculator mockCalculator = mock(VatCalculator.class);
            CsvWriterService mockWriter = mock(CsvWriterService.class);

            CsvVatCalculator calc = new CsvVatCalculator(config, mockReader, mockCalculator, mockWriter);

            assertThat(calc.getConfig()).isSameAs(config);
            assertThat(calc.getReaderService()).isSameAs(mockReader);
            assertThat(calc.getVatCalculator()).isSameAs(mockCalculator);
            assertThat(calc.getWriterService()).isSameAs(mockWriter);
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

        @Test
        @DisplayName("Constructor & Factory chặn null cho các tham số khởi tạo")
        void constructorsAndFactory_nullArguments_shouldThrowNullPointerException() {
            CsvReaderService mockReader = mock(CsvReaderService.class);
            VatCalculator mockCalculator = mock(VatCalculator.class);
            CsvWriterService mockWriter = mock(CsvWriterService.class);
            CalculatorConfig validConfig = CalculatorConfig.defaultConfig();

            assertThatThrownBy(() -> new CsvVatCalculator((CalculatorConfig) null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("CalculatorConfig must not be null");

            assertThatThrownBy(() -> new CsvVatCalculator(null, mockReader, mockCalculator, mockWriter))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("CalculatorConfig must not be null");

            assertThatThrownBy(() -> new CsvVatCalculator(validConfig, null, mockCalculator, mockWriter))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("CsvReaderService must not be null");

            assertThatThrownBy(() -> new CsvVatCalculator(validConfig, mockReader, null, mockWriter))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("VatCalculator must not be null");

            assertThatThrownBy(() -> new CsvVatCalculator(validConfig, mockReader, mockCalculator, null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("CsvWriterService must not be null");

            assertThatThrownBy(() -> CsvVatCalculator.create(null))
                    .isInstanceOf(NullPointerException.class);
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
        @DisplayName("Chế độ 1b: Xuất ra file CSV vật lý từ File (process File, File)")
        void process_fromFileToFile_shouldProduceEnrichedCsvFile(@TempDir Path tempDir) throws IOException {
            File inputFile = tempDir.resolve("input_file.csv").toFile();
            File outputFile = tempDir.resolve("output_file.csv").toFile();

            Files.writeString(inputFile.toPath(), SAMPLE_CSV, StandardCharsets.UTF_8);

            OrderCalculationResult result = calculator.process(inputFile, outputFile);

            assertThat(result).isNotNull();
            assertThat(result.subtotal()).isEqualByComparingTo("154000.00");
            assertThat(result.totalVat()).isEqualByComparingTo("14000.00");
            assertThat(result.grandTotal()).isEqualByComparingTo("168000.00");

            assertThat(outputFile.exists()).isTrue();
            String outputContent = Files.readString(outputFile.toPath(), StandardCharsets.UTF_8);
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
        @DisplayName("Chế độ 3b: Xuất ra chuỗi In-Memory String CSV từ File (processToString(File))")
        void processToString_fromFile_shouldReturnEnrichedString(@TempDir Path tempDir) throws IOException {
            File inputFile = tempDir.resolve("input_string_file.csv").toFile();
            Files.writeString(inputFile.toPath(), SAMPLE_CSV, StandardCharsets.UTF_8);

            String csvFromFile = calculator.processToString(inputFile);
            assertThat(csvFromFile).contains("Bánh mì xíu mại").contains("168000.00");
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

        @Test
        @DisplayName("Chế độ 4b: Chỉ trả về POJO/Record kết quả từ File (processToResult(File))")
        void processToResult_fromFile_shouldReturnPureRecord(@TempDir Path tempDir) throws IOException {
            File inputFile = tempDir.resolve("input_pojo_file.csv").toFile();
            Files.writeString(inputFile.toPath(), SAMPLE_CSV, StandardCharsets.UTF_8);

            OrderCalculationResult result = calculator.processToResult(inputFile);
            assertThat(result.totalItemsCount()).isEqualTo(2);
            assertThat(result.grandTotal()).isEqualByComparingTo("168000.00");
        }
    }

    @Nested
    @DisplayName("3. Quản lý an toàn tài nguyên I/O và kiểm tra ngoại lệ")
    class ExceptionAndResourceSafetyTests {

        private Throwable execProcess(Path in, Path out) {
            try {
                calculator.process(in, out);
                return null;
            } catch (Throwable t) {
                return t;
            }
        }

        private Throwable execProcess(File in, File out) {
            try {
                calculator.process(in, out);
                return null;
            } catch (Throwable t) {
                return t;
            }
        }

        private Throwable execProcess(InputStream in, OutputStream out) {
            try {
                calculator.process(in, out);
                return null;
            } catch (Throwable t) {
                return t;
            }
        }

        private Throwable execProcess(Reader in, Writer out) {
            try {
                calculator.process(in, out);
                return null;
            } catch (Throwable t) {
                return t;
            }
        }

        private Throwable execProcessToString(Path in) {
            try {
                calculator.processToString(in);
                return null;
            } catch (Throwable t) {
                return t;
            }
        }

        private Throwable execProcessToString(File in) {
            try {
                calculator.processToString(in);
                return null;
            } catch (Throwable t) {
                return t;
            }
        }

        private Throwable execProcessToString(InputStream in) {
            try {
                calculator.processToString(in);
                return null;
            } catch (Throwable t) {
                return t;
            }
        }

        private Throwable execProcessToString(Reader in) {
            try {
                calculator.processToString(in);
                return null;
            } catch (Throwable t) {
                return t;
            }
        }

        private Throwable execProcessToResult(Path in) {
            try {
                calculator.processToResult(in);
                return null;
            } catch (Throwable t) {
                return t;
            }
        }

        private Throwable execProcessToResult(File in) {
            try {
                calculator.processToResult(in);
                return null;
            } catch (Throwable t) {
                return t;
            }
        }

        private Throwable execProcessToResult(InputStream in) {
            try {
                calculator.processToResult(in);
                return null;
            } catch (Throwable t) {
                return t;
            }
        }

        private Throwable execProcessToResult(Reader in) {
            try {
                calculator.processToResult(in);
                return null;
            } catch (Throwable t) {
                return t;
            }
        }

        @Test
        @DisplayName("Tham số null: Ném NullPointerException cho toàn bộ các hàm Facade")
        void nullArguments_shouldThrowNullPointerException(@TempDir Path tempDir) throws IOException {
            Path validIn = tempDir.resolve("valid_null_check_in.csv");
            Path validOut = tempDir.resolve("valid_null_check_out.csv");
            Files.writeString(validIn, SAMPLE_CSV, StandardCharsets.UTF_8);

            File validInFile = validIn.toFile();
            File validOutFile = validOut.toFile();

            // Thực thi hợp lệ cho tất cả 12 hàm helper
            assertThat(execProcess(validIn, validOut)).isNull();
            assertThat(execProcess(validInFile, validOutFile)).isNull();
            assertThat(execProcess(new ByteArrayInputStream(SAMPLE_CSV.getBytes(StandardCharsets.UTF_8)), new ByteArrayOutputStream())).isNull();
            assertThat(execProcess(new StringReader(SAMPLE_CSV), new StringWriter())).isNull();
            assertThat(execProcessToString(validIn)).isNull();
            assertThat(execProcessToString(validInFile)).isNull();
            assertThat(execProcessToString(new ByteArrayInputStream(SAMPLE_CSV.getBytes(StandardCharsets.UTF_8)))).isNull();
            assertThat(execProcessToString(new StringReader(SAMPLE_CSV))).isNull();
            assertThat(execProcessToResult(validIn)).isNull();
            assertThat(execProcessToResult(validInFile)).isNull();
            assertThat(execProcessToResult(new ByteArrayInputStream(SAMPLE_CSV.getBytes(StandardCharsets.UTF_8)))).isNull();
            assertThat(execProcessToResult(new StringReader(SAMPLE_CSV))).isNull();

            // Kiểm tra truyền tham số null
            // process(Path, Path)
            assertThat(execProcess((Path) null, validOut)).isInstanceOf(NullPointerException.class);
            assertThat(execProcess(validIn, (Path) null)).isInstanceOf(NullPointerException.class);

            // process(File, File)
            assertThat(execProcess((File) null, validOutFile)).isInstanceOf(NullPointerException.class);
            assertThat(execProcess(validInFile, (File) null)).isInstanceOf(NullPointerException.class);

            // process(InputStream, OutputStream)
            assertThat(execProcess((InputStream) null, new ByteArrayOutputStream())).isInstanceOf(NullPointerException.class);
            assertThat(execProcess(new ByteArrayInputStream(new byte[0]), (OutputStream) null)).isInstanceOf(NullPointerException.class);

            // process(Reader, Writer)
            assertThat(execProcess((Reader) null, new StringWriter())).isInstanceOf(NullPointerException.class);
            assertThat(execProcess(new StringReader(""), (Writer) null)).isInstanceOf(NullPointerException.class);

            // processToString(...)
            assertThat(execProcessToString((Path) null)).isInstanceOf(NullPointerException.class);
            assertThat(execProcessToString((File) null)).isInstanceOf(NullPointerException.class);
            assertThat(execProcessToString((InputStream) null)).isInstanceOf(NullPointerException.class);
            assertThat(execProcessToString((Reader) null)).isInstanceOf(NullPointerException.class);

            // processToResult(...)
            assertThat(execProcessToResult((Path) null)).isInstanceOf(NullPointerException.class);
            assertThat(execProcessToResult((File) null)).isInstanceOf(NullPointerException.class);
            assertThat(execProcessToResult((InputStream) null)).isInstanceOf(NullPointerException.class);
            assertThat(execProcessToResult((Reader) null)).isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("File không tồn tại: Ném CsvVatException cho process, processToString và processToResult (Path và File)")
        void nonExistentFile_shouldThrowCsvVatException(@TempDir Path tempDir) {
            Path nonExistentPath = Path.of("missing_input_file.csv");
            File nonExistentFile = tempDir.resolve("missing_file.csv").toFile();

            // Path overloads
            Throwable t1 = execProcess(nonExistentPath, Path.of("out.csv"));
            assertThat(t1).isInstanceOf(CsvVatException.class)
                    .hasMessageContaining("Failed to process order CSV files");

            Throwable t2 = execProcessToString(nonExistentPath);
            assertThat(t2).isInstanceOf(CsvVatException.class)
                    .hasMessageContaining("Failed to read CSV from path");

            Throwable t3 = execProcessToResult(nonExistentPath);
            assertThat(t3).isInstanceOf(CsvVatException.class)
                    .hasMessageContaining("Failed to read CSV from path");

            // File overloads
            Throwable t4 = execProcess(nonExistentFile, tempDir.resolve("out_file.csv").toFile());
            assertThat(t4).isInstanceOf(CsvVatException.class)
                    .hasMessageContaining("Failed to process order CSV files");

            Throwable t5 = execProcessToString(nonExistentFile);
            assertThat(t5).isInstanceOf(CsvVatException.class)
                    .hasMessageContaining("Failed to read CSV from path");

            Throwable t6 = execProcessToResult(nonExistentFile);
            assertThat(t6).isInstanceOf(CsvVatException.class)
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

            Throwable t = execProcess(inputPath, nonDeletableDir);
            assertThat(t).isInstanceOf(CsvVatException.class)
                    .hasMessageContaining("Failed to process order CSV files");
        }

        @Test
        @DisplayName("process(Path, Path) & process(File, File): Tái ném CsvVatException nguyên bản khi nội dung CSV không hợp lệ")
        void process_malformedCsvContent_shouldRethrowCsvVatException(@TempDir Path tempDir) throws IOException {
            // File CSV sai định dạng (header không khớp/thiếu) gây ra CsvVatException trong quá trình process(is, os)
            Path malformedPath = tempDir.resolve("malformed.csv");
            Files.writeString(malformedPath, "BadCol1,BadCol2\nVal1,Val2\n", StandardCharsets.UTF_8);
            Path outputPath = tempDir.resolve("malformed_out.csv");

            assertThatThrownBy(() -> calculator.process(malformedPath, outputPath))
                    .isInstanceOf(CsvVatException.class);
            assertThat(Files.exists(outputPath)).isFalse();

            // File overload
            File malformedFile = malformedPath.toFile();
            File outputFile = tempDir.resolve("malformed_out_file.csv").toFile();
            assertThatThrownBy(() -> calculator.process(malformedFile, outputFile))
                    .isInstanceOf(CsvVatException.class);
            assertThat(outputFile.exists()).isFalse();
        }

        @Test
        @DisplayName("Mockito InputStream ném IOException: Phải được đóng gói và ném thành CsvVatException")
        void mockInputStream_throwingIOException_shouldThrowCsvVatException() throws IOException {
            InputStream faultyInputStream = mock(InputStream.class);
            when(faultyInputStream.read(any(byte[].class), anyInt(), anyInt()))
                    .thenThrow(new IOException("Simulated network stream break"));
            when(faultyInputStream.read(any(byte[].class)))
                    .thenThrow(new IOException("Simulated network stream break"));
            when(faultyInputStream.read())
                    .thenThrow(new IOException("Simulated network stream break"));

            // 1. process(InputStream, OutputStream)
            ByteArrayOutputStream outStream = new ByteArrayOutputStream();
            assertThatThrownBy(() -> calculator.process(faultyInputStream, outStream))
                    .isInstanceOf(CsvVatException.class)
                    .hasMessageContaining("Failed to read CSV from InputStream");

            // 2. processToString(InputStream)
            assertThatThrownBy(() -> calculator.processToString(faultyInputStream))
                    .isInstanceOf(CsvVatException.class)
                    .hasMessageContaining("Failed to read CSV from InputStream");

            // 3. processToResult(InputStream)
            assertThatThrownBy(() -> calculator.processToResult(faultyInputStream))
                    .isInstanceOf(CsvVatException.class)
                    .hasMessageContaining("Failed to read CSV from InputStream");
        }

        @Test
        @DisplayName("Mockito Reader ném IOException: Phải được đóng gói và ném thành CsvVatException")
        void mockReader_throwingIOException_shouldThrowCsvVatException() throws IOException {
            Reader faultyReader = mock(Reader.class);
            when(faultyReader.read(any(char[].class), anyInt(), anyInt()))
                    .thenThrow(new IOException("Simulated disk read error"));
            when(faultyReader.read())
                    .thenThrow(new IOException("Simulated disk read error"));

            // 1. process(Reader, Writer)
            StringWriter writer = new StringWriter();
            assertThatThrownBy(() -> calculator.process(faultyReader, writer))
                    .isInstanceOf(CsvVatException.class);

            // 2. processToString(Reader)
            assertThatThrownBy(() -> calculator.processToString(faultyReader))
                    .isInstanceOf(CsvVatException.class);

            // 3. processToResult(Reader)
            assertThatThrownBy(() -> calculator.processToResult(faultyReader))
                    .isInstanceOf(CsvVatException.class);
        }

        @Test
        @DisplayName("Mockito OutputStream & Writer ném IOException: Phải được đóng gói thành CsvVatException")
        void mockOutputStreamAndWriter_throwingIOException_shouldThrowCsvVatException() throws IOException {
            // Faulty OutputStream
            OutputStream faultyOutputStream = mock(OutputStream.class);
            doThrow(new IOException("Disk write full"))
                    .when(faultyOutputStream).write(any(byte[].class), anyInt(), anyInt());
            doThrow(new IOException("Disk write full"))
                    .when(faultyOutputStream).write(any(byte[].class));
            doThrow(new IOException("Disk write full"))
                    .when(faultyOutputStream).write(anyInt());

            ByteArrayInputStream inStream = new ByteArrayInputStream(SAMPLE_CSV.getBytes(StandardCharsets.UTF_8));
            assertThatThrownBy(() -> calculator.process(inStream, faultyOutputStream))
                    .isInstanceOf(CsvVatException.class);

            // Faulty Writer
            Writer faultyWriter = mock(Writer.class);
            doThrow(new IOException("Pipe write error"))
                    .when(faultyWriter).write(any(char[].class), anyInt(), anyInt());
            doThrow(new IOException("Pipe write error"))
                    .when(faultyWriter).write(anyString());
            doThrow(new IOException("Pipe write error"))
                    .when(faultyWriter).write(anyString(), anyInt(), anyInt());
            doThrow(new IOException("Pipe write error"))
                    .when(faultyWriter).append(any(CharSequence.class));
            doThrow(new IOException("Pipe write error"))
                    .when(faultyWriter).append(any(CharSequence.class), anyInt(), anyInt());
            doThrow(new IOException("Pipe write error"))
                    .when(faultyWriter).append(Mockito.anyChar());
            doThrow(new IOException("Pipe write error"))
                    .when(faultyWriter).flush();

            StringReader reader = new StringReader(SAMPLE_CSV);
            assertThatThrownBy(() -> calculator.process(reader, faultyWriter))
                    .isInstanceOf(CsvVatException.class);
        }
    }
}
