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
