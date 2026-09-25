// Nguồn gốc AI: kiểm thử đơn vị cho WorkOrderMetrics
package com.gpc.oms.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Kiểm thử đơn vị cho lớp hằng số {@link WorkOrderMetrics}.
 */
@DisplayName("Kiểm thử đơn vị WorkOrderMetrics")
class WorkOrderMetricsTest {

    @Test
    @DisplayName("WorkOrderMetrics định nghĩa chính xác các hằng số tên metric và tags")
    void constants_areConfiguredCorrectly() {
        assertThat(WorkOrderMetrics.COUNTER_CREATED).isEqualTo("oms_workorders_created_total");
        assertThat(WorkOrderMetrics.COUNTER_TRANSITIONS).isEqualTo("oms_workorder_status_transitions_total");
        assertThat(WorkOrderMetrics.TAG_PRIORITY).isEqualTo("priority");
        assertThat(WorkOrderMetrics.TAG_STATUS).isEqualTo("status");
        assertThat(WorkOrderMetrics.TAG_FROM_STATUS).isEqualTo("from_status");
        assertThat(WorkOrderMetrics.TAG_TO_STATUS).isEqualTo("to_status");
    }

    @Test
    @DisplayName("Constructor riêng tư có thể triệu gọi qua Reflection để đạt độ bao phủ 100%")
    void privateConstructor_forCoverage() throws Exception {
        Constructor<WorkOrderMetrics> constructor = WorkOrderMetrics.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        WorkOrderMetrics instance = constructor.newInstance();
        assertThat(instance).isNotNull();
    }
}
