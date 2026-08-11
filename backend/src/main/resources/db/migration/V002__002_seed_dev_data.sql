-- Seed development and test data.

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
    ('SFT-001', '早番', '17:00', '01:00', FALSE, TRUE, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('SFT-002', '遅番', '01:00', '09:00', FALSE, TRUE, 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('SFT-003', '夜勤', '09:00', '17:00', FALSE, TRUE, 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (shift_code) DO NOTHING;

INSERT INTO qualifications (qualification_name, is_active, created_at, updated_at)
VALUES
    ('Level 1', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Level 2', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (qualification_name) DO NOTHING;

INSERT INTO system_settings (setting_key, setting_value_boolean, updated_at)
VALUES
    ('member_calendar_share_enabled', FALSE, CURRENT_TIMESTAMP),
    ('member_initial_login_mail_enabled', FALSE, CURRENT_TIMESTAMP)
ON CONFLICT (setting_key) DO NOTHING;

INSERT INTO system_settings (setting_key, setting_value_text, updated_at)
VALUES
    ('member_initial_login_access_base_url', 'https://example.com/first-login', CURRENT_TIMESTAMP)
ON CONFLICT (setting_key) DO NOTHING;
