// Nguồn gốc AI: sinh từ docs/01-domain-model.md, docs/02-database-migration-spec.md §3
package com.gpc.oms.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

/**
 * Giao diện lưu trữ dữ liệu Spring Data JPA cho thực thể phiếu công tác {@link WorkOrder}.
 *
 * <p>Cung cấp các thao tác CRUD tiêu chuẩn cùng phương thức tra cứu danh sách có phân trang.</p>
 *
 * @author Đội ngũ Kiến trúc GPC OMS
 * @version 1.0.0
 * @since 1.0.0
 */
public interface WorkOrderRepository extends JpaRepository<WorkOrder, UUID> {

    /**
     * Tra cứu danh sách các phiếu công tác có trạng thái chỉ định kèm phân trang.
     *
     * @param status Trạng thái cần lọc (OPEN, IN_PROGRESS, DONE)
     * @param pageable Thông tin phân trang và sắp xếp
     * @return Trang dữ liệu {@link Page} chứa danh sách các phiếu công tác phù hợp
     */
    Page<WorkOrder> findByStatus(WorkOrderStatus status, Pageable pageable);
}
