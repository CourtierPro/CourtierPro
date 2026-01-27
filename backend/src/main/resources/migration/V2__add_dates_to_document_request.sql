ALTER TABLE document_request ADD COLUMN due_date TIMESTAMP;
ALTER TABLE document_request ADD COLUMN created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

-- BACKFILL: Prevent NullPointers on existing data
UPDATE document_request 
SET created_at = CURRENT_TIMESTAMP 
WHERE created_at IS NULL;

-- Set a default due date (7 days from now) for existing pending docs to avoid immediate overdue status
UPDATE document_request
SET due_date = DATE_ADD(CURRENT_TIMESTAMP, INTERVAL 7 DAY)
WHERE due_date IS NULL AND status = 'REQUESTED';
