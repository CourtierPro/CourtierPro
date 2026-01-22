-- Add user_id column to transaction_participants
ALTER TABLE transaction_participants ADD COLUMN user_id UUID;

-- Add foreign key constraint to user_accounts
ALTER TABLE transaction_participants 
ADD CONSTRAINT fk_transaction_participants_user_id 
FOREIGN KEY (user_id) REFERENCES user_accounts(id);

-- Backfill user_id based on matching email
UPDATE transaction_participants tp
SET user_id = ua.id
FROM user_accounts ua
WHERE LOWER(tp.email) = LOWER(ua.email);

-- Index for performance
CREATE INDEX idx_transaction_participants_user_id ON transaction_participants(user_id);
