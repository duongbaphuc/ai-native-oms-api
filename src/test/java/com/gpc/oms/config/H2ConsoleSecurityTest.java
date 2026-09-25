// Nguồn gốc AI: rà soát từ issue #29 (SEC-01, CWE-200)
package com.gpc.oms.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Kiểm thử quyền truy cập H2 Console trên môi trường phát triển (profile mặc định !prod).
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Kiểm thử truy cập H2 Console trên môi trường Dev (!prod)")
class H2ConsoleDevAccessTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    @DisplayName("Truy cập nặc danh H2 Console khi không phải profile prod không bị chặn 401/403")
    void h2Console_anonymousAccessWithoutProdProfile_isNotDenied() throws Exception {
        mockMvc.perform(get("/h2-console/"))
            .andExpect(result -> assertNotEquals(401, result.getResponse().getStatus()))
            .andExpect(result -> assertNotEquals(403, result.getResponse().getStatus()));
    }
}

/**
 * Kiểm thử quyền truy cập H2 Console trên môi trường sản xuất (profile prod).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("prod")
@DisplayName("Kiểm thử khóa chặn H2 Console trên môi trường Production (prod)")
class H2ConsoleProdAccessTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    @DisplayName("Truy cập nặc danh H2 Console trên profile prod bị từ chối trả về 401 Unauthorized")
    void h2Console_anonymousAccessOnProdProfile_returns401() throws Exception {
        mockMvc.perform(get("/h2-console/"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.type").value("urn:problem-type:unauthorized"));
    }
}
