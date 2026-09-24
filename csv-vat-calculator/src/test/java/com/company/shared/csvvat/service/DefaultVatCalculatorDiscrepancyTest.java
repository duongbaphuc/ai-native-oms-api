package com.company.shared.csvvat.service;

import com.company.shared.csvvat.config.CalculatorConfig;
import com.company.shared.csvvat.config.CsvColumnMapping;
import com.company.shared.csvvat.config.LineTotalDiscrepancyStrategy;
import com.company.shared.csvvat.exception.LineTotalDiscrepancyException;
import com.company.shared.csvvat.model.CalculatedItem;
import com.company.shared.csvvat.model.OrderItem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Kiểm thử DefaultVatCalculator - Chiến lược kiểm tra chéo thành tiền (Discrepancy Strategy)")
class DefaultVatCalculatorDiscrepancyTest {

    @Test
    @DisplayName("Khởi tạo bộ test DefaultVatCalculatorDiscrepancyTest")
    void contextLoads() {
        assertThat(new DefaultVatCalculatorDiscrepancyTest()).isNotNull();
        assertThat(new DefaultVatCalculatorDiscrepancyTest().new NormalCalculationTests()).isNotNull();
        assertThat(new DefaultVatCalculatorDiscrepancyTest().new DiscrepancyHandlingTests()).isNotNull();
        assertThat(new DefaultVatCalculatorDiscrepancyTest().new ValidationConstraintsTests()).isNotNull();
    }

    @Nested
    @DisplayName("1. Trường hợp bình thường: Không có hoặc khớp hoàn toàn với cột tổng đầu vào")
    class NormalCalculationTests {

        @Test
        @DisplayName("Không có cột tổng đầu vào (null): Tính toán bình thường như chuẩn v1")
        void calculateItem_noInputLineTotal_shouldComputeStandardSubtotal() {
            DefaultVatCalculator calculator = new DefaultVatCalculator(CalculatorConfig.defaultConfig());

            OrderItem item = new OrderItem(
                    1,
                    "Bánh mì chảo",
                    new BigDecimal("2"),
                    new BigDecimal("50000.00"),
                    BigDecimal.TEN,
                    null
            );

            CalculatedItem result = calculator.calculateItem(item);

            assertThat(result).isNotNull();
            assertThat(result.lineSubtotal()).isEqualByComparingTo("100000.00");
            assertThat(result.vatAmount()).isEqualByComparingTo("10000.00");
            assertThat(result.lineTotalWithVat()).isEqualByComparingTo("110000.00");
        }

        @Test
        @DisplayName("Khớp tuyệt đối: Số lượng 2 * Đơn giá 50.000 = 100.000, cột tổng đầu vào là 100.000")
        void calculateItem_matchingInputLineTotal_shouldComputeCleanlyWithoutDiscrepancy() {
            DefaultVatCalculator calculator = new DefaultVatCalculator(CalculatorConfig.defaultConfig());

            OrderItem item = new OrderItem(
                    1,
                    "Bánh mì chảo",
                    new BigDecimal("2"),
                    new BigDecimal("50000.00"),
                    BigDecimal.TEN,
                    new BigDecimal("100000.00")
            );

            CalculatedItem result = calculator.calculateItem(item);

            assertThat(result.lineSubtotal()).isEqualByComparingTo("100000.00");
            assertThat(result.vatAmount()).isEqualByComparingTo("10000.00");
            assertThat(result.lineTotalWithVat()).isEqualByComparingTo("110000.00");
        }
    }

    @Nested
    @DisplayName("2. Xử lý sai lệch với các chiến lược kiểm tra chéo (Discrepancy Strategies)")
    class DiscrepancyHandlingTests {

