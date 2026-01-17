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
    -- Centris Number (for sell-side transactions)
    centris_number VARCHAR(50),
    -- Enums stored as strings
    side VARCHAR(50),
    buyer_stage VARCHAR(50),
    seller_stage VARCHAR(50),
    status VARCHAR(50),
    opened_at TIMESTAMP,
    closed_at TIMESTAMP,
    -- Optimistic locking version column to prevent concurrent updates.
    -- Existing rows created before this column was added will start at 0, which is intentional.
    version BIGINT DEFAULT 0,
    -- Soft Delete Columns
    deleted_at TIMESTAMP,
    deleted_by UUID
);

CREATE INDEX IF NOT EXISTS idx_transactions_transaction_id ON transactions(transaction_id);
CREATE INDEX IF NOT EXISTS idx_transactions_client_id ON transactions(client_id);
CREATE INDEX IF NOT EXISTS idx_transactions_broker_id ON transactions(broker_id);
CREATE INDEX IF NOT EXISTS idx_transactions_status ON transactions(status);
CREATE INDEX IF NOT EXISTS idx_transactions_deleted_at ON transactions(deleted_at);

-- Add archived columns and indexes (moved from V2__add_archived_column.sql)
ALTER TABLE transactions ADD COLUMN archived BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE transactions ADD COLUMN archived_at TIMESTAMP;
ALTER TABLE transactions ADD COLUMN archived_by UUID;

