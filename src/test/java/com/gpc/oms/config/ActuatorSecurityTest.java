// Nguồn gốc AI: sinh từ Giai đoạn 10 Containerization & CI/CD Pipeline - Kiểm định an ninh Actuator
package com.gpc.oms.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Bộ kiểm thử kiểm định cấu hình bảo mật các endpoint giám sát Actuator.
 */
@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureObservability
@DisplayName("Kiểm thử bảo mật endpoint Spring Boot Actuator")
class ActuatorSecurityTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    @DisplayName("Endpoint /actuator/health cho phép truy cập nặc danh trả về 200 UP")
    void healthEndpoint_anonymousAccess_returns200AndStatusUp() throws Exception {
        mockMvc.perform(get("/actuator/health"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    @DisplayName("Endpoint /actuator/info cho phép truy cập nặc danh trả về 200")
    void infoEndpoint_anonymousAccess_returns200() throws Exception {
        mockMvc.perform(get("/actuator/info"))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Endpoint nhạy cảm /actuator/env từ chối truy cập nặc danh trả về 401")
    void sensitiveActuatorEndpoint_anonymousAccess_returns401() throws Exception {
        mockMvc.perform(get("/actuator/env"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.type").value("urn:problem-type:unauthorized"));
    }

    @Test
    @DisplayName("Endpoint /actuator/prometheus từ chối truy cập nặc danh trả về 401")
    void prometheusEndpoint_anonymousAccess_returns401() throws Exception {
        mockMvc.perform(get("/actuator/prometheus"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.type").value("urn:problem-type:unauthorized"));
    }

    @Test
    @WithMockUser(roles = "DISPATCHER")
    @DisplayName("Endpoint /actuator/prometheus từ chối vai trò không phải ADMIN trả về 403")
    void prometheusEndpoint_nonAdminRole_returns403() throws Exception {
        mockMvc.perform(get("/actuator/prometheus"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Endpoint /actuator/prometheus cho phép vai trò ADMIN truy cập trả về 200")
    void prometheusEndpoint_adminRole_returns200() throws Exception {
        mockMvc.perform(get("/actuator/prometheus"))
            .andExpect(status().isOk());
    }
}
