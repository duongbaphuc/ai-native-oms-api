<!--
Role: Senior Engineer. Task: Comprehensive Acceptance Matrix & Test Sketches for Outage Work Order API (Testing Pyramid: Unit, Slice, DataJpa, Security, Integration).
Context files: docs/00-coding-rules.md, docs/01-domain-model.md, docs/02-api-spec.md, docs/02-security-auth-spec.md, docs/02-observability-and-logging.md, docs/08-SYSTEM_HANDOVER.md
Constraints: 
- JUnit 5 + Mockito + MockMvc + @DataJpaTest + @SpringBootTest.
- Testing Pyramid: 10 Unit Tests, 3 Slice Tests, 4 Security Tests, 1 DataJpa Test, 1 Full Integration Test, 1 Smoke Test (Total 20 test classes, 129 test cases).
- 100% Line Coverage & 100% Branch Coverage via JaCoCo.
- RFC 7807 assertions: type, title, status, instance, invalidParams.
- Zero Lombok, Pure Java 17, Constructor Injection.
DRAFT ONLY — scoring target, never wired into app.
-->
# Draft: Comprehensive Testing Pyramid & Acceptance Blueprints

## 1. Target Files (Complete 20 Test Classes)

| # | Test Class | Layer / Category | Path | Action | Phạm Vi & Mục Đích |
|---|---|---|---|---|---|
| 1 | `OmsApiApplicationTests` | Smoke / Context | `src/test/java/com/gpc/oms/OmsApiApplicationTests.java` | NEW | Kiểm tra nạp Spring Context thành công (0 failure) |
| 2 | `WorkOrderIntegrationTest` | Full Integration | `src/test/java/com/gpc/oms/WorkOrderIntegrationTest.java` | NEW | E2E integration test: vòng đời đầy đủ của Work Order qua HTTP REST |
| 3 | `ActuatorSecurityTest` | Security Integration | `src/test/java/com/gpc/oms/config/ActuatorSecurityTest.java` | NEW | Phân quyền RBAC cho /actuator/health (public) vs /actuator/prometheus (ADMIN) |
| 4 | `CorrelationIdFilterTest` | Filter Unit | `src/test/java/com/gpc/oms/config/CorrelationIdFilterTest.java` | NEW | Trích xuất/sinh header `X-Correlation-Id`, nạp & dọn dẹp MDC context |
| 5 | `H2ConsoleSecurityTest` | Security Slice | `src/test/java/com/gpc/oms/config/H2ConsoleSecurityTest.java` | NEW | Kiểm tra mở H2 console ở dev (`!prod`) và chặn ở production |
| 6 | `JwtRoleConverterTest` | Security Unit | `src/test/java/com/gpc/oms/config/JwtRoleConverterTest.java` | NEW | Giải mã JWT claims, chuẩn hóa tiền tố `ROLE_` |
| 7 | `OAuth2JwtSecurityIntegrationTest`| Security Integration | `src/test/java/com/gpc/oms/config/OAuth2JwtSecurityIntegrationTest.java` | NEW | Xác thực Bearer Token JWT cho toàn bộ WorkOrder endpoints |
| 8 | `RateLimitingFilterTest` | Filter Unit | `src/test/java/com/gpc/oms/config/RateLimitingFilterTest.java` | NEW | Token Bucket 60 req/min (Read), 20 req/min (Write), mã 429 RFC 7807 |
| 9 | `StringToWorkOrderStatusConverterTest` | Config Unit | `src/test/java/com/gpc/oms/config/StringToWorkOrderStatusConverterTest.java` | NEW | Chuyển đổi query param String sang enum (hỗ trợ hoa/thường) |
| 10 | `GlobalExceptionHandlerUnitTest` | Slice Unit | `src/test/java/com/gpc/oms/controller/GlobalExceptionHandlerUnitTest.java` | NEW | Cô lập kiểm thử 7 handlers ánh xạ sang cấu trúc RFC 7807 Problem Details |
| 11 | `WorkOrderControllerTest` | WebMvc Slice | `src/test/java/com/gpc/oms/controller/WorkOrderControllerTest.java` | NEW | MockMvc slice test 4 endpoint, validation, phân quyền, HTTP statuses |
| 12 | `PriorityTest` | Domain Unit | `src/test/java/com/gpc/oms/domain/PriorityTest.java` | NEW | Kiểm tra enum Priority (LOW, MEDIUM, HIGH, CRITICAL) |
| 13 | `WorkOrderStatusTest` | Domain Unit | `src/test/java/com/gpc/oms/domain/WorkOrderStatusTest.java` | NEW | Kiểm tra toàn bộ ma trận chuyển đổi 3x3 trạng thái của WorkOrderStatus |
| 14 | `WorkOrderTest` | Domain Unit | `src/test/java/com/gpc/oms/domain/WorkOrderTest.java` | NEW | Invariants của Aggregate Root: constructor, advanceStatus, resolvedAt |
| 15 | `DtoMappingTest` | DTO Unit | `src/test/java/com/gpc/oms/dto/DtoMappingTest.java` | NEW | Kiểm tra tính bất biến của Java Records, factory methods, mapping |
| 16 | `ProblemTypesTest` | Exception Unit | `src/test/java/com/gpc/oms/exception/ProblemTypesTest.java` | NEW | Kiểm tra 7 hằng số URI RFC 7807 và non-instantiability |
| 17 | `ResourceNotFoundExceptionTest` | Exception Unit | `src/test/java/com/gpc/oms/exception/ResourceNotFoundExceptionTest.java` | NEW | Khởi tạo ngoại lệ NotFound và format message chuẩn |
| 18 | `WorkOrderRepositoryTest` | DataJpa Slice | `src/test/java/com/gpc/oms/repository/WorkOrderRepositoryTest.java` | NEW | Lưu trữ H2 DB, JPA auditing, phân trang và truy vấn findByStatus |
| 19 | `WorkOrderServiceTest` | Service Unit | `src/test/java/com/gpc/oms/service/WorkOrderServiceTest.java` | NEW | Business logic với Mockito, kiểm thử transition, entity mapping, exceptions |
| 20 | `WorkOrderTestFixtures` | Test Utilities | `src/test/java/com/gpc/oms/testutil/WorkOrderTestFixtures.java` | NEW | Object Mother tạo mẫu Entity và DTOs dùng chung cho toàn bộ suite test |

