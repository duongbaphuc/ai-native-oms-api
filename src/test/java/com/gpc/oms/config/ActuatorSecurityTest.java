// AI Provenance: Phase 10 Containerization & CI/CD Pipeline - Actuator Probe Security Verification
package com.gpc.oms.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ActuatorSecurityTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    void healthEndpoint_anonymousAccess_returns200AndStatusUp() throws Exception {
        mockMvc.perform(get("/actuator/health"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void infoEndpoint_anonymousAccess_returns200() throws Exception {
        mockMvc.perform(get("/actuator/info"))
            .andExpect(status().isOk());
    }

    @Test
    void sensitiveActuatorEndpoint_anonymousAccess_returns401() throws Exception {
        mockMvc.perform(get("/actuator/env"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.type").value("urn:problem-type:unauthorized"));
    }
}
