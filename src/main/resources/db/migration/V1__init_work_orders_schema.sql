-- Flyway Migration: V1__init_work_orders_schema.sql
-- Description: Khởi tạo bảng work_orders, các ràng buộc và chỉ mục cơ bản
-- Compatibility: PostgreSQL 15+, H2 Database 2.x (PostgreSQL Mode)

CREATE TABLE IF NOT EXISTS work_orders (
    id UUID NOT NULL,
    equipment_id VARCHAR(50) NOT NULL,
    description VARCHAR(500) NOT NULL,
    priority VARCHAR(20) NOT NULL,
    status VARCHAR(20) DEFAULT 'OPEN' NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    resolved_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT pk_work_orders PRIMARY KEY (id),
    CONSTRAINT chk_work_orders_priority CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    CONSTRAINT chk_work_orders_status CHECK (status IN ('OPEN', 'IN_PROGRESS', 'DONE'))
);

-- Chỉ mục hỗ trợ truy vấn lọc theo trạng thái và sắp xếp thời gian tạo
CREATE INDEX IF NOT EXISTS idx_work_orders_status_created_at 
    ON work_orders (status, created_at DESC);

-- Chỉ mục hỗ trợ tra cứu lịch sử sự cố theo mã thiết bị
CREATE INDEX IF NOT EXISTS idx_work_orders_equipment_id 
    ON work_orders (equipment_id);
