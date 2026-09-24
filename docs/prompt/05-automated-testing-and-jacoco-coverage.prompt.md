# Prompt Giai Đoạn 5: Xây Dựng Hệ Thống Kiểm Thử & Kiểm Soát Coverage (100% JaCoCo)

```markdown
# ROLE:
Bạn là một "Principal QA Automation Engineer" và "Spring Boot Testing Specialist" với hơn 10 năm kinh nghiệm về Test-Driven Development (TDD), Testing Pyramid, và JaCoCo Coverage Enforcement. Mục tiêu tối thượng là xây dựng hệ thống kiểm thử tự động toàn diện bao phủ 100% các nhánh rẽ logic (100% Branch & Line Coverage).

---

# TASK:
1. **Cấu hình JaCoCo Plugin trong `pom.xml`:**
   - Thiết lập Quality Gate chặn build nếu `LINE` hoặc `BRANCH` coverage trên các package nghiệp vụ (`domain`, `service`, `controller`, `dto`, `exception`) không đạt tối thiểu `1.00` (100%).
2. **Xây dựng Tầng Unit Tests:**
   - `WorkOrderStatusTest.java`: Thẩm định toàn bộ 9 nhánh rẽ chuyển trạng thái của `canTransitionTo()` ($3 \times 3$).
   - `PriorityTest.java`: Thẩm định `values()` và `valueOf()`.
   - `WorkOrderTest.java`: Thẩm định constructor, getters, default values, linear transition, và ném `IllegalStateException` cho mọi vi phạm (skip, rollback, self).
   - `DtoMappingTest.java`: Thẩm định `WorkOrderResponse.from()`, `PagedResponse.from()` (trang đầu, trang giữa, trang cuối, rỗng), và Record contracts (`equals`, `hashCode`, `toString`).
   - `ResourceNotFoundExceptionTest.java`: Thẩm định exception message và kế thừa.
   - `WorkOrderServiceTest.java` (`@ExtendWith(MockitoExtension.class)`): Thẩm định 100% các nhánh của 4 methods, đặc biệt phân nhánh `status != null` vs `status == null`, `Optional.empty()`, và `IllegalStateException` re-throw.
3. **Xây dựng Tầng Slice & Exception Tests:**
   - `GlobalExceptionHandlerUnitTest.java`: Thẩm định độc lập 6 handler chuẩn RFC 7807 (Validation 400 có/không có message, Malformed JSON 400, Forbidden 403, Not Found 404, Invalid State 422, Unexpected Error 500).
   - `WorkOrderRepositoryTest.java` (`@DataJpaTest`): Thẩm định UUID PK generation, `findByStatus` phân trang, và DB check constraints (`chk_work_orders_priority`, `chk_work_orders_status`).
4. **Xây dựng Tầng End-to-End Integration Tests:**
   - `WorkOrderIntegrationTest.java` (`@SpringBootTest` + `@AutoConfigureMockMvc`):
     * Kịch bản 1 (Happy Path Vòng đời sự cố): `DISPATCHER` POST 201 → `DISPATCHER` GET list 200 → `TECHNICIAN` GET id 200 → `TECHNICIAN` PATCH InProgress 200 → `TECHNICIAN` PATCH Done 200 (`resolvedAt != null`).
     * Kịch bản 2 (RBAC Boundary): Unauthenticated 401 (`urn:problem-type:unauthorized`) → `DISPATCHER` cố tình PATCH 403 (`urn:problem-type:forbidden`).
     * Kịch bản 3 (Validation & State Invariant): Thiếu trường/độ dài ngắn 400 → JSON lạ/sai enum 400 → ID ngẫu nhiên không có 404 → Nhảy cóc Open sang Done 422.

---

# CONSTRAINTS:
1. **Testing Stack:** Dùng JUnit 5 (Jupiter), AssertJ (`assertThat`), Mockito, và Spring Test Framework. CẤM dùng JUnit 4.
2. **Strict RFC 7807 Assertions:** Mọi assert lỗi phải kiểm tra đủ 4 trường: `status`, `type`, `title`, và `detail`/`invalidParams`.
3. **100% Branch Coverage:** Không sử dụng annotation loại trừ `@Generated` trên code nghiệp vụ. Mọi toán tử `if/else`, `switch`, `? :`, và `Optional.orElseThrow` đều phải có test cho cả 2 nhánh TRUE và FALSE.

---

# DONE WHEN:
1. Toàn bộ các file test được tạo đúng cấu trúc thư mục `src/test/java/com/gpc/oms/`.
2. Chạy lệnh `mvn clean verify` đạt 100% Green: 0 failures, 0 errors, 0 skipped.
3. Báo cáo JaCoCo `target/site/jacoco/jacoco.csv` hiển thị:
   - Line Coverage: 100% trên toàn bộ 11 classes nghiệp vụ.
   - Branch Coverage: 100% trên toàn bộ các phương thức có rẽ nhánh logic.
   - Quality Gate: `All coverage checks have been met. BUILD SUCCESS`.
```
