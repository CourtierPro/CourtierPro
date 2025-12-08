CREATE TABLE password_reset_events (
                                       id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                       user_id VARCHAR(255) NOT NULL,
                                       email VARCHAR(255) NOT NULL,
                                       event_type VARCHAR(50) NOT NULL CHECK (event_type IN ('REQUESTED', 'COMPLETED')),
                                       timestamp TIMESTAMP NOT NULL,
                                       ip_address VARCHAR(100),
                                       user_agent VARCHAR(500)
);

CREATE INDEX idx_password_reset_events_user_id ON password_reset_events(user_id);
CREATE INDEX idx_password_reset_events_email ON password_reset_events(email);
CREATE INDEX idx_password_reset_events_timestamp ON password_reset_events(timestamp DESC);

COMMENT ON TABLE password_reset_events IS 'Audit log for password reset requests and completions';
COMMENT ON COLUMN password_reset_events.event_type IS 'REQUESTED: User requested reset, COMPLETED: User successfully reset password';