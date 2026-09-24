package com.company.shared.csvvat;

import com.company.shared.csvvat.config.CalculatorConfig;
import com.company.shared.csvvat.config.CsvColumnMapping;
import com.company.shared.csvvat.config.LineTotalDiscrepancyStrategy;
import com.company.shared.csvvat.exception.CsvVatException;
import com.company.shared.csvvat.exception.InvalidCsvFormatException;
import com.company.shared.csvvat.model.OrderCalculationResult;
import com.company.shared.csvvat.service.CommonsCsvReaderService;
import com.company.shared.csvvat.service.CommonsCsvWriterService;
import com.company.shared.csvvat.service.CsvReaderService;
import com.company.shared.csvvat.service.CsvWriterService;
import com.company.shared.csvvat.service.DefaultVatCalculator;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Comprehensive coverage test suite targeting 100% line, method, and branch coverage
 * for the {@code com.company.shared.csvvat} package (Facade {@link CsvVatCalculator} and its {@link CsvVatCalculator.Builder}).
 */
@DisplayName("Kiểm thử chuyên sâu CsvVatCalculatorCoverageTest - Độ bao phủ 100% Facade & Builder")
class CsvVatCalculatorCoverageTest {

    private static final String VALID_CSV = """
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
    @DisplayName("Khởi tạo bộ test CsvVatCalculatorCoverageTest")
    void contextLoads() {
        assertThat(new CsvVatCalculatorCoverageTest()).isNotNull();
        assertThat(new CsvVatCalculatorCoverageTest().new BuilderDefaultsTests()).isNotNull();
        assertThat(new CsvVatCalculatorCoverageTest().new OverloadedMethodsTests()).isNotNull();
        assertThat(new CsvVatCalculatorCoverageTest().new ExceptionAndEdgeCasesTests()).isNotNull();
        assertThat(new CsvVatCalculatorCoverageTest().new MainRunnerTests()).isNotNull();
    }

    // =========================================================================
    // Yêu cầu 1: Cover Builder Defaults & Constructor Variations
    // =========================================================================
    @Nested
    @DisplayName("1. Kiểm thử Builder Defaults & Constructors")
    class BuilderDefaultsTests {

        @Test
        @DisplayName("createDefault() khởi tạo với đầy đủ service và config mặc định")
        void createDefault_shouldInitializeWithProductionDefaults() {
            CsvVatCalculator defaultCalc = CsvVatCalculator.createDefault();

            assertThat(defaultCalc).isNotNull();
            assertThat(defaultCalc.getConfig()).isNotNull();
            assertThat(defaultCalc.getConfig()).isEqualTo(CalculatorConfig.defaultConfig());
            assertThat(defaultCalc.getReaderService()).isInstanceOf(CommonsCsvReaderService.class);
            assertThat(defaultCalc.getVatCalculator()).isInstanceOf(DefaultVatCalculator.class);
            assertThat(defaultCalc.getWriterService()).isInstanceOf(CommonsCsvWriterService.class);
        }

        @Test
        @DisplayName(".builder().build() không truyền tham số nào tạo cấu hình mặc định (phủ các nhánh ternary null)")
        void builderBuild_withNoParameters_shouldFallbackToDefaultServices() {
            CsvVatCalculator builtCalc = CsvVatCalculator.builder().build();

            assertThat(builtCalc).isNotNull();
            assertThat(builtCalc.getConfig()).isEqualTo(CalculatorConfig.defaultConfig());
            assertThat(builtCalc.getReaderService()).isInstanceOf(CommonsCsvReaderService.class);
            assertThat(builtCalc.getVatCalculator()).isInstanceOf(DefaultVatCalculator.class);
            assertThat(builtCalc.getWriterService()).isInstanceOf(CommonsCsvWriterService.class);
        }

        @Test
        @DisplayName("create(config) và new CsvVatCalculator(config) thiết lập config tùy biến và production services")
        void createAndConstructorWithConfig_shouldApplyConfigCorrectly() {
            CalculatorConfig customConfig = CalculatorConfig.builder()
                    .currencyScale(0)
                    .roundingMode(RoundingMode.HALF_UP)
                    .discrepancyStrategy(LineTotalDiscrepancyStrategy.FAIL_ON_MISMATCH)
                    .discrepancyTolerance(new BigDecimal("0.02"))
                    .writeUtf8Bom(false)
                    .build();

            CsvVatCalculator calcFromFactory = CsvVatCalculator.create(customConfig);
            assertThat(calcFromFactory.getConfig()).isEqualTo(customConfig);
            assertThat(calcFromFactory.getReaderService()).isInstanceOf(CommonsCsvReaderService.class);

            CsvVatCalculator calcFromConstructor = new CsvVatCalculator(customConfig);
            assertThat(calcFromConstructor.getConfig()).isEqualTo(customConfig);
            assertThat(calcFromConstructor.getVatCalculator()).isInstanceOf(DefaultVatCalculator.class);
        }

        @Test
        @DisplayName("Builder: Tiêm từng service riêng lẻ để phủ 100% các nhánh rẽ điều kiện trong build()")
        void builder_individualCustomServices_shouldCoverAllTernaryBranches() {
            CsvReaderService mockReader = mock(CsvReaderService.class);
            VatCalculator mockCalculator = mock(VatCalculator.class);
            CsvWriterService mockWriter = mock(CsvWriterService.class);

            // 1. Chỉ tiêm ReaderService, VatCalculator & WriterService dùng mặc định
            CsvVatCalculator calcOnlyReader = CsvVatCalculator.builder()
                    .readerService(mockReader)
                    .build();
            assertThat(calcOnlyReader.getReaderService()).isSameAs(mockReader);
            assertThat(calcOnlyReader.getVatCalculator()).isInstanceOf(DefaultVatCalculator.class);
            assertThat(calcOnlyReader.getWriterService()).isInstanceOf(CommonsCsvWriterService.class);

            // 2. Chỉ tiêm VatCalculator, ReaderService & WriterService dùng mặc định
            CsvVatCalculator calcOnlyCalculator = CsvVatCalculator.builder()
                    .vatCalculator(mockCalculator)
                    .build();
            assertThat(calcOnlyCalculator.getReaderService()).isInstanceOf(CommonsCsvReaderService.class);
            assertThat(calcOnlyCalculator.getVatCalculator()).isSameAs(mockCalculator);
            assertThat(calcOnlyCalculator.getWriterService()).isInstanceOf(CommonsCsvWriterService.class);

            // 3. Chỉ tiêm WriterService, ReaderService & VatCalculator dùng mặc định
            CsvVatCalculator calcOnlyWriter = CsvVatCalculator.builder()
                    .writerService(mockWriter)
                    .build();
            assertThat(calcOnlyWriter.getReaderService()).isInstanceOf(CommonsCsvReaderService.class);
            assertThat(calcOnlyWriter.getVatCalculator()).isInstanceOf(DefaultVatCalculator.class);
            assertThat(calcOnlyWriter.getWriterService()).isSameAs(mockWriter);

            // 4. Tiêm cả 3 service tùy biến
            CsvVatCalculator calcAllCustom = CsvVatCalculator.builder()
                    .readerService(mockReader)
                    .vatCalculator(mockCalculator)
                    .writerService(mockWriter)
                    .build();
            assertThat(calcAllCustom.getReaderService()).isSameAs(mockReader);
            assertThat(calcAllCustom.getVatCalculator()).isSameAs(mockCalculator);
            assertThat(calcAllCustom.getWriterService()).isSameAs(mockWriter);
        }

        @Test
        @DisplayName("Builder: Cấu hình tất cả các thuộc tính metadata mapping và business options")
        void builder_allConfigSetters_shouldSetPropertiesCorrectly() {
            CsvColumnMapping customMapping = CsvColumnMapping.builder()
                    .itemNameColumn("Ma_SP")
                    .quantityColumn("So_Luong")
                    .unitPriceColumn("Don_Gia")
                    .vatPercentageColumn("Thue_VAT")
                    .lineTotalColumn("Tong_Tien")
                    .build();

            CsvVatCalculator fullyCustom = CsvVatCalculator.builder()
                    .columnMapping(customMapping)
                    .discrepancyStrategy(LineTotalDiscrepancyStrategy.ACCEPT_INPUT_TOTAL)
                    .discrepancyTolerance(new BigDecimal("0.10"))
                    .currencyScale(3)
                    .roundingMode(RoundingMode.HALF_EVEN)
                    .writeUtf8Bom(false)
                    .build();

            CalculatorConfig config = fullyCustom.getConfig();
            assertThat(config.columnMapping()).isEqualTo(customMapping);
            assertThat(config.discrepancyStrategy()).isEqualTo(LineTotalDiscrepancyStrategy.ACCEPT_INPUT_TOTAL);
            assertThat(config.discrepancyTolerance()).isEqualByComparingTo("0.10");
            assertThat(config.currencyScale()).isEqualTo(3);
            assertThat(config.roundingMode()).isEqualTo(RoundingMode.HALF_EVEN);
            assertThat(config.writeUtf8Bom()).isFalse();

            // Builder writeUtf8Bom(true)
            CsvVatCalculator withBom = CsvVatCalculator.builder().writeUtf8Bom(true).build();
            assertThat(withBom.getConfig().writeUtf8Bom()).isTrue();
        }
    }

    // =========================================================================
    // Yêu cầu 2: Cover Overloaded Methods (Path, File, InputStream, Reader)
    // =========================================================================
    @Nested
    @DisplayName("2. Kiểm thử Overloaded Methods (process, processToString, processToResult)")
    class OverloadedMethodsTests {

        @Test
        @DisplayName("process(Path, Path): Xử lý thành công từ Path đến Path")
        void process_pathOverload_shouldProcessAndWriteOutput(@TempDir Path tempDir) throws IOException {
            Path inPath = tempDir.resolve("orders_input.csv");
            Path outPath = tempDir.resolve("orders_output.csv");
            Files.writeString(inPath, VALID_CSV, StandardCharsets.UTF_8);

            OrderCalculationResult result = calculator.process(inPath, outPath);

            assertThat(result).isNotNull();
            assertThat(result.totalItemsCount()).isEqualTo(2);
            assertThat(result.subtotal()).isEqualByComparingTo("154000.00");
            assertThat(result.totalVat()).isEqualByComparingTo("14000.00");
            assertThat(result.grandTotal()).isEqualByComparingTo("168000.00");

            assertThat(Files.exists(outPath)).isTrue();
            String outputContent = Files.readString(outPath, StandardCharsets.UTF_8);
            assertThat(outputContent).contains("Bánh mì xíu mại").contains("TỔNG THANH TOÁN (GRAND TOTAL)");
        }

        @Test
        @DisplayName("process(File, File): Xử lý thành công từ File đến File")
        void process_fileOverload_shouldProcessAndWriteOutput(@TempDir Path tempDir) throws IOException {
            File inFile = tempDir.resolve("orders_file_in.csv").toFile();
            File outFile = tempDir.resolve("orders_file_out.csv").toFile();
            Files.writeString(inFile.toPath(), VALID_CSV, StandardCharsets.UTF_8);

            OrderCalculationResult result = calculator.process(inFile, outFile);

            assertThat(result).isNotNull();
            assertThat(result.totalItemsCount()).isEqualTo(2);
            assertThat(result.grandTotal()).isEqualByComparingTo("168000.00");

            assertThat(outFile.exists()).isTrue();
            String content = Files.readString(outFile.toPath(), StandardCharsets.UTF_8);
            assertThat(content).contains("Cà phê muối").contains("168000.00");
        }

        @Test
        @DisplayName("process(InputStream, OutputStream): Xử lý thành công qua Stream I/O")
        void process_streamOverload_shouldProcessAndWriteOutput() {
            ByteArrayInputStream inStream = new ByteArrayInputStream(VALID_CSV.getBytes(StandardCharsets.UTF_8));
            ByteArrayOutputStream outStream = new ByteArrayOutputStream();

            OrderCalculationResult result = calculator.process(inStream, outStream);

            assertThat(result).isNotNull();
            assertThat(result.totalItemsCount()).isEqualTo(2);
            assertThat(result.grandTotal()).isEqualByComparingTo("168000.00");

            String outputContent = outStream.toString(StandardCharsets.UTF_8);
            assertThat(outputContent).contains("Line Subtotal").contains("168000.00");
        }

        @Test
        @DisplayName("process(Reader, Writer): Xử lý thành công qua Reader và Writer")
        void process_readerWriterOverload_shouldProcessAndWriteOutput() {
            StringReader reader = new StringReader(VALID_CSV);
            StringWriter writer = new StringWriter();

            OrderCalculationResult result = calculator.process(reader, writer);

            assertThat(result).isNotNull();
            assertThat(result.totalItemsCount()).isEqualTo(2);
            assertThat(result.grandTotal()).isEqualByComparingTo("168000.00");

            String outputContent = writer.toString();
            assertThat(outputContent).contains("Line Subtotal").contains("168000.00");
        }

        @Test
        @DisplayName("processToString: Gọi đầy đủ 4 phiên bản nạp chồng (Path, File, InputStream, Reader)")
        void processToString_allOverloads_shouldReturnConsistentCsvString(@TempDir Path tempDir) throws IOException {
            Path inPath = tempDir.resolve("input_string.csv");
            Files.writeString(inPath, VALID_CSV, StandardCharsets.UTF_8);
            File inFile = inPath.toFile();

            // 1. Path overload
            String fromPath = calculator.processToString(inPath);
            assertThat(fromPath).contains("Bánh mì xíu mại").contains("168000.00");

            // 2. File overload
            String fromFile = calculator.processToString(inFile);
            assertThat(fromFile).isEqualTo(fromPath);

            // 3. InputStream overload
            ByteArrayInputStream inStream = new ByteArrayInputStream(VALID_CSV.getBytes(StandardCharsets.UTF_8));
            String fromStream = calculator.processToString(inStream);
            assertThat(fromStream).isEqualTo(fromPath);

            // 4. Reader overload
            StringReader reader = new StringReader(VALID_CSV);
            String fromReader = calculator.processToString(reader);
            assertThat(fromReader).isEqualTo(fromPath);
        }

        @Test
        @DisplayName("processToResult: Gọi đầy đủ 4 phiên bản nạp chồng (Path, File, InputStream, Reader)")
        void processToResult_allOverloads_shouldReturnIdenticalDomainRecords(@TempDir Path tempDir) throws IOException {
            Path inPath = tempDir.resolve("input_result.csv");
            Files.writeString(inPath, VALID_CSV, StandardCharsets.UTF_8);
            File inFile = inPath.toFile();

            // 1. Path overload
            OrderCalculationResult resFromPath = calculator.processToResult(inPath);
            assertThat(resFromPath.totalItemsCount()).isEqualTo(2);
            assertThat(resFromPath.grandTotal()).isEqualByComparingTo("168000.00");

            // 2. File overload
            OrderCalculationResult resFromFile = calculator.processToResult(inFile);
            assertThat(resFromFile.totalItemsCount()).isEqualTo(2);
            assertThat(resFromFile.grandTotal()).isEqualByComparingTo("168000.00");

            // 3. InputStream overload
            ByteArrayInputStream inStream = new ByteArrayInputStream(VALID_CSV.getBytes(StandardCharsets.UTF_8));
            OrderCalculationResult resFromStream = calculator.processToResult(inStream);
            assertThat(resFromStream.totalItemsCount()).isEqualTo(2);
            assertThat(resFromStream.grandTotal()).isEqualByComparingTo("168000.00");

            // 4. Reader overload
            StringReader reader = new StringReader(VALID_CSV);
            OrderCalculationResult resFromReader = calculator.processToResult(reader);
            assertThat(resFromReader.totalItemsCount()).isEqualTo(2);
            assertThat(resFromReader.grandTotal()).isEqualByComparingTo("168000.00");
        }
    }

    // =========================================================================
    // Yêu cầu 3: Cover Exception Blocks & Edge Cases (Mock IOException, Nulls)
    // =========================================================================
    @Nested
    @DisplayName("3. Kiểm thử Exception Blocks & Edge Cases")
    class ExceptionAndEdgeCasesTests {

        @Test
        @DisplayName("Mock InputStream ném IOException khi gọi read() kích hoạt catch (IOException)")
        void mockedInputStream_throwingIOException_shouldTriggerCatchBlock() throws IOException {
            InputStream mockStream = mock(InputStream.class);
            when(mockStream.read()).thenThrow(new IOException("Simulated disk read error on read()"));
            when(mockStream.read(any(byte[].class))).thenThrow(new IOException("Simulated disk read error on read(byte[])"));
            when(mockStream.read(any(byte[].class), anyInt(), anyInt())).thenThrow(new IOException("Simulated disk read error on read(byte[], int, int)"));

            // 1. process(InputStream, OutputStream)
            Throwable t1 = null;
            try { calculator.process(mockStream, new ByteArrayOutputStream()); } catch (CsvVatException e) { t1 = e; }
            assertThat(t1).isInstanceOf(CsvVatException.class)
                    .hasCauseInstanceOf(IOException.class)
                    .hasMessageContaining("Simulated disk read error");

            // 2. processToString(InputStream)
            Throwable t2 = null;
            try { calculator.processToString(mockStream); } catch (CsvVatException e) { t2 = e; }
            assertThat(t2).isInstanceOf(CsvVatException.class)
                    .hasCauseInstanceOf(IOException.class)
                    .hasMessageContaining("Simulated disk read error");

            // 3. processToResult(InputStream)
            Throwable t3 = null;
            try { calculator.processToResult(mockStream); } catch (CsvVatException e) { t3 = e; }
            assertThat(t3).isInstanceOf(CsvVatException.class)
                    .hasCauseInstanceOf(IOException.class)
                    .hasMessageContaining("Simulated disk read error");
        }

        @Test
        @DisplayName("Mock Reader ném IOException khi gọi read() kích hoạt catch (IOException)")
        void mockedReader_throwingIOException_shouldTriggerCatchBlock() throws IOException {
            Reader mockReader = mock(Reader.class);
            when(mockReader.read(any(char[].class), anyInt(), anyInt()))
                    .thenThrow(new IOException("Simulated character read error"));

            // 1. process(Reader, Writer)
            Throwable t1 = null;
            try { calculator.process(mockReader, new StringWriter()); } catch (CsvVatException e) { t1 = e; }
            assertThat(t1).isInstanceOf(CsvVatException.class)
                    .hasCauseInstanceOf(IOException.class)
                    .hasMessageContaining("Simulated character read error");

            // 2. processToString(Reader)
            Throwable t2 = null;
            try { calculator.processToString(mockReader); } catch (CsvVatException e) { t2 = e; }
            assertThat(t2).isInstanceOf(CsvVatException.class)
                    .hasCauseInstanceOf(IOException.class)
                    .hasMessageContaining("Simulated character read error");

            // 3. processToResult(Reader)
            Throwable t3 = null;
            try { calculator.processToResult(mockReader); } catch (CsvVatException e) { t3 = e; }
            assertThat(t3).isInstanceOf(CsvVatException.class)
                    .hasCauseInstanceOf(IOException.class)
                    .hasMessageContaining("Simulated character read error");
        }

        @Test
        @DisplayName("Mock OutputStream và Writer ném IOException khi gọi write()")
        void mockedOutputStreamAndWriter_throwingIOException_shouldTriggerCatchBlock() throws IOException {
            // Mock OutputStream throwing IOException
            OutputStream mockOs = mock(OutputStream.class);
            doThrow(new IOException("Simulated write error on OutputStream")).when(mockOs).write(any(byte[].class), anyInt(), anyInt());
            doThrow(new IOException("Simulated write error on OutputStream")).when(mockOs).write(any(byte[].class));
            doThrow(new IOException("Simulated write error on OutputStream")).when(mockOs).write(anyInt());

            ByteArrayInputStream inStream = new ByteArrayInputStream(VALID_CSV.getBytes(StandardCharsets.UTF_8));
            Throwable t1 = null;
            try { calculator.process(inStream, mockOs); } catch (CsvVatException e) { t1 = e; }
            assertThat(t1).isInstanceOf(CsvVatException.class)
                    .hasCauseInstanceOf(IOException.class)
                    .hasMessageContaining("Simulated write error on OutputStream");

            // Mock Writer throwing IOException on write/append
            Writer mockWriter = mock(Writer.class);
            doThrow(new IOException("Simulated write error on Writer")).when(mockWriter).write(any(char[].class), anyInt(), anyInt());
            doThrow(new IOException("Simulated write error on Writer")).when(mockWriter).write(any(char[].class));
            doThrow(new IOException("Simulated write error on Writer")).when(mockWriter).write(anyString());
            doThrow(new IOException("Simulated write error on Writer")).when(mockWriter).write(anyString(), anyInt(), anyInt());
            doThrow(new IOException("Simulated write error on Writer")).when(mockWriter).write(anyInt());
            doThrow(new IOException("Simulated write error on Writer")).when(mockWriter).append(any(CharSequence.class));
            doThrow(new IOException("Simulated write error on Writer")).when(mockWriter).append(any(CharSequence.class), anyInt(), anyInt());
            doThrow(new IOException("Simulated write error on Writer")).when(mockWriter).append(Mockito.anyChar());
            doThrow(new IOException("Simulated write error on Writer")).when(mockWriter).flush();

            StringReader inReader = new StringReader(VALID_CSV);
            Throwable t2 = null;
            try { calculator.process(inReader, mockWriter); } catch (CsvVatException e) { t2 = e; }
            assertThat(t2).isInstanceOf(CsvVatException.class)
                    .hasCauseInstanceOf(IOException.class)
                    .hasMessageContaining("Simulated write error on Writer");
        }

        @Test
        @DisplayName("Kiểm tra an toàn: Truyền null vào tất cả các tham số của mọi hàm Facade & Builder")
        void nullArguments_shouldThrowNullPointerExceptionAcrossAllApis() {
            Path validPath = Path.of("sample.csv");
            File validFile = new File("sample.csv");
            InputStream validIs = new ByteArrayInputStream(new byte[0]);
            OutputStream validOs = new ByteArrayOutputStream();
            Reader validReader = new StringReader("");
            Writer validWriter = new StringWriter();
            CalculatorConfig validConfig = CalculatorConfig.defaultConfig();
            CsvReaderService validReaderService = new CommonsCsvReaderService();
            VatCalculator validVatCalculator = new DefaultVatCalculator(validConfig);
            CsvWriterService validWriterService = new CommonsCsvWriterService();

            Throwable t;

            // process(Path, Path)
            t = null; try { calculator.process((Path) null, validPath); } catch (NullPointerException e) { t = e; }
            assertThat(t).isInstanceOf(NullPointerException.class).hasMessageContaining("inputPath");

            t = null; try { calculator.process(validPath, (Path) null); } catch (NullPointerException e) { t = e; }
            assertThat(t).isInstanceOf(NullPointerException.class).hasMessageContaining("outputPath");

            // process(File, File)
            t = null; try { calculator.process((File) null, validFile); } catch (NullPointerException e) { t = e; }
            assertThat(t).isInstanceOf(NullPointerException.class).hasMessageContaining("inputFile");

            t = null; try { calculator.process(validFile, (File) null); } catch (NullPointerException e) { t = e; }
            assertThat(t).isInstanceOf(NullPointerException.class).hasMessageContaining("outputFile");

            // process(InputStream, OutputStream)
            t = null; try { calculator.process((InputStream) null, validOs); } catch (NullPointerException e) { t = e; }
            assertThat(t).isInstanceOf(NullPointerException.class).hasMessageContaining("inputStream");

            t = null; try { calculator.process(validIs, (OutputStream) null); } catch (NullPointerException e) { t = e; }
            assertThat(t).isInstanceOf(NullPointerException.class).hasMessageContaining("outputStream");

            // process(Reader, Writer)
            t = null; try { calculator.process((Reader) null, validWriter); } catch (NullPointerException e) { t = e; }
            assertThat(t).isInstanceOf(NullPointerException.class).hasMessageContaining("reader");

            t = null; try { calculator.process(validReader, (Writer) null); } catch (NullPointerException e) { t = e; }
            assertThat(t).isInstanceOf(NullPointerException.class).hasMessageContaining("writer");

            // processToString(Path / File / InputStream / Reader)
            t = null; try { calculator.processToString((Path) null); } catch (NullPointerException e) { t = e; }
            assertThat(t).isInstanceOf(NullPointerException.class).hasMessageContaining("inputPath");

            t = null; try { calculator.processToString((File) null); } catch (NullPointerException e) { t = e; }
            assertThat(t).isInstanceOf(NullPointerException.class).hasMessageContaining("inputFile");

            t = null; try { calculator.processToString((InputStream) null); } catch (NullPointerException e) { t = e; }
            assertThat(t).isInstanceOf(NullPointerException.class).hasMessageContaining("inputStream");

            t = null; try { calculator.processToString((Reader) null); } catch (NullPointerException e) { t = e; }
            assertThat(t).isInstanceOf(NullPointerException.class).hasMessageContaining("reader");

            // processToResult(Path / File / InputStream / Reader)
            t = null; try { calculator.processToResult((Path) null); } catch (NullPointerException e) { t = e; }
            assertThat(t).isInstanceOf(NullPointerException.class).hasMessageContaining("inputPath");

            t = null; try { calculator.processToResult((File) null); } catch (NullPointerException e) { t = e; }
            assertThat(t).isInstanceOf(NullPointerException.class).hasMessageContaining("inputFile");

            t = null; try { calculator.processToResult((InputStream) null); } catch (NullPointerException e) { t = e; }
            assertThat(t).isInstanceOf(NullPointerException.class).hasMessageContaining("inputStream");

            t = null; try { calculator.processToResult((Reader) null); } catch (NullPointerException e) { t = e; }
            assertThat(t).isInstanceOf(NullPointerException.class).hasMessageContaining("reader");

            // Constructors & Factories
            t = null; try { new CsvVatCalculator(null); } catch (NullPointerException e) { t = e; }
            assertThat(t).isInstanceOf(NullPointerException.class).hasMessageContaining("CalculatorConfig");

            t = null; try { CsvVatCalculator.create(null); } catch (NullPointerException e) { t = e; }
            assertThat(t).isInstanceOf(NullPointerException.class).hasMessageContaining("CalculatorConfig");

            t = null; try { new CsvVatCalculator(null, validReaderService, validVatCalculator, validWriterService); } catch (NullPointerException e) { t = e; }
            assertThat(t).isInstanceOf(NullPointerException.class).hasMessageContaining("CalculatorConfig");

            t = null; try { new CsvVatCalculator(validConfig, null, validVatCalculator, validWriterService); } catch (NullPointerException e) { t = e; }
            assertThat(t).isInstanceOf(NullPointerException.class).hasMessageContaining("CsvReaderService");

            t = null; try { new CsvVatCalculator(validConfig, validReaderService, null, validWriterService); } catch (NullPointerException e) { t = e; }
            assertThat(t).isInstanceOf(NullPointerException.class).hasMessageContaining("VatCalculator");

            t = null; try { new CsvVatCalculator(validConfig, validReaderService, validVatCalculator, null); } catch (NullPointerException e) { t = e; }
            assertThat(t).isInstanceOf(NullPointerException.class).hasMessageContaining("CsvWriterService");
        }

        @Test
        @DisplayName("File/Path không tồn tại ném CsvVatException bọc IOException")
        void nonExistentFiles_shouldThrowCsvVatExceptionWithIoExceptionCause(@TempDir Path tempDir) {
            Path missingPath = tempDir.resolve("non_existent_file.csv");
            File missingFile = missingPath.toFile();
            Path outPath = tempDir.resolve("out.csv");
            File outFile = outPath.toFile();

            // process
            Throwable t1 = null;
            try { calculator.process(missingPath, outPath); } catch (CsvVatException e) { t1 = e; }
            assertThat(t1).isInstanceOf(CsvVatException.class)
                    .hasMessageContaining("Failed to process order CSV files")
                    .hasCauseInstanceOf(IOException.class);

            Throwable t2 = null;
            try { calculator.process(missingFile, outFile); } catch (CsvVatException e) { t2 = e; }
            assertThat(t2).isInstanceOf(CsvVatException.class)
                    .hasMessageContaining("Failed to process order CSV files")
                    .hasCauseInstanceOf(IOException.class);

            // processToString
            Throwable t3 = null;
            try { calculator.processToString(missingPath); } catch (CsvVatException e) { t3 = e; }
            assertThat(t3).isInstanceOf(CsvVatException.class)
                    .hasMessageContaining("Failed to read CSV from path")
                    .hasCauseInstanceOf(IOException.class);

            Throwable t4 = null;
            try { calculator.processToString(missingFile); } catch (CsvVatException e) { t4 = e; }
            assertThat(t4).isInstanceOf(CsvVatException.class)
                    .hasMessageContaining("Failed to read CSV from path")
                    .hasCauseInstanceOf(IOException.class);

            // processToResult
            Throwable t5 = null;
            try { calculator.processToResult(missingPath); } catch (CsvVatException e) { t5 = e; }
            assertThat(t5).isInstanceOf(CsvVatException.class)
                    .hasMessageContaining("Failed to read CSV from path")
                    .hasCauseInstanceOf(IOException.class);

            Throwable t6 = null;
            try { calculator.processToResult(missingFile); } catch (CsvVatException e) { t6 = e; }
            assertThat(t6).isInstanceOf(CsvVatException.class)
                    .hasMessageContaining("Failed to read CSV from path")
                    .hasCauseInstanceOf(IOException.class);
        }

        @Test
        @DisplayName("process(Path, Path): Bắt CsvVatException từ stream và rethrow trực tiếp (không wrap lại)")
        void process_whenUnderlyingProcessorThrowsCsvVatException_shouldRethrowDirectly(@TempDir Path tempDir) throws IOException {
            Path inPath = tempDir.resolve("corrupt_input.csv");
            Path outPath = tempDir.resolve("out.csv");
            // CSV sai header format gây ném InvalidCsvFormatException (kế thừa CsvVatException)
            Files.writeString(inPath, "Invalid,Header,Only\n1,2,3", StandardCharsets.UTF_8);

            Throwable t = null;
            try { calculator.process(inPath, outPath); } catch (InvalidCsvFormatException e) { t = e; }
            assertThat(t).isInstanceOf(InvalidCsvFormatException.class);

            // File output bị xóa dọn dẹp khi có lỗi
            assertThat(Files.exists(outPath)).isFalse();
        }

        @Test
        @DisplayName("process(Path, Path): Bắt IOException khi Files.deleteIfExists thất bại (catch IOException ignored)")
        void process_whenCleanupFails_shouldIgnoreAndRethrowOriginalException(@TempDir Path tempDir) throws IOException {
            Path inPath = tempDir.resolve("valid_input.csv");
            Files.writeString(inPath, VALID_CSV, StandardCharsets.UTF_8);

            // Tạo thư mục không rỗng làm outputPath để Files.deleteIfExists(outputPath) ném DirectoryNotEmptyException (IOException)
            Path nonDeletableDir = tempDir.resolve("non_empty_dir");
            Files.createDirectory(nonDeletableDir);
            Files.writeString(nonDeletableDir.resolve("locked_file.txt"), "some content");

            Throwable t = null;
            try { calculator.process(inPath, nonDeletableDir); } catch (CsvVatException e) { t = e; }
            assertThat(t).isInstanceOf(CsvVatException.class)
                    .hasMessageContaining("Failed to process order CSV files");
        }
    }

    // =========================================================================
    // Phụ trợ: Kiểm thử Runner MainV2
    // =========================================================================
    @Nested
    @DisplayName("4. Runner Demo (MainV2)")
    class MainRunnerTests {

        @Test
        @DisplayName("MainV2.main() thực thi hoàn tất không ném lỗi")
        void mainV2_shouldExecuteSuccessfully() throws Exception {
            MainV2 runner = new MainV2();
            assertThat(runner).isNotNull();
            MainV2.main(new String[0]);
            assertThat(Files.exists(Path.of("output_custom.csv"))).isTrue();
        }
    }
}
