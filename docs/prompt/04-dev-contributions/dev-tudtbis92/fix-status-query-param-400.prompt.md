# Prompt: Fix Invalid Status Query Parameter Mapping (Issue #21 / Finding F-01)
> **Tác giả / Contributor:** `tudtbis92`  
> **Pull Request:** [#24](https://github.com/duongbaphuc/ai-native-oms-api/pull/24)  
> **Tài liệu đối chiếu:** `docs/api-spec.md §2`, `docs/domain-model.md §Invariants`  

```markdown
# ROLE:
Bạn là một "Senior Spring Boot Engineer" chuyên về RESTful API Contracts, Spring Web Data Binding và chuẩn lỗi RFC 7807 Problem Details.

---

# TASK:
Khắc phục lỗi Finding F-01 được phát hiện trong đợt kiểm tra `docs/archive/audit-logs/review-code-vs-spec-WO-REVIEW-01.md`:
1. Hiện thực hóa bộ chuyển đổi `StringToWorkOrderStatusConverter` tại `src/main/java/com/gpc/oms/config/` để hỗ trợ binding giá trị enum cả dạng UPPER_SNAKE (`OPEN`) lẫn case-insensitive.
2. Ném ra `IllegalArgumentException` nếu giá trị query parameter không hợp lệ (ví dụ: `?status=URGENT`).
3. Bổ sung `@ExceptionHandler(MethodArgumentTypeMismatchException.class)` vào `GlobalExceptionHandler` để chuyển đổi lỗi binding tham số URL thành HTTP 400 Bad Request với định dạng RFC 7807 `urn:problem-type:validation-error` thay vì rơi vào fallback 500 Internal Server Error.
4. Bổ sung bộ kiểm thử:
   - Unit test: `StringToWorkOrderStatusConverterTest.java`
   - Controller slice test: `WorkOrderControllerTest.list_invalidStatusQueryParam_returns400`

---

# CONSTRAINTS:
1. **Chuẩn lỗi RFC 7807:** Response trả về status 400, type `urn:problem-type:validation-error`, invalidParams chứa `name: status`.
2. **Không sửa đổi logic nghiệp vụ cốt lõi:** Chỉ can thiệp tầng chuyển đổi tham số và bắt ngoại lệ.
3. **Green Tests:** Toàn bộ test suite phải pass 100%.

---

# DONE WHEN:
1. Gọi `GET /api/v1/workorders?status=URGENT` trả về HTTP 400 với `invalidParams[0].name = "status"`.
2. `mvn test -Dtest='StringToWorkOrderStatusConverterTest,WorkOrderControllerTest'` PASS 100%.
```
