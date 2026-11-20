-- Run once on an existing database (school server) before deploying blocking and deleting.
BEGIN;
ALTER TABLE users ADD COLUMN blocked boolean NOT NULL DEFAULT false;
ALTER TABLE services ADD COLUMN deleted boolean NOT NULL DEFAULT false;
COMMIT;
