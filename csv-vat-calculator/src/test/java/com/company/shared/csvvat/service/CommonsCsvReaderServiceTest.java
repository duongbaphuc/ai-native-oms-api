package com.company.shared.csvvat.service;

import com.company.shared.csvvat.config.CsvColumnMapping;
import com.company.shared.csvvat.exception.CsvRowValidationException;
import com.company.shared.csvvat.exception.CsvVatException;
import com.company.shared.csvvat.exception.InvalidCsvFormatException;
import com.company.shared.csvvat.model.OrderItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Kiểm thử CommonsCsvReaderService - Bộ đọc file CSV đơn hàng v2.0")
class CommonsCsvReaderServiceTest {

    private CommonsCsvReaderService defaultReader;

    @BeforeEach
    void setUp() {
        defaultReader = new CommonsCsvReaderService();
    }

    @Test
    @DisplayName("Khởi tạo bộ test CommonsCsvReaderServiceTest")
    void contextLoads() {
        assertThat(new CommonsCsvReaderServiceTest()).isNotNull();
        assertThat(new CommonsCsvReaderServiceTest().new ValidCsvReadingTests()).isNotNull();
        assertThat(new CommonsCsvReaderServiceTest().new DynamicMappingTests()).isNotNull();
        assertThat(new CommonsCsvReaderServiceTest().new HeaderValidationTests()).isNotNull();
        assertThat(new CommonsCsvReaderServiceTest().new RowValidationTests()).isNotNull();
        assertThat(new CommonsCsvReaderServiceTest().new EdgeCasesTests()).isNotNull();
        assertThat(new CommonsCsvReaderServiceTest().new ComprehensiveCoverageTests()).isNotNull();
    }

    @Nested
    @DisplayName("1. Đọc CSV hợp lệ và hỗ trợ đa ngôn ngữ / encoding")
    class ValidCsvReadingTests {

        @Test
        @DisplayName("Đọc file CSV tiếng Anh chuẩn không có inputLineTotal")
        void read_validEnglishCsv_shouldParseSuccessfully() {
            String csv = """
                    Item Name,Quantity,Unit Price,VAT (%)
                    Coffee Bean,2,15.50,10
                    Green Tea,3,10.00,8
                    """;

            List<OrderItem> items = defaultReader.read(new StringReader(csv));

            assertThat(items).hasSize(2);
            OrderItem item1 = items.get(0);
            assertThat(item1.rowNumber()).isEqualTo(2);
            assertThat(item1.itemName()).isEqualTo("Coffee Bean");
            assertThat(item1.quantity()).isEqualByComparingTo("2");
            assertThat(item1.unitPrice()).isEqualByComparingTo("15.50");
            assertThat(item1.vatPercentage()).isEqualByComparingTo("10");
            assertThat(item1.inputLineTotal()).isNull();
        }

        @Test
        @DisplayName("Đọc file CSV tiếng Việt có dấu với Alias Auto-Detect")
        void read_validVietnameseCsv_aliasAutoDetect_shouldParseSuccessfully() {
            String csv = """
                    Tên sản phẩm,Số lượng,Đơn giá,Thuế VAT,Thành tiền
                    Bánh mì xíu mại,2,35000,8,70000
                    Cà phê muối,3,28000,10,84000
                    """;

            List<OrderItem> items = defaultReader.read(new StringReader(csv));

            assertThat(items).hasSize(2);
            OrderItem item1 = items.get(0);
            assertThat(item1.itemName()).isEqualTo("Bánh mì xíu mại");
            assertThat(item1.quantity()).isEqualByComparingTo("2");
            assertThat(item1.unitPrice()).isEqualByComparingTo("35000");
            assertThat(item1.vatPercentage()).isEqualByComparingTo("8");
            assertThat(item1.inputLineTotal()).isNotNull();
            assertThat(item1.inputLineTotal()).isEqualByComparingTo("70000");
        }

        @Test
        @DisplayName("Xử lý file có ký tự UTF-8 BOM ở đầu file InputStream")
        void read_csvWithUtf8BomInInputStream_shouldStripBomAndParse() {
            String csv = """
                    Item Name,Quantity,Unit Price,VAT (%)
                    Trà sen,1,50000,10
                    """;
            byte[] bom = new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
            byte[] csvBytes = csv.getBytes(StandardCharsets.UTF_8);
            byte[] combined = new byte[bom.length + csvBytes.length];
            System.arraycopy(bom, 0, combined, 0, bom.length);
            System.arraycopy(csvBytes, 0, combined, bom.length, csvBytes.length);

            List<OrderItem> items = defaultReader.read(new ByteArrayInputStream(combined));

            assertThat(items).hasSize(1);
            assertThat(items.get(0).itemName()).isEqualTo("Trà sen");
        }

