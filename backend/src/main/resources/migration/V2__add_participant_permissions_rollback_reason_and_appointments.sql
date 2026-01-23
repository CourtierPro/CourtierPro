CREATE TABLE IF NOT EXISTS participant_permissions (
    participant_id UUID NOT NULL,
    permission VARCHAR(50) NOT NULL,
    CONSTRAINT fk_participant_permissions_participant FOREIGN KEY (participant_id) REFERENCES transaction_participants(id)
);

CREATE INDEX IF NOT EXISTS idx_participant_permissions_participant_id ON participant_permissions(participant_id);

-- Add reason column to timeline_entries
ALTER TABLE timeline_entries ADD COLUMN reason TEXT;

-- Update transaction_participants
ALTER TABLE transaction_participants ADD COLUMN user_id UUID;
ALTER TABLE transaction_participants ADD COLUMN is_system BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE transaction_participants ADD CONSTRAINT fk_transaction_participants_user_id FOREIGN KEY (user_id) REFERENCES user_accounts(id);
CREATE INDEX IF NOT EXISTS idx_transaction_participants_user_id ON transaction_participants(user_id);

-- Add status column to properties
ALTER TABLE properties ADD COLUMN status VARCHAR(50) NOT NULL DEFAULT 'SUGGESTED';

-- Add appointments table
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

-- Indexes for appointments
CREATE INDEX IF NOT EXISTS idx_appointments_broker_id ON appointments(broker_id);
CREATE INDEX IF NOT EXISTS idx_appointments_client_id ON appointments(client_id);
CREATE INDEX IF NOT EXISTS idx_appointments_transaction_id ON appointments(transaction_id);
CREATE INDEX IF NOT EXISTS idx_appointments_status ON appointments(status);
CREATE INDEX IF NOT EXISTS idx_appointments_from_date_time ON appointments(from_date_time);
CREATE INDEX IF NOT EXISTS idx_appointments_deleted_at ON appointments(deleted_at);
CREATE INDEX IF NOT EXISTS idx_appointments_broker_date ON appointments(broker_id, from_date_time);
CREATE INDEX IF NOT EXISTS idx_appointments_client_date ON appointments(client_id, from_date_time);
