CREATE TABLE login_audit_events (
                                    id UUID PRIMARY KEY,
                                    user_id VARCHAR(255) NOT NULL,
                                    email VARCHAR(255) NOT NULL,
                                    role VARCHAR(50) NOT NULL,
                                    timestamp TIMESTAMP NOT NULL,
                                    ip_address VARCHAR(45),
                                    user_agent VARCHAR(500)
);

CREATE INDEX idx_login_audit_user_id ON login_audit_events(user_id);
CREATE INDEX idx_login_audit_role ON login_audit_events(role);
CREATE INDEX idx_login_audit_timestamp ON login_audit_events(timestamp);