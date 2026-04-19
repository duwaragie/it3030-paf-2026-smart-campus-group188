-- Run this once on any existing Postgres database that was created before
-- TECHNICAL_STAFF was added to the Role enum. The check constraint is generated
-- by Hibernate on first table creation and is not updated by ddl-auto=update,
-- so inserts for TECHNICAL_STAFF users fail with users_role_check until the
-- constraint is rebuilt.
--
-- Fresh databases do not need this script; Hibernate will create the constraint
-- with all four roles on first boot.

ALTER TABLE users DROP CONSTRAINT IF EXISTS users_role_check;
ALTER TABLE users ADD CONSTRAINT users_role_check
  CHECK (role IN ('STUDENT', 'ADMIN', 'LECTURER', 'TECHNICAL_STAFF'));
