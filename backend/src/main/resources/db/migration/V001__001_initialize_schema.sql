-- Initialize the core Shift Scheduler schema.

CREATE TABLE IF NOT EXISTS groups (
    id BIGSERIAL PRIMARY KEY,
    group_code VARCHAR(50) UNIQUE NOT NULL,
    group_name VARCHAR(100) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS staffs (
    id BIGSERIAL PRIMARY KEY,
    staff_code VARCHAR(50) UNIQUE NOT NULL,
    staff_name VARCHAR(100) NOT NULL,
    email VARCHAR(255),
    phone VARCHAR(20),
    responsibility VARCHAR(100) NOT NULL,
    role_level VARCHAR(20) NOT NULL CHECK (role_level IN ('MEMBER', 'CHIEF', 'MASTER')),
    group_id BIGINT REFERENCES groups(id) ON DELETE SET NULL,
    password_hash VARCHAR(255),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS qualifications (
    id BIGSERIAL PRIMARY KEY,
    qualification_name VARCHAR(100) UNIQUE NOT NULL,
    description TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS staff_qualifications (
    staff_id BIGINT NOT NULL REFERENCES staffs(id) ON DELETE CASCADE,
    qualification_id BIGINT NOT NULL REFERENCES qualifications(id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (staff_id, qualification_id)
);

CREATE TABLE IF NOT EXISTS shift_types (
    id BIGSERIAL PRIMARY KEY,
    shift_code VARCHAR(20) UNIQUE NOT NULL,
    shift_name VARCHAR(100) NOT NULL,
    start_time TIME,
    end_time TIME,
    is_off_type BOOLEAN NOT NULL DEFAULT FALSE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    sort_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS shift_assignments (
    id BIGSERIAL PRIMARY KEY,
    staff_id BIGINT NOT NULL REFERENCES staffs(id) ON DELETE CASCADE,
    work_date DATE NOT NULL,
    shift_type_id BIGINT NOT NULL REFERENCES shift_types(id) ON DELETE RESTRICT,
    note VARCHAR(255),
    updated_by_staff_id BIGINT NOT NULL REFERENCES staffs(id) ON DELETE RESTRICT,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(staff_id, work_date)
);

CREATE TABLE IF NOT EXISTS shift_requests (
    id BIGSERIAL PRIMARY KEY,
    staff_id BIGINT NOT NULL REFERENCES staffs(id) ON DELETE CASCADE,
    work_date DATE NOT NULL,
    desired_shift_type_id BIGINT REFERENCES shift_types(id) ON DELETE RESTRICT,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT' CHECK (status IN ('DRAFT', 'SUBMITTED', 'APPLIED', 'REJECTED')),
    submitted_at TIMESTAMP,
    decided_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(staff_id, work_date)
);

CREATE TABLE IF NOT EXISTS calendar_view_permissions (
    id BIGSERIAL PRIMARY KEY,
    requester_staff_id BIGINT NOT NULL REFERENCES staffs(id) ON DELETE CASCADE,
    target_staff_id BIGINT NOT NULL REFERENCES staffs(id) ON DELETE CASCADE,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'CANCELED', 'EXPIRED')),
    requested_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    responded_at TIMESTAMP,
    expired_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CHECK (requester_staff_id != target_staff_id)
);

CREATE TABLE IF NOT EXISTS system_settings (
    setting_key VARCHAR(100) PRIMARY KEY,
    setting_value_boolean BOOLEAN,
    setting_value_text TEXT,
    updated_by_staff_id BIGINT REFERENCES staffs(id) ON DELETE SET NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS member_login_provisionings (
    id BIGSERIAL PRIMARY KEY,
    staff_id BIGINT NOT NULL UNIQUE REFERENCES staffs(id) ON DELETE CASCADE,
    login_code VARCHAR(64) NOT NULL,
    initial_password_hash VARCHAR(255) NOT NULL,
    access_url TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ISSUED' CHECK (status IN ('ISSUED', 'SENT', 'FAILED', 'EXPIRED')),
    issued_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    sent_at TIMESTAMP,
    last_error_message TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS shifts (
    id SERIAL PRIMARY KEY,
    employee_name VARCHAR(100) NOT NULL,
    shift_date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    role VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_groups_group_code ON groups (group_code);
CREATE INDEX IF NOT EXISTS idx_groups_is_active ON groups (is_active);
CREATE UNIQUE INDEX IF NOT EXISTS idx_staffs_staff_code ON staffs (staff_code);
CREATE INDEX IF NOT EXISTS idx_staffs_email ON staffs (email);
CREATE INDEX IF NOT EXISTS idx_staffs_group_id ON staffs (group_id);
CREATE INDEX IF NOT EXISTS idx_staffs_role_level ON staffs (role_level);
CREATE INDEX IF NOT EXISTS idx_staffs_is_active ON staffs (is_active);
CREATE INDEX IF NOT EXISTS idx_staffs_staff_code_active ON staffs (staff_code, is_active);
CREATE INDEX IF NOT EXISTS idx_qualifications_is_active ON qualifications (is_active);
CREATE INDEX IF NOT EXISTS idx_staff_qualifications_qualification_id ON staff_qualifications (qualification_id);
CREATE INDEX IF NOT EXISTS idx_shift_types_is_active ON shift_types (is_active);
CREATE INDEX IF NOT EXISTS idx_shift_types_sort_order ON shift_types (sort_order);
CREATE INDEX IF NOT EXISTS idx_shift_assignments_staff_id ON shift_assignments (staff_id);
CREATE INDEX IF NOT EXISTS idx_shift_assignments_work_date ON shift_assignments (work_date);
CREATE INDEX IF NOT EXISTS idx_shift_assignments_shift_type_id ON shift_assignments (shift_type_id);
CREATE INDEX IF NOT EXISTS idx_shift_requests_staff_id ON shift_requests (staff_id);
CREATE INDEX IF NOT EXISTS idx_shift_requests_work_date ON shift_requests (work_date);
CREATE INDEX IF NOT EXISTS idx_shift_requests_status ON shift_requests (status);
CREATE INDEX IF NOT EXISTS idx_calendar_view_permissions_requester ON calendar_view_permissions (requester_staff_id);
CREATE INDEX IF NOT EXISTS idx_calendar_view_permissions_target ON calendar_view_permissions (target_staff_id);
CREATE INDEX IF NOT EXISTS idx_calendar_view_permissions_status ON calendar_view_permissions (status);
CREATE INDEX IF NOT EXISTS idx_member_login_provisionings_staff_id ON member_login_provisionings (staff_id);
CREATE INDEX IF NOT EXISTS idx_member_login_provisionings_login_code ON member_login_provisionings (login_code);
CREATE INDEX IF NOT EXISTS idx_member_login_provisionings_status ON member_login_provisionings (status);
CREATE INDEX IF NOT EXISTS idx_member_login_provisionings_expires_at ON member_login_provisionings (expires_at);
CREATE INDEX IF NOT EXISTS idx_shifts_shift_date ON shifts (shift_date);
