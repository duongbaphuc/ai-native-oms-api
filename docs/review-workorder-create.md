# Review: draft-workorder-create (ordered)

1. [Spec delta] Thiếu PATCH status + field `description`/`resolved_at` trong BR — draft chỉ cover POST tối thiểu.
2. [Spec delta] `status` String vs BR Enum — chốt một kiểu trước khi code entity.
3. [Security] Thiếu `@ControllerAdvice` RFC 7807 — lỗi validation hiện lộ default Spring, cần problem+json.
4. [Security] `equipmentId().hashCode()` log vẫn lộ tương quan — thay bằng correlation id hoặc[last4].
5. [Security] RBAC có ở draft nhưng app chưa có SecurityConfig — endpoint thật đang public.
6. [Testing] Chưa có test `@WebMvcTest` 201 + 400 RFC 7807 + 403 khi thiếu role.
7. [Testing] Catch generic chưa định nghĩa — cần test enum `priority` invalid bị 400.
8. [Complexity] `WorkOrderResponse.from` đặt ở DTO đúng lớp, không để logic map trong controller.
9. [Style] Logger tên `log` đúng SLF4J chuẩn, giữ.

Fixed sketch: thêm `@ControllerAdvice` + `SecurityFilterChain` + entity enforce one-way `Open->InProgress->Done`, test theo mục 6-7 rồi mới merge.