        @Test
        @DisplayName("Xử lý file có ký tự BOM \\uFEFF trực tiếp qua Reader")
        void read_csvWithBomInReader_shouldStripBomAndParse() {
            String csv = "\uFEFFItem Name,Quantity,Unit Price,VAT (%)\nNước khoáng,4,8000,0";
            List<OrderItem> items = defaultReader.read(new StringReader(csv));

            assertThat(items).hasSize(1);
            assertThat(items.get(0).itemName()).isEqualTo("Nước khoáng");
        }

        @Test
        @DisplayName("Hỗ trợ đọc từ Path file")
        void read_fromPath_shouldParseFileSuccessfully(@TempDir Path tempDir) throws IOException {
            Path csvFile = tempDir.resolve("orders.csv");
            String content = "Item Name,Quantity,Unit Price,VAT (%)\nBánh bao,2,15000,8";
            Files.writeString(csvFile, content, StandardCharsets.UTF_8);

            List<OrderItem> items = defaultReader.read(csvFile);

            assertThat(items).hasSize(1);
            assertThat(items.get(0).itemName()).isEqualTo("Bánh bao");
        }

        @Test
        @DisplayName("Tự động bỏ qua các dòng trống và xử lý VAT có ký hiệu %")
        void read_withBlankLinesAndPercentSymbol_shouldSkipAndParse() {
            String csv = """
                    Item Name,Quantity,Unit Price,VAT (%)
                    
                    Bánh quy bơ,2,40000,10%
                    
                    Kẹo dừa,5,20000,8.5%
                    
                    """;

            List<OrderItem> items = defaultReader.read(new StringReader(csv));

            assertThat(items).hasSize(2);
            assertThat(items.get(0).vatPercentage()).isEqualByComparingTo("10");
            assertThat(items.get(1).vatPercentage()).isEqualByComparingTo("8.5");
        }
    }

    @Nested
    @DisplayName("2. Dynamic Metadata Mapping với cấu hình tùy biến")
    class DynamicMappingTests {

        @Test
        @DisplayName("Cấu hình CsvColumnMapping tùy biến khớp hoàn toàn với CSV")
        void read_dynamicMapping_exactMatch_shouldParseAllColumns() {
            CsvColumnMapping mapping = CsvColumnMapping.builder()
                    .itemNameColumn("Tên hàng hóa")
                    .quantityColumn("SL")
                    .unitPriceColumn("Giá bán lẻ")
                    .vatPercentageColumn("% VAT")
                    .lineTotalColumn("Thành tiền đầu vào")
                    .build();

            CommonsCsvReaderService reader = new CommonsCsvReaderService(mapping);
            assertThat(reader.getColumnMapping()).isSameAs(mapping);

            String csv = """
                    Mã SP,Tên hàng hóa,SL,Giá bán lẻ,% VAT,Thành tiền đầu vào
                    SP01,Bánh mì xíu mại,2,35000,8,70000
                    SP02,Cà phê muối,3,28000,10,84000
                    """;

            List<OrderItem> items = reader.read(new StringReader(csv));

            assertThat(items).hasSize(2);
            OrderItem item1 = items.get(0);
            assertThat(item1.itemName()).isEqualTo("Bánh mì xíu mại");
            assertThat(item1.quantity()).isEqualByComparingTo("2");
            assertThat(item1.unitPrice()).isEqualByComparingTo("35000");
            assertThat(item1.vatPercentage()).isEqualByComparingTo("8");
            assertThat(item1.inputLineTotal()).isEqualByComparingTo("70000");
        }

        @Test
        @DisplayName("Cấu hình Dynamic thiếu cột trong CSV -> Ném InvalidCsvFormatException")
        void read_dynamicMapping_missingConfiguredColumn_shouldThrowInvalidCsvFormatException() {
            CsvColumnMapping mapping = CsvColumnMapping.builder()
                    .quantityColumn("So_Luong_Xuat")
                    .build();

            CommonsCsvReaderService reader = new CommonsCsvReaderService(mapping);

            String csv = """
                    Item Name,Quantity,Unit Price,VAT (%)
                    Bánh mì,2,20000,10
                    """;

            Throwable t = null;
            try {
                reader.read(new StringReader(csv));
            } catch (InvalidCsvFormatException e) {
                t = e;
            }
            assertThat(t)
                    .isInstanceOf(InvalidCsvFormatException.class)
                    .hasMessageContaining("Configured column for Quantity 'So_Luong_Xuat' was not found");
        }

