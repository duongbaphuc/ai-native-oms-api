package com.gpc.order.processor.internal.validator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.gpc.order.processor.api.exception.CsvProcessingException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

class StrictRowValidatorTest {

  @Test
  @DisplayName("Giá trị không phải số ném CsvProcessingException ghi rõ số dòng và tên cột")
  void testNonNumericThrowsException() {
    assertThatThrownBy(() -> StrictRowValidator.parseBigDecimal(3, "Số lượng", "Mười", "quantity"))
        .isInstanceOf(CsvProcessingException.class)
        .satisfies(e -> {
          CsvProcessingException ex = (CsvProcessingException) e;
          assertThat(ex.getLineNumber()).isEqualTo(3);
          assertThat(ex.getColumnIdentifier()).isEqualTo("Số lượng");
        });
  }

  @Test
  @DisplayName("Dòng thiếu ô dữ liệu hoặc rỗng ném CsvProcessingException")
  void testMissingCellThrowsException() {
    List<String> row = List.of("SP01", "10"); // Chỉ có 2 ô

    assertThatThrownBy(() -> StrictRowValidator.extractCellValue(2, row, 3, "Đơn giá"))
        .isInstanceOf(CsvProcessingException.class)
        .satisfies(e -> {
          CsvProcessingException ex = (CsvProcessingException) e;
          assertThat(ex.getLineNumber()).isEqualTo(2);
          assertThat(ex.getColumnIdentifier()).isEqualTo("Đơn giá");
        });
  }
}
