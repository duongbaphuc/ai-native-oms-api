package com.company.shared.csvvat.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Kiểm thử toàn diện Domain Models - OrderItem, CalculatedItem, OrderCalculationResult")
class DomainModelTest {

    @Test
    @DisplayName("Khởi tạo bộ test DomainModelTest thành công")
    void contextLoads() {
        DomainModelTest testInstance = new DomainModelTest();
        assertThat(testInstance).isNotNull();
        assertThat(testInstance.new OrderItemTests()).isNotNull();
        assertThat(testInstance.new CalculatedItemTests()).isNotNull();
        assertThat(testInstance.new OrderCalculationResultTests()).isNotNull();
    }

    // =========================================================================
    // 1. OrderItem Tests
    // =========================================================================
    @Nested
    @DisplayName("1. OrderItem Model Tests")
    class OrderItemTests {

        @Test
        @DisplayName("Khởi tạo OrderItem 6 tham số với inputLineTotal hợp lệ")
        void orderItem_sixArgumentsConstructor_shouldStoreAllFields() {
            BigDecimal qty = new BigDecimal("3");
            BigDecimal price = new BigDecimal("25000");
            BigDecimal vat = new BigDecimal("8.0");
            BigDecimal inputTotal = new BigDecimal("75000");

            OrderItem item = new OrderItem(1, "Bánh mì pate", qty, price, vat, inputTotal);

            assertThat(item.rowNumber()).isEqualTo(1);
            assertThat(item.itemName()).isEqualTo("Bánh mì pate");
            assertThat(item.quantity()).isEqualByComparingTo(qty);
            assertThat(item.unitPrice()).isEqualByComparingTo(price);
            assertThat(item.vatPercentage()).isEqualByComparingTo(vat);
            assertThat(item.inputLineTotal()).isEqualByComparingTo(inputTotal);
            assertThat(item.getInputLineTotal()).isPresent().contains(inputTotal);
        }

        @Test
        @DisplayName("Khởi tạo OrderItem 5 tham số overload: inputLineTotal mặc định là null và Optional rỗng")
        void orderItem_fiveArgumentsConstructor_shouldDefaultInputLineTotalToNull() {
            OrderItem item = new OrderItem(2, "Cà phê sữa đá", new BigDecimal("2"), new BigDecimal("30000"), BigDecimal.TEN);

            assertThat(item.rowNumber()).isEqualTo(2);
            assertThat(item.itemName()).isEqualTo("Cà phê sữa đá");
            assertThat(item.quantity()).isEqualByComparingTo("2");
            assertThat(item.unitPrice()).isEqualByComparingTo("30000");
            assertThat(item.vatPercentage()).isEqualByComparingTo("10");
            assertThat(item.inputLineTotal()).isNull();
            assertThat(item.getInputLineTotal()).isEmpty();
        }

        @Test
        @DisplayName("Giá trị biên hợp lệ: quantity > 0 cực nhỏ, unitPrice = 0, vatPercentage = 0")
        void orderItem_validBoundaryValues_shouldSucceed() {
            BigDecimal tinyQty = new BigDecimal("0.0001");
            BigDecimal zeroPrice = BigDecimal.ZERO;
            BigDecimal zeroVat = BigDecimal.ZERO;

            OrderItem item = new OrderItem(10, "Món quà tặng 0đ", tinyQty, zeroPrice, zeroVat);

            assertThat(item.quantity()).isEqualByComparingTo("0.0001");
            assertThat(item.unitPrice()).isEqualByComparingTo("0");
            assertThat(item.vatPercentage()).isEqualByComparingTo("0");
        }