        @Test
        @DisplayName("Chiến lược FAIL_ON_MISMATCH: Lệch tiền (tính 100.000, đầu vào 120.000) -> Bắt buộc ném LineTotalDiscrepancyException")
        void calculateItem_discrepancyWithFailOnMismatch_mustThrowLineTotalDiscrepancyException() {
            CalculatorConfig config = CalculatorConfig.builder()
                    .discrepancyStrategy(LineTotalDiscrepancyStrategy.FAIL_ON_MISMATCH)
                    .discrepancyTolerance(new BigDecimal("0.01"))
                    .build();

            DefaultVatCalculator calculator = new DefaultVatCalculator(config);

            OrderItem item = new OrderItem(
                    2,
                    "Cà phê sữa đá",
                    new BigDecimal("2"),
                    new BigDecimal("50000.00"),
                    BigDecimal.TEN,
                    new BigDecimal("120000.00") // Lệch 20.000
            );

            Throwable thrown = null;
            try {
                calculator.calculateItem(item);
            } catch (LineTotalDiscrepancyException e) {
                thrown = e;
            }
            assertThat(thrown).isInstanceOf(LineTotalDiscrepancyException.class);
            LineTotalDiscrepancyException ex = (LineTotalDiscrepancyException) thrown;
            assertThat(ex.getRowNumber()).isEqualTo(2);
            assertThat(ex.getCalculatedTotal()).isEqualByComparingTo("100000.00");
            assertThat(ex.getInputTotal()).isEqualByComparingTo("120000.00");
            assertThat(ex.getDifference()).isEqualByComparingTo("20000.00");
        }

        @Test
        @DisplayName("Chiến lược FAIL_ON_MISMATCH với tên cột tùy biến: Tên cột trong ngoại lệ phải là tên tùy biến")
        void calculateItem_discrepancyWithCustomColumnName_shouldIncludeCustomColumnInException() {
            CsvColumnMapping mapping = CsvColumnMapping.builder()
                    .lineTotalColumn("Tong_Tien_Hang")
                    .build();
            CalculatorConfig config = CalculatorConfig.builder()
                    .discrepancyStrategy(LineTotalDiscrepancyStrategy.FAIL_ON_MISMATCH)
                    .columnMapping(mapping)
                    .build();

            DefaultVatCalculator calculator = new DefaultVatCalculator(config);
            OrderItem item = new OrderItem(
                    1, "Mặt hàng", BigDecimal.ONE, new BigDecimal("100.00"), BigDecimal.TEN, new BigDecimal("200.00")
            );

            Throwable thrown = null;
            try {
                calculator.calculateItem(item);
            } catch (LineTotalDiscrepancyException e) {
                thrown = e;
            }
            assertThat(thrown).isInstanceOf(LineTotalDiscrepancyException.class);
            LineTotalDiscrepancyException ex = (LineTotalDiscrepancyException) thrown;
            assertThat(ex.getColumnName()).isEqualTo("Tong_Tien_Hang");
        }

        @Test
        @DisplayName("Chiến lược WARN_AND_RECALCULATE: Lệch tiền -> Tự động dùng giá trị tự tính 100.000 để tính VAT và Total")
        void calculateItem_discrepancyWithWarnAndRecalculate_shouldUseCalculatedSubtotal() {
            CalculatorConfig config = CalculatorConfig.builder()
                    .discrepancyStrategy(LineTotalDiscrepancyStrategy.WARN_AND_RECALCULATE)
                    .build();

            DefaultVatCalculator calculator = new DefaultVatCalculator(config);

            OrderItem item = new OrderItem(
                    3,
                    "Trà đào cam sả",
                    new BigDecimal("2"),
                    new BigDecimal("50000.00"),
                    BigDecimal.TEN,
                    new BigDecimal("120000.00") // Lệch tiền đầu vào
            );

            CalculatedItem result = calculator.calculateItem(item);

            // Bỏ qua 120.000 đầu vào, sử dụng 100.000 tự tính toán
            assertThat(result.lineSubtotal()).isEqualByComparingTo("100000.00");
            assertThat(result.vatAmount()).isEqualByComparingTo("10000.00");
            assertThat(result.lineTotalWithVat()).isEqualByComparingTo("110000.00");
        }

