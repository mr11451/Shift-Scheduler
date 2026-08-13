# Database Migrations

This folder contains Flyway migrations for the Shift Scheduler database.

## Conventions

- Files are named in Flyway format: `V<version>__<description>.sql`
- Migrations are applied in version order
- Schema changes should be idempotent where practical for local/dev reruns
- Seed data is intentionally limited to development/test usage

## Current migration order

1. `V001__001_initialize_schema.sql` - creates the core schema and indexes
2. `V002__002_seed_dev_data.sql` - seeds development and test data
3. `V003__003_seed_production_defaults.sql` - seeds minimal production-safe defaults
4. `V004__004_add_staff_ng_shift_time_bands.sql` - adds optional NG shift time bands per staff
5. `V005__005_add_staff_preferred_shift_time_bands.sql` - adds optional preferred shift time bands per staff
6. `V006__006_allow_null_desired_shift_type.sql` - allows vacation requests without a shift type
7. `V007__007_add_role_labels_system_setting.sql` - adds the role label system setting
8. `V008__008_add_password_reset_tokens.sql` - adds one-time password reset tokens
9. `V009__009_add_password_changed_at.sql` - records password changes to invalidate older JWTs
