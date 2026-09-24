package com.company.shared.csvvat.service;

import com.company.shared.csvvat.exception.EmptyOrderException;
import com.company.shared.csvvat.model.CalculatedItem;
import com.company.shared.csvvat.model.OrderCalculationResult;
import com.company.shared.csvvat.model.OrderItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Kiểm thử DefaultVatCalculator - Bộ tính toán tiền tệ và thuế VAT")
class DefaultVatCalculatorTest {

    private DefaultVatCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new DefaultVatCalculator();
    }

    @Test
    @DisplayName("Khởi tạo bộ test DefaultVatCalculatorTest")
    void contextLoads() {
        assertThat(new DefaultVatCalculatorTest()).isNotNull();
        assertThat(new DefaultVatCalculatorTest().new RoundingAndScaleTests()).isNotNull();
        assertThat(new DefaultVatCalculatorTest().new ValidationTests()).isNotNull();
        assertThat(new DefaultVatCalculatorTest().new OrderLevelTests()).isNotNull();
        assertThat(new DefaultVatCalculatorTest().new ConfigAndGetterTests()).isNotNull();
    }

    @Nested
    @DisplayName("1. Quy tắc làm tròn: scale = 2, RoundingMode.HALF_UP")
    class RoundingAndScaleTests {

        @Test
        @DisplayName("Làm tròn HALF_UP chuẩn: 1.055 -> 1.06, 1.054 -> 1.05")
        void calculateItem_roundingHalfUpBehavior() {
            OrderItem itemRoundUp = new OrderItem(
                    1, "Item Round Up", BigDecimal.ONE, new BigDecimal("10.55"), new BigDecimal("10")
            );
            CalculatedItem resultUp = calculator.calculateItem(itemRoundUp);
            assertThat(resultUp.lineSubtotal()).isEqualByComparingTo("10.55");
            assertThat(resultUp.vatAmount()).isEqualByComparingTo("1.06");
            assertThat(resultUp.lineTotalWithVat()).isEqualByComparingTo("11.61");

            OrderItem itemRoundDown = new OrderItem(
                    2, "Item Round Down", BigDecimal.ONE, new BigDecimal("10.54"), new BigDecimal("10")
            );
            CalculatedItem resultDown = calculator.calculateItem(itemRoundDown);
            assertThat(resultDown.lineSubtotal()).isEqualByComparingTo("10.54");
            assertThat(resultDown.vatAmount()).isEqualByComparingTo("1.05");
            assertThat(resultDown.lineTotalWithVat()).isEqualByComparingTo("11.59");
        }

        @Test
        @DisplayName("Tùy biến cho tiền Đồng Việt Nam (VND: scale = 0, làm tròn HALF_UP)")
        void calculateItem_withScaleZeroForVnd() {
            DefaultVatCalculator vndCalculator = new DefaultVatCalculator(0, RoundingMode.HALF_UP);

            OrderItem item = new OrderItem(
                    1, "Cà phê phin", new BigDecimal("3"), new BigDecimal("33333"), new BigDecimal("8")
            );

            CalculatedItem result = vndCalculator.calculateItem(item);
            assertThat(result.lineSubtotal()).isEqualByComparingTo("99999");
            assertThat(result.vatAmount()).isEqualByComparingTo("8000");
            assertThat(result.lineTotalWithVat()).isEqualByComparingTo("107999");
        }

        @ParameterizedTest(name = "VAT {0}% với đơn giá {1} và số lượng {2}")
        @CsvSource({
                "5.5, 1000.00, 2, 2000.00, 110.00, 2110.00",
                "8.0, 125000.00, 4, 500000.00, 40000.00, 540000.00",
                "10.0, 33333.33, 3, 99999.99, 10000.00, 109999.99"
        })
        @DisplayName("Kiểm tra tính toán với nhiều tỷ lệ VAT và số lượng lẻ")
        void calculateItem_parameterizedInputs_shouldComputeExpectedTotals(
                String vatRate, String unitPrice, String qty,
                String expectedSubtotal, String expectedVat, String expectedTotal
        ) {
            OrderItem item = new OrderItem(1, "Test Item", new BigDecimal(qty), new BigDecimal(unitPrice), new BigDecimal(vatRate));
            CalculatedItem result = calculator.calculateItem(item);

            assertThat(result.lineSubtotal()).isEqualByComparingTo(expectedSubtotal);
            assertThat(result.vatAmount()).isEqualByComparingTo(expectedVat);
            assertThat(result.lineTotalWithVat()).isEqualByComparingTo(expectedTotal);
        }

        @Test
        @DisplayName("Mặt hàng miễn thuế (VAT 0%): Tiền VAT bằng 0.00")
        void calculateItem_zeroVat_shouldComputeZeroTax() {
            OrderItem item = new OrderItem(2, "Nước suối tinh khiết", new BigDecimal("5"), new BigDecimal("10000.00"), BigDecimal.ZERO);
            CalculatedItem result = calculator.calculateItem(item);

            assertThat(result.lineSubtotal()).isEqualByComparingTo("50000.00");
            assertThat(result.vatAmount()).isEqualByComparingTo("0.00");
            assertThat(result.lineTotalWithVat()).isEqualByComparingTo("50000.00");
        }
    }

    @Nested
    @DisplayName("2. Kiểm tra các ngoại lệ và validation bắt buộc")
    class ValidationTests {

        @Test
        @DisplayName("Số lượng âm: Ném IllegalArgumentException")
        void orderItem_negativeQuantity_mustThrowIllegalArgumentException() {
            Throwable t = null;
            try {
                new OrderItem(1, "SP Âm", new BigDecimal("-2"), new BigDecimal("100.00"), BigDecimal.TEN);
            } catch (IllegalArgumentException e) {
                t = e;
            }
            assertThat(t).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Quantity cannot be negative");
        }

        @Test
        @DisplayName("calculateItem với item null: Ném IllegalArgumentException")
        void calculateItem_nullItem_shouldThrowIllegalArgumentException() {
            Throwable t = null;
            try {
                calculator.calculateItem(null);
            } catch (IllegalArgumentException e) {
                t = e;
            }
            assertThat(t).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("OrderItem must not be null");
        }

        @Test
        @DisplayName("calculateOrder với list rỗng hoặc null: Ném EmptyOrderException")
        void calculateOrder_emptyOrNullList_shouldThrowEmptyOrderException() {
            Throwable t1 = null;
            try {
                calculator.calculateOrder(Collections.emptyList());
            } catch (EmptyOrderException e) {
                t1 = e;
            }
            assertThat(t1).isInstanceOf(EmptyOrderException.class)
                    .hasMessageContaining("does not contain any items");

            Throwable t2 = null;
            try {
                calculator.calculateOrder(null);
            } catch (EmptyOrderException e) {
                t2 = e;
            }
            assertThat(t2).isInstanceOf(EmptyOrderException.class);
        }
    }

    @Nested
    @DisplayName("3. Tính toán tổng hợp cấp đơn hàng")
    class OrderLevelTests {

        @Test
        @DisplayName("Tổng hợp đơn hàng nhiều mặt hàng: Grand Total = Subtotal + Total VAT")
        void calculateOrder_multipleItems_shouldAggregateCorrectly() {
            OrderItem item1 = new OrderItem(1, "Bánh mì", new BigDecimal("2"), new BigDecimal("45000.00"), new BigDecimal("8"));
            OrderItem item2 = new OrderItem(2, "Cà phê", new BigDecimal("3"), new BigDecimal("25000.00"), new BigDecimal("10"));

            OrderCalculationResult result = calculator.calculateOrder("ORD-001", List.of(item1, item2));

            assertThat(result.orderId()).isEqualTo("ORD-001");
            assertThat(result.totalItemsCount()).isEqualTo(2);
            assertThat(result.totalQuantity()).isEqualByComparingTo("5");
            assertThat(result.subtotal()).isEqualByComparingTo("165000.00");
            assertThat(result.totalVat()).isEqualByComparingTo("14700.00");
            assertThat(result.grandTotal()).isEqualByComparingTo("179700.00");
            assertThat(result.grandTotal()).isEqualByComparingTo(result.subtotal().add(result.totalVat()));
        }

        @Test
        @DisplayName("calculateOrder overload không truyền orderId")
        void calculateOrder_withoutOrderId_shouldDefaultNullOrderId() {
            OrderItem item = new OrderItem(1, "Bánh mì", new BigDecimal("1"), new BigDecimal("20000.00"), new BigDecimal("10"));
            OrderCalculationResult result = calculator.calculateOrder(List.of(item));

            assertThat(result.orderId()).isNull();
            assertThat(result.subtotal()).isEqualByComparingTo("20000.00");
        }
    }

    @Nested
    @DisplayName("4. Kiểm tra cấu hình và các hàm getter")
    class ConfigAndGetterTests {

        @Test
        @DisplayName("Getter trả về đúng cấu hình")
        void getters_shouldReturnConfiguredValues() {
            assertThat(calculator.getConfig()).isNotNull();
            assertThat(calculator.getCurrencyScale()).isEqualTo(2);
            assertThat(calculator.getRoundingMode()).isEqualTo(RoundingMode.HALF_UP);
        }

        @Test
        @DisplayName("calculateItem với item có quantity âm hoặc 0 (thông qua mock)")
        void calculateItem_mockNegativeOrZeroQuantity_shouldThrowIllegalArgumentException() {
            OrderItem mockNegative = org.mockito.Mockito.mock(OrderItem.class);
            org.mockito.Mockito.when(mockNegative.quantity()).thenReturn(new BigDecimal("-1"));
            Throwable t1 = null;
            try {
                calculator.calculateItem(mockNegative);
            } catch (IllegalArgumentException e) {
                t1 = e;
            }
            assertThat(t1).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Quantity cannot be negative");

            OrderItem mockZero = org.mockito.Mockito.mock(OrderItem.class);
            org.mockito.Mockito.when(mockZero.quantity()).thenReturn(BigDecimal.ZERO);
            Throwable t2 = null;
            try {
                calculator.calculateItem(mockZero);
            } catch (IllegalArgumentException e) {
                t2 = e;
            }
            assertThat(t2).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Quantity must be greater than 0");
        }
    }
}
