-- ============================================================================
-- V5__add_soft_delete_and_audit_log.sql
--
-- Adds soft-delete columns to core business tables and creates
-- admin deletion audit log table.
-- ============================================================================

-- =============================================================================
-- SOFT DELETE COLUMNS
-- =============================================================================

-- Transactions
ALTER TABLE transactions
ADD COLUMN deleted_at TIMESTAMP,
ADD COLUMN deleted_by UUID;

CREATE INDEX IF NOT EXISTS idx_transactions_deleted_at ON transactions(deleted_at);

-- Timeline Entries
ALTER TABLE timeline_entries
ADD COLUMN deleted_at TIMESTAMP,
ADD COLUMN deleted_by UUID;

CREATE INDEX IF NOT EXISTS idx_timeline_entries_deleted_at ON timeline_entries(deleted_at);

-- Document Requests
ALTER TABLE document_requests
ADD COLUMN deleted_at TIMESTAMP,
ADD COLUMN deleted_by UUID;

CREATE INDEX IF NOT EXISTS idx_document_requests_deleted_at ON document_requests(deleted_at);

-- Submitted Documents
ALTER TABLE submitted_documents
ADD COLUMN deleted_at TIMESTAMP,
ADD COLUMN deleted_by UUID;

CREATE INDEX IF NOT EXISTS idx_submitted_documents_deleted_at ON submitted_documents(deleted_at);

-- =============================================================================
-- ADMIN DELETION AUDIT LOG
-- =============================================================================
CREATE TABLE IF NOT EXISTS admin_deletion_audit_logs (
    id BIGSERIAL PRIMARY KEY,
    deleted_at TIMESTAMP NOT NULL,
    admin_id UUID NOT NULL,
    resource_type VARCHAR(50) NOT NULL,
    resource_id UUID NOT NULL,
    resource_snapshot JSONB,
    cascaded_deletions JSONB
);

CREATE INDEX IF NOT EXISTS idx_admin_deletion_audit_deleted_at ON admin_deletion_audit_logs(deleted_at DESC);
CREATE INDEX IF NOT EXISTS idx_admin_deletion_audit_admin_id ON admin_deletion_audit_logs(admin_id);
CREATE INDEX IF NOT EXISTS idx_admin_deletion_audit_resource_type ON admin_deletion_audit_logs(resource_type);
