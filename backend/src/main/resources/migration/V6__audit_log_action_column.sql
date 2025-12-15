-- V6: Add action column to audit log and rename deleted_at to timestamp
-- This allows tracking both DELETE and RESTORE actions

-- Add action column (default to DELETE for existing records)
ALTER TABLE admin_deletion_audit_logs 
    ADD COLUMN action VARCHAR(20) DEFAULT 'DELETE' NOT NULL;

-- Rename deleted_at to timestamp for more generic usage
ALTER TABLE admin_deletion_audit_logs 
    RENAME COLUMN deleted_at TO timestamp;

-- Create index on action for filtering
CREATE INDEX idx_audit_logs_action ON admin_deletion_audit_logs(action);
