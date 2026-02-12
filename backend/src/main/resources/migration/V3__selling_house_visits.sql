-- =============================================================================
-- V3: Selling House Visits (CP-28)
-- Add visitors table for sell-side transactions and visitor tracking on appointments
-- =============================================================================

-- Visitors table for sell-side transactions
CREATE TABLE IF NOT EXISTS visitors (
    visitor_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    transaction_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255),
    phone_number VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_visitors_transaction FOREIGN KEY (transaction_id)
        REFERENCES transactions(transaction_id) ON DELETE CASCADE
);

CREATE INDEX idx_visitors_transaction_id ON visitors(transaction_id);

-- Add visitor tracking fields to appointments
ALTER TABLE appointments ADD COLUMN number_of_visitors INTEGER;
ALTER TABLE appointments ADD COLUMN visitor_id UUID;

ALTER TABLE appointments ADD CONSTRAINT fk_appointments_visitor
    FOREIGN KEY (visitor_id) REFERENCES visitors(visitor_id) ON DELETE SET NULL;

CREATE INDEX idx_appointments_visitor_id ON appointments(visitor_id);
