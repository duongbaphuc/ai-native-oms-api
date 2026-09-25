// Nguồn gốc AI: sinh từ docs/00-internal-coding-standards.md, docs/02-api-spec.md
package com.gpc.oms.dto;

import com.gpc.oms.domain.Priority;
import com.gpc.oms.domain.WorkOrder;
import com.gpc.oms.domain.WorkOrderStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Kiểm thử đơn vị cho việc ánh xạ DTO và các hợp đồng bất biến của Java 17 Records.
 */
@DisplayName("Kiểm thử đơn vị ánh xạ DTO và hợp đồng bất biến Record")
class DtoMappingTest {

    @Test
    @DisplayName("WorkOrderResponse.from(entity) ánh xạ chính xác mọi trường bao gồm resolvedAt null")
    void workOrderResponse_from_openEntity() {
        WorkOrder entity = new WorkOrder("EQ-01", "Transformer inspection", Priority.HIGH);

        WorkOrderResponse dto = WorkOrderResponse.from(entity);

        assertThat(dto.id()).isEqualTo(entity.getId());
        assertThat(dto.equipmentId()).isEqualTo("EQ-01");
        assertThat(dto.description()).isEqualTo("Transformer inspection");
        assertThat(dto.priority()).isEqualTo(Priority.HIGH);
        assertThat(dto.status()).isEqualTo(WorkOrderStatus.OPEN);
        assertThat(dto.createdAt()).isEqualTo(entity.getCreatedAt());
        assertThat(dto.resolvedAt()).isNull();
    }

    @Test
    @DisplayName("WorkOrderResponse.from(entity) ánh xạ đúng resolvedAt khi trạng thái là DONE")
    void workOrderResponse_from_doneEntity() {
        WorkOrder entity = new WorkOrder("EQ-02", "Line maintenance", Priority.CRITICAL);
        entity.advanceStatus(WorkOrderStatus.IN_PROGRESS);
        entity.advanceStatus(WorkOrderStatus.DONE);

        WorkOrderResponse dto = WorkOrderResponse.from(entity);

        assertThat(dto.status()).isEqualTo(WorkOrderStatus.DONE);
        assertThat(dto.resolvedAt()).isNotNull();
        assertThat(dto.resolvedAt()).isEqualTo(entity.getResolvedAt());
    }

    @Test
    @DisplayName("WorkOrderResponse.from(null) ném NullPointerException")
    void workOrderResponse_from_nullEntity_throwsException() {
        assertThatThrownBy(() -> WorkOrderResponse.from(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("workOrder must not be null");
    }

    @Test
    @DisplayName("Hợp đồng Record WorkOrderResponse: accessors, equals, hashCode, toString")
    void workOrderResponse_recordContract() {
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();
        WorkOrderResponse res1 = new WorkOrderResponse(
                id, "EQ-01", "Desc", Priority.LOW, WorkOrderStatus.OPEN, now, null);
        WorkOrderResponse res2 = new WorkOrderResponse(
                id, "EQ-01", "Desc", Priority.LOW, WorkOrderStatus.OPEN, now, null);
        WorkOrderResponse res3 = new WorkOrderResponse(
                UUID.randomUUID(), "EQ-02", "Desc2", Priority.HIGH, WorkOrderStatus.DONE, now, now);

        assertThat(res1).isEqualTo(res2);
        assertThat(res1.hashCode()).isEqualTo(res2.hashCode());
        assertThat(res1.toString()).contains("EQ-01");
        assertThat(res1).isNotEqualTo(res3);
        assertThat(res1).isNotEqualTo(null);
        assertThat(res1).isNotEqualTo(new Object());
    }

    @Test
    @DisplayName("PagedResponse.from(page) trên trang đầu tiên (isFirst=true, isLast=false)")
    void pagedResponse_from_firstPage() {
        List<String> content = List.of("A", "B", "C");
        Page<String> page = new PageImpl<>(content, PageRequest.of(0, 3), 9);

        PagedResponse<String> response = PagedResponse.from(page);

        assertThat(response.content()).containsExactly("A", "B", "C");
        assertThat(response.pageNumber()).isEqualTo(0);
        assertThat(response.pageSize()).isEqualTo(3);
        assertThat(response.totalElements()).isEqualTo(9L);
        assertThat(response.totalPages()).isEqualTo(3);
        assertThat(response.isFirst()).isTrue();
        assertThat(response.isLast()).isFalse();
    }

    @Test
    @DisplayName("PagedResponse.from(page) trên trang ở giữa (isFirst=false, isLast=false)")
    void pagedResponse_from_middlePage() {
        List<String> content = List.of("D", "E", "F");
        Page<String> page = new PageImpl<>(content, PageRequest.of(1, 3), 9);

        PagedResponse<String> response = PagedResponse.from(page);

        assertThat(response.content()).containsExactly("D", "E", "F");
        assertThat(response.pageNumber()).isEqualTo(1);
        assertThat(response.pageSize()).isEqualTo(3);
        assertThat(response.totalElements()).isEqualTo(9L);
        assertThat(response.totalPages()).isEqualTo(3);
        assertThat(response.isFirst()).isFalse();
        assertThat(response.isLast()).isFalse();
    }

    @Test
    @DisplayName("PagedResponse.from(page) trên trang cuối cùng (isFirst=false, isLast=true)")
    void pagedResponse_from_lastPage() {
        List<String> content = List.of("G", "H", "I");
        Page<String> page = new PageImpl<>(content, PageRequest.of(2, 3), 9);

        PagedResponse<String> response = PagedResponse.from(page);

        assertThat(response.content()).containsExactly("G", "H", "I");
        assertThat(response.pageNumber()).isEqualTo(2);
        assertThat(response.pageSize()).isEqualTo(3);
        assertThat(response.totalElements()).isEqualTo(9L);
        assertThat(response.totalPages()).isEqualTo(3);
        assertThat(response.isFirst()).isFalse();
        assertThat(response.isLast()).isTrue();
    }

    @Test
    @DisplayName("PagedResponse.from(page) trên trang rỗng (isFirst=true, isLast=true)")
    void pagedResponse_from_emptyPage() {
        Page<String> emptyPage = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 10), 0);

        PagedResponse<String> response = PagedResponse.from(emptyPage);

        assertThat(response.content()).isEmpty();
        assertThat(response.pageNumber()).isEqualTo(0);
        assertThat(response.pageSize()).isEqualTo(10);
        assertThat(response.totalElements()).isZero();
        assertThat(response.totalPages()).isZero();
        assertThat(response.isFirst()).isTrue();
        assertThat(response.isLast()).isTrue();
    }

