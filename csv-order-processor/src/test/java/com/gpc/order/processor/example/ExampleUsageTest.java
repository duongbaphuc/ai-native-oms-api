package com.gpc.order.processor.example;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;

class ExampleUsageTest {

  @Test
  @DisplayName("Kiểm tra ExampleUsage chạy thành công không có lỗi ngoại lệ nào")
  void testExampleUsageRunsSuccessfully() {
    assertThatCode(() -> ExampleUsage.main(new String[0]))
        .doesNotThrowAnyException();
  }
}
