// Nguồn gốc AI: kiểm thử đơn vị cho RateLimitProperties
package com.gpc.oms.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Kiểm thử đơn vị cho {@link RateLimitProperties}.
 */
@DisplayName("Kiểm thử đơn vị RateLimitProperties")
class RateLimitPropertiesTest {

    @Test
    @DisplayName("Constructor mặc định khởi tạo đúng các tham số chuẩn")
    void defaultConstructor_initializesCorrectly() {
        RateLimitProperties props = new RateLimitProperties();

        assertThat(props.readCapacity()).isEqualTo(60L);
        assertThat(props.writeCapacity()).isEqualTo(20L);
        assertThat(props.refillDuration()).isEqualTo(Duration.ofMinutes(1));
        assertThat(props.cacheMaxSize()).isEqualTo(10_000);
        assertThat(props.cacheExpireDuration()).isEqualTo(Duration.ofMinutes(10));
    }

    @Test
    @DisplayName("Constructor tùy biến khởi tạo chính xác các giá trị cấu hình")
    void customConstructor_setsValuesCorrectly() {
        RateLimitProperties props = new RateLimitProperties(100L, 50L, Duration.ofSeconds(30), 5000, Duration.ofMinutes(5));

        assertThat(props.readCapacity()).isEqualTo(100L);
        assertThat(props.writeCapacity()).isEqualTo(50L);
        assertThat(props.refillDuration()).isEqualTo(Duration.ofSeconds(30));
        assertThat(props.cacheMaxSize()).isEqualTo(5000);
        assertThat(props.cacheExpireDuration()).isEqualTo(Duration.ofMinutes(5));
    }
}