-- Create index for faster filtering on archived status
CREATE INDEX idx_transactions_archived ON transactions(archived);
CREATE INDEX idx_transactions_broker_archived ON transactions(broker_id, archived);

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
    -- Offer-related TransactionInfo fields
    buyer_name VARCHAR(255),
    offer_amount NUMERIC(12, 2),
    offer_status VARCHAR(50),
    previous_offer_status VARCHAR(50),
    -- Condition-related TransactionInfo fields
    condition_type VARCHAR(50),
    condition_custom_title VARCHAR(255),
    condition_description TEXT,
    condition_deadline DATE,
    condition_previous_status VARCHAR(50),
    condition_new_status VARCHAR(50),
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
    stage VARCHAR(64),
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
    title_key VARCHAR(255),
    message_key VARCHAR(255),
    params TEXT,
    is_read BOOLEAN DEFAULT FALSE NOT NULL,
    related_transaction_id VARCHAR(255),
    type VARCHAR(50) NOT NULL DEFAULT 'GENERAL',
    category VARCHAR(50) NOT NULL DEFAULT 'GENERAL',
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
-- TRANSACTION PARTICIPANTS
-- =============================================================================
CREATE TABLE IF NOT EXISTS transaction_participants (
    id UUID PRIMARY KEY,
    transaction_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    email VARCHAR(255),
    phone_number VARCHAR(50),
    CONSTRAINT fk_participant_transaction FOREIGN KEY (transaction_id) REFERENCES transactions(transaction_id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_transaction_participants_transaction_id ON transaction_participants(transaction_id);

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

CREATE INDEX IF NOT EXISTS idx_broadcast_audit_admin_id ON broadcast_audit(admin_id);
CREATE INDEX IF NOT EXISTS idx_broadcast_audit_sent_at ON broadcast_audit(sent_at);

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

-- =============================================================================
-- PROPERTIES (for buyer transactions - tracking multiple properties)
-- =============================================================================
CREATE TABLE IF NOT EXISTS properties (
    id BIGSERIAL PRIMARY KEY,
    property_id UUID NOT NULL UNIQUE,
    transaction_id UUID NOT NULL,
    -- Embedded PropertyAddress
    street VARCHAR(255),
    city VARCHAR(255),
    province VARCHAR(255),
    postal_code VARCHAR(20),
    -- Pricing
    asking_price DECIMAL(15,2),
    offer_amount DECIMAL(15,2),
    centris_number VARCHAR(50),
    -- Status tracking
    offer_status VARCHAR(50) NOT NULL DEFAULT 'OFFER_TO_BE_MADE',
    -- Broker notes (not visible to clients)
    notes TEXT,
    -- Timestamps
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    
    CONSTRAINT fk_properties_transaction 
        FOREIGN KEY (transaction_id) 
        REFERENCES transactions(transaction_id)
        ON DELETE CASCADE,
    CONSTRAINT chk_offer_status 
        CHECK (offer_status IN ('OFFER_TO_BE_MADE', 'OFFER_MADE', 'COUNTERED', 'ACCEPTED', 'DECLINED'))
);

CREATE INDEX IF NOT EXISTS idx_properties_transaction_id ON properties(transaction_id);
CREATE INDEX IF NOT EXISTS idx_properties_property_id ON properties(property_id);
CREATE INDEX IF NOT EXISTS idx_properties_offer_status ON properties(offer_status);
CREATE INDEX IF NOT EXISTS idx_properties_centris_number ON properties(centris_number);

-- =============================================================================
-- OFFERS (for seller transactions - tracking offers from potential buyers)
-- =============================================================================
CREATE TABLE IF NOT EXISTS offers (
    id BIGSERIAL PRIMARY KEY,
    offer_id UUID NOT NULL UNIQUE,
    transaction_id UUID NOT NULL,
    buyer_name VARCHAR(255) NOT NULL,
    offer_amount DECIMAL(15,2),
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    expiry_date DATE,
    notes TEXT,
    -- Client decision fields for sell-side client review workflow
    client_decision VARCHAR(50),
    client_decision_at TIMESTAMP,
    client_decision_notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    
    CONSTRAINT fk_offers_transaction 
        FOREIGN KEY (transaction_id) 
        REFERENCES transactions(transaction_id)
        ON DELETE CASCADE,
    CONSTRAINT chk_sell_offer_status 
        CHECK (status IN ('PENDING', 'UNDER_REVIEW', 'COUNTERED', 'ACCEPTED', 'DECLINED')),
    CONSTRAINT chk_client_decision 
        CHECK (client_decision IS NULL OR client_decision IN ('ACCEPT', 'DECLINE', 'COUNTER'))
);

CREATE INDEX IF NOT EXISTS idx_offers_transaction_id ON offers(transaction_id);
CREATE INDEX IF NOT EXISTS idx_offers_offer_id ON offers(offer_id);
CREATE INDEX IF NOT EXISTS idx_offers_status ON offers(status);
CREATE INDEX IF NOT EXISTS idx_offers_expiry_date ON offers(expiry_date);
CREATE INDEX IF NOT EXISTS idx_offers_client_decision ON offers(client_decision);

-- =============================================================================
-- PROPERTY OFFERS (for buyer transactions - tracking offers made on properties)
-- =============================================================================
CREATE TABLE IF NOT EXISTS property_offers (
    id BIGSERIAL PRIMARY KEY,
    property_offer_id UUID NOT NULL UNIQUE,
    property_id UUID NOT NULL,
    offer_round INTEGER NOT NULL DEFAULT 1,
    offer_amount DECIMAL(15,2) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'OFFER_MADE',
    counterparty_response VARCHAR(50),
    expiry_date DATE,
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    
    CONSTRAINT fk_property_offers_property 
        FOREIGN KEY (property_id) 
        REFERENCES properties(property_id)
        ON DELETE CASCADE,
    CONSTRAINT chk_property_offer_status 
        CHECK (status IN ('OFFER_MADE', 'COUNTERED', 'ACCEPTED', 'DECLINED', 'WITHDRAWN', 'EXPIRED')),
    CONSTRAINT chk_counterparty_response 
        CHECK (counterparty_response IS NULL OR counterparty_response IN ('PENDING', 'ACCEPTED', 'COUNTERED', 'DECLINED'))
);

CREATE INDEX IF NOT EXISTS idx_property_offers_property_id ON property_offers(property_id);
CREATE INDEX IF NOT EXISTS idx_property_offers_property_offer_id ON property_offers(property_offer_id);
CREATE INDEX IF NOT EXISTS idx_property_offers_status ON property_offers(status);

-- =============================================================================
-- OFFER DOCUMENTS (for both sell-side offers and buy-side property_offers)
-- =============================================================================
CREATE TABLE IF NOT EXISTS offer_documents (
    id BIGSERIAL PRIMARY KEY,
    document_id UUID NOT NULL UNIQUE,
    offer_id UUID,
    property_offer_id UUID,
    s3_key VARCHAR(1000) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    mime_type VARCHAR(100),
    size_bytes BIGINT,
    uploaded_by UUID NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    
    CONSTRAINT fk_offer_documents_offer 
        FOREIGN KEY (offer_id) 
        REFERENCES offers(offer_id)
        ON DELETE CASCADE,
    CONSTRAINT fk_offer_documents_property_offer 
        FOREIGN KEY (property_offer_id) 
        REFERENCES property_offers(property_offer_id)
        ON DELETE CASCADE,
    CONSTRAINT chk_offer_document_parent 
        CHECK (
            (offer_id IS NOT NULL AND property_offer_id IS NULL) OR 
            (offer_id IS NULL AND property_offer_id IS NOT NULL)
        )
);

CREATE INDEX IF NOT EXISTS idx_offer_documents_offer_id ON offer_documents(offer_id);
CREATE INDEX IF NOT EXISTS idx_offer_documents_property_offer_id ON offer_documents(property_offer_id);
CREATE INDEX IF NOT EXISTS idx_offer_documents_document_id ON offer_documents(document_id);

-- =============================================================================
-- OFFER REVISIONS (for tracking sell-side offer negotiation history)
-- =============================================================================
CREATE TABLE IF NOT EXISTS offer_revisions (
    id BIGSERIAL PRIMARY KEY,
    revision_id UUID NOT NULL UNIQUE,
    offer_id UUID NOT NULL,
    revision_number INTEGER NOT NULL,
    previous_amount DECIMAL(15,2),
    new_amount DECIMAL(15,2),
    previous_status VARCHAR(50),
    new_status VARCHAR(50),
    changed_by UUID NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    
    CONSTRAINT fk_offer_revisions_offer 
        FOREIGN KEY (offer_id) 
        REFERENCES offers(offer_id)
        ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_offer_revisions_offer_id ON offer_revisions(offer_id);
CREATE INDEX IF NOT EXISTS idx_offer_revisions_revision_id ON offer_revisions(revision_id);

-- =============================================================================
-- CONDITIONS (for all transactions - tracking conditional clauses)
-- =============================================================================
CREATE TABLE IF NOT EXISTS conditions (
    id BIGSERIAL PRIMARY KEY,
    condition_id UUID NOT NULL UNIQUE,
    transaction_id UUID NOT NULL,
    type VARCHAR(50) NOT NULL,
    custom_title VARCHAR(255),
    description TEXT NOT NULL,
    deadline_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    notes TEXT,
    satisfied_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    
    CONSTRAINT fk_conditions_transaction 
        FOREIGN KEY (transaction_id) 
        REFERENCES transactions(transaction_id)
        ON DELETE CASCADE,
    CONSTRAINT chk_condition_type 
        CHECK (type IN ('FINANCING', 'INSPECTION', 'SALE_OF_PROPERTY', 'OTHER')),
    CONSTRAINT chk_condition_status
        CHECK (status IN ('PENDING', 'SATISFIED', 'FAILED'))
);

CREATE INDEX IF NOT EXISTS idx_conditions_transaction_id ON conditions(transaction_id);
CREATE INDEX IF NOT EXISTS idx_conditions_condition_id ON conditions(condition_id);
CREATE INDEX IF NOT EXISTS idx_conditions_deadline_date ON conditions(deadline_date);
CREATE INDEX IF NOT EXISTS idx_conditions_status ON conditions(status);

-- =============================================================================
-- DOCUMENT CONDITIONS (links conditions to offers/property offers/document requests)
-- =============================================================================
CREATE TABLE IF NOT EXISTS document_conditions (
    id BIGSERIAL PRIMARY KEY,
    condition_id UUID NOT NULL,
    offer_id UUID,
    property_offer_id UUID,
    document_request_id UUID,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    
    CONSTRAINT fk_document_conditions_condition 
        FOREIGN KEY (condition_id) 
        REFERENCES conditions(condition_id)
        ON DELETE CASCADE,
    CONSTRAINT fk_document_conditions_offer 
        FOREIGN KEY (offer_id) 
        REFERENCES offers(offer_id)
        ON DELETE CASCADE,
    CONSTRAINT fk_document_conditions_property_offer 
        FOREIGN KEY (property_offer_id) 
        REFERENCES property_offers(property_offer_id)
        ON DELETE CASCADE,
    CONSTRAINT fk_document_conditions_request 
        FOREIGN KEY (document_request_id) 
        REFERENCES document_requests(request_id)
        ON DELETE CASCADE,
    -- Exactly one of offer_id, property_offer_id, document_request_id must be set
    CONSTRAINT chk_document_conditions_one_source
        CHECK (
            (offer_id IS NOT NULL AND property_offer_id IS NULL AND document_request_id IS NULL) OR
            (offer_id IS NULL AND property_offer_id IS NOT NULL AND document_request_id IS NULL) OR
            (offer_id IS NULL AND property_offer_id IS NULL AND document_request_id IS NOT NULL)
        ),
    -- Prevent duplicate links
    CONSTRAINT uq_document_conditions_unique_link
        UNIQUE (condition_id, offer_id, property_offer_id, document_request_id)
);

CREATE INDEX IF NOT EXISTS idx_document_conditions_condition_id ON document_conditions(condition_id);
CREATE INDEX IF NOT EXISTS idx_document_conditions_offer_id ON document_conditions(offer_id);
CREATE INDEX IF NOT EXISTS idx_document_conditions_property_offer_id ON document_conditions(property_offer_id);
CREATE INDEX IF NOT EXISTS idx_document_conditions_document_request_id ON document_conditions(document_request_id);

-- =============================================================================
-- SYSTEM ALERTS
-- =============================================================================
CREATE TABLE IF NOT EXISTS system_alert (
    id BIGSERIAL PRIMARY KEY,
    message VARCHAR(255) NOT NULL,
    severity VARCHAR(50) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
