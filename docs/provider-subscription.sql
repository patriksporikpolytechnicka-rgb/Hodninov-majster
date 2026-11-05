-- Run once by the database administrator before deploying this version.
-- Existing profiles receive one 30-day trial from this migration.
BEGIN;
ALTER TABLE provider_profiles ADD COLUMN subscription_start timestamp(6) with time zone;
ALTER TABLE provider_profiles ADD COLUMN subscription_end timestamp(6) with time zone;
ALTER TABLE provider_profiles ADD COLUMN subscription_paid boolean NOT NULL DEFAULT false;
UPDATE provider_profiles SET subscription_start = CURRENT_TIMESTAMP,
    subscription_end = CURRENT_TIMESTAMP + INTERVAL '30 days';
COMMIT;
