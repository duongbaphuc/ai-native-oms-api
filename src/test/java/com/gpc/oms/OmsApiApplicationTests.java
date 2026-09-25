// Nguồn gốc AI: sinh từ docs/00-coding-rules.md
package com.gpc.oms;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Kiểm thử kiểm tra việc tải ngữ cảnh ứng dụng Spring Boot (Smoke Test).
 */
@SpringBootTest
@DisplayName("Kiểm thử tải ngữ cảnh Spring Boot (Context Loads)")
class OmsApiApplicationTests {

    @Test
    @DisplayName("Ngữ cảnh ứng dụng Spring Boot khởi tạo thành công")
    void contextLoads() {
        // Kiểm tra smoke test đảm bảo ApplicationContext nạp đủ các bean
    }
}
