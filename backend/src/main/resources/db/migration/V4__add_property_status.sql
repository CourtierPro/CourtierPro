-- Add status column to properties table
ALTER TABLE properties ADD COLUMN status VARCHAR(50);

-- Migrate existing data: All existing properties are treated as ACCEPTED
UPDATE properties SET status = 'ACCEPTED' WHERE status IS NULL;

-- Make status column NOT NULL after population
ALTER TABLE properties ALTER COLUMN status SET NOT NULL;
