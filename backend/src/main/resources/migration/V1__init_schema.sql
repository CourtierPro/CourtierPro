-- ============================================================================
-- V1__init_schema.sql
--
-- Schema initialization.
-- Includes:
-- - Core Tables (user_accounts, transactions, documents, timeline)
-- - Audit Tables (login, logout, password_reset, org_settings, admin_deletion)
-- - Notifications
-- - Search Indexes
-- - Pinned Transactions
-- ============================================================================

-- Enable the pg_trgm extension for trigram-based indexing (needed for search)
-- Enable the pg_trgm extension for trigram-based indexing (needed for search)
-- CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- =============================================================================
-- USER ACCOUNTS
-- =============================================================================
CREATE TABLE IF NOT EXISTS user_accounts (
    id UUID PRIMARY KEY,
    auth0user_id VARCHAR(255) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    first_name VARCHAR(255),
    last_name VARCHAR(255),
    role VARCHAR(50) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT true,
    preferred_language VARCHAR(10) NOT NULL DEFAULT 'en',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_user_accounts_auth0_id ON user_accounts(auth0user_id);
CREATE INDEX IF NOT EXISTS idx_user_accounts_email ON user_accounts(email);
CREATE INDEX IF NOT EXISTS idx_user_accounts_role ON user_accounts(role);

-- Search Indexes
-- Search Indexes
-- CREATE INDEX IF NOT EXISTS idx_user_accounts_first_name_trgm 
--     ON user_accounts USING GIN (first_name gin_trgm_ops);
-- CREATE INDEX IF NOT EXISTS idx_user_accounts_last_name_trgm 
--     ON user_accounts USING GIN (last_name gin_trgm_ops);
-- CREATE INDEX IF NOT EXISTS idx_user_accounts_email_trgm 
--     ON user_accounts USING GIN (email gin_trgm_ops);

-- =============================================================================
-- TRANSACTIONS
-- =============================================================================
CREATE TABLE IF NOT EXISTS transactions (
    id BIGSERIAL PRIMARY KEY,
    transaction_id UUID UNIQUE,
    client_id UUID,
    broker_id UUID,
    -- Embedded PropertyAddress
    street VARCHAR(255),
    city VARCHAR(255),
    province VARCHAR(255),
    postal_code VARCHAR(20),
    -- Enums stored as strings
    side VARCHAR(50),
    buyer_stage VARCHAR(50),
    seller_stage VARCHAR(50),
    status VARCHAR(50),
    opened_at TIMESTAMP,
    closed_at TIMESTAMP,
    -- Soft Delete Columns
    deleted_at TIMESTAMP,
    deleted_by UUID
);

CREATE INDEX IF NOT EXISTS idx_transactions_transaction_id ON transactions(transaction_id);
CREATE INDEX IF NOT EXISTS idx_transactions_client_id ON transactions(client_id);
CREATE INDEX IF NOT EXISTS idx_transactions_broker_id ON transactions(broker_id);
CREATE INDEX IF NOT EXISTS idx_transactions_status ON transactions(status);
CREATE INDEX IF NOT EXISTS idx_transactions_deleted_at ON transactions(deleted_at);

-- Search Indexes
-- Search Indexes
-- CREATE INDEX IF NOT EXISTS idx_transactions_street_trgm 
--     ON transactions USING GIN (street gin_trgm_ops);
-- CREATE INDEX IF NOT EXISTS idx_transactions_city_trgm 
--     ON transactions USING GIN (city gin_trgm_ops);

-- =============================================================================
-- TIMELINE ENTRIES
-- =============================================================================
CREATE TABLE IF NOT EXISTS timeline_entries (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    transaction_id UUID NOT NULL,
    timestamp TIMESTAMP NOT NULL,
    actor_id UUID NOT NULL,
    type VARCHAR(50) NOT NULL,
    note TEXT,
    doc_type VARCHAR(100),
    visible_to_client BOOLEAN DEFAULT false,
    client_name VARCHAR(255),
    address VARCHAR(255),
    actor_name VARCHAR(255),
    stage VARCHAR(100),
    previous_stage VARCHAR(100),
    new_stage VARCHAR(100),
    -- Soft Delete Columns
    deleted_at TIMESTAMP,
    deleted_by UUID,
    
    CONSTRAINT fk_transaction FOREIGN KEY (transaction_id) REFERENCES transactions(transaction_id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_timeline_entries_transaction ON timeline_entries(transaction_id);
CREATE INDEX IF NOT EXISTS idx_timeline_entries_deleted_at ON timeline_entries(deleted_at);

-- =============================================================================
-- DOCUMENT REQUESTS
-- =============================================================================
CREATE TABLE IF NOT EXISTS document_requests (
    id BIGSERIAL PRIMARY KEY,
    request_id UUID UNIQUE,
    -- Embedded TransactionRef
    transaction_id UUID,
    client_id UUID,
    side VARCHAR(50),
    -- Document fields
    doc_type VARCHAR(100),
    custom_title VARCHAR(255),
    status VARCHAR(50),
    expected_from VARCHAR(50),
    related_buyer_stage VARCHAR(50),
    related_seller_stage VARCHAR(50),
    broker_notes TEXT,
    last_updated_at TIMESTAMP,
    visible_to_client BOOLEAN DEFAULT true,
    -- Soft Delete Columns
    deleted_at TIMESTAMP,
    deleted_by UUID
);

CREATE INDEX IF NOT EXISTS idx_document_requests_request_id ON document_requests(request_id);
CREATE INDEX IF NOT EXISTS idx_document_requests_transaction ON document_requests(transaction_id);
CREATE INDEX IF NOT EXISTS idx_document_requests_status ON document_requests(status);
CREATE INDEX IF NOT EXISTS idx_document_requests_deleted_at ON document_requests(deleted_at);

-- Search Indexes
-- Search Indexes
-- CREATE INDEX IF NOT EXISTS idx_document_requests_custom_title_trgm 
--     ON document_requests USING GIN (custom_title gin_trgm_ops);
-- CREATE INDEX IF NOT EXISTS idx_document_requests_broker_notes_trgm 
--     ON document_requests USING GIN (broker_notes gin_trgm_ops);

-- =============================================================================
-- SUBMITTED DOCUMENTS
-- =============================================================================
CREATE TABLE IF NOT EXISTS submitted_documents (
    id BIGSERIAL PRIMARY KEY,
    document_id UUID,
    uploaded_at TIMESTAMP,
    document_request_id BIGINT REFERENCES document_requests(id) ON DELETE CASCADE,
    -- Embedded UploadedBy
    uploader_type VARCHAR(50),
    party VARCHAR(50),
    uploader_id UUID,
    external_name VARCHAR(255),
    -- Embedded StorageObject  
    s3key VARCHAR(1000),
    file_name VARCHAR(255),
    mime_type VARCHAR(100),
    size_bytes BIGINT,
    -- Soft Delete Columns
    deleted_at TIMESTAMP,
    deleted_by UUID
);

CREATE INDEX IF NOT EXISTS idx_submitted_documents_request ON submitted_documents(document_request_id);
CREATE INDEX IF NOT EXISTS idx_submitted_documents_document_id ON submitted_documents(document_id);
CREATE INDEX IF NOT EXISTS idx_submitted_documents_deleted_at ON submitted_documents(deleted_at);

-- =============================================================================
-- NOTIFICATIONS
-- =============================================================================
CREATE TABLE notifications (
    id BIGSERIAL PRIMARY KEY,
    public_id VARCHAR(255) NOT NULL UNIQUE,
    recipient_id VARCHAR(255) NOT NULL,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    is_read BOOLEAN DEFAULT FALSE NOT NULL,
    related_transaction_id VARCHAR(255),
    type VARCHAR(50) NOT NULL DEFAULT 'GENERAL',
    created_at TIMESTAMP NOT NULL
);

-- =============================================================================
-- PINNED TRANSACTIONS
-- =============================================================================
CREATE TABLE pinned_transactions (
    id BIGSERIAL PRIMARY KEY,
    broker_id UUID NOT NULL,
    transaction_id UUID NOT NULL,
    pinned_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_pinned_broker_transaction UNIQUE (broker_id, transaction_id),
    CONSTRAINT fk_pinned_broker FOREIGN KEY (broker_id) REFERENCES user_accounts(id) ON DELETE CASCADE,
    CONSTRAINT fk_pinned_transaction FOREIGN KEY (transaction_id) REFERENCES transactions(transaction_id) ON DELETE CASCADE
);

CREATE INDEX idx_pinned_transactions_broker_id ON pinned_transactions(broker_id);

-- =============================================================================
-- ORGANIZATION SETTINGS
-- =============================================================================
CREATE TABLE IF NOT EXISTS organization_settings (
    id UUID PRIMARY KEY,
    default_language VARCHAR(2) NOT NULL,
    invite_subject_en VARCHAR(255) NOT NULL,
    invite_body_en TEXT NOT NULL,
    invite_subject_fr VARCHAR(255) NOT NULL,
    invite_body_fr TEXT NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS organization_settings_audit (
    id UUID PRIMARY KEY,
    timestamp TIMESTAMP,
    admin_user_id VARCHAR(255),
    admin_email VARCHAR(255),
    action VARCHAR(255),
    previous_default_language VARCHAR(255),
    new_default_language VARCHAR(255),
    invite_template_en_changed BOOLEAN DEFAULT false,
    invite_template_fr_changed BOOLEAN DEFAULT false,
    ip_address VARCHAR(45)
);

-- =============================================================================
-- AUDIT EVENTS
-- =============================================================================

-- Broadcast Audit
CREATE TABLE IF NOT EXISTS broadcast_audit (
    id UUID PRIMARY KEY,
    admin_id VARCHAR(255) NOT NULL,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    sent_at TIMESTAMP NOT NULL,
    recipient_count INTEGER NOT NULL
);


-- Login Audit
CREATE TABLE IF NOT EXISTS login_audit_events (
    id UUID PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    timestamp TIMESTAMP NOT NULL,
    ip_address VARCHAR(45),
    user_agent VARCHAR(500)
);

CREATE INDEX IF NOT EXISTS idx_login_audit_user_id ON login_audit_events(user_id);
CREATE INDEX IF NOT EXISTS idx_login_audit_role ON login_audit_events(role);
CREATE INDEX IF NOT EXISTS idx_login_audit_timestamp ON login_audit_events(timestamp);

-- Logout Audit
CREATE TABLE IF NOT EXISTS logout_audit_events (
    id UUID PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    reason VARCHAR(50) NOT NULL,
    timestamp TIMESTAMP NOT NULL,
    ip_address VARCHAR(45),
    user_agent VARCHAR(500),
    CONSTRAINT chk_logout_reason CHECK (reason IN ('MANUAL', 'SESSION_TIMEOUT', 'FORCED'))
);

CREATE INDEX IF NOT EXISTS idx_logout_audit_user_id ON logout_audit_events(user_id);
CREATE INDEX IF NOT EXISTS idx_logout_audit_reason ON logout_audit_events(reason);
CREATE INDEX IF NOT EXISTS idx_logout_audit_timestamp ON logout_audit_events(timestamp);

-- Password Reset Events
CREATE TABLE IF NOT EXISTS password_reset_events (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    event_type VARCHAR(50) NOT NULL CHECK (event_type IN ('REQUESTED', 'COMPLETED')),
    timestamp TIMESTAMP NOT NULL,
    ip_address VARCHAR(100),
    user_agent VARCHAR(500)
);

CREATE INDEX IF NOT EXISTS idx_password_reset_events_user_id ON password_reset_events(user_id);
CREATE INDEX IF NOT EXISTS idx_password_reset_events_email ON password_reset_events(email);
CREATE INDEX IF NOT EXISTS idx_password_reset_events_timestamp ON password_reset_events(timestamp DESC);

-- =============================================================================
-- ADMIN DELETION AUDIT LOG
-- =============================================================================
CREATE TABLE IF NOT EXISTS admin_deletion_audit_logs (
    id BIGSERIAL PRIMARY KEY,
    timestamp TIMESTAMP NOT NULL,
    admin_id UUID NOT NULL,
    resource_type VARCHAR(50) NOT NULL,
    resource_id UUID NOT NULL,
    resource_snapshot JSONB,
    cascaded_deletions JSONB,
    action VARCHAR(20) DEFAULT 'DELETE' NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_admin_deletion_audit_timestamp ON admin_deletion_audit_logs(timestamp DESC);
CREATE INDEX IF NOT EXISTS idx_admin_deletion_audit_admin_id ON admin_deletion_audit_logs(admin_id);
CREATE INDEX IF NOT EXISTS idx_admin_deletion_audit_resource_type ON admin_deletion_audit_logs(resource_type);
CREATE INDEX IF NOT EXISTS idx_admin_deletion_audit_action ON admin_deletion_audit_logs(action);