        @Test
        @DisplayName("Cấu hình Dynamic với lineTotalColumn thiếu trong CSV -> Ném InvalidCsvFormatException")
        void read_dynamicMapping_missingLineTotalColumn_shouldThrowInvalidCsvFormatException() {
            CsvColumnMapping mapping = CsvColumnMapping.builder()
                    .lineTotalColumn("Custom_Total")
                    .build();

            CommonsCsvReaderService reader = new CommonsCsvReaderService(mapping);

            String csv = """
                    Item Name,Quantity,Unit Price,VAT (%)
                    Bánh mì,2,20000,10
                    """;

            Throwable t = null;
            try {
                reader.read(new StringReader(csv));
            } catch (InvalidCsvFormatException e) {
                t = e;
            }
            assertThat(t)
                    .isInstanceOf(InvalidCsvFormatException.class)
                    .hasMessageContaining("Configured column for Line Total 'Custom_Total' was not found");
        }

        @Test
        @DisplayName("Cấu hình Dynamic thiếu Item Name -> Ném InvalidCsvFormatException")
        void read_dynamicMapping_missingItemName_shouldThrowInvalidCsvFormatException() {
            CsvColumnMapping mapping = CsvColumnMapping.builder()
                    .itemNameColumn("Ten_San_Pham_Rieng")
                    .build();

            CommonsCsvReaderService reader = new CommonsCsvReaderService(mapping);
            String csv = "Item Name,Quantity,Unit Price,VAT (%)\nItem,1,10,10";

            Throwable t = null;
            try {
                reader.read(new StringReader(csv));
            } catch (InvalidCsvFormatException e) {
                t = e;
            }
            assertThat(t)
                    .isInstanceOf(InvalidCsvFormatException.class)
                    .hasMessageContaining("Configured column for Item Name 'Ten_San_Pham_Rieng' was not found");
        }

        @Test
        @DisplayName("Cấu hình Dynamic thiếu Unit Price -> Ném InvalidCsvFormatException")
        void read_dynamicMapping_missingUnitPrice_shouldThrowInvalidCsvFormatException() {
            CsvColumnMapping mapping = CsvColumnMapping.builder()
                    .unitPriceColumn("Gia_Rieng")
                    .build();

            CommonsCsvReaderService reader = new CommonsCsvReaderService(mapping);
            String csv = "Item Name,Quantity,Unit Price,VAT (%)\nItem,1,10,10";

            Throwable t = null;
            try {
                reader.read(new StringReader(csv));
            } catch (InvalidCsvFormatException e) {
                t = e;
            }
            assertThat(t)
                    .isInstanceOf(InvalidCsvFormatException.class)
                    .hasMessageContaining("Configured column for Unit Price 'Gia_Rieng' was not found");
        }

        @Test
        @DisplayName("Cấu hình Dynamic thiếu VAT Percentage -> Ném InvalidCsvFormatException")
        void read_dynamicMapping_missingVat_shouldThrowInvalidCsvFormatException() {
            CsvColumnMapping mapping = CsvColumnMapping.builder()
                    .vatPercentageColumn("Thue_Rieng")
                    .build();

            CommonsCsvReaderService reader = new CommonsCsvReaderService(mapping);
            String csv = "Item Name,Quantity,Unit Price,VAT (%)\nItem,1,10,10";

            Throwable t = null;
            try {
                reader.read(new StringReader(csv));
            } catch (InvalidCsvFormatException e) {
                t = e;
            }
            assertThat(t)
                    .isInstanceOf(InvalidCsvFormatException.class)
                    .hasMessageContaining("Configured column for VAT Percentage 'Thue_Rieng' was not found");
        }

        @Test
        @DisplayName("Cấu hình Dynamic chỉ chỉ định 1 cột: các cột còn lại fallback về alias matching")
        void read_dynamicMapping_partial_shouldFallbackToAliases() {
            CsvColumnMapping mapping = CsvColumnMapping.builder()
                    .quantityColumn("SL")
                    .build();

            CommonsCsvReaderService reader = new CommonsCsvReaderService(mapping);
            String csv = "Item Name,SL,Unit Price,VAT (%)\nItem,2,100,10";

            List<OrderItem> items = reader.read(new StringReader(csv));
            assertThat(items).hasSize(1);
            assertThat(items.get(0).quantity()).isEqualByComparingTo("2");
        }
    }

    @Nested
    @DisplayName("3. Kiểm tra Header và lỗi định dạng CSV")
    class HeaderValidationTests {

