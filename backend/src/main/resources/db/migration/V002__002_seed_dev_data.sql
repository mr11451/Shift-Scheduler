-- Apply optional schema additions and seed development/production defaults.

ALTER TABLE staffs
    ADD COLUMN IF NOT EXISTS ng_shift_time_bands VARCHAR(1000);

ALTER TABLE staffs
    ADD COLUMN IF NOT EXISTS preferred_shift_time_bands VARCHAR(1000);

ALTER TABLE shift_requests
    ALTER COLUMN desired_shift_type_id DROP NOT NULL;

CREATE TABLE IF NOT EXISTS password_reset_tokens (
    id BIGSERIAL PRIMARY KEY,
    staff_id BIGINT NOT NULL REFERENCES staffs(id) ON DELETE CASCADE,
    token_hash VARCHAR(255) NOT NULL,
    verification_code_hash VARCHAR(255) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    used_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_password_reset_tokens_staff_id ON password_reset_tokens (staff_id);
CREATE INDEX IF NOT EXISTS idx_password_reset_tokens_token_hash ON password_reset_tokens (token_hash);
CREATE INDEX IF NOT EXISTS idx_password_reset_tokens_expires_at ON password_reset_tokens (expires_at);

ALTER TABLE staffs
    ADD COLUMN IF NOT EXISTS password_changed_at TIMESTAMP;

INSERT INTO groups (group_code, group_name, is_active)
VALUES
    ('GRP-A', 'Group A', TRUE),
    ('GRP-B', 'Group B', TRUE)
ON CONFLICT (group_code) DO NOTHING;

INSERT INTO staffs (
    staff_code, staff_name, email, phone, responsibility, role_level,
    group_id, is_active, created_at, updated_at
)
SELECT 'STF-00001', '佐藤次郎', 'satou@example.com', '090-0001-0001', 'Manager', 'MASTER',
       (SELECT id FROM groups WHERE group_code = 'GRP-A'), TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM staffs WHERE staff_code = 'STF-00001');

INSERT INTO staffs (
    staff_code, staff_name, email, phone, responsibility, role_level,
    group_id, is_active, created_at, updated_at
)
SELECT 'STF-00002', '田中太郎', 'tanaka@example.com', '090-0002-0002', 'Staff', 'MEMBER',
       (SELECT id FROM groups WHERE group_code = 'GRP-A'), TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM staffs WHERE staff_code = 'STF-00002');

INSERT INTO staffs (
    staff_code, staff_name, email, phone, responsibility, role_level,
    group_id, is_active, created_at, updated_at
)
SELECT 'STF-00003', '鈴木花子', 'suzuki@example.com', '090-0003-0003', 'Staff', 'MEMBER',
       (SELECT id FROM groups WHERE group_code = 'GRP-B'), TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM staffs WHERE staff_code = 'STF-00003');

INSERT INTO staffs (
    staff_code, staff_name, email, phone, responsibility, role_level,
    group_id, is_active, created_at, updated_at
)
SELECT 'STF-00004', '伊藤次郎', 'itou@example.com', '090-0004-0004', 'Coordinator', 'CHIEF',
       (SELECT id FROM groups WHERE group_code = 'GRP-A'), TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM staffs WHERE staff_code = 'STF-00004');

INSERT INTO shift_types (
    shift_code, shift_name, start_time, end_time, is_off_type, is_active, sort_order, created_at, updated_at
)
VALUES
    ('SFT-001', '早番', '16:00', '00:00', FALSE, TRUE, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('SFT-002', '遅番', '00:00', '08:00', FALSE, TRUE, 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('SFT-003', '夜勤', '08:00', '16:00', FALSE, TRUE, 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (shift_code) DO NOTHING;

INSERT INTO qualifications (qualification_name, is_active, created_at, updated_at)
VALUES
    ('Level 1', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Level 2', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (qualification_name) DO NOTHING;

INSERT INTO system_settings (setting_key, setting_value_boolean, updated_at)
VALUES
    ('calendarViewPermissionEnabled', FALSE, CURRENT_TIMESTAMP),
    ('memberLoginNotificationEnabled', FALSE, CURRENT_TIMESTAMP)
ON CONFLICT (setting_key) DO NOTHING;

INSERT INTO system_settings (setting_key, setting_value_text, updated_at)
VALUES
    ('memberLoginNotificationBaseUrl', 'https://example.com/first-login', CURRENT_TIMESTAMP),
    ('roleLabels', '{"MASTER":"マスター","CHIEF":"チーフ","MEMBER":"メンバー"}', CURRENT_TIMESTAMP)
ON CONFLICT (setting_key) DO NOTHING;
