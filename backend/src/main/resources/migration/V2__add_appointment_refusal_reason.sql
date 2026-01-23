ALTER TABLE appointments ADD COLUMN refusal_reason TEXT;

CREATE TABLE IF NOT EXISTS appointment_audits (
    id UUID PRIMARY KEY,
    appointment_id UUID NOT NULL,
    action VARCHAR(255) NOT NULL,
    performed_by UUID NOT NULL,
    performed_at TIMESTAMP NOT NULL,
    details TEXT,
    CONSTRAINT fk_audit_appointment FOREIGN KEY (appointment_id) REFERENCES appointments(appointment_id) ON DELETE CASCADE
);

CREATE INDEX idx_appointment_audits_appointment_id ON appointment_audits(appointment_id);
