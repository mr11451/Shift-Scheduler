ALTER TABLE shift_types
    ADD COLUMN IF NOT EXISTS created_by_staff_id BIGINT REFERENCES staffs(id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_shift_types_created_by_staff_id ON shift_types (created_by_staff_id);
