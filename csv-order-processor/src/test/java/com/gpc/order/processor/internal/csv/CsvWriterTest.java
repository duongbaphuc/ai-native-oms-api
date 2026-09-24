package com.gpc.order.processor.internal.csv;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

class CsvWriterTest {

  @Test
  @DisplayName("Format dòng CSV với ký tự đặc biệt cần escape")
  void testFormatRowWithEscaping() {
    List<String> cells = List.of("Bút bi \"Hồng Hà\", xanh", "10", "5000.00");
    String formatted = CsvWriter.formatRow(cells, ',');
    assertThat(formatted).isEqualTo("\"Bút bi \"\"Hồng Hà\"\", xanh\",10,5000.00");
  }

  @Test
  @DisplayName("Format dòng CSV bình thường không cần escape")
  void testFormatRowStandard() {
    List<String> cells = List.of("SP01", "10", "5000.00");
    String formatted = CsvWriter.formatRow(cells, ';');
    assertThat(formatted).isEqualTo("SP01;10;5000.00");
  }
}
