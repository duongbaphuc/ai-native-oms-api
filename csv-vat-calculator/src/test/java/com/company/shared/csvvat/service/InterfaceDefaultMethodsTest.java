package com.company.shared.csvvat.service;

import com.company.shared.csvvat.exception.CsvVatException;
import com.company.shared.csvvat.exception.InvalidCsvFormatException;
import com.company.shared.csvvat.model.OrderCalculationResult;
import com.company.shared.csvvat.model.OrderItem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Kiểm thử các phương thức default trên Interface: OrderCsvProcessor, CsvWriterService, CsvReaderService")
class InterfaceDefaultMethodsTest {

    private OrderCalculationResult createDummyResult() {
        return new OrderCalculationResult(
                "TEST-ORD",
                List.of(),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                0
        );
    }

    @Test
    @DisplayName("Khởi tạo InterfaceDefaultMethodsTest")
    void contextLoads() {
        assertThat(new InterfaceDefaultMethodsTest()).isNotNull();
    }

    @Test
    @DisplayName("OrderCsvProcessor.process(Path, Path): Thành công khi gọi phương thức mặc định")
    void orderCsvProcessor_defaultProcessPath_success(@TempDir Path tempDir) throws IOException {
        Path inPath = tempDir.resolve("input.csv");
        Path outPath = tempDir.resolve("output.csv");
        Files.writeString(inPath, "dummy", StandardCharsets.UTF_8);

        OrderCalculationResult expected = createDummyResult();

        OrderCsvProcessor processor = new OrderCsvProcessor() {
            @Override
            public OrderCalculationResult process(Reader reader, Writer writer) {
                return null;
            }

            @Override
            public OrderCalculationResult process(InputStream inputStream, OutputStream outputStream) throws CsvVatException {
                return expected;
            }
        };

        assertThat(processor.process((Reader) null, (Writer) null)).isNull();
        OrderCalculationResult actual = processor.process(inPath, outPath);
        assertThat(actual).isSameAs(expected);
    }

    @Test
    @DisplayName("OrderCsvProcessor.process(Path, Path): Bắt CsvVatException và rethrow trực tiếp")
    void orderCsvProcessor_defaultProcessPath_rethrowsCsvVatException(@TempDir Path tempDir) throws IOException {
        Path inPath = tempDir.resolve("input.csv");
        Path outPath = tempDir.resolve("output.csv");
        Files.writeString(inPath, "dummy", StandardCharsets.UTF_8);

        OrderCsvProcessor processor = new OrderCsvProcessor() {
            @Override
            public OrderCalculationResult process(Reader reader, Writer writer) { return null; }

            @Override
            public OrderCalculationResult process(InputStream in, OutputStream out) throws CsvVatException {
                throw new InvalidCsvFormatException("Invalid format in stream");
            }
        };

        assertThat(processor.process((Reader) null, (Writer) null)).isNull();
        Throwable t1 = null;
        try {
            processor.process(inPath, outPath);
        } catch (InvalidCsvFormatException e) {
            t1 = e;
        }
        assertThat(t1).isInstanceOf(InvalidCsvFormatException.class)
                .hasMessageContaining("Invalid format in stream");
    }

    @Test
    @DisplayName("OrderCsvProcessor.process(Path, Path): Bắt IOException/Exception khác và wrap thành CsvVatException")
    void orderCsvProcessor_defaultProcessPath_wrapsGenericException(@TempDir Path tempDir) {
        Path inPath = tempDir.resolve("non_existent_input.csv");
        Path outPath = tempDir.resolve("output.csv");

        OrderCsvProcessor processor = new OrderCsvProcessor() {
            @Override
            public OrderCalculationResult process(Reader reader, Writer writer) { return null; }

            @Override
            public OrderCalculationResult process(InputStream in, OutputStream out) { return null; }
        };

        assertThat(processor.process((Reader) null, (Writer) null)).isNull();
        assertThat(processor.process((InputStream) null, (OutputStream) null)).isNull();
        Throwable t2 = null;
        try {
            processor.process(inPath, outPath);
        } catch (CsvVatException e) {
            t2 = e;
        }
        assertThat(t2).isInstanceOf(CsvVatException.class)
                .hasMessageContaining("Failed to process order CSV files");
    }

