package com.gpc.order.processor.api;

import com.gpc.order.processor.api.config.ColumnKey;
import com.gpc.order.processor.api.config.CsvConfig;
import com.gpc.order.processor.api.config.MetadataConfig;
import com.gpc.order.processor.api.config.OutputMode;
import com.gpc.order.processor.api.model.CsvProcessingResult;
import com.gpc.order.processor.api.model.Order;
import com.gpc.order.processor.api.model.OrderItem;
import com.gpc.order.processor.api.model.OrderSummary;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.StringReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderProcessorOverloadsTest {

  private final CsvOrderProcessor processor = CsvOrderProcessor.create();

  @Test
  @DisplayName("Kiểm tra OrderProcessor.forCsv() factory method")
  void testForCsvFactory() {
    OrderProcessor p = OrderProcessor.forCsv();
    assertThat(p).isNotNull();
    assertThat(p).isInstanceOf(CsvOrderProcessor.class);
  }

  @Test
  @DisplayName("Kiểm tra các overload của MetadataConfig trên OrderProcessor (Reader, InputStream, Path, String)")
  void testMetadataConfigOverloads() throws Exception {
    String csv = """
        Mã SP,Số lượng,Đơn giá,% VAT
        SP01,2,50000.00,10
        """;

    MetadataConfig config = MetadataConfig.builder()
        .hasHeader(true)
        .delimiter(',')
        .mapColumn("Số lượng", ColumnKey.QUANTITY)
        .mapColumn("Đơn giá", ColumnKey.UNIT_PRICE)
        .mapColumn("% VAT", ColumnKey.VAT_RATE)
        .outputMode(OutputMode.DATA_MODE)
        .build();

    // 1. String
    CsvProcessingResult r1 = processor.process(csv, config);
    assertThat(r1.summary().subtotal()).isEqualByComparingTo("100000.00");
    assertThat(r1.summary().totalVat()).isEqualByComparingTo("10000.00");

    // 2. Reader
    CsvProcessingResult r2 = processor.process(new StringReader(csv), config);
    assertThat(r2.summary()).isEqualTo(r1.summary());

    // 3. InputStream
    InputStream is = new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8));
    CsvProcessingResult r3 = processor.process(is, config);
    assertThat(r3.summary()).isEqualTo(r1.summary());

    // 4. Path
    Path tempFile = Files.createTempFile("order_test_", ".csv");
    Files.writeString(tempFile, csv, StandardCharsets.UTF_8);
    CsvProcessingResult r4 = processor.process(tempFile, config);
    assertThat(r4.summary()).isEqualTo(r1.summary());
    Files.deleteIfExists(tempFile);
  }

  @Test
  @DisplayName("Kiểm tra các overload của processToOrder với InputStream và Path")
  void testProcessToOrderOverloads() throws Exception {
    String csv = """
        Mã SP,Số lượng,Đơn giá,% VAT
        SP01,1,20000.00,10
        """;

    CsvConfig csvConfig = CsvConfig.builder()
        .hasHeader(true)
        .delimiter(',')
        .mapColumn("Số lượng", ColumnKey.QUANTITY)
        .mapColumn("Đơn giá", ColumnKey.UNIT_PRICE)
        .mapColumn("% VAT", ColumnKey.VAT_RATE)
        .build();

    MetadataConfig metaConfig = MetadataConfig.from(csvConfig);

    // 1. processToOrder(orderId, InputStream, CsvConfig)
    InputStream is1 = new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8));
    Order o1 = processor.processToOrder("ORD-01", is1, csvConfig);
    assertThat(o1.orderId()).isEqualTo("ORD-01");
    assertThat(o1.summary().finalTotal()).isEqualByComparingTo("22000.00");

    // 2. processToOrder(orderId, InputStream, MetadataConfig)
    InputStream is2 = new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8));
    Order o2 = processor.processToOrder("ORD-02", is2, metaConfig);
    assertThat(o2.orderId()).isEqualTo("ORD-02");
    assertThat(o2.summary().finalTotal()).isEqualByComparingTo("22000.00");

    // 3. processToOrder(orderId, Path, MetadataConfig)
    Path temp = Files.createTempFile("order_path_", ".csv");
    Files.writeString(temp, csv, StandardCharsets.UTF_8);
    Order o3 = processor.processToOrder("ORD-03", temp, metaConfig);
    assertThat(o3.orderId()).isEqualTo("ORD-03");
    Files.deleteIfExists(temp);
  }

  @Test
  @DisplayName("Kiểm tra chuyển đổi 2 chiều giữa CsvConfig và MetadataConfig")
  void testConfigInteroperability() {
    CsvConfig csvConfig = CsvConfig.builder()
        .hasHeader(false)
        .delimiter(';')
        .mapColumn(1, ColumnKey.QUANTITY)
        .mapColumn(2, ColumnKey.UNIT_PRICE)
        .mapColumn(3, ColumnKey.VAT_RATE)
        .outputMode(OutputMode.REPORT_MODE)
        .build();

    MetadataConfig metaConfig = csvConfig.toMetadataConfig();
    assertThat(metaConfig.hasHeader()).isFalse();
    assertThat(metaConfig.delimiter()).isEqualTo(';');
    assertThat(metaConfig.outputMode()).isEqualTo(OutputMode.REPORT_MODE);

    CsvConfig backToCsvConfig = metaConfig.toCsvConfig();
    assertThat(backToCsvConfig).isEqualTo(csvConfig);

    CsvConfig fromMeta = CsvConfig.from(metaConfig);
    assertThat(fromMeta).isEqualTo(csvConfig);

    MetadataConfig fromCsv = MetadataConfig.from(csvConfig);
    assertThat(fromCsv).isEqualTo(metaConfig);
  }

  @Test
  @DisplayName("Kiểm tra OrderItem và OrderSummary calculation methods trong com.gpc.order.processor.api.model")
  void testGpcModelCalculationMethods() {
    OrderItem item = OrderItem.of(1, List.of("SP1"), new BigDecimal("2"), new BigDecimal("15000.00"), new BigDecimal("10"));
    assertThat(item.lineTotal()).isEqualByComparingTo("30000.00");
    assertThat(item.vatAmount()).isEqualByComparingTo("3000.00");
    assertThat(item.lineTotalWithVat()).isEqualByComparingTo("33000.00");

    OrderItem itemWithTotal = OrderItem.ofWithLineTotal(2, List.of("SP2"), new BigDecimal("40000.00"), new BigDecimal("8"));
    assertThat(itemWithTotal.lineTotal()).isEqualByComparingTo("40000.00");
    assertThat(itemWithTotal.vatAmount()).isEqualByComparingTo("3200.00");

    OrderSummary summary = OrderSummary.fromItems(List.of(item, itemWithTotal));
    assertThat(summary.subtotal()).isEqualByComparingTo("70000.00");
    assertThat(summary.totalVat()).isEqualByComparingTo("6200.00");
    assertThat(summary.finalTotal()).isEqualByComparingTo("76200.00");

    assertThat(OrderSummary.fromItems(null)).isEqualTo(OrderSummary.zero());
    assertThat(OrderSummary.fromItems(List.of())).isEqualTo(OrderSummary.zero());
  }

  @Test
  @DisplayName("Kiểm tra các trường hợp ngoại lệ trong DefaultCsvOrderProcessor: out of bounds index, header rỗng, file rỗng")
  void testProcessorEdgeCases() {
    // 1. File hoàn toàn rỗng
    CsvConfig config = CsvConfig.builder()
        .hasHeader(true)
        .delimiter(',')
        .mapColumn("SL", ColumnKey.QUANTITY)
        .mapColumn("Gia", ColumnKey.UNIT_PRICE)
        .mapColumn("VAT", ColumnKey.VAT_RATE)
        .build();

    CsvProcessingResult emptyResult = processor.process("", config);
    assertThat(emptyResult.lineItems()).isEmpty();
    assertThat(emptyResult.summary()).isEqualTo(OrderSummary.zero());

    CsvProcessingResult newlineResult = processor.process("\n\n", config);
    assertThat(newlineResult.lineItems()).isEmpty();
    assertThat(newlineResult.summary()).isEqualTo(OrderSummary.zero());

    // 2. Cấu hình có header nhưng header thiếu cột cấu hình
    assertThatThrownBy(() -> processor.process("CotKhac1,CotKhac2\n1,2", config))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("was not found in CSV header");

    // 3. Index vượt quá số cột của header
    CsvConfig outOfBoundsConfig = CsvConfig.builder()
        .hasHeader(true)
        .delimiter(',')
        .mapColumn(99, ColumnKey.QUANTITY)
        .mapColumn("Gia", ColumnKey.UNIT_PRICE)
        .mapColumn("VAT", ColumnKey.VAT_RATE)
        .build();

    assertThatThrownBy(() -> processor.process("SL,Gia,VAT\n1,100,10", outOfBoundsConfig))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("out of bounds for header");

    // 4. File không header nhưng map bằng String không phải số
    CsvConfig nonNumericIndexConfig = CsvConfig.builder()
        .hasHeader(false)
        .delimiter(',')
        .mapColumn("ChuoiKhongPhaiSo", ColumnKey.QUANTITY)
        .mapColumn(1, ColumnKey.UNIT_PRICE)
        .mapColumn(2, ColumnKey.VAT_RATE)
        .build();

    assertThatThrownBy(() -> processor.process("1,100,10", nonNumericIndexConfig))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Cannot map column by non-numeric String name");

    // 5. Index âm
    CsvConfig negativeIndexConfig = CsvConfig.builder()
        .hasHeader(false)
        .delimiter(',')
        .mapColumn(-1, ColumnKey.QUANTITY)
        .mapColumn(1, ColumnKey.UNIT_PRICE)
        .mapColumn(2, ColumnKey.VAT_RATE)
        .build();

    assertThatThrownBy(() -> processor.process("1,100,10", negativeIndexConfig))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Column index cannot be negative");

    // 6. Thiếu cột bắt buộc (không có VAT_RATE)
    CsvConfig noVatConfig = CsvConfig.builder()
        .hasHeader(true)
        .delimiter(',')
        .mapColumn("SL", ColumnKey.QUANTITY)
        .mapColumn("Gia", ColumnKey.UNIT_PRICE)
        .build();

    assertThatThrownBy(() -> processor.process("SL,Gia\n1,100", noVatConfig))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Missing required column mapping for VAT_RATE");

    // 7. Thiếu cột bắt buộc (có VAT nhưng không có QUANTITY/UNIT_PRICE cũng không có TOTAL)
    CsvConfig onlyVatConfig = CsvConfig.builder()
        .hasHeader(true)
        .delimiter(',')
        .mapColumn("VAT", ColumnKey.VAT_RATE)
        .build();

    assertThatThrownBy(() -> processor.process("VAT\n10", onlyVatConfig))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("must map either (QUANTITY and UNIT_PRICE) or TOTAL");
  }

  @Test
  @DisplayName("Kiểm tra ánh xạ chỉ với cột TOTAL và VAT_RATE (không có QUANTITY và UNIT_PRICE)")
  void testTotalAndVatOnlyMapping() {
    String csv = """
        Mã SP,Tổng tiền,% VAT
        SP01,150000.00,10%
        SP02,50000.00,8
        """;

    CsvConfig config = CsvConfig.builder()
        .hasHeader(true)
        .delimiter(',')
        .mapColumn("Tổng tiền", ColumnKey.TOTAL)
        .mapColumn("% VAT", ColumnKey.VAT_RATE)
        .outputMode(OutputMode.DATA_MODE)
        .build();

    CsvProcessingResult result = processor.process(csv, config);
    assertThat(result.lineItems()).hasSize(2);
    assertThat(result.summary().subtotal()).isEqualByComparingTo("200000.00");
    assertThat(result.summary().totalVat()).isEqualByComparingTo("19000.00"); // 15000 + 4000
    assertThat(result.summary().finalTotal()).isEqualByComparingTo("219000.00");
  }
}
