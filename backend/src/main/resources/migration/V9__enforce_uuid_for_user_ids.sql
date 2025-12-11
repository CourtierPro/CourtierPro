-- ============================================================================
-- V9__enforce_uuid_for_user_ids.sql
--
-- Enforces the dual ID structure by changing client_id and broker_id columns
-- from VARCHAR to UUID type. This ensures only internal UUIDs (not Auth0 IDs)
-- can be stored in business-data tables.
--
-- Prerequisites: V8 truncated all transaction data, so no data migration needed.
-- ============================================================================

-- =============================================================================
-- TRANSACTIONS TABLE
-- =============================================================================

-- Change client_id from VARCHAR to UUID
ALTER TABLE transactions 
    ALTER COLUMN client_id TYPE UUID USING client_id::uuid;

-- Change broker_id from VARCHAR to UUID  
ALTER TABLE transactions 
    ALTER COLUMN broker_id TYPE UUID USING broker_id::uuid;

-- =============================================================================
-- TIMELINE ENTRIES TABLE
-- =============================================================================

-- Change added_by_broker_id from VARCHAR to UUID
ALTER TABLE timeline_entries 
    ALTER COLUMN added_by_broker_id TYPE UUID USING added_by_broker_id::uuid;

-- =============================================================================
-- DOCUMENT REQUESTS TABLE
-- =============================================================================

-- Change client_id from VARCHAR to UUID
ALTER TABLE document_requests 
    ALTER COLUMN client_id TYPE UUID USING client_id::uuid;

-- =============================================================================
-- SUBMITTED DOCUMENTS TABLE
-- =============================================================================

-- Change uploader_id from VARCHAR to UUID
ALTER TABLE submitted_documents 
    ALTER COLUMN uploader_id TYPE UUID USING uploader_id::uuid;

-- =============================================================================
-- Add foreign key constraints (optional but recommended)
-- This ensures referential integrity with user_accounts table
-- =============================================================================

-- Note: Adding FK constraints is commented out for now as they may fail if
-- there are NULLs or if all users aren't yet in user_accounts. Uncomment
-- after confirming data integrity.

-- ALTER TABLE transactions 
--     ADD CONSTRAINT fk_transactions_client 
--     FOREIGN KEY (client_id) REFERENCES user_accounts(id);

-- ALTER TABLE transactions 
--     ADD CONSTRAINT fk_transactions_broker 
--     FOREIGN KEY (broker_id) REFERENCES user_accounts(id);

-- ALTER TABLE document_requests 
--     ADD CONSTRAINT fk_document_requests_client 
--     FOREIGN KEY (client_id) REFERENCES user_accounts(id);

-- ALTER TABLE submitted_documents 
--     ADD CONSTRAINT fk_submitted_documents_uploader 
--     FOREIGN KEY (uploader_id) REFERENCES user_accounts(id);