    @Test
    @DisplayName("PagedResponse.from(null) ném NullPointerException")
    void pagedResponse_from_nullPage_throwsException() {
        assertThatThrownBy(() -> PagedResponse.from(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("page must not be null");
    }

    @Test
    @DisplayName("Hợp đồng Record PagedResponse: accessors, equals, hashCode, toString")
    void pagedResponse_recordContract() {
        PagedResponse<String> p1 = new PagedResponse<>(List.of("item"), 0, 10, 1, 1, true, true);
        PagedResponse<String> p2 = new PagedResponse<>(List.of("item"), 0, 10, 1, 1, true, true);
        PagedResponse<String> p3 = new PagedResponse<>(List.of("diff"), 1, 5, 2, 2, false, false);

        assertThat(p1).isEqualTo(p2);
        assertThat(p1.hashCode()).isEqualTo(p2.hashCode());
        assertThat(p1.toString()).contains("pageNumber=0");
        assertThat(p1).isNotEqualTo(p3);
        assertThat(p1).isNotEqualTo(null);
        assertThat(p1).isNotEqualTo("String");
    }

    @Test
    @DisplayName("Hợp đồng Record WorkOrderRequest: getters, equals, hashCode, toString")
    void workOrderRequest_recordContract() {
        WorkOrderRequest r1 = new WorkOrderRequest("EQ-10", "Description text", Priority.MEDIUM);
        WorkOrderRequest r2 = new WorkOrderRequest("EQ-10", "Description text", Priority.MEDIUM);
        WorkOrderRequest r3 = new WorkOrderRequest("EQ-20", "Other description", Priority.LOW);

        assertThat(r1.equipmentId()).isEqualTo("EQ-10");
        assertThat(r1.description()).isEqualTo("Description text");
        assertThat(r1.priority()).isEqualTo(Priority.MEDIUM);

        assertThat(r1).isEqualTo(r2);
        assertThat(r1.hashCode()).isEqualTo(r2.hashCode());
        assertThat(r1.toString()).contains("EQ-10");
        assertThat(r1).isNotEqualTo(r3);
        assertThat(r1).isNotEqualTo(null);
        assertThat(r1).isNotEqualTo("other");
    }

    @Test
    @DisplayName("Hợp đồng Record WorkOrderStatusRequest: getter, equals, hashCode, toString")
    void workOrderStatusRequest_recordContract() {
        WorkOrderStatusRequest r1 = new WorkOrderStatusRequest(WorkOrderStatus.IN_PROGRESS);
        WorkOrderStatusRequest r2 = new WorkOrderStatusRequest(WorkOrderStatus.IN_PROGRESS);
        WorkOrderStatusRequest r3 = new WorkOrderStatusRequest(WorkOrderStatus.DONE);

        assertThat(r1.status()).isEqualTo(WorkOrderStatus.IN_PROGRESS);

        assertThat(r1).isEqualTo(r2);
        assertThat(r1.hashCode()).isEqualTo(r2.hashCode());
        assertThat(r1.toString()).contains("IN_PROGRESS");
        assertThat(r1).isNotEqualTo(r3);
        assertThat(r1).isNotEqualTo(null);
        assertThat(r1).isNotEqualTo("other");
    }
}