    @Test
    @DisplayName("OrderCsvProcessor.process(File, File): Thành công và kiểm tra null")
    void orderCsvProcessor_defaultProcessFile(@TempDir Path tempDir) throws IOException {
        Path inPath = tempDir.resolve("input_file.csv");
        Path outPath = tempDir.resolve("output_file.csv");
        Files.writeString(inPath, "dummy", StandardCharsets.UTF_8);

        OrderCalculationResult expected = createDummyResult();
        OrderCsvProcessor processor = new OrderCsvProcessor() {
            @Override
            public OrderCalculationResult process(Reader reader, Writer writer) { return null; }

            @Override
            public OrderCalculationResult process(InputStream in, OutputStream out) { return expected; }
        };

        assertThat(processor.process((Reader) null, (Writer) null)).isNull();
        OrderCalculationResult result = processor.process(inPath.toFile(), outPath.toFile());
        assertThat(result).isSameAs(expected);

        Throwable tNull1 = null;
        try {
            processor.process((java.io.File) null, outPath.toFile());
        } catch (NullPointerException e) {
            tNull1 = e;
        }
        assertThat(tNull1).isInstanceOf(NullPointerException.class);

        Throwable tNull2 = null;
        try {
            processor.process(inPath.toFile(), (java.io.File) null);
        } catch (NullPointerException e) {
            tNull2 = e;
        }
        assertThat(tNull2).isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("CsvWriterService.writeToString: Sử dụng phương thức mặc định của interface")
    void csvWriterService_defaultWriteToString_success() {
        CsvWriterService writerService = new CsvWriterService() {
            @Override
            public void write(OrderCalculationResult result, Writer writer) throws CsvVatException {
                try {
                    writer.write("CSV_OUTPUT_DATA");
                } catch (IOException e) {
                    throw new CsvVatException("Write error", e);
                }
            }

            @Override
            public void write(OrderCalculationResult result, OutputStream outputStream) {}
        };

        writerService.write(null, (OutputStream) null);
        String output = writerService.writeToString(createDummyResult());
        assertThat(output).isEqualTo("CSV_OUTPUT_DATA");
    }

    @Test
    @DisplayName("CsvWriterService.write(Result, Path): Thành công ghi ra file")
    void csvWriterService_defaultWritePath_success(@TempDir Path tempDir) {
        Path outPath = tempDir.resolve("writer_out.csv");
        CsvWriterService writerService = new CsvWriterService() {
            @Override
            public void write(OrderCalculationResult result, Writer writer) {}

            @Override
            public void write(OrderCalculationResult result, OutputStream outputStream) throws CsvVatException {
                try {
                    outputStream.write("DEFAULT_WRITER_TEST".getBytes(StandardCharsets.UTF_8));
                } catch (IOException e) {
                    throw new CsvVatException("Stream error", e);
                }
            }
        };

        writerService.write(null, (Writer) null);
        writerService.write(createDummyResult(), outPath);
        assertThat(Files.exists(outPath)).isTrue();
    }

    @Test
    @DisplayName("CsvWriterService.write(Result, Path): Rethrows CsvVatException khi write(os) ném ngoại lệ")
    void csvWriterService_defaultWritePath_rethrowsCsvVatException(@TempDir Path tempDir) {
        Path outPath = tempDir.resolve("writer_err.csv");
        CsvWriterService writerService = new CsvWriterService() {
            @Override
            public void write(OrderCalculationResult result, Writer writer) {}

            @Override
            public void write(OrderCalculationResult result, OutputStream outputStream) throws CsvVatException {
                throw new CsvVatException("Stream write failed");
            }
        };

        writerService.write(null, (Writer) null);
        Throwable t3 = null;
        try {
            writerService.write(createDummyResult(), outPath);
        } catch (CsvVatException e) {
            t3 = e;
        }
        assertThat(t3).isInstanceOf(CsvVatException.class)
                .hasMessage("Stream write failed");
    }

    @Test
    @DisplayName("CsvWriterService.write(Result, Path): Wrap IOException khi path không hợp lệ")
    void csvWriterService_defaultWritePath_wrapsGenericException(@TempDir Path tempDir) {
        // Đường dẫn không thể ghi file (directory không tồn tại hoặc path trỏ tới thư mục)
        Path invalidPath = tempDir.resolve("non_existent_dir").resolve("file.csv");

        CsvWriterService writerService = new CsvWriterService() {
            @Override
            public void write(OrderCalculationResult result, Writer writer) {}

            @Override
            public void write(OrderCalculationResult result, OutputStream outputStream) {}
        };

        writerService.write(null, (Writer) null);
        writerService.write(null, (OutputStream) null);
        Throwable t4 = null;
        try {
            writerService.write(createDummyResult(), invalidPath);
        } catch (CsvVatException e) {
            t4 = e;
        }
        assertThat(t4).isInstanceOf(CsvVatException.class)
                .hasMessageContaining("Failed to write CSV to path");
    }

    @Test
    @DisplayName("CsvReaderService.read(Path): Rethrows CsvVatException khi stream reader ném ngoại lệ")
    void csvReaderService_defaultReadPath_rethrowsCsvVatException(@TempDir Path tempDir) throws IOException {
        Path inPath = tempDir.resolve("reader_input.csv");
        Files.writeString(inPath, "some,data", StandardCharsets.UTF_8);

        CsvReaderService readerService = new CsvReaderService() {
            @Override
            public List<OrderItem> read(Reader reader) { return List.of(); }

            @Override
            public List<OrderItem> read(InputStream inputStream) throws CsvVatException {
                throw new InvalidCsvFormatException("Corrupted header in stream");
            }
        };

        assertThat(readerService.read((Reader) null)).isEmpty();
        Throwable t5 = null;
        try {
            readerService.read(inPath);
        } catch (InvalidCsvFormatException e) {
            t5 = e;
        }
        assertThat(t5).isInstanceOf(InvalidCsvFormatException.class)
                .hasMessage("Corrupted header in stream");
    }
}
