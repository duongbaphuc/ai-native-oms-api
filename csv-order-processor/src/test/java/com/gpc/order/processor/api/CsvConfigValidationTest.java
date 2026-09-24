package com.gpc.order.processor.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.gpc.order.processor.api.config.ColumnKey;
import com.gpc.order.processor.api.config.CsvConfig;
import com.gpc.order.processor.api.config.OutputMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

class CsvConfigValidationTest {

  @Test
  @DisplayName("Kiểm tra khởi tạo CsvConfig hợp lệ với Builder")
  void testValidConfig() {
    CsvConfig config = CsvConfig.builder()
        .hasHeader(true)
        .delimiter(';')
        .mapColumn("SL", ColumnKey.QUANTITY)
        .mapColumn("Gia", ColumnKey.UNIT_PRICE)
        .mapColumn("VAT", ColumnKey.VAT_RATE)
        .outputMode(OutputMode.REPORT_MODE)
        .build();

    assertThat(config.hasHeader()).isTrue();
    assertThat(config.delimiter()).isEqualTo(';');
    assertThat(config.outputMode()).isEqualTo(OutputMode.REPORT_MODE);
    assertThat(config.columnMapping()).containsEntry("SL", ColumnKey.QUANTITY);
  }

  @Test
  @DisplayName("Cấu hình rỗng hoặc thiếu cột bắt buộc ném IllegalArgumentException")
  void testEmptyMappingThrowsException() {
    assertThatThrownBy(() -> new CsvConfig(true, ',', Map.of(), OutputMode.DATA_MODE))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("columnMapping must contain at least required columns");
  }

  @Test
  @DisplayName("Ký tự delimiter không hợp lệ ném IllegalArgumentException")
  void testInvalidDelimiterThrowsException() {
    assertThatThrownBy(() -> CsvConfig.builder()
        .delimiter('"')
        .mapColumn("SL", ColumnKey.QUANTITY)
        .build())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Invalid CSV delimiter");
  }
}