        @Test
        @DisplayName("CSV rỗng hoàn toàn -> Ném InvalidCsvFormatException")
        void read_emptyCsv_shouldThrowInvalidCsvFormatException() {
            Throwable t1 = null;
            try {
                defaultReader.read(new StringReader(""));
            } catch (InvalidCsvFormatException e) {
                t1 = e;
            }
            assertThat(t1)
                    .isInstanceOf(InvalidCsvFormatException.class)
                    .hasMessageContaining("empty or does not contain a header");

            Throwable t2 = null;
            try {
                defaultReader.read(new StringReader("   \n   \n"));
            } catch (InvalidCsvFormatException e) {
                t2 = e;
            }
            assertThat(t2).isInstanceOf(InvalidCsvFormatException.class);
        }

        @Test
        @DisplayName("CSV thiếu cột bắt buộc trong chế độ alias -> Ném InvalidCsvFormatException")
        void read_missingMandatoryVatColumn_shouldThrowInvalidCsvFormatException() {
            String csv = """
                    Item Name,Quantity,Unit Price
                    Bánh mì,2,20000
                    """;

            Throwable t = null;
            try {
                defaultReader.read(new StringReader(csv));
            } catch (InvalidCsvFormatException e) {
                t = e;
            }
            assertThat(t)
                    .isInstanceOf(InvalidCsvFormatException.class)
                    .hasMessageContaining("Mandatory column 'VAT (%)'");
        }

        @Test
        @DisplayName("CSV thiếu cột Item Name trong chế độ alias -> Ném InvalidCsvFormatException")
        void read_missingItemNameColumn_shouldThrowInvalidCsvFormatException() {
            String csv = """
                    Unknown,Quantity,Unit Price,VAT (%)
                    X,2,20000,10
                    """;

            Throwable t = null;
            try {
                defaultReader.read(new StringReader(csv));
            } catch (InvalidCsvFormatException e) {
                t = e;
            }
            assertThat(t)
                    .isInstanceOf(InvalidCsvFormatException.class)
                    .hasMessageContaining("Mandatory column 'Item Name'");
        }
    }

    @Nested
    @DisplayName("4. Bắt lỗi chi tiết từng dòng (CsvRowValidationException)")
    class RowValidationTests {

        @Test
        @DisplayName("Item Name bị rỗng hoặc khoảng trắng")
        void read_blankItemName_shouldThrowCsvRowValidationException() {
            String csv = """
                    Item Name,Quantity,Unit Price,VAT (%)
                    "  ",2,50000,10
                    """;

            CsvRowValidationException ex = null;
            try {
                defaultReader.read(new StringReader(csv));
            } catch (CsvRowValidationException e) {
                ex = e;
            }
            assertThat(ex).isNotNull();
            assertThat(ex.getRowNumber()).isEqualTo(2);
            assertThat(ex.getReason()).contains("Item name must not be blank");
        }

        @Test
        @DisplayName("Quantity không phải số hợp lệ")
        void read_invalidQuantity_shouldThrowCsvRowValidationException() {
            String csv = """
                    Item Name,Quantity,Unit Price,VAT (%)
                    Bánh mì,hai,50000,10
                    """;

            CsvRowValidationException ex = null;
            try {
                defaultReader.read(new StringReader(csv));
            } catch (CsvRowValidationException e) {
                ex = e;
            }
            assertThat(ex).isNotNull();
            assertThat(ex.getRowNumber()).isEqualTo(2);
            assertThat(ex.getInvalidValue()).isEqualTo("hai");
            assertThat(ex.getReason()).contains("Quantity is not a valid number");
        }

        @Test
        @DisplayName("Quantity là số âm")
        void read_negativeQuantity_shouldThrowCsvRowValidationException() {
            String csv = """
                    Item Name,Quantity,Unit Price,VAT (%)
                    Bánh mì,-5,50000,10
                    """;

            CsvRowValidationException ex = null;
            try {
                defaultReader.read(new StringReader(csv));
            } catch (CsvRowValidationException e) {
                ex = e;
            }
            assertThat(ex).isNotNull();
            assertThat(ex.getReason()).contains("Quantity cannot be negative");
        }

        @Test
        @DisplayName("Quantity bằng 0")
        void read_zeroQuantity_shouldThrowCsvRowValidationException() {
            String csv = """
                    Item Name,Quantity,Unit Price,VAT (%)
                    Bánh mì,0,50000,10
                    """;

            CsvRowValidationException ex = null;
            try {
                defaultReader.read(new StringReader(csv));
            } catch (CsvRowValidationException e) {
                ex = e;
            }
            assertThat(ex).isNotNull();
            assertThat(ex.getReason()).contains("Quantity must be greater than 0");
        }

        @Test
        @DisplayName("Unit Price không hợp lệ hoặc âm")
        void read_invalidUnitPrice_shouldThrowCsvRowValidationException() {
            String csv = """
                    Item Name,Quantity,Unit Price,VAT (%)
                    Bánh mì,2,-50000,10
                    """;

            CsvRowValidationException ex = null;
            try {
                defaultReader.read(new StringReader(csv));
            } catch (CsvRowValidationException e) {
                ex = e;
            }
            assertThat(ex).isNotNull();
            assertThat(ex.getReason()).contains("Unit price must not be negative");
        }

