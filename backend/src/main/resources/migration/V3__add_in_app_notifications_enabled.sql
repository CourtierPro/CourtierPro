-- V3__add_in_app_notifications_enabled.sql
-- Adds in_app_notifications_enabled column to user_accounts table

ALTER TABLE user_accounts
ADD COLUMN IF NOT EXISTS in_app_notifications_enabled BOOLEAN NOT NULL DEFAULT true;
