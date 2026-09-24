package com.gpc.order.processor.internal.csv;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

class ExcelCsvParserTest {

  @Test
  @DisplayName("Parse dòng CSV cơ bản với dấu phẩy và chấm phẩy")
  void testBasicParse() {
    String csv = "A,B,C\n1,2,3";
    List<List<String>> rows = ExcelCsvParser.parse(csv, ',');
    assertThat(rows).hasSize(2);
    assertThat(rows.get(0)).containsExactly("A", "B", "C");
    assertThat(rows.get(1)).containsExactly("1", "2", "3");
  }

  @Test
  @DisplayName("Parse trường CSV chứa dấu ngoặc kép lồng nhau (escaped quotes) và dấu phân cách")
  void testQuotedFieldWithEscapedQuotes() {
    String csv = "\"Bút bi \"\"Kim Tinh\"\", xanh\",10,5000.00";
    List<List<String>> rows = ExcelCsvParser.parse(csv, ',');
    assertThat(rows).hasSize(1);
    assertThat(rows.get(0).get(0)).isEqualTo("Bút bi \"Kim Tinh\", xanh");
    assertThat(rows.get(0).get(1)).isEqualTo("10");
    assertThat(rows.get(0).get(2)).isEqualTo("5000.00");
  }
}
