# Database Migrations

This folder contains Flyway migrations for the Shift Scheduler database.

## Conventions

- Files are named in Flyway format: `V<version>__<description>.sql`
- Migrations are applied in version order
- Schema changes should be idempotent where practical for local/dev reruns
- Seed data is intentionally limited to development/test usage

## Current migration order

1. `V001__001_initialize_schema.sql` - creates the core schema and indexes
2. `V002__002_seed_dev_data.sql` - applies optional schema additions and seeds all canonical development/production defaults
3. `V003__003_shift_types_created_by.sql` - adds `created_by_staff_id` to `shift_types` so CHIEF-created shift types can be attributed to their creator
4. `V004__004_add_closing_day_setting.sql` - seeds the month-end (`31`) default closing day used to define shift periods

The migration set is intentionally squashed into two files. Existing databases created with the previous V003-V010 history must be recreated, or their Flyway history must be reset, before applying this layout.
