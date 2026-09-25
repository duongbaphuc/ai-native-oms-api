// Nguồn gốc AI: sinh từ docs/00-coding-rules.md, docs/00-api-rules.md, docs/01-domain-model.md
package com.gpc.oms.service;

import com.gpc.oms.domain.Priority;
import com.gpc.oms.domain.WorkOrder;
import com.gpc.oms.domain.WorkOrderRepository;
import com.gpc.oms.domain.WorkOrderStatus;
import com.gpc.oms.dto.PagedResponse;
import com.gpc.oms.dto.WorkOrderRequest;
import com.gpc.oms.dto.WorkOrderResponse;
import com.gpc.oms.dto.WorkOrderStatusRequest;
import com.gpc.oms.exception.ResourceNotFoundException;
import com.gpc.oms.testutil.WorkOrderTestFixtures;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Kiểm thử đơn vị cô lập tầng nghiệp vụ cho {@link WorkOrderService}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Kiểm thử đơn vị tầng nghiệp vụ WorkOrderService")
class WorkOrderServiceTest {

    @Mock
    private WorkOrderRepository repo;

    private MeterRegistry registry;
    private WorkOrderService service;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        service = new WorkOrderService(repo, registry);
    }

    @Nested
    @DisplayName("Nghiệp vụ tạo mới phiếu công tác (createWorkOrder)")
    class CreateWorkOrderTests {

        @Test
        @DisplayName("createWorkOrder lưu thực thể thành công và trả về DTO phản hồi đầy đủ dữ liệu")
        void createWorkOrder_success() {
            final WorkOrderRequest request = WorkOrderTestFixtures.createRequest(
                    "EQ-100", "Faulty transformer", Priority.CRITICAL);
            final WorkOrder saved = WorkOrderTestFixtures.createEntity(
                    "EQ-100", "Faulty transformer", Priority.CRITICAL);
            when(repo.save(any(WorkOrder.class))).thenReturn(saved);

            final WorkOrderResponse response = service.createWorkOrder(request);

            assertThat(response).isNotNull();
            assertThat(response.equipmentId()).isEqualTo("EQ-100");
            assertThat(response.description()).isEqualTo("Faulty transformer");
            assertThat(response.priority()).isEqualTo(Priority.CRITICAL);
            assertThat(response.status()).isEqualTo(WorkOrderStatus.OPEN);
            verify(repo, times(1)).save(any(WorkOrder.class));
        }

        @Test
        @DisplayName("createWorkOrder tăng biến đếm oms_workorders_created_total với tag priority và status")
        void createWorkOrder_incrementsCreatedCounter() {
            final WorkOrderRequest request = WorkOrderTestFixtures.createRequest(
                    "EQ-101", "Feeder pillar fault", Priority.HIGH);
            final WorkOrder saved = WorkOrderTestFixtures.createEntity(
                    "EQ-101", "Feeder pillar fault", Priority.HIGH);
            when(repo.save(any(WorkOrder.class))).thenReturn(saved);

            service.createWorkOrder(request);

            assertThat(registry.get("oms_workorders_created_total")
                .tags("priority", "HIGH", "status", "OPEN").counter().count()).isEqualTo(1.0);
        }
    }

    @Nested
    @DisplayName("Nghiệp vụ tra cứu danh sách có phân trang (getWorkOrders)")
    class GetWorkOrdersTests {

        @Test
        @DisplayName("getWorkOrders khi status != null ủy quyền sang repo.findByStatus()")
        void getWorkOrders_withStatusFilter_callsFindByStatus() {
            final Pageable pageable = PageRequest.of(0, 10);
            final WorkOrder wo = WorkOrderTestFixtures.createEntity(
                    "EQ-100", "Faulty transformer", Priority.CRITICAL);
            final Page<WorkOrder> page = new PageImpl<>(List.of(wo), pageable, 1);
            when(repo.findByStatus(WorkOrderStatus.OPEN, pageable)).thenReturn(page);

            final PagedResponse<WorkOrderResponse> result = service.getWorkOrders(pageable, WorkOrderStatus.OPEN);

            assertThat(result.content()).hasSize(1);
            assertThat(result.content().get(0).equipmentId()).isEqualTo("EQ-100");
            verify(repo, times(1)).findByStatus(WorkOrderStatus.OPEN, pageable);
            verify(repo, never()).findAll(any(Pageable.class));
        }

        @Test
        @DisplayName("getWorkOrders khi status == null ủy quyền sang repo.findAll()")
        void getWorkOrders_withoutStatusFilter_callsFindAll() {
            final Pageable pageable = PageRequest.of(0, 10);
            final WorkOrder wo = WorkOrderTestFixtures.createEntity(
                    "EQ-200", "Line sagging", Priority.MEDIUM);
            final Page<WorkOrder> page = new PageImpl<>(List.of(wo), pageable, 1);
            when(repo.findAll(pageable)).thenReturn(page);

            final PagedResponse<WorkOrderResponse> result = service.getWorkOrders(pageable, null);

            assertThat(result.content()).hasSize(1);
            assertThat(result.content().get(0).equipmentId()).isEqualTo("EQ-200");
            verify(repo, times(1)).findAll(pageable);
            verify(repo, never()).findByStatus(any(), any());
        }
    }

    @Nested
    @DisplayName("Nghiệp vụ tra cứu chi tiết phiếu công tác (getWorkOrderById)")
    class GetWorkOrderByIdTests {

        @Test
        @DisplayName("getWorkOrderById khi tìm thấy trả về DTO phản hồi đã ánh xạ")
        void getWorkOrderById_found_returnsResponse() {
            final UUID id = UUID.randomUUID();
            final WorkOrder wo = WorkOrderTestFixtures.createEntity(
                    "EQ-300", "Cable snapped", Priority.HIGH);
            when(repo.findById(id)).thenReturn(Optional.of(wo));

            final WorkOrderResponse response = service.getWorkOrderById(id);

            assertThat(response).isNotNull();
            assertThat(response.equipmentId()).isEqualTo("EQ-300");
            assertThat(response.description()).isEqualTo("Cable snapped");
            verify(repo, times(1)).findById(id);
        }

        @Test
        @DisplayName("getWorkOrderById khi không tìm thấy ném ngoại lệ ResourceNotFoundException")
        void getWorkOrderById_notFound_throwsException() {
            final UUID id = UUID.randomUUID();
            when(repo.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getWorkOrderById(id))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("WorkOrder not found with id: " + id);

            verify(repo, times(1)).findById(id);
        }
    }

    @Nested
    @DisplayName("Nghiệp vụ cập nhật chuyển trạng thái (updateStatus)")
    class UpdateStatusTests {

        @Test
        @DisplayName("updateStatus với chuyển trạng thái hợp lệ cập nhật, lưu trữ và trả về DTO")
        void updateStatus_validTransition_savesAndReturnsDto() {
            final UUID id = UUID.randomUUID();
            final WorkOrder wo = WorkOrderTestFixtures.createEntity(
                    "EQ-400", "Underground cable fault", Priority.HIGH);
            when(repo.findById(id)).thenReturn(Optional.of(wo));
            when(repo.save(wo)).thenReturn(wo);

            final WorkOrderStatusRequest req = WorkOrderTestFixtures.createStatusRequest(WorkOrderStatus.IN_PROGRESS);
            final WorkOrderResponse response = service.updateStatus(id, req);

            assertThat(response).isNotNull();
            assertThat(response.status()).isEqualTo(WorkOrderStatus.IN_PROGRESS);
            verify(repo, times(1)).findById(id);
            verify(repo, times(1)).save(wo);
        }

        @Test
        @DisplayName("updateStatus tăng biến đếm oms_workorder_status_transitions_total với tag from và to")
        void updateStatus_incrementsTransitionCounter() {
            final UUID id = UUID.randomUUID();
            final WorkOrder wo = WorkOrderTestFixtures.createEntity(
                    "EQ-401", "Pole mounted fault", Priority.MEDIUM);
            when(repo.findById(id)).thenReturn(Optional.of(wo));
            when(repo.save(wo)).thenReturn(wo);

            service.updateStatus(id, WorkOrderTestFixtures.createStatusRequest(WorkOrderStatus.IN_PROGRESS));

            assertThat(registry.get("oms_workorder_status_transitions_total")
                .tags("from_status", "OPEN", "to_status", "IN_PROGRESS").counter().count()).isEqualTo(1.0);
        }

        @Test
        @DisplayName("updateStatus khi phiếu công tác không tồn tại ném ResourceNotFoundException")
        void updateStatus_notFound_throwsException() {
            final UUID id = UUID.randomUUID();
            when(repo.findById(id)).thenReturn(Optional.empty());

            final WorkOrderStatusRequest req = WorkOrderTestFixtures.createStatusRequest(WorkOrderStatus.IN_PROGRESS);
            assertThatThrownBy(() -> service.updateStatus(id, req))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("WorkOrder not found with id: " + id);

            verify(repo, times(1)).findById(id);
            verify(repo, never()).save(any());
        }

        @Test
        @DisplayName("updateStatus khi chuyển trạng thái không hợp lệ ném IllegalStateException")
        void updateStatus_invalidTransition_rethrowsIllegalStateException() {
            final UUID id = UUID.randomUUID();
            final WorkOrder wo = WorkOrderTestFixtures.createEntity(
                    "EQ-500", "Meter defect", Priority.LOW);
            when(repo.findById(id)).thenReturn(Optional.of(wo));

            // Thử nhảy cóc OPEN -> DONE (chuyển trạng thái không hợp lệ)
            final WorkOrderStatusRequest req = WorkOrderTestFixtures.createStatusRequest(WorkOrderStatus.DONE);

            assertThatThrownBy(() -> service.updateStatus(id, req))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Invalid state transition from OPEN to DONE");

            verify(repo, times(1)).findById(id);
            verify(repo, never()).save(any());
        }
    }
}
