// Nguồn gốc AI: sinh từ docs/00-internal-coding-standards.md §3, docs/02-api-spec.md §2
package com.gpc.oms.dto;

import org.springframework.data.domain.Page;
import java.util.List;
import java.util.Objects;

/**
 * Record phong bì chuẩn hóa kết quả phân trang (Generic Pagination Envelope) cho các phản hồi REST API.
 *
 * <p>Đóng gói danh sách phần tử cùng siêu dữ liệu phân trang (số trang, kích thước, tổng bản ghi).</p>
 *
 * @param <T> Kiểu dữ liệu phần tử trong danh sách nội dung
 * @param content Danh sách dữ liệu của trang hiện tại
 * @param pageNumber Chỉ số trang hiện tại (bắt đầu từ 0)
 * @param pageSize Số lượng bản ghi tối đa trên một trang
 * @param totalElements Tổng số lượng bản ghi trên toàn bộ hệ thống
 * @param totalPages Tổng số trang
 * @param isFirst Cờ đánh dấu có phải trang đầu tiên hay không
 * @param isLast Cờ đánh dấu có phải trang cuối cùng hay không
 * @author Đội ngũ Kiến trúc GPC OMS
 * @version 1.0.0
 * @since 1.0.0
 */
public record PagedResponse<T>(
    List<T> content,
    int pageNumber,
    int pageSize,
    long totalElements,
    int totalPages,
    boolean isFirst,
    boolean isLast
) {
    /**
     * Phương thức nhà máy (Factory method) chuyển đổi {@link Page} sang {@link PagedResponse}.
     *
     * @param <T> Kiểu dữ liệu của phần tử
     * @param page Đối tượng trang của Spring Data
     * @return Đối tượng {@link PagedResponse} chuẩn hóa
     */
    public static <T> PagedResponse<T> from(final Page<T> page) {
        Objects.requireNonNull(page, "page must not be null");

        return new PagedResponse<>(
            page.getContent(),
            page.getNumber(),
            page.getSize(),
            page.getTotalElements(),
            page.getTotalPages(),
            page.isFirst(),
            page.isLast()
        );
    }
}