---

## 2. Comprehensive Acceptance Matrix (Given/When/Then)

| # | Layer | Target Component | Given | When | Then |
|---|---|---|---|---|---|
| 1 | Unit | `WorkOrderStatus` | Trạng thái OPEN | canTransitionTo(IN_PROGRESS) | Trả về `true` |
| 2 | Unit | `WorkOrderStatus` | Trạng thái OPEN | canTransitionTo(DONE) | Trả về `false` (nhảy cóc bị cấm) |
| 3 | Unit | `WorkOrderStatus` | Trạng thái DONE | canTransitionTo(IN_PROGRESS) | Trả về `false` (quay lui bị cấm) |
| 4 | Unit | `WorkOrder` | Entity mới tạo | Kiểm tra getter | Status = OPEN, resolvedAt = null, createdAt != null |
| 5 | Unit | `WorkOrder` | Status OPEN | advanceStatus(IN_PROGRESS) | Status chuyển thành IN_PROGRESS, resolvedAt = null |
| 6 | Unit | `WorkOrder` | Status IN_PROGRESS | advanceStatus(DONE) | Status chuyển thành DONE, resolvedAt được gán thời gian hiện tại |
| 7 | Unit | `WorkOrder` | Status OPEN | advanceStatus(DONE) | Throw `IllegalStateException("Invalid state transition...")` |
| 8 | Unit | `WorkOrderService` | Request hợp lệ | createWorkOrder(req) | Lưu repository, trả về WorkOrderResponse status OPEN |
| 9 | Unit | `WorkOrderService` | ID không tồn tại | getWorkOrderById(id) | Ném `ResourceNotFoundException` |
| 10 | Unit | `WorkOrderService` | ID tồn tại | updateStatus(id, req) | Cập nhật entity, lưu DB, trả về response với status mới |
| 11 | Slice | `WorkOrderController` | Body hợp lệ, role TECHNICIAN | POST /api/v1/workorders | 201 Created, header Location, JSON status "Open" |
| 12 | Slice | `WorkOrderController` | Body hợp lệ, role DISPATCHER | POST /api/v1/workorders | 201 Created |
| 13 | Slice | `WorkOrderController` | Không có credentials | POST /api/v1/workorders | 401 Unauthorized, RFC 7807 type unauthorized |
| 14 | Slice | `WorkOrderController` | Body thiếu equipmentId | POST /api/v1/workorders | 400 Bad Request, invalidParams chứa "equipmentId" |
| 15 | Slice | `WorkOrderController` | Priority = "URGENT" (sai enum)| POST /api/v1/workorders | 400 Bad Request, type = urn:problem-type:malformed-json |
| 16 | Slice | `WorkOrderController` | Body chứa extra field lạ | POST /api/v1/workorders | 400 Bad Request (ignoreUnknown=false) |
| 17 | Slice | `WorkOrderController` | Role DISPATCHER / TECHNICIAN | GET /api/v1/workorders | 200 OK, PagedResponse (content, pageNumber, totalElements) |
| 18 | Slice | `WorkOrderController` | ID không tồn tại | GET /api/v1/workorders/{id} | 404 Not Found, type = urn:problem-type:not-found |
| 19 | Slice | `WorkOrderController` | Chuyển đổi hợp lệ | PATCH /{id}/status | 200 OK, status cập nhật |
| 20 | Slice | `WorkOrderController` | Chuyển đổi vi phạm | PATCH /{id}/status | 422 Unprocessable Entity, invalid-state-transition |
| 21 | DataJpa| `WorkOrderRepository` | Entity hợp lệ | repository.save(wo) | Tạo UUID tự động, createdAt tự động sinh, lưu vào bảng work_orders |
| 22 | DataJpa| `WorkOrderRepository` | Status OPEN | findByStatus(OPEN, PageRequest)| Trả về đúng danh sách các record khớp status |
| 23 | Security| `SecurityConfig` | Profile dev (!prod) | GET /h2-console | 200 OK (Console mở) |
| 24 | Security| `SecurityConfig` | Profile prod | GET /h2-console ẩn danh | 401/403 Bị chặn truy cập |
| 25 | Security| `SecurityConfig` | Anonymous | GET /actuator/health | 200 OK `{"status":"UP"}` |
| 26 | Security| `SecurityConfig` | Anonymous | GET /actuator/prometheus | 401 Unauthorized |
| 27 | Security| `SecurityConfig` | User role ADMIN | GET /actuator/prometheus | 200 OK nạp số liệu Prometheus |
| 28 | Security| `JwtRoleConverter` | JWT claim roles: ["DISPATCHER"]| convert(jwt) | Trả về authority `ROLE_DISPATCHER` |
| 29 | Filter | `CorrelationIdFilter` | Request có X-Correlation-Id | doFilter | MDC & response header giữ nguyên ID này |
| 30 | Filter | `CorrelationIdFilter` | Request không có ID | doFilter | Sinh UUID ngẫu nhiên, gán MDC & response header, clear() ở finally |
| 31 | Filter | `RateLimitingFilter` | 60 GET requests liên tiếp | Lượt 61 | 429 Too Many Requests, header Retry-After, RFC 7807 problem detail |
| 32 | Filter | `RateLimitingFilter` | 20 POST requests liên tiếp | Lượt 21 | 429 Too Many Requests, header Retry-After |
| 33 | Integration| `WorkOrderIntegrationTest`| Chuỗi thao tác thực tế | POST -> GET -> PATCH -> GET | Toàn bộ vòng đời dữ liệu nhất quán xuyên suốt qua H2 DB thực tế |

