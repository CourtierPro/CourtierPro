-- Update PropertyStatus values
UPDATE transaction_properties
SET status = 'INTERESTED'
WHERE status = 'ACCEPTED';

UPDATE transaction_properties
SET status = 'NOT_INTERESTED'
WHERE status = 'REJECTED';
