INSERT INTO system_settings (setting_key, setting_value_text, updated_at)
VALUES ('closingDay', '31', CURRENT_TIMESTAMP)
ON CONFLICT (setting_key) DO NOTHING;