-- ============================================================================
-- V8__truncate_transaction_data.sql
--
-- Clean start: Truncate ALL application tables.
-- user_accounts is preserved (synced from Auth0)
-- ============================================================================

TRUNCATE TABLE 
    -- Core application data
    submitted_documents,
    document_requests,
    timeline_entries,
    transactions,
    -- Audit tables
    login_audit_events,
    logout_audit_events,
    password_reset_events,
    organization_settings_audit
CASCADE;
