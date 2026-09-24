// AI Provenance: generated from docs/coding-rules.md, docs/api-rules.md, docs/domain-model.md
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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
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

@ExtendWith(MockitoExtension.class)
@DisplayName("WorkOrderService Unit Tests")
class WorkOrderServiceTest {

    @Mock
    private WorkOrderRepository repo;

    @InjectMocks
    private WorkOrderService service;

    @Nested
    @DisplayName("createWorkOrder")
    class CreateWorkOrderTests {

        @Test
        @DisplayName("createWorkOrder saves entity and returns populated WorkOrderResponse")
        void createWorkOrder_success() {
            WorkOrderRequest request = new WorkOrderRequest("EQ-100", "Faulty transformer", Priority.CRITICAL);
            WorkOrder saved = new WorkOrder("EQ-100", "Faulty transformer", Priority.CRITICAL);
            when(repo.save(any(WorkOrder.class))).thenReturn(saved);

            WorkOrderResponse response = service.createWorkOrder(request);

            assertThat(response).isNotNull();
            assertThat(response.equipmentId()).isEqualTo("EQ-100");
            assertThat(response.description()).isEqualTo("Faulty transformer");
            assertThat(response.priority()).isEqualTo(Priority.CRITICAL);
            assertThat(response.status()).isEqualTo(WorkOrderStatus.OPEN);
            verify(repo, times(1)).save(any(WorkOrder.class));
        }
    }

    @Nested
    @DisplayName("getWorkOrders")
    class GetWorkOrdersTests {

        @Test
        @DisplayName("getWorkOrders with status != null delegates to repo.findByStatus()")
        void getWorkOrders_withStatusFilter_callsFindByStatus() {
            Pageable pageable = PageRequest.of(0, 10);
            WorkOrder wo = new WorkOrder("EQ-100", "Faulty transformer", Priority.CRITICAL);
            Page<WorkOrder> page = new PageImpl<>(List.of(wo), pageable, 1);
            when(repo.findByStatus(WorkOrderStatus.OPEN, pageable)).thenReturn(page);

            PagedResponse<WorkOrderResponse> result = service.getWorkOrders(pageable, WorkOrderStatus.OPEN);

            assertThat(result.content()).hasSize(1);
            assertThat(result.content().get(0).equipmentId()).isEqualTo("EQ-100");
            verify(repo, times(1)).findByStatus(WorkOrderStatus.OPEN, pageable);
            verify(repo, never()).findAll(any(Pageable.class));
        }

        @Test
        @DisplayName("getWorkOrders with status == null delegates to repo.findAll()")
        void getWorkOrders_withoutStatusFilter_callsFindAll() {
            Pageable pageable = PageRequest.of(0, 10);
            WorkOrder wo = new WorkOrder("EQ-200", "Line sagging", Priority.MEDIUM);
            Page<WorkOrder> page = new PageImpl<>(List.of(wo), pageable, 1);
            when(repo.findAll(pageable)).thenReturn(page);

            PagedResponse<WorkOrderResponse> result = service.getWorkOrders(pageable, null);

            assertThat(result.content()).hasSize(1);
            assertThat(result.content().get(0).equipmentId()).isEqualTo("EQ-200");
            verify(repo, times(1)).findAll(pageable);
            verify(repo, never()).findByStatus(any(), any());
        }
    }

    @Nested
    @DisplayName("getWorkOrderById")
    class GetWorkOrderByIdTests {

        @Test
        @DisplayName("getWorkOrderById when found returns mapped WorkOrderResponse")
        void getWorkOrderById_found_returnsResponse() {
            UUID id = UUID.randomUUID();
            WorkOrder wo = new WorkOrder("EQ-300", "Cable snapped", Priority.HIGH);
            when(repo.findById(id)).thenReturn(Optional.of(wo));

            WorkOrderResponse response = service.getWorkOrderById(id);

            assertThat(response).isNotNull();
            assertThat(response.equipmentId()).isEqualTo("EQ-300");
            assertThat(response.description()).isEqualTo("Cable snapped");
            verify(repo, times(1)).findById(id);
        }

        @Test
        @DisplayName("getWorkOrderById when not found throws ResourceNotFoundException")
        void getWorkOrderById_notFound_throwsException() {
            UUID id = UUID.randomUUID();
            when(repo.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getWorkOrderById(id))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("WorkOrder not found with id: " + id);

            verify(repo, times(1)).findById(id);
        }
    }

    @Nested
    @DisplayName("updateStatus")
    class UpdateStatusTests {

        @Test
        @DisplayName("updateStatus with valid transition advances status, saves, and returns DTO")
        void updateStatus_validTransition_savesAndReturnsDto() {
            UUID id = UUID.randomUUID();
            WorkOrder wo = new WorkOrder("EQ-400", "Underground cable fault", Priority.HIGH);
            when(repo.findById(id)).thenReturn(Optional.of(wo));
            when(repo.save(wo)).thenReturn(wo);

            WorkOrderStatusRequest req = new WorkOrderStatusRequest(WorkOrderStatus.IN_PROGRESS);
            WorkOrderResponse response = service.updateStatus(id, req);

            assertThat(response).isNotNull();
            assertThat(response.status()).isEqualTo(WorkOrderStatus.IN_PROGRESS);
            verify(repo, times(1)).findById(id);
            verify(repo, times(1)).save(wo);
        }

        @Test
        @DisplayName("updateStatus when work order does not exist throws ResourceNotFoundException")
        void updateStatus_notFound_throwsException() {
            UUID id = UUID.randomUUID();
            when(repo.findById(id)).thenReturn(Optional.empty());

            WorkOrderStatusRequest req = new WorkOrderStatusRequest(WorkOrderStatus.IN_PROGRESS);
            assertThatThrownBy(() -> service.updateStatus(id, req))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("WorkOrder not found with id: " + id);

            verify(repo, times(1)).findById(id);
            verify(repo, never()).save(any());
        }

        @Test
        @DisplayName("updateStatus with invalid transition rethrows IllegalStateException")
        void updateStatus_invalidTransition_rethrowsIllegalStateException() {
            UUID id = UUID.randomUUID();
            WorkOrder wo = new WorkOrder("EQ-500", "Meter defect", Priority.LOW);
            when(repo.findById(id)).thenReturn(Optional.of(wo));

            // Attempt OPEN -> DONE (invalid transition)
            WorkOrderStatusRequest req = new WorkOrderStatusRequest(WorkOrderStatus.DONE);

            assertThatThrownBy(() -> service.updateStatus(id, req))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Invalid state transition from OPEN to DONE");

            verify(repo, times(1)).findById(id);
            verify(repo, never()).save(any());
        }
    }
}
