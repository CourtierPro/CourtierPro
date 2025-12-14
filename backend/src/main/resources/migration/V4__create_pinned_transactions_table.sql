-- Create pinned_transactions table for broker transaction pinning
CREATE TABLE pinned_transactions (
    id BIGSERIAL PRIMARY KEY,
    broker_id UUID NOT NULL,
    transaction_id UUID NOT NULL,
    pinned_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_pinned_broker_transaction UNIQUE (broker_id, transaction_id),
    CONSTRAINT fk_pinned_broker FOREIGN KEY (broker_id) REFERENCES user_accounts(id) ON DELETE CASCADE,
    CONSTRAINT fk_pinned_transaction FOREIGN KEY (transaction_id) REFERENCES transactions(transaction_id) ON DELETE CASCADE
);

-- Index for faster lookups by broker
CREATE INDEX idx_pinned_transactions_broker_id ON pinned_transactions(broker_id);
