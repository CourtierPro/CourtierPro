-- V2__add_email_notifications_enabled.sql
-- Adds email_notifications_enabled column to user_accounts table

ALTER TABLE user_accounts
ADD COLUMN IF NOT EXISTS email_notifications_enabled BOOLEAN NOT NULL DEFAULT true;
