<!--
Role: Senior Engineer. Task: Bảng tổng hợp tất cả file Java cần tạo/cập nhật cho dự án OMS API.
Context files: Tất cả draft files trong docs/drafts/
Constraints: Mỗi file Java phải có draft nguồn tương ứng. Không tạo file ngoài danh sách này.
-->
# Draft: Target File Mapping — Outage Work Order API

Bảng này liệt kê **toàn bộ** file Java cần sinh code, package, hành động (NEW/MODIFY), và draft nguồn tham chiếu.  
Copilot/LLM **KHÔNG được tạo file ngoài danh sách này** trừ khi được phê duyệt rõ ràng.

---

## Production Code (`src/main/java/`)

| # | File | Package | Full Path | Action | Draft Nguồn |
|---|---|---|---|---|---|
| 1 | `WorkOrder.java` | `com.gpc.oms.domain` | `src/main/java/com/gpc/oms/domain/WorkOrder.java` | NEW | [`draft-workorder-domain.md`](draft-workorder-domain.md) §4 |
| 2 | `Priority.java` | `com.gpc.oms.domain` | `src/main/java/com/gpc/oms/domain/Priority.java` | NEW | [`draft-dtos.md`](draft-dtos.md) §1.1 |
| 3 | `WorkOrderStatus.java` | `com.gpc.oms.domain` | `src/main/java/com/gpc/oms/domain/WorkOrderStatus.java` | NEW | [`draft-dtos.md`](draft-dtos.md) §1.2 |
| 4 | `WorkOrderRepository.java` | `com.gpc.oms.domain` | `src/main/java/com/gpc/oms/domain/WorkOrderRepository.java` | NEW | [`draft-workorder-domain.md`](draft-workorder-domain.md) §6 |
| 5 | `WorkOrderRequest.java` | `com.gpc.oms.dto` | `src/main/java/com/gpc/oms/dto/WorkOrderRequest.java` | NEW | [`draft-dtos.md`](draft-dtos.md) §2.1 |
| 6 | `WorkOrderStatusRequest.java` | `com.gpc.oms.dto` | `src/main/java/com/gpc/oms/dto/WorkOrderStatusRequest.java` | NEW | [`draft-dtos.md`](draft-dtos.md) §2.2 |
| 7 | `WorkOrderResponse.java` | `com.gpc.oms.dto` | `src/main/java/com/gpc/oms/dto/WorkOrderResponse.java` | NEW | [`draft-dtos.md`](draft-dtos.md) §3.1 |
| 8 | `WorkOrderService.java` | `com.gpc.oms.service` | `src/main/java/com/gpc/oms/service/WorkOrderService.java` | NEW | [`draft-workorder-service.md`](draft-workorder-service.md) |
| 9 | `WorkOrderController.java` | `com.gpc.oms.controller` | `src/main/java/com/gpc/oms/controller/WorkOrderController.java` | NEW | [`draft-workorder-create.md`](draft-workorder-create.md), [`draft-workorder-get.md`](draft-workorder-get.md), [`draft-workorder-patch.md`](draft-workorder-patch.md) |
| 10 | `GlobalExceptionHandler.java` | `com.gpc.oms.exception` | `src/main/java/com/gpc/oms/exception/GlobalExceptionHandler.java` | NEW | [`draft-global-exception-handler.md`](draft-global-exception-handler.md) |

## Test Code (`src/test/java/`)

| # | File | Package | Full Path | Action | Draft Nguồn |
|---|---|---|---|---|---|
| 11 | `WorkOrderTest.java` | `com.gpc.oms.domain` | `src/test/java/com/gpc/oms/domain/WorkOrderTest.java` | NEW | [`draft-workorder-tests.md`](draft-workorder-tests.md) §Sketch 1 |
| 12 | `WorkOrderControllerTest.java` | `com.gpc.oms.controller` | `src/test/java/com/gpc/oms/controller/WorkOrderControllerTest.java` | NEW | [`draft-workorder-tests.md`](draft-workorder-tests.md) §Sketch 2 |

---

## Package Structure (Visualization)

```
src/main/java/com/gpc/oms/
├── OmsApiApplication.java          (existing)
├── domain/
│   ├── WorkOrder.java              (#1)
│   ├── Priority.java               (#2)
│   ├── WorkOrderStatus.java        (#3)
│   └── WorkOrderRepository.java    (#4)
├── dto/
│   ├── WorkOrderRequest.java       (#5)
│   ├── WorkOrderStatusRequest.java (#6)
│   └── WorkOrderResponse.java      (#7)
├── service/
│   └── WorkOrderService.java       (#8)
├── controller/
│   └── WorkOrderController.java    (#9)
└── exception/
    └── GlobalExceptionHandler.java (#10)

src/test/java/com/gpc/oms/
├── OmsApiApplicationTests.java     (existing)
├── domain/
│   └── WorkOrderTest.java          (#11)
└── controller/
    └── WorkOrderControllerTest.java (#12)
```

## Thứ tự Implement (Dependencies First)

1. **Enums** → `Priority.java`, `WorkOrderStatus.java` (không dependency)
2. **Entity** → `WorkOrder.java` (phụ thuộc Enums)
3. **Repository** → `WorkOrderRepository.java` (phụ thuộc Entity)
4. **DTOs** → `WorkOrderRequest.java`, `WorkOrderStatusRequest.java`, `WorkOrderResponse.java` (phụ thuộc Enums + Entity)
5. **Service** → `WorkOrderService.java` (phụ thuộc Repository + DTOs)
6. **Controller** → `WorkOrderController.java` (phụ thuộc Service + DTOs)
7. **Exception Handler** → `GlobalExceptionHandler.java` (independent)
8. **Tests** → `WorkOrderTest.java`, `WorkOrderControllerTest.java` (phụ thuộc tất cả)
