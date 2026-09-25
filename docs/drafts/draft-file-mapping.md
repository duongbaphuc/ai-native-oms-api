<!--
Role: Senior Engineer. Task: Bảng tổng hợp tất cả file Java cần tạo/cập nhật cho dự án OMS API (Zero-Draft-Drift 100%).
Context files: Tất cả draft files trong docs/drafts/
Constraints: Mỗi file Java trong toàn bộ src/ (39 files: 19 production + 20 test) phải có draft nguồn tương ứng. Không tạo file ngoài danh sách này.
-->
# Draft: Target File Mapping — Outage Work Order API (100% Zero-Draft-Drift)

Bảng này liệt kê **toàn bộ 39 file Java** trong dự án OMS API (19 production files + 20 test files), bao gồm package, full path, hành động, và tài liệu bản thảo (draft) nguồn làm Single Source of Truth.  
Copilot/LLM **KHÔNG được tạo file ngoài danh sách này** trừ khi có phê duyệt rõ ràng từ Kiến trúc sư trưởng.

---

## 1. Production Code (`src/main/java/`) — 19 Files

| # | File | Package | Full Path | Action | Draft Nguồn Tham Chiếu |
|---|---|---|---|---|---|
| 1 | `OmsApiApplication.java` | `com.gpc.oms` | `src/main/java/com/gpc/oms/OmsApiApplication.java` | NEW | [`draft-file-mapping.md`](draft-file-mapping.md) / Spring Initializr Entrypoint |
| 2 | `WorkOrder.java` | `com.gpc.oms.domain` | `src/main/java/com/gpc/oms/domain/WorkOrder.java` | NEW | [`draft-workorder-domain.md`](draft-workorder-domain.md) §4 |
| 3 | `Priority.java` | `com.gpc.oms.domain` | `src/main/java/com/gpc/oms/domain/Priority.java` | NEW | [`draft-workorder-domain.md`](draft-workorder-domain.md) §5.1 |
| 4 | `WorkOrderStatus.java` | `com.gpc.oms.domain` | `src/main/java/com/gpc/oms/domain/WorkOrderStatus.java` | NEW | [`draft-workorder-domain.md`](draft-workorder-domain.md) §5.2 |
| 5 | `WorkOrderRepository.java` | `com.gpc.oms.domain` | `src/main/java/com/gpc/oms/domain/WorkOrderRepository.java` | NEW | [`draft-workorder-domain.md`](draft-workorder-domain.md) §6 |
| 6 | `WorkOrderRequest.java` | `com.gpc.oms.dto` | `src/main/java/com/gpc/oms/dto/WorkOrderRequest.java` | NEW | [`draft-dtos.md`](draft-dtos.md) §2.1 |
| 7 | `WorkOrderStatusRequest.java` | `com.gpc.oms.dto` | `src/main/java/com/gpc/oms/dto/WorkOrderStatusRequest.java` | NEW | [`draft-dtos.md`](draft-dtos.md) §2.2 |
| 8 | `WorkOrderResponse.java` | `com.gpc.oms.dto` | `src/main/java/com/gpc/oms/dto/WorkOrderResponse.java` | NEW | [`draft-dtos.md`](draft-dtos.md) §3.1 |
| 9 | `PagedResponse.java` | `com.gpc.oms.dto` | `src/main/java/com/gpc/oms/dto/PagedResponse.java` | NEW | [`draft-dtos.md`](draft-dtos.md) §3.2 |
| 10 | `WorkOrderService.java` | `com.gpc.oms.service` | `src/main/java/com/gpc/oms/service/WorkOrderService.java` | NEW | [`draft-workorder-service.md`](draft-workorder-service.md) |
| 11 | `WorkOrderController.java` | `com.gpc.oms.controller` | `src/main/java/com/gpc/oms/controller/WorkOrderController.java` | NEW | [`draft-workorder-create.md`](draft-workorder-create.md), [`draft-workorder-get.md`](draft-workorder-get.md), [`draft-workorder-patch.md`](draft-workorder-patch.md) |
| 12 | `GlobalExceptionHandler.java` | `com.gpc.oms.exception` | `src/main/java/com/gpc/oms/exception/GlobalExceptionHandler.java` | NEW | [`draft-global-exception-handler.md`](draft-global-exception-handler.md) |
| 13 | `ProblemTypes.java` | `com.gpc.oms.exception` | `src/main/java/com/gpc/oms/exception/ProblemTypes.java` | NEW | [`draft-shared-components.md`](draft-shared-components.md) §3 |
| 14 | `ResourceNotFoundException.java` | `com.gpc.oms.exception` | `src/main/java/com/gpc/oms/exception/ResourceNotFoundException.java` | NEW | [`02-api-spec.md`](../02-api-spec.md) §3, [`draft-shared-components.md`](draft-shared-components.md) §6 |
| 15 | `SecurityConfig.java` | `com.gpc.oms.config` | `src/main/java/com/gpc/oms/config/SecurityConfig.java` | NEW | [`draft-security-config.md`](draft-security-config.md) §3 |
| 16 | `JwtRoleConverter.java` | `com.gpc.oms.config` | `src/main/java/com/gpc/oms/config/JwtRoleConverter.java` | NEW | [`draft-security-config.md`](draft-security-config.md) §4 |
| 17 | `CorrelationIdFilter.java` | `com.gpc.oms.config` | `src/main/java/com/gpc/oms/config/CorrelationIdFilter.java` | NEW | [`draft-observability-filters.md`](draft-observability-filters.md) §3 |
| 18 | `RateLimitingFilter.java` | `com.gpc.oms.config` | `src/main/java/com/gpc/oms/config/RateLimitingFilter.java` | NEW | [`draft-observability-filters.md`](draft-observability-filters.md) §4 |
| 19 | `StringToWorkOrderStatusConverter.java` | `com.gpc.oms.config` | `src/main/java/com/gpc/oms/config/StringToWorkOrderStatusConverter.java` | NEW | [`draft-shared-components.md`](draft-shared-components.md) §4 |
| 20 | `RoleConstants.java` | `com.gpc.oms.config` | `src/main/java/com/gpc/oms/config/RoleConstants.java` | NEW | [`draft-security-config.md`](draft-security-config.md), [`00-coding-rules.md`](../00-coding-rules.md) |
| 21 | `RateLimitProperties.java` | `com.gpc.oms.config` | `src/main/java/com/gpc/oms/config/RateLimitProperties.java` | NEW | [`draft-observability-filters.md`](draft-observability-filters.md), [`00-coding-rules.md`](../00-coding-rules.md) |
| 22 | `WorkOrderMetrics.java` | `com.gpc.oms.service` | `src/main/java/com/gpc/oms/service/WorkOrderMetrics.java` | NEW | [`draft-workorder-service.md`](draft-workorder-service.md), [`00-coding-rules.md`](../00-coding-rules.md) |

