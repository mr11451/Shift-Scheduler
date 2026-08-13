-- Add configurable role label texts used by the frontend.

INSERT INTO system_settings (setting_key, setting_value_text, updated_at)
VALUES (
    'roleLabels',
    '{"MASTER":"マスター","CHIEF":"チーフ","MEMBER":"メンバー"}',
    CURRENT_TIMESTAMP
)
ON CONFLICT (setting_key) DO NOTHING;
