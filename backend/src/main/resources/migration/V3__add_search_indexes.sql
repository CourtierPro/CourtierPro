-- ============================================================================
-- V2__add_search_indexes.sql
--
-- Adds trigram (pg_trgm) indexes to support efficient LIKE '%...%' queries
-- for search functionality across users, transactions, and documents.
-- ============================================================================

-- Enable the pg_trgm extension for trigram-based indexing
-- This allows GIN indexes to accelerate LIKE '%pattern%' queries
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- =============================================================================
-- USER ACCOUNTS SEARCH INDEXES
-- Supports: searchClientsOfBroker queries on first_name, last_name, email
-- =============================================================================
CREATE INDEX IF NOT EXISTS idx_user_accounts_first_name_trgm 
    ON user_accounts USING GIN (first_name gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_user_accounts_last_name_trgm 
    ON user_accounts USING GIN (last_name gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_user_accounts_email_trgm 
    ON user_accounts USING GIN (email gin_trgm_ops);

-- =============================================================================
-- TRANSACTIONS SEARCH INDEXES
-- Supports: searchTransactions queries on street, city addresses
-- =============================================================================
CREATE INDEX IF NOT EXISTS idx_transactions_street_trgm 
    ON transactions USING GIN (street gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_transactions_city_trgm 
    ON transactions USING GIN (city gin_trgm_ops);

-- =============================================================================
-- DOCUMENT REQUESTS SEARCH INDEXES
-- Supports: searchDocuments queries on custom_title, broker_notes
-- =============================================================================
CREATE INDEX IF NOT EXISTS idx_document_requests_custom_title_trgm 
    ON document_requests USING GIN (custom_title gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_document_requests_broker_notes_trgm 
    ON document_requests USING GIN (broker_notes gin_trgm_ops);
