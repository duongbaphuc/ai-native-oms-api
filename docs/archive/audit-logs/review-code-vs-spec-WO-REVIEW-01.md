# Báo cáo Review: Generated Code vs Markdown Specs (WO-REVIEW-01)

- **Issue:** #21 — review code đã generated đối chiếu spec, tìm chênh lệch/mâu thuẫn.
- **Phạm vi:** `src/main`, `src/test` trên `main` tại PR #20 (`80a55c1`) so với `docs/*.md` và `docs/drafts/*`.
- **Phương pháp:** đọc từng file code, đối chiếu mục spec tương ứng, kiểm tra diff `e202390...80a55c1` để biết spec nào đã được audit đồng bộ.
- **Kết luận chung:** khung WO-core khớp spec sau audit-remediation (#16). Còn **2 blocker** (F-01 query-status 500, F-02 demo creds plaintext), **4 minor**, 1 quyết định tồn đọng (CSV). Không phát hiện hallucination package/API ảo.

## Bảng phát hiện

| # | Mức | Spec nguồn | Code hiện tại | Đề xuất |
|---|-----|------------|---------------|---------|
| F-01 | Blocker | `02-api-spec.md §2`: `?status=` lọc theo trạng thái; sai giá trị phải 400 | `main` không có `StringToWorkOrderStatusConverter` (chỉ tồn tại ở nhánh JaCoCo). `?status=Open` lẫn `?status=URGENT` đều rơi vào fallback 500 thay vì 400 | Port converter + handler `MethodArgumentTypeMismatchException` → 400 từ PR #19/nhánh JaCoCo vào `main` |
| F-02 | Blocker (prod) | `00-security-rules.md`, `10-devops-pipeline-spec.md §4` (cấm secret trong repo) | `SecurityConfig.userDetailsService()` chứa `admin/admin123`, `dispatcher/dispatcher123`, `technician/technician123` plaintext `{noop}` (PR #20) | Guard bằng `@Profile("dev")` hoặc tách file, chặn merge ra prod |
| F-03 | Minor | `02-api-spec.md §1`: response mẫu ghi `status` luôn là `"OPEN"` | Code serialize `"Open"` qua `@JsonValue`, test assert `"Open"`; drafts cũng dùng `Open/InProgress/Done` | Sửa spec §1 thành `"Open"` cho khớp implementation (không sửa code) |
| F-04 | Minor | `02-api-spec.md §2`: `size` max 100 | Controller chỉ `@PageableDefault(size=20)`, không chặn `size=1000` | Thêm cap `size <= 100` hoặc ghi nhận chấp nhận hành vi hiện tại |
| F-05 | Minor | `02-api-spec.md §4` ma trận lỗi: DISPATCHER gọi PATCH → 403 detail `"Access Denied: Only TECHNICIAN can update status"` | Code cho phép `TECHNICIAN + ADMIN`, message 403 mặc định theo `@PreAuthorize` | Cập nhật spec theo code (ADMIN được phép là đúng theo RBAC matrix) |
| F-06 | Minor | `02-security-auth-spec.md §2-5`: JWT claims, CORS, security headers, rate-limit Bucket4j | Code mới `httpBasic` + entry-point 401; chưa có JWT/CORS/rate-limit | Lane riêng, không chặn release dev; ghi vào roadmap thay vì fix lẻ |
| F-07 | Quyết định | `.planning/REQUIREMENTS.md` CSV-01..PKG-01 | Code CSV lib chỉ ở nhánh `WO-csv-core-tudt`, `main` không có | Team chốt port hay drop, rồi đóng hoặc mở issue theo |

## Kết luận seed trong issue #21

1. `description` min-10: **đã resolve** — code hiện tại có `@Size(min=10, max=500)`, audit đã fix.
2. Error `type` URI: **by-design** — drafts cũ dùng `https://...`, audit đã đồng bộ drafts + code về `urn:problem-type:*`. Không action.
3. PATCH RBAC: **theo code** — `TECHNICIAN + ADMIN` khớp `02-api-spec.md §4` và `01-br-analysis-wo.md` sau audit (draft-patch cũ ghi DISPATCHER là stale). Ghi nhận F-05 cho câu message.
4. CSV: xem F-07.
5. Demo creds: xem F-02.

## Verdict từng nhóm

- **Domain:** PASS. Entity, enums, `advanceStatus` delegate `canTransitionTo`, cấm setter, đúng `01-domain-model.md`.
- **API:** PASS có điều kiện (F-01, F-03, F-04, F-05). 4 endpoints, pagination `PagedResponse`, 201 + `Location`, validation `@Valid`, `ignoreUnknown=false`.
- **Exception/RFC7807:** PASS. 6 handlers + `ResourceNotFoundException`, `urn:problem-type:*` khớp spec sau audit.
- **DB:** PASS có điều kiện. V1 SQL + index đúng spec; thiếu flyway starter + `ddl-auto: update` còn bật (đã ghi ở lane infra, không lặp ở đây).
- **Security:** FAIL tạm cho prod (F-02, F-06). Dev/test OK: RBAC matrix đúng sau audit, 401 RFC7807 có.
- **Observability/DevOps:** spec đã bổ sung Target Files, code chưa có — thuộc lane infra riêng, không phải lệch.
- **Drafts:** PASS. Drafts đã được audit đồng bộ theo code (`urn:`, `ResourceNotFoundException`, pagination). Draft-patch cũ (DISPATCHER) và draft-get cũ (list chỉ DISPATCHER) là stale, spec chính đã thắng.
- **CDC/Kafka (`01-br-analysis-wo.md` Data Layer):** feature thiếu duy nhất ở tầng nghiệp vụ — 0 dòng code. Đề xuất issue/lane riêng, không gộp vào review này.

## Đề xuất PR này

Chỉ thêm file báo cáo này, không sửa code. Đóng #21 khi merge. Các fix F-01/F-02 mở issue hoặc branch riêng theo bảng trên.