---

## 2. Test Code (`src/test/java/`) — 23 Files

| # | File | Package | Full Path | Action | Draft Nguồn Tham Chiếu |
|---|---|---|---|---|---|
| 23 | `OmsApiApplicationTests.java` | `com.gpc.oms` | `src/test/java/com/gpc/oms/OmsApiApplicationTests.java` | NEW | [`draft-workorder-tests.md`](draft-workorder-tests.md) §1 (#1) |
| 24 | `WorkOrderIntegrationTest.java` | `com.gpc.oms` | `src/test/java/com/gpc/oms/WorkOrderIntegrationTest.java` | NEW | [`draft-workorder-tests.md`](draft-workorder-tests.md) §1 (#2) |
| 25 | `ActuatorSecurityTest.java` | `com.gpc.oms.config` | `src/test/java/com/gpc/oms/config/ActuatorSecurityTest.java` | NEW | [`draft-security-config.md`](draft-security-config.md) §5, [`draft-workorder-tests.md`](draft-workorder-tests.md) |
| 26 | `CorrelationIdFilterTest.java` | `com.gpc.oms.config` | `src/test/java/com/gpc/oms/config/CorrelationIdFilterTest.java` | NEW | [`draft-observability-filters.md`](draft-observability-filters.md) §5, [`draft-workorder-tests.md`](draft-workorder-tests.md) |
| 27 | `H2ConsoleSecurityTest.java` | `com.gpc.oms.config` | `src/test/java/com/gpc/oms/config/H2ConsoleSecurityTest.java` | NEW | [`draft-security-config.md`](draft-security-config.md) §5, [`draft-workorder-tests.md`](draft-workorder-tests.md) |
| 28 | `JwtRoleConverterTest.java` | `com.gpc.oms.config` | `src/test/java/com/gpc/oms/config/JwtRoleConverterTest.java` | NEW | [`draft-security-config.md`](draft-security-config.md) §5, [`draft-workorder-tests.md`](draft-workorder-tests.md) |
| 29 | `OAuth2JwtSecurityIntegrationTest.java` | `com.gpc.oms.config` | `src/test/java/com/gpc/oms/config/OAuth2JwtSecurityIntegrationTest.java` | NEW | [`draft-security-config.md`](draft-security-config.md) §5, [`draft-workorder-tests.md`](draft-workorder-tests.md) |
| 30 | `RateLimitingFilterTest.java` | `com.gpc.oms.config` | `src/test/java/com/gpc/oms/config/RateLimitingFilterTest.java` | NEW | [`draft-observability-filters.md`](draft-observability-filters.md) §5, [`draft-workorder-tests.md`](draft-workorder-tests.md) |
| 31 | `StringToWorkOrderStatusConverterTest.java` | `com.gpc.oms.config` | `src/test/java/com/gpc/oms/config/StringToWorkOrderStatusConverterTest.java` | NEW | [`draft-shared-components.md`](draft-shared-components.md) §6, [`draft-workorder-tests.md`](draft-workorder-tests.md) |
| 32 | `RoleConstantsTest.java` | `com.gpc.oms.config` | `src/test/java/com/gpc/oms/config/RoleConstantsTest.java` | NEW | [`draft-security-config.md`](draft-security-config.md) |
| 33 | `RateLimitPropertiesTest.java` | `com.gpc.oms.config` | `src/test/java/com/gpc/oms/config/RateLimitPropertiesTest.java` | NEW | [`draft-observability-filters.md`](draft-observability-filters.md) |
| 34 | `GlobalExceptionHandlerUnitTest.java` | `com.gpc.oms.controller` | `src/test/java/com/gpc/oms/controller/GlobalExceptionHandlerUnitTest.java` | NEW | [`draft-workorder-tests.md`](draft-workorder-tests.md) §1 (#10) |
| 35 | `WorkOrderControllerTest.java` | `com.gpc.oms.controller` | `src/test/java/com/gpc/oms/controller/WorkOrderControllerTest.java` | NEW | [`draft-workorder-tests.md`](draft-workorder-tests.md) §Sketch 2 |
| 36 | `PriorityTest.java` | `com.gpc.oms.domain` | `src/test/java/com/gpc/oms/domain/PriorityTest.java` | NEW | [`draft-workorder-tests.md`](draft-workorder-tests.md) §1 (#12) |
| 37 | `WorkOrderStatusTest.java` | `com.gpc.oms.domain` | `src/test/java/com/gpc/oms/domain/WorkOrderStatusTest.java` | NEW | [`draft-workorder-tests.md`](draft-workorder-tests.md) §1 (#13) |
| 38 | `WorkOrderTest.java` | `com.gpc.oms.domain` | `src/test/java/com/gpc/oms/domain/WorkOrderTest.java` | NEW | [`draft-workorder-tests.md`](draft-workorder-tests.md) §Sketch 1 |
| 39 | `DtoMappingTest.java` | `com.gpc.oms.dto` | `src/test/java/com/gpc/oms/dto/DtoMappingTest.java` | NEW | [`draft-workorder-tests.md`](draft-workorder-tests.md) §1 (#15) |
| 40 | `ProblemTypesTest.java` | `com.gpc.oms.exception` | `src/test/java/com/gpc/oms/exception/ProblemTypesTest.java` | NEW | [`draft-shared-components.md`](draft-shared-components.md) §6 |
| 41 | `ResourceNotFoundExceptionTest.java` | `com.gpc.oms.exception` | `src/test/java/com/gpc/oms/exception/ResourceNotFoundExceptionTest.java` | NEW | [`draft-shared-components.md`](draft-shared-components.md) §6 |
| 42 | `WorkOrderRepositoryTest.java` | `com.gpc.oms.repository` | `src/test/java/com/gpc/oms/repository/WorkOrderRepositoryTest.java` | NEW | [`draft-workorder-tests.md`](draft-workorder-tests.md) §Sketch 3 |
| 43 | `WorkOrderServiceTest.java` | `com.gpc.oms.service` | `src/test/java/com/gpc/oms/service/WorkOrderServiceTest.java` | NEW | [`draft-workorder-tests.md`](draft-workorder-tests.md) §Sketch 2 |
| 44 | `WorkOrderMetricsTest.java` | `com.gpc.oms.service` | `src/test/java/com/gpc/oms/service/WorkOrderMetricsTest.java` | NEW | [`draft-workorder-service.md`](draft-workorder-service.md) |
| 45 | `WorkOrderTestFixtures.java` | `com.gpc.oms.testutil` | `src/test/java/com/gpc/oms/testutil/WorkOrderTestFixtures.java` | NEW | [`draft-shared-components.md`](draft-shared-components.md) §5 |

---

## 3. Package Structure (Complete Visualization Tree)

```
src/main/java/com/gpc/oms/
├── OmsApiApplication.java                   (#1)
├── config/
│   ├── CorrelationIdFilter.java             (#17)
│   ├── JwtRoleConverter.java                (#16)
│   ├── RateLimitingFilter.java              (#18)
│   ├── SecurityConfig.java                  (#15)
│   └── StringToWorkOrderStatusConverter.java (#19)
├── controller/
│   └── WorkOrderController.java             (#11)
├── domain/
│   ├── Priority.java                        (#3)
│   ├── WorkOrder.java                       (#2)
│   ├── WorkOrderRepository.java             (#5)
│   └── WorkOrderStatus.java                 (#4)
├── dto/
│   ├── PagedResponse.java                   (#9)
│   ├── WorkOrderRequest.java                (#6)
│   ├── WorkOrderResponse.java               (#8)
│   └── WorkOrderStatusRequest.java          (#7)
├── exception/
│   ├── GlobalExceptionHandler.java          (#12)
│   ├── ProblemTypes.java                    (#13)
│   └── ResourceNotFoundException.java       (#14)
└── service/
    └── WorkOrderService.java                (#10)

src/test/java/com/gpc/oms/
├── OmsApiApplicationTests.java              (#20)
├── WorkOrderIntegrationTest.java            (#21)
├── config/
│   ├── ActuatorSecurityTest.java            (#22)
│   ├── CorrelationIdFilterTest.java         (#23)
│   ├── H2ConsoleSecurityTest.java           (#24)
│   ├── JwtRoleConverterTest.java            (#25)
│   ├── OAuth2JwtSecurityIntegrationTest.java (#26)
│   ├── RateLimitingFilterTest.java          (#27)
│   └── StringToWorkOrderStatusConverterTest.java (#28)
├── controller/
│   ├── GlobalExceptionHandlerUnitTest.java  (#29)
│   └── WorkOrderControllerTest.java         (#30)
├── domain/
│   ├── PriorityTest.java                    (#31)
│   ├── WorkOrderStatusTest.java             (#32)
│   └── WorkOrderTest.java                   (#33)
├── dto/
│   └── DtoMappingTest.java                  (#34)
├── exception/
│   ├── ProblemTypesTest.java                (#35)
│   └── ResourceNotFoundExceptionTest.java   (#36)
├── repository/
│   └── WorkOrderRepositoryTest.java         (#37)
├── service/
│   └── WorkOrderServiceTest.java            (#38)
└── testutil/
    └── WorkOrderTestFixtures.java           (#39)
```

---

## 4. Thứ Tự Implement Chuẩn Hóa (Dependencies First)

1. **Shared Foundation & Constants** → `ProblemTypes.java` (không dependency).
2. **Enums & Converters** → `Priority.java`, `WorkOrderStatus.java`, `StringToWorkOrderStatusConverter.java`.
3. **Domain Entity & Exceptions** → `WorkOrder.java`, `ResourceNotFoundException.java`.
4. **Repository Layer** → `WorkOrderRepository.java` (phụ thuộc Domain Entity).
5. **DTOs (Java 17 Records)** → `WorkOrderRequest.java`, `WorkOrderStatusRequest.java`, `WorkOrderResponse.java`, `PagedResponse.java`.
6. **Security & Infrastructure Filters** → `JwtRoleConverter.java`, `SecurityConfig.java`, `CorrelationIdFilter.java`, `RateLimitingFilter.java`.
7. **Service Layer** → `WorkOrderService.java` (phụ thuộc Repository + DTOs).
8. **Controller & Global Exception Handler** → `WorkOrderController.java`, `GlobalExceptionHandler.java`.
9. **Test Fixtures & Suite** → `WorkOrderTestFixtures.java` và toàn bộ 19 test classes còn lại trong kim tự tháp kiểm thử.
