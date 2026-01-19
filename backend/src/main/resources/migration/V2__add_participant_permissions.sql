CREATE TABLE IF NOT EXISTS participant_permissions (
    participant_id UUID NOT NULL,
    permission VARCHAR(50) NOT NULL,
    CONSTRAINT fk_participant_permissions_participant FOREIGN KEY (participant_id) REFERENCES transaction_participants(id)
);

CREATE INDEX idx_participant_permissions_participant_id ON participant_permissions(participant_id);