---

## 3. Representative Test Implementation Sketches

### Sketch 1: Domain State Machine Unit Test (`WorkOrderTest.java`)
```java
// AI Provenance: generated from docs/01-domain-model.md §Invariants
package com.gpc.oms.domain;

import com.gpc.oms.testutil.WorkOrderTestFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WorkOrderTest {

    @Test
    @DisplayName("advanceStatus: Linear flow OPEN -> IN_PROGRESS -> DONE sets resolvedAt")
    void advanceStatus_allowsLinearFlow_andSetsResolvedAtOnDone() {
        final WorkOrder wo = WorkOrderTestFixtures.createDefaultEntity();
        assertEquals(WorkOrderStatus.OPEN, wo.getStatus());
        assertNull(wo.getResolvedAt());

        wo.advanceStatus(WorkOrderStatus.IN_PROGRESS);
        assertEquals(WorkOrderStatus.IN_PROGRESS, wo.getStatus());
        assertNull(wo.getResolvedAt());

        wo.advanceStatus(WorkOrderStatus.DONE);
        assertEquals(WorkOrderStatus.DONE, wo.getStatus());
        assertNotNull(wo.getResolvedAt());
    }

    @Test
    @DisplayName("advanceStatus: Reject skipping from OPEN directly to DONE")
    void advanceStatus_rejectsSkip_openToDone() {
        final WorkOrder wo = WorkOrderTestFixtures.createDefaultEntity();
        assertThrows(IllegalStateException.class, () -> wo.advanceStatus(WorkOrderStatus.DONE));
    }

    @Test
    @DisplayName("advanceStatus: Reject rolling back from DONE to IN_PROGRESS")
    void advanceStatus_rejectsRollback_doneToInProgress() {
        final WorkOrder wo = WorkOrderTestFixtures.createDefaultEntity();
        wo.advanceStatus(WorkOrderStatus.IN_PROGRESS);
        wo.advanceStatus(WorkOrderStatus.DONE);
        assertThrows(IllegalStateException.class, () -> wo.advanceStatus(WorkOrderStatus.IN_PROGRESS));
    }
}
```

