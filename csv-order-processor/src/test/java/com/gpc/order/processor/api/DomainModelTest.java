package com.gpc.order.processor.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.gpc.order.processor.api.config.ColumnKey;
import com.gpc.order.processor.api.config.CsvConfig;
import com.gpc.order.processor.api.config.OutputMode;
import com.gpc.order.processor.api.model.CsvProcessingResult;
import com.gpc.order.processor.api.model.Order;
import com.gpc.order.processor.api.model.OrderItem;
import com.gpc.order.processor.api.model.OrderSummary;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

class DomainModelTest {

  @Test
  @DisplayName("Kiểm tra tính Bất biến (Immutability) và Defensive Copying của Order Aggregate Root")
  void testOrderImmutabilityAndDefensiveCopying() {
    List<OrderItem> mutableList = new ArrayList<>();
    OrderItem item1 = new OrderItem(
        1,
        List.of("SP1", "10", "100.00", "10"),
        new BigDecimal("10"),
        new BigDecimal("100.00"),
        new BigDecimal("1000.00"),
        new BigDecimal("10"),
        new BigDecimal("100.00"),
        new BigDecimal("1100.00"));
    mutableList.add(item1);

    OrderSummary summary = new OrderSummary(
        new BigDecimal("1000.00"),
        new BigDecimal("100.00"),
        new BigDecimal("1100.00"));

    Order order = new Order("ORD-001", mutableList, summary);

    // Thay đổi danh sách ban đầu không làm thay đổi Order
    mutableList.clear();
    assertThat(order.items()).hasSize(1);
    assertThat(order.items().get(0)).isEqualTo(item1);

    // Thao tác chỉnh sửa trực tiếp trên order.items() phải ném ngoại lệ
    assertThatThrownBy(() -> order.items().add(item1))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  @DisplayName("Kiểm tra ràng buộc hợp lệ của Order (Null & Blank OrderId)")
  void testOrderValidationInvariants() {
    OrderSummary summary = OrderSummary.zero();

    assertThatThrownBy(() -> new Order(null, List.of(), summary))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("orderId must not be null");

    assertThatThrownBy(() -> new Order("   ", List.of(), summary))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("orderId must not be blank");

    assertThatThrownBy(() -> new Order("ORD-1", null, summary))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("items must not be null");

    assertThatThrownBy(() -> new Order("ORD-1", List.of(), null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("summary must not be null");
  }

  @Test
  @DisplayName("Kiểm tra tính bất biến và chuẩn hóa số tiền của OrderItem")
  void testOrderItemImmutabilityAndScale() {
    List<String> raw = new ArrayList<>(List.of("A", "B", "C"));
    OrderItem item = new OrderItem(
        1,
        raw,
        new BigDecimal("2.5"),
        new BigDecimal("10.555"), // Làm tròn HALF_UP thành 10.56
        new BigDecimal("26.387"), // Làm tròn HALF_UP thành 26.39
        new BigDecimal("10"),
        new BigDecimal("2.639"),  // Làm tròn HALF_UP thành 2.64
        new BigDecimal("29.026")  // Làm tròn HALF_UP thành 29.03
    );

    // Defensive copy rawValues
    raw.clear();
    assertThat(item.rawValues()).hasSize(3);

    // Kiểm tra scale = 2 và HALF_UP
    assertThat(item.unitPrice()).isEqualByComparingTo("10.56");
    assertThat(item.lineTotal()).isEqualByComparingTo("26.39");
    assertThat(item.vatAmount()).isEqualByComparingTo("2.64");
    assertThat(item.lineTotalWithVat()).isEqualByComparingTo("29.03");

    // Line number < 1 ném ngoại lệ
    assertThatThrownBy(() -> new OrderItem(
        0,
        List.of(),
        BigDecimal.ONE,
        BigDecimal.TEN,
        BigDecimal.TEN,
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        BigDecimal.TEN))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("lineNumber must be positive");
  }

  @Test
  @DisplayName("Kiểm tra OrderSummary scale = 2 và phương thức zero()")
  void testOrderSummaryScaleAndZero() {
    OrderSummary zero = OrderSummary.zero();
    assertThat(zero.subtotal()).isEqualByComparingTo("0.00");
    assertThat(zero.totalVat()).isEqualByComparingTo("0.00");
    assertThat(zero.finalTotal()).isEqualByComparingTo("0.00");

    OrderSummary summary = new OrderSummary(
        new BigDecimal("100.125"), // 100.13
        new BigDecimal("10.005"),  // 10.01
        new BigDecimal("110.130")  // 110.13
    );

    assertThat(summary.subtotal()).isEqualByComparingTo("100.13");
    assertThat(summary.totalVat()).isEqualByComparingTo("10.01");
    assertThat(summary.finalTotal()).isEqualByComparingTo("110.13");
  }

  @Test
  @DisplayName("Kiểm tra chuyển đổi CsvProcessingResult sang Order aggregate root (toOrder & processToOrder)")
  void testCsvProcessingResultToOrder() {
    String csv = """
        Mã SP,Số lượng,Đơn giá,% VAT
        SP01,2,50000.00,10
        SP02,1,40000.00,8
        """;

    CsvConfig config = CsvConfig.builder()
        .hasHeader(true)
        .delimiter(',')
        .mapColumn("Số lượng", ColumnKey.QUANTITY)
        .mapColumn("Đơn giá", ColumnKey.UNIT_PRICE)
        .mapColumn("% VAT", ColumnKey.VAT_RATE)
        .outputMode(OutputMode.DATA_MODE)
        .build();

    CsvOrderProcessor processor = CsvOrderProcessor.create();

    // 1. Kiểm tra CsvProcessingResult.toOrder()
    CsvProcessingResult result = processor.process(csv, config);
    Order order1 = result.toOrder("ORD-2026-001");

    assertThat(order1.orderId()).isEqualTo("ORD-2026-001");
    assertThat(order1.items()).hasSize(2);
    assertThat(order1.summary().subtotal()).isEqualByComparingTo("140000.00");
    assertThat(order1.summary().totalVat()).isEqualByComparingTo("13200.00");
    assertThat(order1.summary().finalTotal()).isEqualByComparingTo("153200.00");

    // 2. Kiểm tra processor.processToOrder()
    Order order2 = processor.processToOrder("ORD-2026-002", csv, config);
    assertThat(order2.orderId()).isEqualTo("ORD-2026-002");
    assertThat(order2.items()).hasSize(2);
    assertThat(order2.summary()).isEqualTo(order1.summary());
  }
}