        @Test
        @DisplayName("Chặn lỗi null: Ném NullPointerException cho 4 trường bắt buộc")
        void orderItem_nullArguments_shouldThrowNullPointerException() {
            BigDecimal qty = BigDecimal.ONE;
            BigDecimal price = BigDecimal.TEN;
            BigDecimal vat = BigDecimal.ZERO;

            assertThatThrownBy(() -> new OrderItem(1, null, qty, price, vat))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("itemName must not be null");

            assertThatThrownBy(() -> new OrderItem(1, "SP", null, price, vat))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("quantity must not be null");

            assertThatThrownBy(() -> new OrderItem(1, "SP", qty, null, vat))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("unitPrice must not be null");

            assertThatThrownBy(() -> new OrderItem(1, "SP", qty, price, null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("vatPercentage must not be null");
        }

        @ParameterizedTest(name = "Tên mặt hàng rỗng hoặc khoảng trắng: ''{0}''")
        @ValueSource(strings = {"", "   ", "\t", "\n"})
        @DisplayName("Chặn lỗi blank itemName: Ném IllegalArgumentException")
        void orderItem_blankItemName_shouldThrowIllegalArgumentException(String blankName) {
            assertThatThrownBy(() -> new OrderItem(1, blankName, BigDecimal.ONE, BigDecimal.TEN, BigDecimal.ZERO))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("itemName must not be blank");
        }

        @Test
        @DisplayName("Chặn lỗi số lượng (quantity <= 0): Ném IllegalArgumentException")
        void orderItem_invalidQuantity_shouldThrowIllegalArgumentException() {
            // Negative quantity
            assertThatThrownBy(() -> new OrderItem(1, "SP", new BigDecimal("-1"), BigDecimal.TEN, BigDecimal.ZERO))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Quantity cannot be negative");

            // Zero quantity
            assertThatThrownBy(() -> new OrderItem(1, "SP", BigDecimal.ZERO, BigDecimal.TEN, BigDecimal.ZERO))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Quantity must be greater than 0");
        }

        @Test
        @DisplayName("Chặn lỗi đơn giá âm: Ném IllegalArgumentException")
        void orderItem_negativeUnitPrice_shouldThrowIllegalArgumentException() {
            assertThatThrownBy(() -> new OrderItem(1, "SP", BigDecimal.ONE, new BigDecimal("-0.01"), BigDecimal.ZERO))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("unitPrice must not be negative");
        }

        @Test
        @DisplayName("Chặn lỗi thuế VAT âm: Ném IllegalArgumentException")
        void orderItem_negativeVatPercentage_shouldThrowIllegalArgumentException() {
            assertThatThrownBy(() -> new OrderItem(1, "SP", BigDecimal.ONE, BigDecimal.TEN, new BigDecimal("-5")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("vatPercentage must not be negative");
        }

        @Test
        @DisplayName("Record contracts: equals, hashCode, toString")
        void orderItem_recordContracts() {
            OrderItem item1 = new OrderItem(1, "SP A", BigDecimal.ONE, BigDecimal.TEN, BigDecimal.ZERO, new BigDecimal("10"));
            OrderItem item2 = new OrderItem(1, "SP A", BigDecimal.ONE, BigDecimal.TEN, BigDecimal.ZERO, new BigDecimal("10"));
            OrderItem item3 = new OrderItem(2, "SP B", BigDecimal.ONE, BigDecimal.TEN, BigDecimal.ZERO);

            assertThat(item1).isEqualTo(item2)
                    .hasSameHashCodeAs(item2)
                    .isNotEqualTo(item3);

            assertThat(item1.toString()).contains("OrderItem").contains("SP A");
        }
    }

    // =========================================================================
    // 2. CalculatedItem Tests
    // =========================================================================
    @Nested
    @DisplayName("2. CalculatedItem Model Tests")
    class CalculatedItemTests {

        private final OrderItem sampleRawItem = new OrderItem(
                3, "Bánh bao trứng cút", new BigDecimal("2"), new BigDecimal("15000"), new BigDecimal("8")
        );

        @Test
        @DisplayName("Khởi tạo CalculatedItem và kiểm tra toàn bộ các hàm delegation sang OrderItem")
        void calculatedItem_validInitialization_shouldDelegateToOrderItem() {
            BigDecimal subtotal = new BigDecimal("30000");
            BigDecimal vatAmount = new BigDecimal("2400");
            BigDecimal totalWithVat = new BigDecimal("32400");

            CalculatedItem calc = new CalculatedItem(sampleRawItem, subtotal, vatAmount, totalWithVat);

            assertThat(calc.item()).isSameAs(sampleRawItem);
            assertThat(calc.rowNumber()).isEqualTo(3);
            assertThat(calc.itemName()).isEqualTo("Bánh bao trứng cút");
            assertThat(calc.quantity()).isEqualByComparingTo("2");
            assertThat(calc.unitPrice()).isEqualByComparingTo("15000");
            assertThat(calc.vatPercentage()).isEqualByComparingTo("8");
            assertThat(calc.lineSubtotal()).isEqualByComparingTo("30000");
            assertThat(calc.vatAmount()).isEqualByComparingTo("2400");
            assertThat(calc.lineTotalWithVat()).isEqualByComparingTo("32400");
        }

        @Test
        @DisplayName("Giá trị biên hợp lệ: Các giá trị tính toán bằng 0 (hàng miễn thuế/khuyến mãi)")
        void calculatedItem_zeroBoundaryValues_shouldSucceed() {
            CalculatedItem calc = new CalculatedItem(sampleRawItem, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);

            assertThat(calc.lineSubtotal()).isEqualByComparingTo("0");
            assertThat(calc.vatAmount()).isEqualByComparingTo("0");
            assertThat(calc.lineTotalWithVat()).isEqualByComparingTo("0");
        }

        @Test
        @DisplayName("Chặn lỗi null: Ném NullPointerException cho toàn bộ các tham số của CalculatedItem")
        void calculatedItem_nullArguments_shouldThrowNullPointerException() {
            BigDecimal zero = BigDecimal.ZERO;

            assertThatThrownBy(() -> new CalculatedItem(null, zero, zero, zero))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("item must not be null");

            assertThatThrownBy(() -> new CalculatedItem(sampleRawItem, null, zero, zero))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("lineSubtotal must not be null");

            assertThatThrownBy(() -> new CalculatedItem(sampleRawItem, zero, null, zero))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("vatAmount must not be null");

            assertThatThrownBy(() -> new CalculatedItem(sampleRawItem, zero, zero, null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("lineTotalWithVat must not be null");
        }

        @Test
        @DisplayName("Chặn lỗi số âm: Ném IllegalArgumentException khi bất kỳ số tiền tính toán nào bị âm")
        void calculatedItem_negativeAmounts_shouldThrowIllegalArgumentException() {
            BigDecimal zero = BigDecimal.ZERO;
            BigDecimal negative = new BigDecimal("-1");

            assertThatThrownBy(() -> new CalculatedItem(sampleRawItem, negative, zero, zero))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("lineSubtotal must not be negative");

            assertThatThrownBy(() -> new CalculatedItem(sampleRawItem, zero, negative, zero))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("vatAmount must not be negative");

            assertThatThrownBy(() -> new CalculatedItem(sampleRawItem, zero, zero, negative))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("lineTotalWithVat must not be negative");
        }

        @Test
        @DisplayName("Record contracts: equals, hashCode, toString")
        void calculatedItem_recordContracts() {
            CalculatedItem calc1 = new CalculatedItem(sampleRawItem, BigDecimal.TEN, BigDecimal.ONE, new BigDecimal("11"));
            CalculatedItem calc2 = new CalculatedItem(sampleRawItem, BigDecimal.TEN, BigDecimal.ONE, new BigDecimal("11"));
            CalculatedItem calc3 = new CalculatedItem(sampleRawItem, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);

            assertThat(calc1).isEqualTo(calc2)
                    .hasSameHashCodeAs(calc2)
                    .isNotEqualTo(calc3);

            assertThat(calc1.toString()).contains("CalculatedItem").contains("Bánh bao trứng cút");
        }
    }

    // =========================================================================
    // 3. OrderCalculationResult Tests
    // =========================================================================
    @Nested
    @DisplayName("3. OrderCalculationResult Model Tests")
    class OrderCalculationResultTests {

        private CalculatedItem createSampleCalculatedItem() {
            OrderItem raw = new OrderItem(1, "Trà đào cam sả", new BigDecimal("2"), new BigDecimal("35000"), BigDecimal.TEN);
            return new CalculatedItem(raw, new BigDecimal("70000"), new BigDecimal("7000"), new BigDecimal("77000"));
        }

        @Test
        @DisplayName("Khởi tạo OrderCalculationResult hợp lệ và kiểm tra các hàm Getters / Alias")
        void orderCalculationResult_validInitialization_shouldExposeGettersAndAliases() {
            CalculatedItem item = createSampleCalculatedItem();
            List<CalculatedItem> items = List.of(item);

            OrderCalculationResult result = new OrderCalculationResult(
                    "ORD-2026-001",
                    items,
                    new BigDecimal("70000.00"),
                    new BigDecimal("7000.00"),
                    new BigDecimal("77000.00"),
                    new BigDecimal("2"),
                    1
            );

            assertThat(result.orderId()).isEqualTo("ORD-2026-001");
            assertThat(result.items()).hasSize(1).containsExactly(item);
            assertThat(result.subtotal()).isEqualByComparingTo("70000.00");
            assertThat(result.totalVat()).isEqualByComparingTo("7000.00");
            assertThat(result.grandTotal()).isEqualByComparingTo("77000.00");
            assertThat(result.totalQuantity()).isEqualByComparingTo("2");
            assertThat(result.totalItemsCount()).isEqualTo(1);

            // Kiểm tra các phương thức Alias chuẩn kế toán
            assertThat(result.getTotalBeforeTax()).isEqualByComparingTo("70000.00");
            assertThat(result.getTotalVatAmount()).isEqualByComparingTo("7000.00");
            assertThat(result.getFinalPayableAmount()).isEqualByComparingTo("77000.00");
        }

        @Test
        @DisplayName("orderId có thể nhận giá trị null mà không gây lỗi")
        void orderCalculationResult_nullOrderId_shouldBeAllowed() {
            OrderCalculationResult result = new OrderCalculationResult(
                    null,
                    List.of(),
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    0
            );

            assertThat(result.orderId()).isNull();
            assertThat(result.items()).isEmpty();
            assertThat(result.totalItemsCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("Bảo vệ tính bất biến (Defensive Immutability) của danh sách items")
        void orderCalculationResult_itemsImmutability_shouldDefendAgainstModifications() {
            List<CalculatedItem> mutableList = new ArrayList<>();
            mutableList.add(createSampleCalculatedItem());

            OrderCalculationResult result = new OrderCalculationResult(
                    "ORD-IMMUTABLE",
                    mutableList,
                    new BigDecimal("70000"),
                    new BigDecimal("7000"),
                    new BigDecimal("77000"),
                    new BigDecimal("2"),
                    1
            );

            // Thêm phần tử vào danh sách ban đầu không được làm thay đổi kết quả trong record
            mutableList.add(createSampleCalculatedItem());
            assertThat(result.items()).hasSize(1);

            // Thao tác chỉnh sửa trực tiếp trên danh sách items() phải ném UnsupportedOperationException
            assertThatThrownBy(() -> result.items().add(createSampleCalculatedItem()))
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("Chặn lỗi null: Ném NullPointerException cho các trường bắt buộc")
        void orderCalculationResult_nullArguments_shouldThrowNullPointerException() {
            List<CalculatedItem> items = List.of();
            BigDecimal zero = BigDecimal.ZERO;

            assertThatThrownBy(() -> new OrderCalculationResult("ID", null, zero, zero, zero, zero, 0))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("items must not be null");

            assertThatThrownBy(() -> new OrderCalculationResult("ID", items, null, zero, zero, zero, 0))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("subtotal must not be null");

            assertThatThrownBy(() -> new OrderCalculationResult("ID", items, zero, null, zero, zero, 0))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("totalVat must not be null");

            assertThatThrownBy(() -> new OrderCalculationResult("ID", items, zero, zero, null, zero, 0))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("grandTotal must not be null");

            assertThatThrownBy(() -> new OrderCalculationResult("ID", items, zero, zero, zero, null, 0))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("totalQuantity must not be null");
        }

        @Test
        @DisplayName("Chặn lỗi số âm: Ném IllegalArgumentException khi các chỉ số tài chính hoặc số lượng âm")
        void orderCalculationResult_negativeValues_shouldThrowIllegalArgumentException() {
            List<CalculatedItem> items = List.of();
            BigDecimal zero = BigDecimal.ZERO;
            BigDecimal negative = new BigDecimal("-0.01");

            // totalItemsCount âm
            assertThatThrownBy(() -> new OrderCalculationResult("ID", items, zero, zero, zero, zero, -1))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("totalItemsCount must not be negative");

            // totalQuantity âm
            assertThatThrownBy(() -> new OrderCalculationResult("ID", items, zero, zero, zero, negative, 0))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("totalQuantity must not be negative");

            // subtotal âm
            assertThatThrownBy(() -> new OrderCalculationResult("ID", items, negative, zero, zero, zero, 0))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("subtotal must not be negative");

            // totalVat âm
            assertThatThrownBy(() -> new OrderCalculationResult("ID", items, zero, negative, zero, zero, 0))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("totalVat must not be negative");

            // grandTotal âm
            assertThatThrownBy(() -> new OrderCalculationResult("ID", items, zero, zero, negative, zero, 0))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("grandTotal must not be negative");
        }

        @Test
        @DisplayName("Record contracts: equals, hashCode, toString")
        void orderCalculationResult_recordContracts() {
            CalculatedItem item = createSampleCalculatedItem();
            OrderCalculationResult res1 = new OrderCalculationResult("ORD-1", List.of(item), BigDecimal.TEN, BigDecimal.ONE, new BigDecimal("11"), BigDecimal.ONE, 1);
            OrderCalculationResult res2 = new OrderCalculationResult("ORD-1", List.of(item), BigDecimal.TEN, BigDecimal.ONE, new BigDecimal("11"), BigDecimal.ONE, 1);
            OrderCalculationResult res3 = new OrderCalculationResult("ORD-2", List.of(item), BigDecimal.TEN, BigDecimal.ONE, new BigDecimal("11"), BigDecimal.ONE, 1);

            assertThat(res1).isEqualTo(res2)
                    .hasSameHashCodeAs(res2)
                    .isNotEqualTo(res3);

            assertThat(res1.toString()).contains("OrderCalculationResult").contains("ORD-1");
        }
    }
}
