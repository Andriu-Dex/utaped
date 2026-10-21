-- Memberships granted by a scope coordinator (not by administration) never qualify for contextual password recovery.
ALTER TABLE scope_member ADD COLUMN delegated BOOLEAN NOT NULL DEFAULT FALSE;
