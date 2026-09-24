package com.gpc.order.processor.api;

import com.gpc.order.processor.api.config.ColumnKey;
import com.gpc.order.processor.api.config.CsvConfig;
import com.gpc.order.processor.api.config.MetadataConfig;
import com.gpc.order.processor.api.config.OutputMode;
import com.gpc.order.processor.api.exception.CsvProcessingException;
import com.gpc.order.processor.api.model.CsvProcessingResult;
import com.gpc.order.processor.api.model.OrderSummary;
import com.gpc.order.processor.internal.calculator.SupermarketVatCalculator;
import com.gpc.order.processor.internal.csv.CsvWriter;
import com.gpc.order.processor.internal.csv.ExcelCsvParser;
import com.gpc.order.processor.internal.validator.StrictRowValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HighCoverageEdgeCasesTest {

  private final CsvOrderProcessor processor = CsvOrderProcessor.create();

  @Test
  @DisplayName("Cover CsvProcessingResult byte and stream methods")
  void testCsvProcessingResultByteMethods() {
    CsvProcessingResult result = new CsvProcessingResult(
        "A,B\n1,2\n", OrderSummary.zero(), List.of(), OutputMode.DATA_MODE);

    byte[] utf8 = result.getCsvBytes();
    assertThat(new String(utf8, StandardCharsets.UTF_8)).isEqualTo("A,B\n1,2\n");

    byte[] custom = result.getCsvBytes(StandardCharsets.ISO_8859_1);
    assertThat(custom).isNotEmpty();

    InputStream is = result.getCsvInputStream();
    assertThat(is).isNotNull().hasContent("A,B\n1,2\n");
  }

  @Test
  @DisplayName("Cover CsvWriter null/empty edge cases")
  void testCsvWriterNullAndEmpty() {
    assertThat(CsvWriter.formatRow(null, ',')).isEmpty();
    assertThat(CsvWriter.formatRow(List.of(), ',')).isEmpty();
    assertThat(CsvWriter.escapeField(null, ',')).isEmpty();
    assertThat(CsvWriter.escapeField("Simple", ',')).isEqualTo("Simple");
    assertThat(CsvWriter.escapeField("With,Comma", ',')).isEqualTo("\"With,Comma\"");
    assertThat(CsvWriter.escapeField("With\"Quote", ',')).isEqualTo("\"With\"\"Quote\"");
    assertThat(CsvWriter.escapeField("With\nNewline", ',')).isEqualTo("\"With\nNewline\"");
    assertThat(CsvWriter.escapeField("With\rReturn", ',')).isEqualTo("\"With\rReturn\"");
  }

  @Test
  @DisplayName("Cover ExcelCsvParser CRLF and null edge cases")
  void testExcelCsvParserEdgeCases() {
    assertThat(ExcelCsvParser.parse(null, ',')).isEmpty();
    assertThat(ExcelCsvParser.parse("", ',')).isEmpty();

    // CRLF (\r\n)
    String crlfCsv = "A,B\r\n1,2\r\n3,4\r\n";
    List<List<String>> rows1 = ExcelCsvParser.parse(crlfCsv, ',');
    assertThat(rows1).hasSize(3);

    // CR alone (\r)
    String crOnlyCsv = "A,B\r1,2\r";
    List<List<String>> rows2 = ExcelCsvParser.parse(crOnlyCsv, ',');
    assertThat(rows2).hasSize(2);

    // CR followed by non-newline character
    String crCharCsv = "A,B\rC,D";
    List<List<String>> rows4 = ExcelCsvParser.parse(crCharCsv, ',');
    assertThat(rows4).hasSize(2);

    // Quoted field with quote at EOF
    String quoteEof = "\"Unclosed quote";
    List<List<String>> rows3 = ExcelCsvParser.parse(quoteEof, ',');
    assertThat(rows3).hasSize(1);
  }

  @Test
  @DisplayName("Cover StrictRowValidator and SupermarketVatCalculator null cases")
  void testStrictValidatorAndCalculatorNull() {
    assertThat(SupermarketVatCalculator.roundMoney(null)).isEqualByComparingTo("0.00");
    assertThat(SupermarketVatCalculator.calculateOrderSummary(null)).isEqualTo(OrderSummary.zero());
    assertThat(SupermarketVatCalculator.calculateOrderSummary(List.of())).isEqualTo(OrderSummary.zero());

    // Row missing cell value (null or empty)
    assertThatThrownBy(() -> StrictRowValidator.extractCellValue(1, Collections.singletonList("   "), 0, "Col"))
        .isInstanceOf(CsvProcessingException.class)
        .hasMessageContaining("Required cell value is empty or missing");
  }

  @Test
  @DisplayName("Cover MetadataConfig.Builder columnMapping null check and methods")
  void testMetadataConfigBuilder() {
    MetadataConfig.Builder builder = MetadataConfig.builder();
    builder.hasHeader(false);
    builder.delimiter(';');
    builder.outputMode(OutputMode.REPORT_MODE);
    builder.columnMapping(null); // clears
    builder.mapColumn(0, ColumnKey.QUANTITY);
    builder.mapColumn(1, ColumnKey.UNIT_PRICE);
    builder.mapColumn(2, ColumnKey.VAT_RATE);

    Map<Object, ColumnKey> map = new HashMap<>();
    map.put(0, ColumnKey.QUANTITY);
    map.put(1, ColumnKey.UNIT_PRICE);
    map.put(2, ColumnKey.VAT_RATE);
    builder.columnMapping(map);

    MetadataConfig config = builder.build();
    assertThat(config.hasHeader()).isFalse();
    assertThat(config.delimiter()).isEqualTo(';');
    assertThat(config.outputMode()).isEqualTo(OutputMode.REPORT_MODE);
  }

  @Test
  @DisplayName("Cover DefaultCsvOrderProcessor integer mapping with Header mode")
  void testHeaderWithIntegerColumnMapping() {
    String csv = "SL,Gia,VAT\n2,500,10";
    CsvConfig config = CsvConfig.builder()
        .hasHeader(true)
        .delimiter(',')
        .mapColumn(0, ColumnKey.QUANTITY)
        .mapColumn(1, ColumnKey.UNIT_PRICE)
        .mapColumn(2, ColumnKey.VAT_RATE)
        .outputMode(OutputMode.REPORT_MODE)
        .build();

    CsvProcessingResult result = processor.process(csv, config);
    assertThat(result.lineItems()).hasSize(1);
    assertThat(result.summary().finalTotal()).isEqualByComparingTo("1100.00");
    assertThat(result.outputCsvContent()).contains("Tổng trước thuế");
  }

  @Test
  @DisplayName("Cover DefaultCsvOrderProcessor file not found and IO exceptions")
  void testFileNotFoundIOException() {
    CsvConfig config = CsvConfig.builder()
        .hasHeader(true)
        .delimiter(',')
        .mapColumn("A", ColumnKey.QUANTITY)
        .mapColumn("B", ColumnKey.UNIT_PRICE)
        .mapColumn("C", ColumnKey.VAT_RATE)
        .build();

    Path nonExistent = Path.of("c:/this_file_does_not_exist_at_all_123456789.csv");
    assertThatThrownBy(() -> processor.process(nonExistent, config))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Failed to read CSV file");
  }

  @Test
  @DisplayName("Cover CsvProcessingException getters and constructor with cause")
  void testCsvProcessingException() {
    Throwable cause = new IllegalArgumentException("Root cause");
    CsvProcessingException ex = new CsvProcessingException(5, "ColX", "Custom msg", cause);
    assertThat(ex.getLineNumber()).isEqualTo(5);
    assertThat(ex.getColumnIdentifier()).isEqualTo("ColX");
    assertThat(ex.getCause()).isEqualTo(cause);
    assertThat(ex.getMessage()).contains("line 5").contains("ColX").contains("Custom msg");
  }

  @Test
  @DisplayName("Cover OrderProcessor factory and default processToOrder overloads")
  void testOrderProcessorFactoryAndOverloads() throws Exception {
    OrderProcessor proc = OrderProcessor.forCsv();
    assertThat(proc).isNotNull();

    MetadataConfig meta = MetadataConfig.builder()
        .hasHeader(true)
        .mapColumn("SL", ColumnKey.QUANTITY)
        .mapColumn("Gia", ColumnKey.UNIT_PRICE)
        .mapColumn("VAT", ColumnKey.VAT_RATE)
        .build();
    CsvConfig csvConfig = meta.toCsvConfig();

    String csv = "SL,Gia,VAT\n2,100,10\n";
    com.gpc.order.processor.api.model.Order order1 = proc.processToOrder("ORD-001", csv, csvConfig);
    assertThat(order1.orderId()).isEqualTo("ORD-001");
    assertThat(order1.items()).hasSize(1);

    com.gpc.order.processor.api.model.Order order2 = proc.processToOrder(
        "ORD-002", new java.io.ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)), meta);
    assertThat(order2.orderId()).isEqualTo("ORD-002");
    assertThat(order2.items()).hasSize(1);

    com.gpc.order.processor.api.model.Order order3 = proc.processToOrder(
        "ORD-003", new java.io.ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)), csvConfig);
    assertThat(order3.orderId()).isEqualTo("ORD-003");

    Path tempFile = java.nio.file.Files.createTempFile("order_proc_test", ".csv");
    try {
      java.nio.file.Files.writeString(tempFile, csv, StandardCharsets.UTF_8);
      com.gpc.order.processor.api.model.Order order4 = proc.processToOrder("ORD-004", tempFile, csvConfig);
      assertThat(order4.orderId()).isEqualTo("ORD-004");

      com.gpc.order.processor.api.model.Order order5 = proc.processToOrder("ORD-005", tempFile, meta);
      assertThat(order5.orderId()).isEqualTo("ORD-005");
    } finally {
      java.nio.file.Files.deleteIfExists(tempFile);
    }
  }

  @Test
  @DisplayName("Cover OrderItem validation, ofWithLineTotal, and roundMoney null")
  void testOrderItemEdgeCases() {
    assertThatThrownBy(() -> new com.gpc.order.processor.api.model.OrderItem(
        0, List.of(), BigDecimal.ONE, BigDecimal.TEN, BigDecimal.TEN, BigDecimal.TEN, BigDecimal.ONE, new BigDecimal("11")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("lineNumber must be positive");

    com.gpc.order.processor.api.model.OrderItem item = com.gpc.order.processor.api.model.OrderItem.ofWithLineTotal(
        1, List.of("100", "10"), new BigDecimal("100.00"), new BigDecimal("10"));
    assertThat(item.lineTotal()).isEqualByComparingTo("100.00");
    assertThat(item.vatAmount()).isEqualByComparingTo("10.00");
    assertThat(item.lineTotalWithVat()).isEqualByComparingTo("110.00");
    assertThat(item.quantity()).isNull();
    assertThat(item.unitPrice()).isNull();

    assertThat(com.gpc.order.processor.api.model.OrderItem.roundMoney(null)).isEqualByComparingTo("0.00");

    // null rawValues defensive copy
    com.gpc.order.processor.api.model.OrderItem nullRaw = new com.gpc.order.processor.api.model.OrderItem(
        1, null, BigDecimal.ONE, BigDecimal.TEN, BigDecimal.TEN, BigDecimal.TEN, BigDecimal.ONE, new BigDecimal("11"));
    assertThat(nullRaw.rawValues()).isEmpty();

    // calculateLineTotal null checks
    assertThatThrownBy(() -> com.gpc.order.processor.api.model.OrderItem.calculateLineTotal(null, BigDecimal.TEN))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> com.gpc.order.processor.api.model.OrderItem.calculateLineTotal(BigDecimal.ONE, null))
        .isInstanceOf(NullPointerException.class);

    // calculateVatAmount null checks
    assertThatThrownBy(() -> com.gpc.order.processor.api.model.OrderItem.calculateVatAmount(null, BigDecimal.TEN))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> com.gpc.order.processor.api.model.OrderItem.calculateVatAmount(BigDecimal.TEN, null))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  @DisplayName("Cover CsvProcessingResult with null lineItems defensive copy")
  void testCsvProcessingResultNullLineItems() {
    CsvProcessingResult res = new CsvProcessingResult(
        "header\n", OrderSummary.zero(), null, OutputMode.DATA_MODE);
    assertThat(res.lineItems()).isEmpty();
  }

  @Test
  @DisplayName("Cover MetadataConfig validation and conversion")
  void testMetadataConfigValidationAndConversion() {
    Map<Object, ColumnKey> validMap = Map.of("VAT", ColumnKey.VAT_RATE, "Total", ColumnKey.TOTAL);

    assertThatThrownBy(() -> new MetadataConfig(true, '\0', validMap, OutputMode.DATA_MODE))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Invalid CSV delimiter");

    assertThatThrownBy(() -> new MetadataConfig(true, '\n', validMap, OutputMode.DATA_MODE))
        .isInstanceOf(IllegalArgumentException.class);

    assertThatThrownBy(() -> new MetadataConfig(true, '\r', validMap, OutputMode.DATA_MODE))
        .isInstanceOf(IllegalArgumentException.class);

    assertThatThrownBy(() -> new MetadataConfig(true, '"', validMap, OutputMode.DATA_MODE))
        .isInstanceOf(IllegalArgumentException.class);

    assertThatThrownBy(() -> new MetadataConfig(true, ',', Collections.emptyMap(), OutputMode.DATA_MODE))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("columnMapping must contain at least required columns");

    CsvConfig csvConfig = CsvConfig.builder()
        .hasHeader(true)
        .delimiter(';')
        .mapColumn("VAT", ColumnKey.VAT_RATE)
        .mapColumn("Total", ColumnKey.TOTAL)
        .outputMode(OutputMode.REPORT_MODE)
        .build();

    MetadataConfig fromConfig = MetadataConfig.from(csvConfig);
    assertThat(fromConfig.delimiter()).isEqualTo(';');
    assertThat(fromConfig.toCsvConfig().delimiter()).isEqualTo(';');
  }

  @Test
  @DisplayName("Cover DefaultCsvOrderProcessor mapping validation edge cases")
  void testMappingValidationEdgeCases() {
    // 1. Missing VAT_RATE
    CsvConfig missingVat = CsvConfig.builder()
        .hasHeader(true)
        .mapColumn("SL", ColumnKey.QUANTITY)
        .mapColumn("Gia", ColumnKey.UNIT_PRICE)
        .build();
    assertThatThrownBy(() -> processor.process("SL,Gia\n1,10", missingVat))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Missing required column mapping for VAT_RATE");

    // 2. Missing QUANTITY and UNIT_PRICE and TOTAL
    CsvConfig missingTotals = CsvConfig.builder()
        .hasHeader(true)
        .mapColumn("VAT", ColumnKey.VAT_RATE)
        .build();
    assertThatThrownBy(() -> processor.process("VAT\n10", missingTotals))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Missing required column mapping: must map either (QUANTITY and UNIT_PRICE) or TOTAL");

    // 3. Reverse mapping in normalizeMapping (key is ColumnKey)
    @SuppressWarnings({"rawtypes", "unchecked"})
    Map rawMap = new HashMap();
    rawMap.put(ColumnKey.VAT_RATE, "VAT");
    rawMap.put(ColumnKey.TOTAL, "Total");
    @SuppressWarnings("unchecked")
    CsvConfig reverseConfig = new CsvConfig(true, ',', (Map<Object, ColumnKey>) rawMap, OutputMode.DATA_MODE);
    CsvProcessingResult res = processor.process("VAT,Total\n10,100", reverseConfig);
    assertThat(res.lineItems()).hasSize(1);

    // 4. Invalid mapping entry (neither key nor value is ColumnKey)
    @SuppressWarnings({"rawtypes", "unchecked"})
    Map invalidMap = new HashMap();
    invalidMap.put("BadKey", "BadValue");
    @SuppressWarnings("unchecked")
    CsvConfig badConfig = new CsvConfig(true, ',', (Map<Object, ColumnKey>) invalidMap, OutputMode.DATA_MODE);
    assertThatThrownBy(() -> processor.process("A,B\n1,2", badConfig))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Invalid mapping entry");

    // 5. Header column not found by name
    CsvConfig notFoundCol = CsvConfig.builder()
        .hasHeader(true)
        .mapColumn("NonExistent", ColumnKey.VAT_RATE)
        .mapColumn("Total", ColumnKey.TOTAL)
        .build();
    assertThatThrownBy(() -> processor.process("VAT,Total\n10,100", notFoundCol))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("declared in configuration was not found in CSV header");

    // 6. Header column index out of bounds
    CsvConfig oobCol = CsvConfig.builder()
        .hasHeader(true)
        .mapColumn(99, ColumnKey.VAT_RATE)
        .mapColumn(0, ColumnKey.TOTAL)
        .build();
    assertThatThrownBy(() -> processor.process("Total,VAT\n100,10", oobCol))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("out of bounds for header");

    CsvConfig negativeHeaderCol = CsvConfig.builder()
        .hasHeader(true)
        .mapColumn(-1, ColumnKey.VAT_RATE)
        .mapColumn(0, ColumnKey.TOTAL)
        .build();
    assertThatThrownBy(() -> processor.process("Total,VAT\n100,10", negativeHeaderCol))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("out of bounds for header");

    // 7. Unsupported column identifier type in header mode
    Map<Object, ColumnKey> unsupportedIdMap = Map.of(1.5, ColumnKey.VAT_RATE, 0, ColumnKey.TOTAL);
    CsvConfig unsupportedConfig = new CsvConfig(true, ',', unsupportedIdMap, OutputMode.DATA_MODE);
    assertThatThrownBy(() -> processor.process("Total,VAT\n100,10", unsupportedConfig))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Unsupported column identifier type");

    // 8. No header mode: non-numeric string name
    CsvConfig noHeaderNonNumeric = CsvConfig.builder()
        .hasHeader(false)
        .mapColumn("NotANumber", ColumnKey.VAT_RATE)
        .mapColumn(0, ColumnKey.TOTAL)
        .build();
    assertThatThrownBy(() -> processor.process("100,10", noHeaderNonNumeric))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Cannot map column by non-numeric String name");

    // 9. No header mode: negative column index
    CsvConfig noHeaderNegative = CsvConfig.builder()
        .hasHeader(false)
        .mapColumn(-1, ColumnKey.VAT_RATE)
        .mapColumn(0, ColumnKey.TOTAL)
        .build();
    assertThatThrownBy(() -> processor.process("100,10", noHeaderNegative))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Column index cannot be negative");

    // 10. No header mode: unsupported identifier type
    Map<Object, ColumnKey> noHeaderUnsupportedId = Map.of(1.5, ColumnKey.VAT_RATE, 0, ColumnKey.TOTAL);
    CsvConfig noHeaderUnsupported = new CsvConfig(false, ',', noHeaderUnsupportedId, OutputMode.DATA_MODE);
    assertThatThrownBy(() -> processor.process("100,10", noHeaderUnsupported))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Unsupported column identifier type");
  }

  @Test
  @DisplayName("Cover percentage suffix, Reader IO exception, and report footer with empty rows")
  void testAdditionalEdgeCases() {
    // VAT rate with % sign: e.g. "10%"
    String csvWithPercent = "Total,VAT\n100,10%\n";
    CsvConfig config = CsvConfig.builder()
        .hasHeader(true)
        .mapColumn("Total", ColumnKey.TOTAL)
        .mapColumn("VAT", ColumnKey.VAT_RATE)
        .build();
    CsvProcessingResult res = processor.process(csvWithPercent, config);
    assertThat(res.lineItems().get(0).vatRate()).isEqualByComparingTo("10");

    // Report mode footer when header present but 0 data rows
    CsvConfig reportConfig = CsvConfig.builder()
        .hasHeader(true)
        .mapColumn("Total", ColumnKey.TOTAL)
        .mapColumn("VAT", ColumnKey.VAT_RATE)
        .outputMode(OutputMode.REPORT_MODE)
        .build();
    CsvProcessingResult emptyDataReport = processor.process("Total,VAT\n", reportConfig);
    assertThat(emptyDataReport.outputCsvContent()).contains("Tổng trước thuế");

    // StrictRowValidator with null cell value
    assertThatThrownBy(() -> StrictRowValidator.extractCellValue(1, Collections.singletonList(null), 0, "Col"))
        .isInstanceOf(CsvProcessingException.class)
        .hasMessageContaining("Required cell value is empty or missing");

    // Reader that throws IOException
    Reader brokenReader = new Reader() {
      @Override
      public int read(char[] cbuf, int off, int len) throws java.io.IOException {
        throw new java.io.IOException("Forced test exception");
      }

      @Override
      public void close() {}
    };
    assertThatThrownBy(() -> processor.process(brokenReader, config))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Failed to read CSV content from reader");

    // ExcelCsvParser escaped quote inside quotes: "a""b"
    List<List<String>> parsedEscaped = ExcelCsvParser.parse("\"a\"\"b\",c\n", ',');
    assertThat(parsedEscaped.get(0).get(0)).isEqualTo("a\"b");
  }
}
