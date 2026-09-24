package com.gpc.order.processor.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.gpc.order.processor.api.config.ColumnKey;
import com.gpc.order.processor.api.config.CsvConfig;
import com.gpc.order.processor.api.config.OutputMode;
import com.gpc.order.processor.api.exception.CsvProcessingException;
import com.gpc.order.processor.api.model.CsvProcessingResult;
import com.gpc.order.processor.api.model.OrderSummary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Map;

class CsvOrderProcessorTest {

  private CsvOrderProcessor processor;

  @BeforeEach
  void setUp() {
    processor = CsvOrderProcessor.create();
  }

  @Test
  @DisplayName("Test Case 1: Tính toán DATA_MODE (Có Header) - Khớp số liệu tính tay, CSV chỉ chứa mặt hàng")
  void testCase1_DataModeWithHeader() throws Exception {
    // Arrange: Đọc từ test resource file
    InputStream is = getClass().getResourceAsStream("/test-data/valid_order_with_header.csv");
    assertThat(is).isNotNull();
    String csvContent = new String(is.readAllBytes(), StandardCharsets.UTF_8);

    CsvConfig config = CsvConfig.builder()
        .hasHeader(true)
        .delimiter(',')
        .columnMapping(Map.of(
            "Số lượng", ColumnKey.QUANTITY,
            "Đơn giá", ColumnKey.UNIT_PRICE,
            "% VAT", ColumnKey.VAT_RATE))
        .outputMode(OutputMode.DATA_MODE)
        .build();

    // Act
    CsvProcessingResult result = processor.process(csvContent, config);

    // Assert
    OrderSummary summary = result.summary();
    assertThat(summary.subtotal()).isEqualByComparingTo(new BigDecimal("140000.00"));
    assertThat(summary.totalVat()).isEqualByComparingTo(new BigDecimal("11300.00"));
    assertThat(summary.finalTotal()).isEqualByComparingTo(new BigDecimal("151300.00"));

    assertThat(result.lineItems()).hasSize(3);
    assertThat(result.lineItems().get(0).vatAmount()).isEqualByComparingTo(new BigDecimal("5000.00"));
    assertThat(result.lineItems().get(1).vatAmount()).isEqualByComparingTo(new BigDecimal("4800.00"));
    assertThat(result.lineItems().get(2).vatAmount()).isEqualByComparingTo(new BigDecimal("1500.00"));

    String outputCsv = result.outputCsvContent();
    assertThat(outputCsv).contains("Tiền VAT");
    assertThat(outputCsv).doesNotContain("Tổng trước thuế");
    assertThat(outputCsv).doesNotContain("Tổng VAT");
    assertThat(outputCsv).doesNotContain("Tổng thanh toán");

    String[] lines = outputCsv.trim().split("\n");
    assertThat(lines).hasSize(4); // 1 header + 3 item rows
    assertThat(lines[0].trim()).isEqualTo("Mã SP,Tên SP,Số lượng,Đơn giá,% VAT,Tiền VAT");
  }

  @Test
  @DisplayName("Test Case 2: Tạo báo cáo REPORT_MODE (Không Header) - Map index, output chứa 3 dòng footer ở cuối")
  void testCase2_ReportModeWithoutHeader() throws Exception {
    // Arrange
    InputStream is = getClass().getResourceAsStream("/test-data/valid_order_no_header.csv");
    assertThat(is).isNotNull();
    String csvContent = new String(is.readAllBytes(), StandardCharsets.UTF_8);

    CsvConfig config = CsvConfig.builder()
        .hasHeader(false)
        .delimiter(';')
        .columnMapping(Map.of(
            2, ColumnKey.QUANTITY,
            3, ColumnKey.UNIT_PRICE,
            4, ColumnKey.VAT_RATE))
        .outputMode(OutputMode.REPORT_MODE)
        .build();

    // Act
    CsvProcessingResult result = processor.process(csvContent, config);

    // Assert
    assertThat(result.summary().subtotal()).isEqualByComparingTo(new BigDecimal("84000.00"));
    assertThat(result.summary().totalVat()).isEqualByComparingTo(new BigDecimal("7720.00"));
    assertThat(result.summary().finalTotal()).isEqualByComparingTo(new BigDecimal("91720.00"));

    String outputCsv = result.outputCsvContent();
    String[] lines = outputCsv.trim().split("\n");

    // 2 dòng mặt hàng + 3 dòng footer = 5 dòng
    assertThat(lines).hasSize(5);

    String footer1 = lines[2].trim();
    String footer2 = lines[3].trim();
    String footer3 = lines[4].trim();

    assertThat(footer1).startsWith("Tổng trước thuế;").contains("84000.00");
    assertThat(footer2).startsWith("Tổng VAT;").contains("7720.00");
    assertThat(footer3).startsWith("Tổng thanh toán;").contains("91720.00");
  }

