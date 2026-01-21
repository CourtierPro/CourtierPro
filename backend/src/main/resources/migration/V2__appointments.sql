-- ============================================================================
-- V2__appointments.sql
--
-- Add appointments table for managing scheduled meetings between brokers and clients.
-- ============================================================================

CREATE TABLE IF NOT EXISTS appointments (
    id BIGSERIAL PRIMARY KEY,
    appointment_id UUID NOT NULL UNIQUE,
    title VARCHAR(255) NOT NULL,
    transaction_id UUID,
    broker_id UUID NOT NULL,
    client_id UUID NOT NULL,
    from_date_time TIMESTAMP NOT NULL,
    to_date_time TIMESTAMP NOT NULL,
    status VARCHAR(50) NOT NULL,
    initiated_by VARCHAR(50) NOT NULL,
    responded_by UUID,
    responded_at TIMESTAMP,
    location VARCHAR(500),
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    version BIGINT DEFAULT 0,
    deleted_at TIMESTAMP,
    deleted_by UUID,
    CONSTRAINT fk_appointments_broker FOREIGN KEY (broker_id) REFERENCES user_accounts(id) ON DELETE CASCADE,
    CONSTRAINT fk_appointments_client FOREIGN KEY (client_id) REFERENCES user_accounts(id) ON DELETE CASCADE,
    CONSTRAINT fk_appointments_transaction FOREIGN KEY (transaction_id) REFERENCES transactions(transaction_id) ON DELETE SET NULL
);

-- Indexes for common queries
CREATE INDEX idx_appointments_broker_id ON appointments(broker_id);
CREATE INDEX idx_appointments_client_id ON appointments(client_id);
CREATE INDEX idx_appointments_transaction_id ON appointments(transaction_id);
CREATE INDEX idx_appointments_status ON appointments(status);
CREATE INDEX idx_appointments_from_date_time ON appointments(from_date_time);
CREATE INDEX idx_appointments_deleted_at ON appointments(deleted_at);

-- Composite index for common broker date range queries
CREATE INDEX idx_appointments_broker_date ON appointments(broker_id, from_date_time);

-- Composite index for common client date range queries  
CREATE INDEX idx_appointments_client_date ON appointments(client_id, from_date_time);