### Sketch 2: Service Unit Test with Mockito (`WorkOrderServiceTest.java`)
```java
// AI Provenance: generated from docs/01-domain-model.md, docs/02-api-spec.md
package com.gpc.oms.service;

import com.gpc.oms.domain.WorkOrder;
import com.gpc.oms.domain.WorkOrderRepository;
import com.gpc.oms.domain.WorkOrderStatus;
import com.gpc.oms.dto.WorkOrderRequest;
import com.gpc.oms.dto.WorkOrderResponse;
import com.gpc.oms.dto.WorkOrderStatusRequest;
import com.gpc.oms.exception.ResourceNotFoundException;
import com.gpc.oms.testutil.WorkOrderTestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkOrderServiceTest {

    @Mock
    private WorkOrderRepository repository;

    @InjectMocks
    private WorkOrderService service;

    private WorkOrder sampleWorkOrder;

    @BeforeEach
    void setUp() {
        sampleWorkOrder = WorkOrderTestFixtures.createDefaultEntity();
    }

    @Test
    void createWorkOrder_savesAndReturnsResponse() {
        when(repository.save(any(WorkOrder.class))).thenReturn(sampleWorkOrder);

        final WorkOrderRequest request = WorkOrderTestFixtures.createDefaultRequest();
        final WorkOrderResponse response = service.createWorkOrder(request);

        assertNotNull(response);
        assertEquals(sampleWorkOrder.getEquipmentId(), response.equipmentId());
        verify(repository, times(1)).save(any(WorkOrder.class));
    }

    @Test
    void getWorkOrderById_notFound_throwsException() {
        final UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.getWorkOrderById(id));
    }
}
```

### Sketch 3: DataJpa Repository Test (`WorkOrderRepositoryTest.java`)
```java
// AI Provenance: generated from docs/01-domain-model.md
package com.gpc.oms.repository;

import com.gpc.oms.domain.Priority;
import com.gpc.oms.domain.WorkOrder;
import com.gpc.oms.domain.WorkOrderRepository;
import com.gpc.oms.domain.WorkOrderStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class WorkOrderRepositoryTest {

    @Autowired
    private WorkOrderRepository repository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void findByStatus_returnsFilteredWorkOrders() {
        final WorkOrder wo1 = new WorkOrder("EQ-01", "Cắt điện bảo dưỡng", Priority.HIGH);
        final WorkOrder wo2 = new WorkOrder("EQ-02", "Kiểm tra định kỳ", Priority.MEDIUM);
        wo2.advanceStatus(WorkOrderStatus.IN_PROGRESS);

        entityManager.persist(wo1);
        entityManager.persist(wo2);
        entityManager.flush();

        final Page<WorkOrder> openOrders = repository.findByStatus(WorkOrderStatus.OPEN, PageRequest.of(0, 10));
        assertEquals(1, openOrders.getTotalElements());
        assertEquals("EQ-01", openOrders.getContent().get(0).getEquipmentId());
    }
}
```

---

## 4. Testing Hygiene & Best Practices Checklist

- [ ] **No Cross-Layer Contamination:** Controller tests chỉ mock `WorkOrderService` (không mock `WorkOrderRepository`).
- [ ] **Deterministic UUID & Timestamps:** Sử dụng `WorkOrderTestFixtures` để khởi tạo dữ liệu đồng nhất.
- [ ] **RFC 7807 Conformity:** Mọi phản hồi lỗi HTTP 400, 401, 403, 404, 422, 429, 500 đều assert trường `type`, `title`, `status`, `instance`.
- [ ] **ThreadLocal Hygiene:** Khối `finally { MDC.clear(); }` được kiểm tra nghiêm ngặt trong `CorrelationIdFilterTest`.
- [ ] **Zero-Warning & Zero-Failure:** Toàn bộ 129 bài kiểm thử vượt qua tuyệt đối trên môi trường máy chủ CI (`mvn clean verify`).
