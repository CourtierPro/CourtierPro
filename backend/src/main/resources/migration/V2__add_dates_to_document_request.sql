ALTER TABLE document_requests ADD COLUMN due_date TIMESTAMP;
ALTER TABLE document_requests ADD COLUMN created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

-- BACKFILL: Prevent NullPointers on existing data
UPDATE document_requests 
SET created_at = CURRENT_TIMESTAMP 
WHERE created_at IS NULL;

-- Set a default due date (7 days from now) for existing pending docs to avoid immediate overdue status
UPDATE document_requests
SET due_date = CURRENT_TIMESTAMP + INTERVAL '7 days'
WHERE due_date IS NULL AND status = 'REQUESTED';
