-- Seed minimal production-safe defaults only.

INSERT INTO groups (group_code, group_name, is_active)
VALUES
    ('DEFAULT', 'Default Group', TRUE)
ON CONFLICT (group_code) DO NOTHING;

INSERT INTO system_settings (setting_key, setting_value_boolean, updated_at)
VALUES
    ('member_calendar_share_enabled', FALSE, CURRENT_TIMESTAMP),
    ('member_initial_login_mail_enabled', FALSE, CURRENT_TIMESTAMP)
ON CONFLICT (setting_key) DO NOTHING;

INSERT INTO system_settings (setting_key, setting_value_text, updated_at)
VALUES
    ('member_initial_login_access_base_url', 'https://example.com/first-login', CURRENT_TIMESTAMP)
ON CONFLICT (setting_key) DO NOTHING;