  @Test
  @DisplayName("Test Case 3: Kiểm tra làm tròn (HALF_UP, 2 số lẻ) - 1234.565 -> 1234.57, tổng cộng từ dòng đã làm tròn")
  void testCase3_SupermarketRoundingHalfUp() {
    // Arrange
    String csvContent = """
        Mã SP,Số lượng,Đơn giá,% VAT
        SP1,1,12345.65,10
        SP2,1,12345.64,10
        """;

    CsvConfig config = CsvConfig.builder()
        .hasHeader(true)
        .delimiter(',')
        .columnMapping(Map.of(
            "Số lượng", ColumnKey.QUANTITY,
            "Đơn giá", ColumnKey.UNIT_PRICE,
            "% VAT", ColumnKey.VAT_RATE))
        .outputMode(OutputMode.DATA_MODE)
        .build();

    // Act
    CsvProcessingResult result = processor.process(csvContent, config);

    // Assert: 1234.565 -> 1234.57
    assertThat(result.lineItems().get(0).vatAmount())
        .isEqualByComparingTo(new BigDecimal("1234.57"));

    // 1234.564 -> 1234.56
    assertThat(result.lineItems().get(1).vatAmount())
        .isEqualByComparingTo(new BigDecimal("1234.56"));

    // Tổng VAT là tổng của các dòng đã làm tròn: 1234.57 + 1234.56 = 2469.13
    assertThat(result.summary().totalVat())
        .isEqualByComparingTo(new BigDecimal("2469.13"));

    assertThat(result.summary().subtotal())
        .isEqualByComparingTo(new BigDecimal("24691.29"));

    assertThat(result.summary().finalTotal())
        .isEqualByComparingTo(new BigDecimal("27160.42"));
  }

  @Test
  @DisplayName("Test Case 4: Dữ liệu rác (Strict Validation) - Ném CsvProcessingException có số dòng và cột lỗi")
  void testCase4_StrictValidationInvalidDataType() throws Exception {
    // Arrange
    InputStream is = getClass().getResourceAsStream("/test-data/dirty_invalid_order.csv");
    assertThat(is).isNotNull();
    String csvContent = new String(is.readAllBytes(), StandardCharsets.UTF_8);

    CsvConfig config = CsvConfig.builder()
        .hasHeader(true)
        .delimiter(',')
        .columnMapping(Map.of(
            "Số lượng", ColumnKey.QUANTITY,
            "Đơn giá", ColumnKey.UNIT_PRICE,
            "% VAT", ColumnKey.VAT_RATE))
        .outputMode(OutputMode.DATA_MODE)
        .build();

    // Act & Assert
    assertThatThrownBy(() -> processor.process(csvContent, config))
        .isInstanceOf(CsvProcessingException.class)
        .satisfies(e -> {
          CsvProcessingException ex = (CsvProcessingException) e;
          assertThat(ex.getLineNumber()).isEqualTo(3);
          assertThat(ex.getColumnIdentifier()).isEqualTo("Số lượng");
          assertThat(ex.getMessage()).contains("line 3");
          assertThat(ex.getMessage()).contains("Số lượng");
        });
  }

  @Test
  @DisplayName("Test Case 5: Cấu hình Metadata sai - Khai báo sai tên cột ném IllegalArgumentException")
  void testCase5_InvalidMetadataColumnName() {
    // Arrange
    String csvContent = """
        Mã SP,Số lượng,Đơn giá,% VAT
        SP1,5,1000.00,10
        """;

    CsvConfig config = CsvConfig.builder()
        .hasHeader(true)
        .delimiter(',')
        .columnMapping(Map.of(
            "Số lượng", ColumnKey.QUANTITY,
            "Đơn giá", ColumnKey.UNIT_PRICE,
            "Thue VAT", ColumnKey.VAT_RATE))
        .outputMode(OutputMode.DATA_MODE)
        .build();

    // Act & Assert
    assertThatThrownBy(() -> processor.process(csvContent, config))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Thue VAT");
  }
}