        @Test
        @DisplayName("VAT Percentage là số âm")
        void read_negativeVat_shouldThrowCsvRowValidationException() {
            String csv = """
                    Item Name,Quantity,Unit Price,VAT (%)
                    Bánh mì,2,50000,-10
                    """;

            CsvRowValidationException ex = null;
            try {
                defaultReader.read(new StringReader(csv));
            } catch (CsvRowValidationException e) {
                ex = e;
            }
            assertThat(ex).isNotNull();
            assertThat(ex.getReason()).contains("VAT percentage must not be negative");
        }

        @Test
        @DisplayName("Input Line Total không phải số hợp lệ hoặc âm")
        void read_invalidInputLineTotal_shouldThrowCsvRowValidationException() {
            String csv = """
                    Item Name,Quantity,Unit Price,VAT (%),Line Total
                    Bánh mì,2,50000,10,-100000
                    """;

            CsvRowValidationException ex = null;
            try {
                defaultReader.read(new StringReader(csv));
            } catch (CsvRowValidationException e) {
                ex = e;
            }
            assertThat(ex).isNotNull();
            assertThat(ex.getReason()).contains("Input line total must not be negative");
        }
    }

    @Nested
    @DisplayName("5. Các trường hợp biên và kiểm tra tham số (Edge Cases)")
    class EdgeCasesTests {

        @Test
        @DisplayName("Thiếu cột Quantity trong chế độ alias -> Ném InvalidCsvFormatException")
        void read_missingQuantityColumn_shouldThrowInvalidCsvFormatException() {
            String csv = "Item Name,Unit Price,VAT (%)\nItem,10,10";
            Throwable t = null;
            try {
                defaultReader.read(new StringReader(csv));
            } catch (InvalidCsvFormatException e) {
                t = e;
            }
            assertThat(t)
                    .isInstanceOf(InvalidCsvFormatException.class)
                    .hasMessageContaining("Mandatory column 'Quantity'");
        }

        @Test
        @DisplayName("Thiếu cột Unit Price trong chế độ alias -> Ném InvalidCsvFormatException")
        void read_missingUnitPriceColumn_shouldThrowInvalidCsvFormatException() {
            String csv = "Item Name,Quantity,VAT (%)\nItem,2,10";
            Throwable t = null;
            try {
                defaultReader.read(new StringReader(csv));
            } catch (InvalidCsvFormatException e) {
                t = e;
            }
            assertThat(t)
                    .isInstanceOf(InvalidCsvFormatException.class)
                    .hasMessageContaining("Mandatory column 'Unit Price'");
        }