        @Test
        @DisplayName("Chiến lược ACCEPT_INPUT_TOTAL: Lệch tiền -> Chấp nhận 120.000 đầu vào làm subtotal để tính VAT 12.000 và Total 132.000")
        void calculateItem_discrepancyWithAcceptInputTotal_shouldUseInputAsSubtotal() {
            CalculatorConfig config = CalculatorConfig.builder()
                    .discrepancyStrategy(LineTotalDiscrepancyStrategy.ACCEPT_INPUT_TOTAL)
                    .build();

            DefaultVatCalculator calculator = new DefaultVatCalculator(config);

            OrderItem item = new OrderItem(
                    4,
                    "Set Combo tiệc",
                    new BigDecimal("2"),
                    new BigDecimal("50000.00"),
                    BigDecimal.TEN,
                    new BigDecimal("120000.00") // Cột tổng đặc thù từ hệ thống cũ
            );

            CalculatedItem result = calculator.calculateItem(item);

            // Chấp nhận 120.000
            assertThat(result.lineSubtotal()).isEqualByComparingTo("120000.00");
            assertThat(result.vatAmount()).isEqualByComparingTo("12000.00");
            assertThat(result.lineTotalWithVat()).isEqualByComparingTo("132000.00");
        }

        @Test
        @DisplayName("Sai lệch nằm trong phạm vi dung sai (Tolerance = 0.05): Lệch 0.02 không bị coi là lỗi")
        void calculateItem_discrepancyWithinTolerance_shouldNotTriggerMismatch() {
            CalculatorConfig config = CalculatorConfig.builder()
                    .discrepancyStrategy(LineTotalDiscrepancyStrategy.FAIL_ON_MISMATCH)
                    .discrepancyTolerance(new BigDecimal("0.05")) // Cho phép lệch tối đa 0.05
                    .build();

            DefaultVatCalculator calculator = new DefaultVatCalculator(config);

            OrderItem item = new OrderItem(
                    5,
                    "Khăn lạnh cao cấp",
                    BigDecimal.ONE,
                    new BigDecimal("100.00"),
                    BigDecimal.TEN,
                    new BigDecimal("100.02") // Lệch 0.02 <= 0.05
            );

            // Không bị ném ngoại lệ vì nằm trong dung sai
            CalculatedItem result = calculator.calculateItem(item);
            assertThat(result.lineSubtotal()).isEqualByComparingTo("100.00");
            assertThat(result.vatAmount()).isEqualByComparingTo("10.00");
            assertThat(result.lineTotalWithVat()).isEqualByComparingTo("110.00");
        }
    }

    @Nested
    @DisplayName("3. Ràng buộc số lượng âm và các giá trị biên")
    class ValidationConstraintsTests {

        @Test
        @DisplayName("Số lượng âm (< 0): Chặn nghiêm ngặt và ném IllegalArgumentException")
        void orderItem_negativeQuantity_mustThrowIllegalArgumentException() {
            Throwable t1 = null;
            try {
                new OrderItem(1, "Mặt hàng âm", new BigDecimal("-2"), new BigDecimal("50000.00"), BigDecimal.TEN, null);
            } catch (IllegalArgumentException e) {
                t1 = e;
            }
            assertThat(t1).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Quantity cannot be negative");
        }

        @Test
        @DisplayName("Số lượng bằng 0: Chặn nghiêm ngặt và ném IllegalArgumentException")
        void orderItem_zeroQuantity_mustThrowIllegalArgumentException() {
            Throwable t2 = null;
            try {
                new OrderItem(1, "Mặt hàng 0", BigDecimal.ZERO, new BigDecimal("50000.00"), BigDecimal.TEN, null);
            } catch (IllegalArgumentException e) {
                t2 = e;
            }
            assertThat(t2).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Quantity must be greater than 0");
        }
    }
}
