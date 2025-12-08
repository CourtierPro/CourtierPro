CREATE TABLE logout_audit_events (
    id UUID PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    reason VARCHAR(50) NOT NULL,
    timestamp TIMESTAMP NOT NULL,
    ip_address VARCHAR(45),
    user_agent VARCHAR(500),
    CONSTRAINT chk_logout_reason CHECK (reason IN ('MANUAL', 'SESSION_TIMEOUT', 'FORCED'))
);

CREATE INDEX idx_logout_audit_user_id ON logout_audit_events(user_id);
CREATE INDEX idx_logout_audit_reason ON logout_audit_events(reason);
CREATE INDEX idx_logout_audit_timestamp ON logout_audit_events(timestamp);
