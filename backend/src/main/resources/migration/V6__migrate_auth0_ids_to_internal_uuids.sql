-- ============================================================================
-- V6__migrate_auth0_ids_to_internal_uuids.sql
-- 
-- Purpose: Migrate transaction clientId/brokerId fields from Auth0 IDs to 
--          internal UUIDs as part of the dual-ID architecture implementation.
--
-- This migration updates existing transaction records where clientId/brokerId
-- contain Auth0 IDs (format: "auth0|...") to use internal UUIDs from 
-- user_accounts table.
-- ============================================================================

-- Update clientId from Auth0 ID to internal UUID
UPDATE transactions t
SET client_id = ua.id::text
FROM user_accounts ua
WHERE t.client_id = ua.auth0user_id
  AND t.client_id IS NOT NULL
  AND t.client_id LIKE 'auth0|%';

-- Update brokerId from Auth0 ID to internal UUID  
UPDATE transactions t
SET broker_id = ua.id::text
FROM user_accounts ua
WHERE t.broker_id = ua.auth0user_id
  AND t.broker_id IS NOT NULL
  AND t.broker_id LIKE 'auth0|%';

-- Log migration results
DO $$
DECLARE
    client_updated INTEGER;
    broker_updated INTEGER;
BEGIN
    -- Count remaining Auth0 IDs that couldn't be migrated (orphaned)
    SELECT COUNT(*) INTO client_updated 
    FROM transactions 
    WHERE client_id LIKE 'auth0|%';
    
    SELECT COUNT(*) INTO broker_updated 
    FROM transactions 
    WHERE broker_id LIKE 'auth0|%';
    
    IF client_updated > 0 OR broker_updated > 0 THEN
        RAISE NOTICE 'WARNING: % transactions still have Auth0 client_id, % have Auth0 broker_id', 
                     client_updated, broker_updated;
    ELSE
        RAISE NOTICE 'SUCCESS: All transaction IDs migrated to internal UUIDs';
    END IF;
END $$;