        @Test
        @DisplayName("Đọc InputStream chuẩn không có BOM (dưới 3 byte hoặc không khớp BOM)")
        void read_inputStreamWithoutBom_shouldParseCorrectly() {
            String csv = "Item Name,Quantity,Unit Price,VAT (%)\nItem,1,10,10";
            List<OrderItem> items = defaultReader.read(new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)));
            assertThat(items).hasSize(1);
        }

        @Test
        @DisplayName("Truyền null vào read(InputStream) hoặc read(Reader): Ném NullPointerException")
        void read_nullInput_shouldThrowNullPointerException() {
            Throwable t1 = null;
            try {
                defaultReader.read((InputStream) null);
            } catch (NullPointerException e) {
                t1 = e;
            }
            assertThat(t1).isInstanceOf(NullPointerException.class);

            Throwable t2 = null;
            try {
                defaultReader.read((java.io.Reader) null);
            } catch (NullPointerException e) {
                t2 = e;
            }
            assertThat(t2).isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("Đọc từ file không tồn tại: Ném CsvVatException")
        void read_nonExistentPath_shouldThrowCsvVatException() {
            Throwable t = null;
            try {
                defaultReader.read(Path.of("non_existent_file.csv"));
            } catch (CsvVatException e) {
                t = e;
            }
            assertThat(t).isInstanceOf(CsvVatException.class);
        }

        @Test
        @DisplayName("Hàm normalizeForAlias với chuỗi null")
        void normalizeForAlias_nullInput_shouldReturnEmpty() {
            assertThat(CommonsCsvReaderService.normalizeForAlias(null)).isEmpty();
        }
    }

    @Nested
    @DisplayName("7. Kiểm thử chuyên sâu bao phủ 100% các nhánh và ngoại lệ (Comprehensive Coverage)")
    class ComprehensiveCoverageTests {

        @Test
        @DisplayName("read(InputStream): Xử lý các kích thước và byte không phải BOM")
        void read_inputStream_variousNonBomBytes() {
            // 0 byte
            Throwable t1 = null;
            try {
                defaultReader.read(new ByteArrayInputStream(new byte[0]));
            } catch (InvalidCsvFormatException e) {
                t1 = e;
            }
            assertThat(t1).isInstanceOf(InvalidCsvFormatException.class);

            // 1 byte
            Throwable t2 = null;
            try {
                defaultReader.read(new ByteArrayInputStream(new byte[]{'A'}));
            } catch (InvalidCsvFormatException e) {
                t2 = e;
            }
            assertThat(t2).isInstanceOf(InvalidCsvFormatException.class);

            // 2 byte
            Throwable t3 = null;
            try {
                defaultReader.read(new ByteArrayInputStream(new byte[]{(byte) 0xEF, 0x00}));
            } catch (InvalidCsvFormatException e) {
                t3 = e;
            }
            assertThat(t3).isInstanceOf(InvalidCsvFormatException.class);

            // 3 byte: byte đầu 0xEF, byte 2 0xBB, nhưng byte 3 không phải 0xBF
            Throwable t4 = null;
            try {
                defaultReader.read(new ByteArrayInputStream(new byte[]{(byte) 0xEF, (byte) 0xBB, 0x00}));
            } catch (InvalidCsvFormatException e) {
                t4 = e;
            }
            assertThat(t4).isInstanceOf(InvalidCsvFormatException.class);

            // 3 byte: byte đầu 0xEF, nhưng byte 2 không phải 0xBB
            Throwable t5 = null;
            try {
                defaultReader.read(new ByteArrayInputStream(new byte[]{(byte) 0xEF, 0x00, 0x00}));
            } catch (InvalidCsvFormatException e) {
                t5 = e;
            }
            assertThat(t5).isInstanceOf(InvalidCsvFormatException.class);
        }

        @Test
        @DisplayName("read(InputStream): Ném CsvVatException khi InputStream ném IOException")
        void read_inputStream_ioException_shouldThrowCsvVatException() {
            InputStream brokenStream = new InputStream() {
                @Override
                public int read() throws IOException {
                    throw new IOException("Simulated network failure");
                }

                @Override
                public int read(byte[] b, int off, int len) throws IOException {
                    throw new IOException("Simulated network failure");
                }
            };

            Throwable t = null;
            try {
                defaultReader.read(brokenStream);
            } catch (CsvVatException e) {
                t = e;
            }
            assertThat(t)
                    .isInstanceOf(CsvVatException.class)
                    .hasMessageContaining("Failed to read CSV from InputStream");
        }

        @Test
        @DisplayName("read(Reader): Đọc trực tiếp instance BufferedReader")
        void read_reader_alreadyBufferedReader_shouldParseCorrectly() {
            String csv = "Item Name,Quantity,Unit Price,VAT (%)\nItem1,2,50000,10\n";
            BufferedReader br = new BufferedReader(new StringReader(csv));
            List<OrderItem> items = defaultReader.read(br);
            assertThat(items).hasSize(1);
        }

        @Test
        @DisplayName("read(Reader): Header toàn khoảng trắng -> Ném InvalidCsvFormatException")
        void read_reader_allBlankHeaders_shouldThrowInvalidCsvFormatException() {
            String blankHeaders = "   ,   ,   \n";
            Throwable t = null;
            try {
                defaultReader.read(new StringReader(blankHeaders));
            } catch (InvalidCsvFormatException e) {
                t = e;
            }
            assertThat(t)
                    .isInstanceOf(InvalidCsvFormatException.class)
                    .hasMessageContaining("CSV file is empty");
        }

        @Test
        @DisplayName("read(Reader): File rỗng không có header -> Ném InvalidCsvFormatException")
        void read_reader_emptyCsv_shouldThrowInvalidCsvFormatException() {
            Throwable t = null;
            try {
                defaultReader.read(new StringReader(""));
            } catch (InvalidCsvFormatException e) {
                t = e;
            }
            assertThat(t)
                    .isInstanceOf(InvalidCsvFormatException.class)
                    .hasMessageContaining("CSV file is empty or does not contain a header row");
        }

        @Test
        @DisplayName("read(Reader): Bỏ qua dòng trống và dòng chỉ chứa dấu phẩy (isBlankRecord)")
        void read_reader_blankRecords_shouldBeIgnored() {
            String csv = """
                    Item Name,Quantity,Unit Price,VAT (%)
                    Item1,1,10000,0
                    ,,,
                       ,   ,   ,   
                    Item2,2,20000,0
                    """;
            List<OrderItem> items = defaultReader.read(new StringReader(csv));
            assertThat(items).hasSize(2);
            assertThat(items.get(0).itemName()).isEqualTo("Item1");
            assertThat(items.get(1).itemName()).isEqualTo("Item2");
        }

        @Test
        @DisplayName("read(Reader): Kiểm tra các lỗi rỗng hoặc sai định dạng của từng cột")
        void read_reader_columnValidationErrors() {
            // Quantity rỗng
            Throwable t1 = null;
            try {
                defaultReader.read(new StringReader("Item Name,Quantity,Unit Price,VAT (%)\nItem,,10000,0"));
            } catch (CsvRowValidationException e) {
                t1 = e;
            }
            assertThat(t1)
                    .isInstanceOf(CsvRowValidationException.class)
                    .hasMessageContaining("Quantity must not be blank");

            Throwable t2 = null;
            try {
                defaultReader.read(new StringReader("Item Name,Quantity,Unit Price,VAT (%)\nItem,   ,10000,0"));
            } catch (CsvRowValidationException e) {
                t2 = e;
            }
            assertThat(t2)
                    .isInstanceOf(CsvRowValidationException.class)
                    .hasMessageContaining("Quantity must not be blank");

            // Unit Price rỗng
            Throwable t3 = null;
            try {
                defaultReader.read(new StringReader("Item Name,Quantity,Unit Price,VAT (%)\nItem,1,,0"));
            } catch (CsvRowValidationException e) {
                t3 = e;
            }
            assertThat(t3)
                    .isInstanceOf(CsvRowValidationException.class)
                    .hasMessageContaining("Unit price must not be blank");

            Throwable t4 = null;
            try {
                defaultReader.read(new StringReader("Item Name,Quantity,Unit Price,VAT (%)\nItem,1,   ,0"));
            } catch (CsvRowValidationException e) {
                t4 = e;
            }
            assertThat(t4)
                    .isInstanceOf(CsvRowValidationException.class)
                    .hasMessageContaining("Unit price must not be blank");

            // Unit Price không phải số
            Throwable t5 = null;
            try {
                defaultReader.read(new StringReader("Item Name,Quantity,Unit Price,VAT (%)\nItem,1,INVALID_PRICE,0"));
            } catch (CsvRowValidationException e) {
                t5 = e;
            }
            assertThat(t5)
                    .isInstanceOf(CsvRowValidationException.class)
                    .hasMessageContaining("Unit price is not a valid number");

            // VAT rỗng
            Throwable t6 = null;
            try {
                defaultReader.read(new StringReader("Item Name,Quantity,Unit Price,VAT (%)\nItem,1,10000,"));
            } catch (CsvRowValidationException e) {
                t6 = e;
            }
            assertThat(t6)
                    .isInstanceOf(CsvRowValidationException.class)
                    .hasMessageContaining("VAT percentage must not be blank");

            Throwable t7 = null;
            try {
                defaultReader.read(new StringReader("Item Name,Quantity,Unit Price,VAT (%)\nItem,1,10000,   "));
            } catch (CsvRowValidationException e) {
                t7 = e;
            }
            assertThat(t7)
                    .isInstanceOf(CsvRowValidationException.class)
                    .hasMessageContaining("VAT percentage must not be blank");

            // VAT không phải số
            Throwable t8 = null;
            try {
                defaultReader.read(new StringReader("Item Name,Quantity,Unit Price,VAT (%)\nItem,1,10000,NOT_A_VAT%"));
            } catch (CsvRowValidationException e) {
                t8 = e;
            }
            assertThat(t8)
                    .isInstanceOf(CsvRowValidationException.class)
                    .hasMessageContaining("VAT percentage is not a valid number");

            // Line Total rỗng -> inputLineTotal là null
            List<OrderItem> itemsWithBlankTotal = defaultReader.read(new StringReader(
                    "Item Name,Quantity,Unit Price,VAT (%),Line Total\nItem,1,10000,0,   \n"
            ));
            assertThat(itemsWithBlankTotal.get(0).inputLineTotal()).isNull();

            // Line Total không phải số
            Throwable t9 = null;
            try {
                defaultReader.read(new StringReader("Item Name,Quantity,Unit Price,VAT (%),Line Total\nItem,1,10000,0,BAD_TOTAL"));
            } catch (CsvRowValidationException e) {
                t9 = e;
            }
            assertThat(t9)
                    .isInstanceOf(CsvRowValidationException.class)
                    .hasMessageContaining("Input line total is not a valid number");
        }

        @Test
        @DisplayName("read(Reader): Ném CsvVatException khi Reader ném IOException khi đọc")
        void read_reader_ioException_shouldThrowCsvVatException() {
            Reader brokenReader = new Reader() {
                @Override
                public int read(char[] cbuf, int off, int len) throws IOException {
                    throw new IOException("Simulated disk error during parse");
                }

                @Override
                public void close() {}
            };

            Throwable t = null;
            try {
                defaultReader.read(brokenReader);
            } catch (CsvVatException e) {
                t = e;
            }
            assertThat(t)
                    .isInstanceOf(CsvVatException.class)
                    .hasMessageContaining("I/O error during CSV parsing");
        }

        @Test
        @DisplayName("Dynamic Mapping nhưng chỉ định một phần: Các cột thiếu fallback về alias và ném lỗi nếu không tìm thấy")
        void read_partialDynamicMapping_missingAliasHeaders() {
            // Khi cấu hình chỉ có lineTotalColumn -> isDynamic() == true
            CsvColumnMapping partial = CsvColumnMapping.builder().lineTotalColumn("Line Total").build();
            CommonsCsvReaderService partialReader = new CommonsCsvReaderService(partial);

            // Thiếu Item Name
            Throwable t1 = null;
            try {
                partialReader.read(new StringReader("Quantity,Unit Price,VAT (%),Line Total\n1,10000,0,10000"));
            } catch (InvalidCsvFormatException e) {
                t1 = e;
            }
            assertThat(t1)
                    .isInstanceOf(InvalidCsvFormatException.class)
                    .hasMessageContaining("Mandatory column 'Item Name' (or alias) was not found");

            // Thiếu Quantity
            Throwable t2 = null;
            try {
                partialReader.read(new StringReader("Item Name,Unit Price,VAT (%),Line Total\nItem,10000,0,10000"));
            } catch (InvalidCsvFormatException e) {
                t2 = e;
            }
            assertThat(t2)
                    .isInstanceOf(InvalidCsvFormatException.class)
                    .hasMessageContaining("Mandatory column 'Quantity' (or alias) was not found");

            // Thiếu Unit Price
            Throwable t3 = null;
            try {
                partialReader.read(new StringReader("Item Name,Quantity,VAT (%),Line Total\nItem,1,0,10000"));
            } catch (InvalidCsvFormatException e) {
                t3 = e;
            }
            assertThat(t3)
                    .isInstanceOf(InvalidCsvFormatException.class)
                    .hasMessageContaining("Mandatory column 'Unit Price' (or alias) was not found");

            // Thiếu VAT
            Throwable t4 = null;
            try {
                partialReader.read(new StringReader("Item Name,Quantity,Unit Price,Line Total\nItem,1,10000,10000"));
            } catch (InvalidCsvFormatException e) {
                t4 = e;
            }
            assertThat(t4)
                    .isInstanceOf(InvalidCsvFormatException.class)
                    .hasMessageContaining("Mandatory column 'VAT (%)' (or alias) was not found");
        }

        @Test
        @DisplayName("normalizeForAlias: Chuẩn hóa ký tự Đ và d, chuỗi rỗng và BOM")
        void normalizeForAlias_additionalCases() {
            assertThat(CommonsCsvReaderService.normalizeForAlias("")).isEmpty();
            assertThat(CommonsCsvReaderService.normalizeForAlias("  ")).isEmpty();
            assertThat(CommonsCsvReaderService.normalizeForAlias("Đơn giá")).isEqualTo("dongia");
            assertThat(CommonsCsvReaderService.normalizeForAlias("đơn giá")).isEqualTo("dongia");
            assertThat(CommonsCsvReaderService.normalizeForAlias("\uFEFFItem Name")).isEqualTo("itemname");
        }

        @Test
        @DisplayName("findHeaderByExact & stripBom: Kiểm thử trực tiếp các nhánh null, empty và danh sách chứa null")
        void findHeaderByExactAndStripBom_cornerCases() {
            // target == null
            assertThat(CommonsCsvReaderService.findHeaderByExact(List.of("Header1"), null)).isNull();

            // headers chứa phần tử null
            java.util.List<String> headersWithNull = new java.util.ArrayList<>();
            headersWithNull.add(null);
            headersWithNull.add("TargetCol");
            assertThat(CommonsCsvReaderService.findHeaderByExact(headersWithNull, "TargetCol")).isEqualTo("TargetCol");
            assertThat(CommonsCsvReaderService.findHeaderByExact(headersWithNull, "NonExistent")).isNull();

            // stripBom null
            assertThat(CommonsCsvReaderService.stripBom(null)).isNull();
            assertThat(CommonsCsvReaderService.stripBom("NoBom")).isEqualTo("NoBom");
            assertThat(CommonsCsvReaderService.stripBom("\uFEFFHasBom")).isEqualTo("HasBom");
        }
    }
}
