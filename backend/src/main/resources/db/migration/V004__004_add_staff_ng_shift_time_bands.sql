-- Add optional NG shift time bands for per-staff auto-assignment constraints.

ALTER TABLE staffs
ADD COLUMN IF NOT EXISTS ng_shift_time_bands VARCHAR(1000);
