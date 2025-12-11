-- ============================================================================
-- V10__fix_all_id_types.sql
--
-- Fixes the remaining schema mismatch where business IDs were still VARCHAR
-- but the Hibernate entities expect UUID.
--
-- Affected columns:
--   - transactions.transaction_id
--   - document_requests.request_id
--   - document_requests.transaction_id
--   - submitted_documents.document_id
--
-- Prerequisites: V8 truncated data, so conversion using ::uuid should be safe
-- or empty.
-- ============================================================================

-- =============================================================================
-- TRANSACTIONS TABLE
-- =============================================================================

ALTER TABLE transactions
    ALTER COLUMN transaction_id TYPE UUID USING transaction_id::uuid;

-- =============================================================================
-- DOCUMENT REQUESTS TABLE
-- =============================================================================

ALTER TABLE document_requests
    ALTER COLUMN request_id TYPE UUID USING request_id::uuid;

ALTER TABLE document_requests
    ALTER COLUMN transaction_id TYPE UUID USING transaction_id::uuid;

-- =============================================================================
-- SUBMITTED DOCUMENTS TABLE
-- =============================================================================

ALTER TABLE submitted_documents
    ALTER COLUMN document_id TYPE UUID USING document_id::uuid;
